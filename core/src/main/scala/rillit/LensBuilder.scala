package rillit

import language.experimental.macros
import language.dynamics
import scala.reflect.macros._

trait LensBuilder[A, B] extends Dynamic {
  def apply(): Lens[A, B]

  // Maybe implement as follows:
  // 1. infer type of B.propName, call it C
  // 2. create Lens[B, C] for B.propName (new Lenser[B].propName),
  //    call this newLens
  // 3. return new LensBuilder[A, C] = {
  //      def apply() = LensBuilder.this.apply() compose newLens
  //    }
  def selectDynamic(propName: String) = macro LensBuilder.selectDynamic[A,B]
}

object LensBuilder {
  def build[A, B](lens: Lens[A,B]) = new LensBuilder[A,B] {
    def apply() = lens
  }

  def selectDynamic[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context { type PrefixType = LensBuilder[A, B] })(propName: c.Expr[String]) = {
    import c.universe._

    def abort(reason: String) = c.abort(c.enclosingPosition, reason)
    def mkParam(name: String, tpe: Type) =
      ValDef(Modifiers(Flag.PARAM), newTermName(name), TypeTree(tpe), EmptyTree)

    import treeBuild._
    val t = (c.prefix.tree, propName.tree) match {
      case (x, Literal(Constant(name: String))) =>
        println("--")
        val lensSourceTpe = c.weakTypeOf[B]

        val calledMember = lensSourceTpe.member(newTermName(name)) orElse {
          abort("value %s is not a member of %s".format(name, lensSourceTpe))
        }
        val lensTargetTpe = calledMember.typeSignatureIn(lensSourceTpe) match {
          case NullaryMethodType(tpe) => tpe
          case _                      => abort("member %s is not a field".format(name))
        }

        println("%s -> %s".format(lensSourceTpe, lensTargetTpe))

        // apply from parent builder, we use this in compose
        val parentApply = x.tpe.member(newTermName("apply"))
        val parentLens = Apply(Select(x, parentApply), List())
        val lens = Lenser.createLens[B](c)(lensSourceTpe, name)
        val combinedLens = Apply(Select(lens, newTermName("compose")), List(parentLens))

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
            combinedLens//createLens
          )

        println("--")

        Block(
          List(
            ClassDef(Modifiers(Flag.FINAL), newTypeName("$anon"), List(),
              Template(List(
                AppliedTypeTree(
                  Ident(c.mirror.staticClass("rillit.LensBuilder")), List(TypeTree(c.weakTypeOf[A]), TypeTree(lensTargetTpe)))),
                emptyValDef, List(
                  constructor,
                  applyF
                ))
            )),
          Apply(Select(New(Ident(newTypeName("$anon"))), nme.CONSTRUCTOR), List())
        )


      case x =>
        c.abort(c.enclosingPosition, "unexpected c.prefix tree: " + x)
    }
    c.Expr[Any](c.resetAllAttrs(t))
  }
}
