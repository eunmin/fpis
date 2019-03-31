object Problem4 extends App {
  def drop[A](list: List[A], n: Int): List[A] =
    if(n > 0)
      list match {
        case Cons(_, t) => drop(t, n - 1)
        case Nil => Nil
      }
    else
      list

  assert(drop(Nil, 1) == Nil)
  assert(drop(List(1,2,3), 2) == List(3))
}
