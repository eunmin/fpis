package fpis

sealed trait Input
case object Coin extends Input
case object Turn extends Input

case class Machine(locked: Boolean, candies: Int, coins: Int)

object Simulator extends App {
  def update(machine: Machine, input: Input): Machine = (machnie, input) match {
    case (Machine(true, candies, conins), Coin) => Machine(!(candies > 0), candies, conins + 1)
    case (Machine(false, candies, conins), Turn) => Machine(true, candies - 1, conins)
    case (Machine(true, candies, conins), Turn) => machnie
    case (Machine(locked, 0, conins), _) => machnie
  }

  def simulateMachine(inputs: List[Input]): State[Machine, (Int, Int)] =
    for {
    // 우당탕탕~!
    machine <- State.get
    _ <- State.set(Machine(false, 1, 1))
  } yield (machine.coins, machine.candies)

  val machnie = Machine(locked = true, candies = 5, coins = 10)
  val result = simulateMachine(List(Turn, Turn)).run(machnie)

  println(result)
}