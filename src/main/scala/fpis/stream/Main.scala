package fpis.stream

import cats.effect.{ExitCode, IO, IOApp}
import fs2.Stream

object Main extends App {

  val eff = Stream.eval(IO { println("BEING RUN!!"); })
  val eff2 = Stream.eval(IO { println("BEING RUN!!"); })
  val s1 = Stream(1)
  val all = eff ++ eff2 ++ Stream("...moving on")

  println(all.compile.toVector.unsafeRunSync())
}
