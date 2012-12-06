package rillit

import language.experimental.macros
import language.dynamics
import scala.reflect.macros._

trait InitializedLenser[A, B] extends Dynamic {
  def apply(): Lens[A, B]

  def selectDynamic(propName: String) = macro Lenser.selectDynamic[A,B]
}

case class Lenser[A]() extends Dynamic {
  def selectDynamic(propName: String) = macro Lenser.initializeBuilderImpl[A]
}

object Lenser {
  implicit def LenserToLens[A,B](lenser: InitializedLenser[A,B]) = lenser.apply()

  def build[A, B](lens: Lens[A,B]) = new InitializedLenser[A,B] {
    def apply() = lens
  }

  def initializer[A] = new Lenser[A]

  def initializeBuilderImpl[T: c.WeakTypeTag](c: Context)(propName: c.Expr[String]) = {
   import c.universe._

   def abort(reason: String) = c.abort(c.enclosingPosition, reason)

   val t = (c.prefix.tree, propName.tree) match {
     case (x, Literal(Constant(name: String))) =>
       val lensSourceTpe = c.weakTypeOf[T]
       val calledMember = lensSourceTpe.member(newTermName(name)) orElse {
         abort("value %s is not a member of %s".format(name, lensSourceTpe))
       }
       val lensTargetTpe = calledMember.typeSignatureIn(lensSourceTpe) match {
         case NullaryMethodType(tpe) => tpe
         case _                      => abort("member %s is not a field".format(name))
       }
       val lens = createLens[T](c)(lensSourceTpe, name)
       createBuilder(c)(lens, lensSourceTpe, lensTargetTpe, name)

     case x =>
       abort("unexpected c.prefix tree: %s".format(x))
   }
   c.Expr[Any](c.resetAllAttrs(t))
 }


  def selectDynamic[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context { type PrefixType = InitializedLenser[A, B] })(propName: c.Expr[String]) = {
    import c.universe._

    def abort(reason: String) = c.abort(c.enclosingPosition, reason)

    val t = (c.prefix.tree, propName.tree) match {
      case (x, Literal(Constant(name: String))) =>
        val lensSourceTpe = c.weakTypeOf[B]

        val calledMember = lensSourceTpe.member(newTermName(name)) orElse {
          abort("value %s is not a member of %s".format(name, lensSourceTpe))
        }
        val lensTargetTpe = calledMember.typeSignatureIn(lensSourceTpe) match {
          case NullaryMethodType(tpe) => tpe
          case _                      => abort("member %s is not a field".format(name))
        }

        val parentApply = x.tpe.member(newTermName("apply"))
        val parentLens = Apply(Select(x, parentApply), List())
        val lens = createLens[B](c)(lensSourceTpe, name)
        val combinedLens = Apply(Select(lens, newTermName("compose")), List(parentLens))

        createBuilder(c)(combinedLens, c.weakTypeOf[A], lensTargetTpe, name)

      case x =>
        abort("unexpected c.prefix tree: %s".format(x))
    }
    c.Expr[Any](c.resetAllAttrs(t))
  }

  def createBuilder(c: Context)(lens: c.universe.Tree, originalBuilderTpe: c.universe.Type, lensTargetTpe: c.universe.Type, name: String) = {
    import c.universe._

    val constructor =
      DefDef(
        Modifiers(),
        nme.CONSTRUCTOR,
        List(),
        List(List()),
        TypeTree(),
        Block(
          List(Apply(Select(Super(This(""), ""), nme.CONSTRUCTOR), Nil)),
          Literal(Constant(()))))

    val applyF =
      DefDef(
        Modifiers(), newTermName("apply"), List(),
        List(),
        TypeTree(),
        lens
      )

    Block(
      List(
        ClassDef(Modifiers(Flag.FINAL), newTypeName("$anon"), List(),
          Template(List(
            AppliedTypeTree(
              Ident(c.mirror.staticClass("rillit.InitializedLenser")), List(TypeTree(originalBuilderTpe), TypeTree(lensTargetTpe)))),
            emptyValDef, List(
              constructor,
              applyF
            ))
        )),
      Apply(Select(New(Ident(newTypeName("$anon"))), nme.CONSTRUCTOR), List())
    )
  }


  def createLens[T: c.WeakTypeTag](c: Context)(lensTpe: c.universe.Type, name: String) = {
    import c.universe._

    def abort(reason: String) = c.abort(c.enclosingPosition, reason)
    def mkParam(name: String, tpe: Type) =
      ValDef(Modifiers(Flag.PARAM), newTermName(name), TypeTree(tpe), EmptyTree)

    import treeBuild._

    val calledMember = lensTpe.member(newTermName(name)) orElse {
      abort("value %s is not a member of %s".format(name, lensTpe))
    }
    val memberTpe = calledMember.typeSignatureIn(lensTpe) match {
      case NullaryMethodType(tpe) => tpe
      case _                      => abort("member %s is not a field".format(name))
    }

    val constructor =
      DefDef(
        Modifiers(),
        nme.CONSTRUCTOR,
        List(),
        List(List()),
        TypeTree(),
        Block(
          List(Apply(Select(Super(This(""), ""), nme.CONSTRUCTOR), Nil)),
          Literal(Constant(()))))

    val getF =
      DefDef(
        Modifiers(), newTermName("get"), List(),
        List(List(mkParam("x$", lensTpe))),
        TypeTree(),
        Select(Ident(newTermName("x$")), newTermName(name))
      )

    val setF =
      DefDef(
        Modifiers(), newTermName("set"), List(),
        List(List(mkParam("x$", lensTpe), mkParam("v$", memberTpe))),
        TypeTree(),
        Apply(
          Select(Ident(newTermName("x$")), newTermName("copy")),
          List(AssignOrNamedArg(Ident(newTermName(name)), Ident(newTermName("v$"))))
        )
      )

    Block(
      List(
        ClassDef(Modifiers(Flag.FINAL), newTypeName("$anon"), List(),
          Template(List(
            AppliedTypeTree(
              Ident(c.mirror.staticClass("rillit.Lens")), List(TypeTree(lensTpe), TypeTree(memberTpe)))),
            emptyValDef, List(
              constructor,
              getF,
              setF
            ))
        )),
      Apply(Select(New(Ident(newTypeName("$anon"))), nme.CONSTRUCTOR), List())
    )
  }
}
