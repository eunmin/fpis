import Problem10._

object Problem12 extends App {

  object ReverseList {
    def reverse[A](l: List[A]): List[A] =
      List.foldLeft(l, List[A]())((r, x) => List.append(List(x), r))
  }

  implicit def reverseList(l: List.type) = ReverseList

  assert(List.reverse(List(1, 2, 3)) == List(3, 2 ,1))
}
