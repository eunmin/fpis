import Par._
import java.util.concurrent._

object Main extends App {
  val a = lazyUnit(42 + 1)
  val s = Executors.newFixedThreadPool(2)

  println(Par.equal(s)(a, fork(a)))
}
