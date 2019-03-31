object Problem10 extends App {
  object FoldLeftList {
    def foldLeft[A, B](as: List[A], z: B)(f: (B, A) => B): B =
      as match {
        case Nil => z
        case Cons(h, t) => foldLeft(t, f(z, h))(f)
      }
  }

  implicit def foldLeftList(l: List.type) = FoldLeftList
}
