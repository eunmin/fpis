object Problem5 extends App {
  def dropWhile[A](list: List[A])(f: A => Boolean): List[A] =
    list match {
      case Cons(h, t) => if(f(h)) dropWhile(t)(f) else list
      case Nil => Nil
    }

  assert(dropWhile(Nil)(identity) == Nil)
  assert(dropWhile(List(1,2,3))(x => x < 3) == List(3))
}
