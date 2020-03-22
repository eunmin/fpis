package fpis

import java.util.concurrent.Executors

import fpis.Par.Par

object Main extends App {
  def sum(ints: IndexedSeq[Int]): Par[Int] =
    if (ints.size <= 1)
      Par.unit(ints.headOption getOrElse 0)
    else {
      val (l,r) = ints.splitAt(ints.length/2)
      Par.map2(Par.fork(sum(l)), Par.fork(sum(r)))(_ + _)
    }

  val s = Executors.newFixedThreadPool(2)

  val result = sum(IndexedSeq(1,2,3,4,5,6))
  val future = Par.run(s)(result)
  println(s"result: ${future.get()}")
}