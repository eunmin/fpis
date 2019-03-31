object Problem19 extends App {

  object FilterList {
    def filter[A,B](as: List[A])(f: A => Boolean): List[A] =
      List.foldRight(as, List[A]())((x, r) => if(f(x)) Cons(x, r) else r)
  }

  implicit def filterList(l: List.type) = FilterList

  assert(List.filter(List(1, 2, 3))(_ % 2 == 0) == List(2))
}
