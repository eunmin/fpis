import Problem10._

object Problem20 extends App {

  object FlatMapList {
    def flatMap[A,B](as: List[A])(f: A => List[B]): List[B] =
      List.foldLeft(as, List[B]())((r, x) => List.append(r, f(x)))
  }

  implicit def flatMapList(l: List.type) = FlatMapList

  assert(List.flatMap(List(1, 2, 3))(x => List(x, x)) == List(1, 1, 2, 2, 3, 3))
}
