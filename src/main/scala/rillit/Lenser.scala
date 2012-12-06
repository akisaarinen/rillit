package rillit

import language.experimental.macros
import language.dynamics
import scala.reflect.macros._

object Lensify {
  def lens[T] = new Lenser[T]
}

class Lenser[T] extends Dynamic {
  def selectDynamic(propName: String)  = macro Lenser.selectDynamic[T]
  def applyDynamic(propName: String)() = macro Lenser.applyDynamic[T]
}

object Lenser {
  def selectDynamic[T: c.WeakTypeTag](c: Context)(propName: c.Expr[String]) =
    applyDynamic[T](c)(propName)()

  def applyDynamic[T: c.WeakTypeTag]
  (c: Context)
  (propName: c.Expr[String])
  ()
  = {
    import c.universe._
    // Why doesn't this work if I try to use scala.Tuple2's symbol?
    def Tuple2Module = Select(Ident(newTermName("scala")), newTermName("Tuple2"))
    def mkParam(name: String, tpe: Type) =
      ValDef(Modifiers(Flag.PARAM), newTermName(name), TypeTree(tpe), EmptyTree)

    import treeBuild._
    //println(showRaw(_this))
    val t = (c.prefix.tree, propName.tree) match {
      case (TypeApply(
      Select(
      _, lensMethodTermName
      ), List(tpe)), Literal(Constant(methodName: String))) =>
        val getterMember = tpe.tpe.member(newTermName(methodName)) orElse {
          c.abort(c.enclosingPosition, "value " + methodName + " is not a member of " + tpe.tpe)
        }
        val memberType = getterMember.typeSignatureIn(tpe.tpe) match {
          case NullaryMethodType(memberType) => memberType
          case _                             => c.abort(c.enclosingPosition, "member %s is not a field".format(methodName))
        }
        val getter = Function(
          List(mkParam("a$", tpe.tpe)),
          Select(Ident(newTermName("a$")), newTermName(methodName))
        )
        val setter = Function(
          List(mkParam("a$", tpe.tpe), mkParam("x$", memberType)),
          Apply(
            Select(Ident(newTermName("a$")), newTermName("copy")),
            List(AssignOrNamedArg(Ident(newTermName(methodName)), Ident(newTermName("x$"))))
          )
        )
        mkMethodCall(Select(Tuple2Module, newTermName("apply")), List(getter, setter))
      case x =>
        c.abort(c.enclosingPosition, "unexpected c.prefix tree: " + x)
    }
    c.Expr[Any](c.resetAllAttrs(t))
  }
}
