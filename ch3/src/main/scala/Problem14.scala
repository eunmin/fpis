import Problem10._

object Problem14 extends App {

  object AppendList {
    def append2[A](as1: List[A], as2: List[A]): List[A] =
      List.foldRight(as1, as2)((x, r) => Cons(x, r))
  }

  implicit def appendList(l: List.type) = AppendList

  assert(List.append2(List(1, 2, 3), List(4, 5, 6)) == List(1, 2, 3, 4, 5, 6))
}
