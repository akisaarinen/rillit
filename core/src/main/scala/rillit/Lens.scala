package rillit

case class Lens[A,B](
  get: A => B,
  set: (A, B) => A
) {
  def modify(a:A, f: B => B) : A = set(a, f(get(a)))

  def compose[C](that: Lens[C,A]) = Lens[C,B](
    c => get(that.get(c)),
    (c, b) => that.modify(c, set(_, b))
  )

  def andThen[C](that: Lens[B,C]) = that compose this
}