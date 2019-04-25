object Main extends App {
  def failingFn(i: Int): Int = {
    val y: Int = throw new Exception("fail!")
    try {
      val x = 42 + 5
      x + y
    }
    catch { case e: Exception => 43}
  }

  def failingFn2(i: Int): Int = {
    try {
      val x = 42 + 5
      x + ((throw new Exception("fail!")): Int)
    }
    catch { case e: Exception => 43}
  }

  failingFn2(12)

  def meanEx(xs: Seq[Double]): Double =
    if (xs.isEmpty)
      throw new ArithmeticException("mean of empty list!")
    else xs.sum / xs.length

  println(meanEx(Seq()))

  def mean_1(xs: Seq[Double], onEmpty: Double): Double =
    if (xs.isEmpty) onEmpty
    else xs.sum / xs.length

  def mean(xs: Seq[Double]): Option[Double] =
    if (xs.isEmpty) None
    else Some(xs.sum / xs.length)

  case class Employee(name: String, department: String, manager: Option[Employee])

  def lookupByName(name: String): Option[Employee] = ???

  val joeDepartment: Option[String] =
    lookupByName("Joe").map(_.department)

  lookupByName("Joe").flatMap(_.manager)

  lookupByName("Joe").map(_.department).getOrElse("Default Dept.")

  // Problem 4.2
  def variance(xs: Seq[Double]): Option[Double] =
    mean(xs).flatMap(m => mean(xs.map(x => math.pow(x - m, 2))))

  val dept: String =
    lookupByName("Joe").
      map(_.department).
      filter(_ != "Accounting").
      getOrElse("Default Dept")

  def lift[A,B](f: A => B): Option[A] => Option[B] = _ map f

  def absO: Option[Double] => Option[Double] = lift(math.abs)

  def insuranceRateQuote(age: Int, numberOfSpeedingTickets: Int): Double = ???

  def Try[A](a: => A): Option[A] =
    try Some(a)
    catch { case e: Exception => None }

  // Problem 4.3
  def map2[A,B,C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] =
    a flatMap (aa =>
      b map   (bb =>
              f(aa, bb)))

  def parseInsuranceRateQuote(age: String, numberOfSpeedingTickets: String): Option[Double] = {
    val optAge: Option[Int] = Try(age.toInt)
    val optTickets: Option[Int] = Try(numberOfSpeedingTickets.toInt)
    map2(optAge, optTickets)(insuranceRateQuote)
  }

  // Problem 4.4
  def sequence[A](a: List[Option[A]]): Option[List[A]] = ???

  def parseInts(a: List[String]): Option[List[Int]] =
    sequence(a map (i => Try(i.toInt)))

  // Problem 4.5
  def traverse[A,B](a: List[A])(f: A => Option[B]): Option[List[B]] = ???

  def map2_for[A,B,C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] = for {
    aa <- a
    bb <- b
  } yield f(aa, bb)

  def mean_e(xs: IndexedSeq[Double]): Either[String, Double] =
    if (xs.isEmpty)
      Left("mean of empty list!")
    else
      Right(xs.sum / xs. length)

  def safeDiv(x: Int, y: Int): Either[Exception, Int] =
    try Right(x / y)
    catch { case e: Exception => Left(e) }

  def Try2[A](a: => A): Either[Exception, A] =
    try Right(a)
    catch { case e: Exception => Left(e) }

  def parseInsuranceRateQuote2(age: String, numberOfSpeedingTickets: String): Either[Exception, Double] = for {
    a <- Try2 { age.toInt }
    tickets <- Try2 { numberOfSpeedingTickets.toInt }
  } yield insuranceRateQuote(a, tickets)

  def sequence[E, A](es: List[Either[E, A]]): Either[E, List[A]] = ???
  def traverse[E, A, B](as: List[A])(f: A => Either[E, B]): Either[E, List[B]] = ???

  case class Person(name: Name, age: Age)
  sealed class Name(val value: String)
  sealed class Age(val value: Int)

  def mkName(name: String): Either[String, Name] =
    if (name == "" || name == null) Left("Name is empty.")
    else Right(new Name(name))

  def mkAge(age: Int): Either[String, Age] =
    if (age < 0) Left("Age is out of range.")
    else Right(new Age(age))

  def mkPerson(name: String, age: Int): Either[String, Person] =
    mkName(name).map2(mkAge(age))(Person(_, _))
}
