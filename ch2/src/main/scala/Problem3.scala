object Problem3 extends App {
  def curry[A,B,C](f: (A, B) => C): A => (B => C) = a => b => f(a, b)

  def add(x: Int, y: Int): Int = x + y

  assert(curry(add)(1)(2) == 3)
}
