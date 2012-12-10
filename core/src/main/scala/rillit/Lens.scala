package rillit

trait Lens[A, B] {
  def get(x: A): B

  def set(x: A, v: B): A

  def modify(a:A, f: B => B) : A = set(a, f(get(a)))

  def compose[C](that: Lens[C,A]) = new Lens[C,B] {
    def get(c: C)       = Lens.this.get(that.get(c))
    def set(c: C, b: B) = that.modify(c, Lens.this.set(_, b))
  }

  def andThen[C](that: Lens[B,C]) = that compose this
}