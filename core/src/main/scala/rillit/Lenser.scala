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

trait SimpleLens[A, B] {
  def get(x: A): B
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
        val calledMember = lensTpe.tpe.member(newTermName(name)) orElse {
          abort("value %s is not a member of %s".format(name, lensTpe))
        }
        val memberTpe = calledMember.typeSignatureIn(lensTpe.tpe) match {
          case NullaryMethodType(tpe) => tpe
          case _                      => abort("member %s is not a field".format(name))
        }

        val getF =
          DefDef(
            Modifiers(), newTermName("get"), List(),
            List(List(mkParam("x$", lensTpe.tpe))),
            TypeTree(),
            Select(Ident(newTermName("x$")), newTermName(name))
          )

        Block(
          List(
            ClassDef(Modifiers(Flag.FINAL), newTypeName("$anon"), List(),
              Template(List(
                AppliedTypeTree(
                  Ident(c.mirror.staticClass("rillit.SimpleLens")), List(lensTpe, TypeTree(memberTpe)))),
                emptyValDef, List(
                  DefDef(
                    Modifiers(),
                    nme.CONSTRUCTOR,
                    List(),
                    List(List()),
                    TypeTree(),
                    Block(
                      List(
                        Apply(
                          Select(Super(This(""), ""), nme.CONSTRUCTOR), Nil)),
                      Literal(Constant(())))), getF))
            )),
          Apply(Select(New(Ident(newTypeName("$anon"))), nme.CONSTRUCTOR), List())
        )

      case x =>
        println(x._1.tpe)
        c.abort(c.enclosingPosition, "unexpected c.prefix tree: " + x)
    }
    c.Expr[Any](c.resetAllAttrs(t))
  }
}
