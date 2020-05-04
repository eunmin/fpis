package fpis.io

import scala.io.StdIn.readLine

trait Functor[F[_]] {
  def map[A,B](fa: F[A])(f: A => B): F[B]
}

trait Monad[F[_]] extends Functor[F] {
  def unit[A](a: => A): F[A]
  def flatMap[A,B](ma: F[A])(f: A => F[B]): F[B]

  def map[A,B](ma: F[A])(f: A => B): F[B] =
    flatMap(ma)(a => unit(f(a)))

  def map2[A,B,C](ma: F[A], mb: F[B])(f: (A, B) => C): F[C] =
    flatMap(ma)(a => map(mb)(b => f(a, b)))

  def sequence[A](lma: List[F[A]]): F[List[A]] =
    lma.foldRight(unit(List[A]()))((ma, mla) => map2(ma, mla)(_ :: _))

  def traverse[A,B](la: List[A])(f: A => F[B]): F[List[B]] =
    la.foldRight(unit(List[B]()))((a, mlb) => map2(f(a), mlb)(_ :: _))

  def forever[A,B](a: F[A]): F[B] = {
    lazy val t: F[B] = flatMap(a)(_ => t)
    t
  }
}

sealed trait IO[A] { self =>
  def run: A
  def map[B](f: A => B): IO[B] =
    new IO[B] { def run = f(self.run) }
  def flatMap[B](f: A => IO[B]): IO[B] =
    new IO[B] { def run = f(self.run).run }
}

object IO extends Monad[IO] {
  def unit[A](a: => A): IO[A] = new IO[A] { def run = a }
  def flatMap[A,B](fa: IO[A])(f: A => IO[B]) = fa flatMap f
  def apply[A](a: => A): IO[A] = unit(a)
}

object Main extends App {
  def ReadLine: IO[String] = IO { readLine }
  def PrintLine(msg: String):IO[Unit] = IO { println(msg) }

  def x = PrintLine("1").flatMap( _ =>
    PrintLine("2").flatMap( _ =>
      PrintLine("3")
    )
    // new IO[B] { def run = f(new IO[A] { def run = println("2") }.run).run }
  )
  def y = IO.forever(PrintLine("Still going..."))

  y.run
}
