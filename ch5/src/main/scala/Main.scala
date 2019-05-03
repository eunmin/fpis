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

  println(s.append(Stream(4,5,6)).toList)

  println(s.flatMap((x: Int) => Stream(x, x)).toList)

  println(s.headOption2)

  println(Stream().headOption2)

  val ones: Stream[Int] = Stream.cons(1, ones)

  println(ones.take(2).toList)

  println("aaaaaa: " + ones.takeWhile(_ == 1))

  // Problem 5.8
  def constant[A](a: A): Stream[A] =
    Stream.cons(a, constant(a))

  println(constant(2).take(4).toList)

  // Problem 5.9
  def from(n: Int): Stream[Int] =
    Stream.cons(n, from(n + 1))

  println(from(10).take(3).toList)

//   Problem 5.10
  def fibs(x: Int, y: Int): Stream[Int] =
    Stream.cons(x, fibs(y, x + y))

  println(fibs(0, 1).take(10).toList)

  def unfold[A,S](z: S)(f: S => Option[(A,S)]): Stream[A] =
    f(z) match {
      case None => Stream.empty
      case Some((a, s)) => Stream.cons(a, unfold(s)(f))
    }

  def constant2[A](a: A): Stream[A] =
    unfold(0)(x => Some((a, 0)))

  def ones: Stream[Int] =
    unfold(1)(x => )

  println(constant2("a").take(3).toList)

//  println(unfold(0)(Some((1, 0))).take(4).toList)
}
