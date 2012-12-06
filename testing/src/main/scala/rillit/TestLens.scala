package rillit

case class Inner(value: Int)
case class Outer(inner: Inner)

object Main {
  val outer = Outer(Inner(3))

  def main(args: Array[String]) {
    println("--macro--")
    val value = new Lenser[Inner].value
    val inner = new Lenser[Outer].inner
    val composedLens = inner andThen value
    println("Original:       %s".format(outer))
    println("Original value: %s".format(composedLens.get(outer)))
    println("Modified:       %s".format(composedLens.set(outer, 4)))
  }
}

