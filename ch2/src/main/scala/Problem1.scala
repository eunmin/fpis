object Problem1 extends App {
  def fib(n: Int): Int =
    n match {
      case 0 => 0
      case 1 => 1
      case n => fib(n - 2) + fib(n - 1)
    }

  assert(fib(0) == 0)
  assert(fib(1) == 1)
  assert(fib(2) == 1)
  assert(fib(3) == 2)
  assert(fib(4) == 3)
  assert(fib(5) == 5)
}
