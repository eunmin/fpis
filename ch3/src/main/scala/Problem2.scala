object Problem2 extends App {
  def tail[A](list: List[A]): List[A] =
    list match {
      case Cons(_, t) => t
      case Nil => Nil
    }

  assert(tail[Int](List(1, 2, 3)) == List(2, 3))
  assert(tail[Int](Nil) == Nil)
}
