package rillit

import language.experimental.macros
import language.dynamics
import scala.reflect.macros._

trait InitializedLensBuilder[A, B] extends Dynamic {
  def apply(): Lens[A, B]

  def selectDynamic(propName: String) = macro LensBuilder.selectDynamic[A,B]
}

class LensBuilder[A] extends Dynamic {
  def selectDynamic(propName: String) = macro LensBuilder.initializeBuilderImpl[A]
}

object LensBuilder {
  def build[A, B](lens: Lens[A,B]) = new InitializedLensBuilder[A,B] {
    def apply() = lens
  }

  def initializer[A] = new LensBuilder[A]

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
       val lens = Lenser.createLens[T](c)(lensSourceTpe, name)
       createBuilder(c)(lens, lensSourceTpe, lensTargetTpe, name)

     case x =>
       c.abort(c.enclosingPosition, "unexpected c.prefix tree: " + x)
   }
   c.Expr[Any](c.resetAllAttrs(t))
 }


  def selectDynamic[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context { type PrefixType = InitializedLensBuilder[A, B] })(propName: c.Expr[String]) = {
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
        val lens = Lenser.createLens[B](c)(lensSourceTpe, name)
        val combinedLens = Apply(Select(lens, newTermName("compose")), List(parentLens))

        createBuilder(c)(combinedLens, c.weakTypeOf[A], lensTargetTpe, name)

      case x =>
        c.abort(c.enclosingPosition, "unexpected c.prefix tree: " + x)
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
              Ident(c.mirror.staticClass("rillit.InitializedLensBuilder")), List(TypeTree(originalBuilderTpe), TypeTree(lensTargetTpe)))),
            emptyValDef, List(
              constructor,
              applyF
            ))
        )),
      Apply(Select(New(Ident(newTypeName("$anon"))), nme.CONSTRUCTOR), List())
    )
  }
}
