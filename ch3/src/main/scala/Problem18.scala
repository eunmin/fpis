object Problem18 extends App {

  object MapList {
    def map[A,B](as: List[A])(f: A => B): List[B] =
      List.foldRight(as, List[B]())((x, r) => Cons(f(x), r))
  }

  implicit def mapList(l: List.type) = MapList

  assert(List.map(List(1, 2, 3))(_ + 1) == List(2, 3, 4))
}
