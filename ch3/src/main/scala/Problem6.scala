object Problem6 extends App {
  def init[A](list: List[A]): List[A] = {
    def f(newList: List[A], restList: List[A]): List[A] =
      restList match {
        case Nil => Nil
        case Cons(_, Nil) => newList
        case Cons(h, t) => f(List.append(newList, List(h)), t)
      }
    f(Nil, list)
  }

  assert(init(Nil) == Nil)
  assert(init(List(1,2,3)) == List(1,2))
}
