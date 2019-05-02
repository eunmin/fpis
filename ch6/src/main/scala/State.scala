case class State[S,+A](run: S => (A,S)) {
  def map[B](f: A => B): State[S,B] = ???
  def map2[B,C](b: State[S,B])(f: (A,B) => C): State[S, (A,B)] = ???
  def flatMap[B](f: A => State[S,B]): State[S,B] = ???
}

object State {
  def unit[S,A]: State[S,A] = ???
  def sequence[S,A](ss: List[State[S,A]]): State[S,List[A]] = ???
}