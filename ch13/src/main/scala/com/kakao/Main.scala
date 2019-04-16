package com.kakao

case class Player(name: String, score: Int)

object Main extends App {
  // 13.1

  def contest(p1: Player, p2: Player): Unit =
    if (p1.score > p2.score)
      println(s"${p1.name} is the winner!")
    else if (p2.score > p1.score)
      println(s"${p2.name} is the winner!")
    else
      println("It's a draw.")

  contest(Player("A", 10), Player("B", 20))

  def winner(p1: Player, p2: Player): Option[Player] =
    if (p1.score > p2.score) Some(p1)
    else if (p2.score > p1.score) Some(p2)
    else None

  def contest2(p1: Player, p2: Player): Unit =
    winner(p1, p2) match {
      case Some(Player(name, _)) => println(s"$name is the winner!")
      case None => println("It's a draw.")
    }

  contest2(Player("A", 20), Player("B", 10))

  def winnerMsg(p: Option[Player]): String =
    p match {
      case Some(Player(name, _)) => s"$name is the winner!"
      case None => "It's a draw."
    }

  def contest3(p1: Player, p2: Player): Unit =
    println(winnerMsg(winner(p1, p2)))

  contest3(Player("A", 10), Player("B", 10))

  // 13.2
/*
  def PrintLine(msg: String): IO =
    new IO { def run = println(msg) }

  def contest4(p1: Player, p2: Player): IO =
    PrintLine(winnerMsg(winner(p1, p2)))

  contest4(Player("A", 10), Player("B", 20)).run
*/
  // 13.2.1

  def fahrenheitToCelsius(f: Double): Double =
    (f - 32) * 5.0 / 9.0

  def converter: Unit = {
    println("Enter a temperature in degrees Fahrenheit: ")
    val d = readLine.toDouble
    println(fahrenheitToCelsius(d))
  }

  converter

//  def convert: IO = {
//    val prompt: IO = PrintLine(
//      "Enter a temperature in degrees Fahrenheit: "
//      //
//    )
//  }
//
//  def ReadLine: IO[String] = IO { readLine }
//  def PrintLine(msg: String):IO[Unit] = IO { println(msg) }
//  def converter2: IO[Unit] = for {
//    _ <- PrintLine("Enter a temperature in degrees Fahrenheit: ")
//    d <- ReadLine.map(_.toDouble)
//    _ <- PrintLine(fahrenheitToCelsius(d).toString)
//  } yield ()
//
//  converter2.run
//
//  val echo = ReadLine.flatMap(PrintLine)
//  val readInt = ReadLine.map(_.toInt)

//  def factorial(n: Int): IO[Int] = for {
//    acc <- ref(1)
//    _ <- foreachM (1 to n toStream) (i => acc.modify(_ * i).skip)
//    result <- acc.get
//  } yield result

  val f = (x: Int) => x
  val g = List.fill(100000)(f).foldLeft(f)(_ compose _)
  val result = g(42)
}
