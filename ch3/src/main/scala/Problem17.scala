object Problem17 extends App {

  object ConvertStringList {
    def convertString(as: List[Int]): List[String] =
      List.foldRight(as, List[String]())((x, r) => Cons(x.toString, r))
  }

  implicit def convertStringList(l: List.type) = ConvertStringList

  assert(List.convertString(List(1, 2, 3)) == List("1", "2", "3"))
}
