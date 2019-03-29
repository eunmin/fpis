object Problem5 extends App {
  def compose[A,B,C](f: B => C, g: A => B): A => C = a => f(g(a))

  import Problem3._

  def inc = curry(add)(1)
  def intToString: Int => String = _.toString

  assert(compose(intToString, inc)(10) == "11")
}
