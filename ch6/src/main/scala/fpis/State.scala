package fpis

import State._

case class State[S,+A](run: S => (A,S)) {

  def map[B](f: A => B): State[S,B] =
    flatMap[B](x => unit(f(x)))

  def map2[B,C](b: State[S,B])(f: (A,B) => C): State[S,C] =
    flatMap[C](a =>
      b.flatMap[C](x =>
        unit(f(a, x))))

  def flatMap[B](f: A => State[S,B]): State[S,B] =
    State(s => {
      val (x,s2) = run(s)
      f(x).run(s2)
    })
}

object State {

  def unit[S,A](a: A): State[S,A] = State(s => (a, s))

  def sequence[S,A](ss: List[State[S,A]]): State[S,List[A]] =
    ss.foldLeft(unit[S,List[A]](Nil : List[A]))((r, s) => s.map2(r)((x,l) => x :: l))

  def get[S]: State[S, S] = State(s => (s, s))

  def set[S](s: S): State[S, Unit] = State(_ => ((), s))
}

object Main2 extends App {
  var state = 0

  state = 100
  val x = state
  val y = x + 10
  state = x + y
  println(s"$y $state")

  val p = for {
    _ <- set(100)
    x <- get[Int]
    y = x + 10
    _ <- set(x + y)
  } yield y

  println(p.run(0))
}