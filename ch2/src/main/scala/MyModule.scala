import scala.annotation.tailrec

object MyModule {
  def abs(n: Int): Int =
    if (n < 0) -n
    else n


  def factorial(n: Int): Int = {
    @tailrec
    def go(n:Int, acc: Int): Int =
      if (n <= 0) acc
      else go(n - 1, n * acc)

    go(n, 1)
  }

  private def formatAbs(x: Int) = {
    val msg = "The absolute value of %d is %d"
    msg.format(x, abs(x))
  }

  private def formatFactorial(n: Int) = {
    val msg = "The factorial of %d is %d"
    msg.format(n, factorial(n))
  }

  def formatResult(name: String, n: Int, f: Int => Int) = {
    val msg = "The %s of %d is %d"
    msg.format(name, n, f(n))
  }

  def main(args: Array[String]): Unit = {
    println(formatAbs(-42))
    println(formatFactorial(7))
    println(formatResult("absolute value", -42, abs))
    println(formatResult("factorial", 7, factorial))
  }

  def findFirst(ss: Array[String], key: String): Int = {
    @tailrec
    def loop(n: Int): Int =
      if (n >= ss.length) - 1
      else if (ss(n) == key) n
      else loop(n + 1)

    loop(0)
  }

  def findFirst[A](as: Array[A], p: A => Boolean): Int = {
    @tailrec
    def loop(n: Int): Int =
      if (n >= as.length) - 1
      else if (p(as(n))) n
      else loop(n + 1)

    loop(0)
  }

  val lessThan = new Function2[Int, Int, Boolean] {
    def apply(a: Int, b: Int) = a < b
  }

  val b = lessThan.apply(10, 20)
  val c = lessThan(2, 3)

  def partail[A,B,C](a: A, f: (A, B) => C): B => C =
    (b: B) => f(a, b)

  val f = (x: Double) => math.Pi / 2 - x
  val cos = f andThen math.sin
}
