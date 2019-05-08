object Main extends App {
  val rng = new scala.util.Random

  println(rng.nextDouble)
  println(rng.nextDouble)
  println(rng.nextInt)
  println(rng.nextInt(10))

  def rollDie: Int = {
    val rng = new scala.util.Random
    rng.nextInt(6)
  }

  val rng2 = SimpleRNG(42)
  val (n1, rng3) = rng2.nextInt
  println(n1)

  val (n2, rng4) = rng3.nextInt
  println(n2)

  def randomPair(rng: RNG): (Int, Int) = {
    val (i1, _) = rng.nextInt
    val (i2, _) = rng.nextInt
    (i1, i2)
  }

  def randomPair2(rng: RNG): ((Int, Int), RNG) = {
    val (i1, rng2) = rng.nextInt
    val (i2, rng3) = rng2.nextInt
    ((i1, i2), rng3)
  }

  def nonNegativeInt(rng: RNG): (Int, RNG) = {
    val (i, rng2) = rng.nextInt
    (Math.abs(i), rng2)
  }

  val (n, rng5) = nonNegativeInt(rng2)
  println(n)

  def double(rng: RNG): (Double, RNG) = {
    val (i, rng2) = nonNegativeInt(rng)
    (i.toDouble / Int.MaxValue.toDouble, rng2)
  }

  val (n3, rng6) = double(rng5)
  println(n3)

  def intDouble(rng: RNG): ((Int, Double), RNG) = {
    val (n, rng2) = rng.nextInt
    val (d, rng3) = double(rng2)
    ((n,d), rng3)
  }

  def doubleInt(rng: RNG): ((Double, Int), RNG) = {
    val ((n, d), rng2) = intDouble(rng)
    ((d, n), rng2)
  }

  def double3(rng: RNG): ((Double, Double, Double), RNG) = {
    val (d1, rng2) = double(rng)
    val (d2, rng3) = double(rng2)
    val (d3, rng4) = double(rng3)
    ((d1,d2,d3) ,rng4)
  }

  def ints(count: Int)(rng: RNG): (List[Int], RNG) = {
    List.range(0, count).foldLeft((Nil: List[Int], rng))((r, x) => {
      val (xs, rng2) = r
      val (n, rng3) = rng2.nextInt
      ((n :: xs), rng3)
    })
  }

  println(ints(3)(rng4))

  type Rand[+A] = RNG => (A, RNG)

  val int: Rand[Int] = _.nextInt

  def unit[A](a: A): Rand[A] =
    rng => (a, rng)

  def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      (f(a), rng2)
    }

  def nonNegativeEven: Rand[Int] =
    map(nonNegativeInt)(i => i - i % 2)

  def nonNegativeLessThan(n: Int): Rand[Int] = { rng =>
    val (i, rng2) = nonNegativeInt(rng)
    val mod = i % n
    if (i + (n-1) - mod >= 0)
      (mod, rng2)
    else nonNegativeLessThan(n)(rng2)
  }

  def double2: Rand[Double] =
    map(nonNegativeInt)(i => i.toDouble / Int.MaxValue.toDouble)

  println(double2(rng3))

  def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    rng => {
      val (a, rng2) = ra(rng)
      val (b, rng3) = rb(rng2)
      (f(a,b), rng3)
    }

  def both[A, B](ra: Rand[A], rb: Rand[B]): Rand[(A, B)] =
    map2(ra, rb)((_, _))

  def randIntDouble: Rand[(Int, Double)] =
    both(int, double)

  def randDoubleInt: Rand[(Double, Int)] =
    both(double, int)

  println(randIntDouble(rng3))
  println(randDoubleInt(rng3))

  def sequnce[A](fs: List[Rand[A]]): Rand[List[A]] =
    rng => {
      fs.foldLeft((Nil: List[A], rng))((r, f) => {
        val (xs, rng2) = r
        val (x, rng3) = f(rng2)
        ((x :: xs), rng3)
      })
    }

  def ints2(count: Int): Rand[List[Int]] =
    sequnce(List.fill(count)(nonNegativeInt))

  println(ints(10)(rng4))

  def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (x, rng2) = f(rng)
      g(x)(rng2)
    }

  def map_[A,B](s: Rand[A])(f: A => B): Rand[B] =
    flatMap(s)(x => unit(f(x)))

  println(map_(nonNegativeInt)(i => i + 1)(rng3))

  def map2_[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    flatMap(ra)(a =>
      flatMap(rb)(b =>
        unit(f(a, b))))

  def both_[A, B](ra: Rand[A], rb: Rand[B]): Rand[(A, B)] =
    map2_(ra, rb)((_, _))

  def randIntDouble2: Rand[(Int, Double)] =
    both_(int, double)

  println(randIntDouble2(rng3))

  def simulate(input: Input): State[Machine, (Int, Int)] = {
    State(machine => {
      val Machine(locked, candies, coins) = machine
      input match {
        case Coin => {
          if (candies > 0 && locked) ((candies, (coins + 1)), Machine(false, candies, (coins + 1)))
          else ((candies, coins), machine)
        }
        case Turn => {
          if (candies < 1 || locked) ((candies, coins), machine)
          else (((candies - 1), coins), Machine(true, (candies - 1), coins))
        }
      }
    })
  }

  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] =
    inputs.foldLeft(State.unit[Machine,(Int,Int)]((0, 0)))((r, i) => r.map2(simulate(i))((x, y) => (x._1 + y._1, x._2 + y._2)))

  println(simulateMachine(List(Coin, Turn, Coin, Turn, Coin, Turn, Coin, Turn)).run(Machine(true, 5, 10)))
}
