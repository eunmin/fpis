object Main extends App {
  def if2[A](cond: Boolean, onTrue: () => A, onFlase: () => A): A =
    if (cond) onTrue() else onFlase()

  if2(23 < 22,
    () => println("a"),
    () => println("b")
  )

  def if3[A](cond: Boolean, onTrue: => A, onFlase: => A): A =
    if (cond) onTrue else onFlase

  if3(false, sys.error("fail"), 3)

  def maybeTwice(b: Boolean, i: => Int) = if (b) i + i else 0

  val x = maybeTwice(true, { println("hi"); 1 + 41 })

  def maybeTwice2(b: Boolean, i: => Int) = {
    lazy val j = i
    if (b) j + j else 0
  }

  val y = maybeTwice2(true, { println("hi2"); 1 + 41 })

  lazy val z = { println("hoi") }
  z
  z

  val s = Stream(1,2,3)

  println(s.headOption)

  println(s.take(2).toList)

  println(s.drop(2).toList)

  println(s.takeWhile(_ < 3).toList)

  println(s.takeWhile2(_ < 3).toList)

  println(s.forAll(_ < 1))

  println(s.map(_ + 3).toList)

  println(s.flatMap((x: Int) => Stream(x, x)))
}
