object Problem3 extends App {
  def setHead[A](list: List[A], x: A): List[A] =
    list match {
      case _ :: rest => x :: rest
      case Nil => Nil
    }

  assert(setHead[Int](List(1, 2, 3), 4) == List(4, 2, 3))
  assert(setHead[Int](Nil, 4) == Nil)
}

