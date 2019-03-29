object Problem2 extends App {
  def isSorted[A](as: Array[A], ordered: (A, A) => Boolean): Boolean =
    as match {
      case Array(a, b, _*) => ordered(a, b) && isSorted(as.drop(1), ordered)
      case Array(_) => true
    }

  assert(isSorted[Int](Array(1, 1, 3, 100), (x, y) => x <= y))
}
