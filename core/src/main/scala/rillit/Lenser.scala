package rillit

import language.experimental.macros
import language.dynamics
import scala.reflect.macros._

class OhMy extends Dynamic {
  def selectDynamic(methodName: String)() {
    println(s"selectdnamic for $methodName")
  }
  def applyDynamic(methodName: String)() {
    println(s"""| methodName: $methodName""".stripMargin)
  }
}

class Lenser[T] extends Dynamic {
  def selectDynamic(propName: String)  = macro Lenser.selectDynamic[T]
}

object Lenser {
  def create[T]: Lenser[T] = new Lenser[T]

  def selectDynamic[T: c.WeakTypeTag](c: Context)(propName: c.Expr[String]) =
    applyDynamic[T](c)(propName)()

  def applyDynamic[T: c.WeakTypeTag](c: Context)(propName: c.Expr[String])() = {
    import c.universe._

    def abort(reason: String) = c.abort(c.enclosingPosition, reason)
    def mkParam(name: String, tpe: Type) =
      ValDef(Modifiers(Flag.PARAM), newTermName(name), TypeTree(tpe), EmptyTree)

    import treeBuild._
    val t = (c.prefix.tree, propName.tree) match {
      case (Apply(Select(New(tree: TypeTree), nme.CONSTRUCTOR),_), Literal(Constant(name: String))) =>
        val lensTpe = tree.original match {
          case AppliedTypeTree(_, List(tpe)) => tpe
          case _                             => abort("No inner type found")
        }
        val getter = Function(
          List(mkParam("a$", lensTpe.tpe)),
          Select(Ident(newTermName("a$")), newTermName(name))
        )
        getter

      case x =>
        println(x._1.tpe)
        c.abort(c.enclosingPosition, "unexpected c.prefix tree: " + x)
    }
    c.Expr[Any](c.resetAllAttrs(t))
  }
}
