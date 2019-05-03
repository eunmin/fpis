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
  def sequence[S,A](ss: List[State[S,A]]): State[S,List[A]] = ???
  def get[S]: State[S, S] = State(s => (s, s))
  def set[S](s: S): State[S, Unit] = State(_ => ((), s))
}