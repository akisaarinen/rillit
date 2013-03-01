package rillit

import scala.language.experimental.macros
import scala.reflect.macros.Context
import shapeless.Lens

object Lenser {
  trait StackedLens[A, B, C] extends Lens[A, C] {
    protected def parent: Lens[A, B]
    protected def local: Lens[B, C]
    protected def lens = local compose parent
    def get(x: A) = lens.get(x)
    def set(x: A)(v: C) = lens.set(x)(v)
  }

  trait TopLens[A, B] extends StackedLens[A, A, B] {
    protected def parent = ???
    override protected def lens = local
  }

  /** Our point of contact with the outside world. */
  def apply[A]: Any = macro apply_impl[A]

  def apply_impl[A: c.WeakTypeTag](c: Context) = {
    import c.universe._

    // Each node in the tree has a class and a set of definitions it expects.
    trait Node {
      def classDef: ClassDef
      def defs: List[Tree]
    }

    case class RootNode(classDef: ClassDef) extends Node {
      def defs = classDef :: Nil
    }

    case class NonRootNode(
      classDef: ClassDef,
      lensDef: ClassDef,
      acc: MethodSymbol
    ) extends Node {
      def defs = classDef :: lensDef :: Nil
    }

    def instance(node: Node) = Apply(
      Select(New(Ident(node.classDef.name)), nme.CONSTRUCTOR),
      Nil
    )

    // A convenience method for creating a list of a type's accessor methods.
    def accessors(t: Type) = t.declarations.collect {
      case acc: MethodSymbol if acc.isCaseAccessor => acc
    }.toList

    def childMethods(children: List[Node]) = children.collect {
      case node @ NonRootNode(classDef, _, acc) => DefDef(
        Modifiers(),
        newTermName(acc.name.decoded),
        Nil,
        Nil,
        TypeTree(),
        instance(node)
      )
    }

    val lenser = Select(Ident("rillit"), "Lenser")
    val source = c.weakTypeOf[A] 

    val tree = util.Tree.unfold(accessors(source))(
      acc => accessors(acc.returnType)
    ).histo[Node] {
      case (Some(acc), depth, children) =>
        val parent = acc.owner.asClass.toType
        val target = acc.returnType
        val anon = newTypeName(c.fresh)
        val self = newTermName(c.fresh)
        val lens = createLens(c)(acc, parent, target)

        val classDef = ClassDef(
          Modifiers(),
          anon,
          Nil,
          Template(
            TypeTree(
              if (depth == 1) appliedType(
                typeOf[TopLens[_, _]].typeConstructor,
                List(source, target)
              ) else appliedType(
                typeOf[StackedLens[_, _, _]].typeConstructor,
                List(source, parent, target)
              )
            ) :: Nil,
            emptyValDef,
            constructor(c) ::
            ValDef(
              Modifiers(Flag.PROTECTED),
              newTermName("local"),
              TypeTree(),
              Apply(Select(New(Ident(lens.name)), nme.CONSTRUCTOR), Nil)
            ) :: childMethods(children.map(_.value))
          )
        )

        NonRootNode(classDef, lens, acc)

     case (None, _, children) =>
        RootNode(ClassDef(
          Modifiers(),
          newTypeName(c.fresh),
          Nil,
          Template(
            Ident(newTypeName("AnyRef")) :: Nil,
            emptyValDef,
            constructor(c) :: childMethods(children.map(_.value))
          )
        ))
    }

    def assignParents(parentIf: Option[ClassDef])(
      tree: util.Tree[Node]
    ): util.Tree[Node] = {
      val node = parentIf.fold(tree.value) { parent =>
        val NonRootNode(
          ClassDef(mods, className, tps, Template(parents, _, body)),
          other,
          acc
        ) = tree.value

        NonRootNode(
          ClassDef(
            mods,
            className,
            tps,
            Template(
              parents,
              emptyValDef,
              ValDef(
                Modifiers(),
                newTermName("parent"),
                TypeTree(),
                Apply(Select(New(Ident(parent.name)), nme.CONSTRUCTOR), Nil)
              ) :: body
            )
          ),
          other,
          acc
        )
      }

      util.Tree(
        node,
        tree.depth,
        tree.children.map(assignParents(Some(node.classDef)))
      )
    }

    val withParents = util.Tree(
      tree.value,
      tree.depth,
      tree.children.map(assignParents(None))
    )

    c.Expr(
      Block(
        withParents.flatten.flatMap(_.defs),
        instance(withParents.value)
      )
    )
  }

  def createLens(c: Context)(
    acc: c.universe.MethodSymbol,
    source: c.universe.Type,
    target: c.universe.Type
  ) = {
    import c.universe._

      val target = acc.returnType
      val anon = newTypeName(c.fresh)
      val lens = newTermName(c.fresh)
      val gx = newTermName(c.fresh)
      val sx = newTermName(c.fresh)
      val sv = newTermName(c.fresh)

      val getter = DefDef(
        Modifiers(),
        newTermName("get"),
        Nil,
        List(
          ValDef(Modifiers(Flag.PARAM), gx, TypeTree(source), EmptyTree) :: Nil
        ),
        TypeTree(),
        Select(Ident(gx), acc.name)
      )

      val setter = DefDef(
        Modifiers(),
        newTermName("set"),
        Nil,
        List(
          List( ValDef(Modifiers(Flag.PARAM), sx, TypeTree(source), EmptyTree) ),
          List( ValDef(Modifiers(Flag.PARAM), sv, TypeTree(target), EmptyTree) )
        ),
        TypeTree(),
        Apply(
          Select(Ident(sx), "copy"),
          AssignOrNamedArg(Ident(acc.name), Ident(sv)) :: Nil
        )
      )

      ClassDef(
        Modifiers(Flag.FINAL),
        anon,
        Nil,
        Template(
          TypeTree(
            appliedType(
              typeOf[Lens[_, _]].typeConstructor,
              List(source, target)
            )
          ) :: Nil,
          emptyValDef,
          List(constructor(c), getter, setter)
        )
      )
    }

  /** Convenience method for creating an empty constructor tree. */
  def constructor(c: Context) = {
    import c.universe._

    DefDef(
      Modifiers(),
      nme.CONSTRUCTOR,
      Nil,
      Nil :: Nil,
      TypeTree(),
      Block(
        Apply(
          Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), nme.CONSTRUCTOR),
          Nil
        ) :: Nil,
        c.literalUnit.tree
      )
    )
  }
}

