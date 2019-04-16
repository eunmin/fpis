package com.kakao

trait Functor[F[_]] {
  def map[A,B](f: A => B): F[B]
}
trait Applicative[F[_]] extends Functor[F] {
  def map2[A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C]
  def unit[A](a: => A): F[A]
  def map[A,B](fa: F[A])(f: A => B): F[B] =
    map2(fa, unit(()))((a, _) => f(a))
  def traverse[A,B](as: List[A])(f: A => F[B]): F[List[B]] =
    as.foldRight(unit(List[B]()))((a, fbs) => map2(f(a), fbs)(_ :: _))
}

trait Monad[F[_]] extends Applicative[F] {
  def flatMap[A,B](fa: F[A])(f: A => F[B]): F[B] = join(map(fa)(f))
  def join[A](ffa: F[F[A]]): F[A] = flatMap(ffa)(fa => fa)
  def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C] =
    a => flatMap(f(a))(g)
  override def map[A,B](fa: F[A])(f: A => B): F[B] =
    flatMap(fa)((a: A) => unit(f(a)))
  def map2[A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
    flatMap(fa)(a => map(fb)(b => f(a,b)))
}
//
//sealed trait IO[A] {
//  @annotation.tailrec def run[A](io: IO[A]): A = io match {
//    case Return(a) => a
//    case Suspend(r) => r()
//    case FlatMap(x, f) => x match {
//      case Return(a) => run(f(a))
//      case Suspend(r) => run(f(r()))
//      case FlatMap(y, g) => run(y flatMap (a => g(a) flatMap f))
//    }
//  }
//  def map[B](f: A => B): IO[B] =
//    flatMap(f andThen (Return(_)))
//  def flatMap[B](f: A => IO[B]): IO[B] =
//    FlatMap(this, f)
//}
//
//object IO extends Monad[IO] {
//  def unit[A](a: => A): IO[A] = new IO[A] { def run = a }
//  override def flatMap[A,B](fa: IO[A])(f: A => IO[B]) = fa flatMap f
//  def apply[A](a: => A): IO[A] = unit(a)
//  override def map[A, B](f: A => B): IO[B] = ???
//}

//case class Return[A](a: A) extends IO[A]
//case class Suspend[A](resume: () => A) extends IO[A]
//case class FlatMap[A, B](sub: IO[A], k: A => IO[B]) extends IO[B]