case class Gen[A](sample: State[RNG, A])

object Gen {
  def choose(start: Int, stopExclusive: Int): Gen[Int] = Gen[Int](State[RNG, Int]( rng => {
      val (value, newRng) = rng.nextInt
      ((Math.abs(value) % (stopExclusive - start)) + start, newRng)
    }))

  def unit[A](a: => A): Gen[A] = Gen[A](State[RNG, A]( rng => (a, rng)))

  def boolean: Gen[Boolean] = Gen[Boolean](State[RNG, Boolean]( rng => {
    val (value, newRng) = rng.nextInt
    (value % 2 == 0, newRng)
  }))

  def listOfN[A](n: Int, g: Gen[A]): Gen[List[A]] = Gen[List[A]](
    State.sequence(List.fill(n)(g.sample))
  )
}
