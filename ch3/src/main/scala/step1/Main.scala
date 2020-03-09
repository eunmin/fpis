package step1

sealed trait List[+A]
case object Nil extends List[Nothing]
case class Cons[+A](head: A, tail: List[A]) extends List[A]

object Main extends App {
  def append[A](a1: List[A], a2: List[A]): List[A] = a1 match {
    case Nil => a2
    case Cons(h, t) => Cons(h, append(t, a2))
  }

  def foldRight[A, B](as: List[A], z: B)(f: (A, B) => B): B = as match {
    case Nil => z
    case Cons(h ,t) => f(h, foldRight(t, z)(f))
  }

  @annotation.tailrec
  def foldLeft[A, B](as: List[A], z: B)(f: (A, B) => B): B = as match {
    case Nil => z
    case Cons(h ,t) => foldLeft(t, f(h, z))(f)
  }

  def map[A,B](as: List[A])(f: A => B): List[B] = ???

  def flatMap[A,B](as: List[A])(f: A => List[B]): List[B] = ???
}