object Problem4 extends App {
  def uncurry[A,B,C](f: A => (B => C)): (A, B) => C = (a, b) => f(a)(b)

  import Problem3._

  def curriedAdd = curry(add)

  def uncurriedAdd = uncurry(curriedAdd)

  assert(uncurriedAdd(1, 2) == 3)
}
