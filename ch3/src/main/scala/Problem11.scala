import Problem10._

object Problem11 extends App {

  object IntList {
    def sum(l: List[Int]): Int = List.foldLeft(l, 0)(_ + _)
  }

  implicit def intList(l: List.type) = IntList

  object DoubleList {
    def product(l: List[Double]): Double = List.foldLeft(l, 1.0)(_ * _)
  }

  implicit def doubleList(l: List.type) = DoubleList

  object CountableList {
    def count[A](l: List[A]): Int = List.foldLeft(l, 0)((n, _) => n + 1)
  }

  implicit def countableList(l: List.type) = CountableList

  assert(List.sum(List(1, 2, 3)) == 6)
  assert(List.product(List(1.0, 2.0, 3.0)) == 6.0)
  assert(List.count(List(1, 2, 3)) == 3)
}
