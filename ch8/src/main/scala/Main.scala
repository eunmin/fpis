object Main extends App {
  val (value1, _) = Gen.choose(0, 10).sample.run(SimpleRNG(System.currentTimeMillis()))
  println(value1)

  val (value2, _) = Gen.unit(10).sample.run(SimpleRNG(System.currentTimeMillis()))
  println(value2)

  val (value3, _) = Gen.boolean.sample.run(SimpleRNG(System.currentTimeMillis()))
  println(value3)

  val (value4, _) = Gen.listOfN(5, Gen.choose(0, 10)).sample.run(SimpleRNG(System.currentTimeMillis()))
  println(value4)
}
