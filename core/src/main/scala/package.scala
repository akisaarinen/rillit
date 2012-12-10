import language.implicitConversions

package object rillit {
  implicit def lenser2lens[A,B](lenser: InitializedLenser[A,B]) = lenser.apply
}
