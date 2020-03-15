package fpis

trait Monoid[A] {
  def op(a1: A, a2: A): A
  def zero: A
}
