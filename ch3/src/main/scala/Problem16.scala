object Problem16 extends App {

  object AddOneList {
    def addOne(as: List[Int]): List[Int] =
      List.foldRight(as, List[Int]())((x, r) => Cons(x + 1, r))
  }

  implicit def addOneList(l: List.type) = AddOneList

  assert(List.addOne(List(1, 2, 3)) == List(2, 3, 4))
}
