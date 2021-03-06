package step4

case class Coffee() {
  val price: Double = 5000
}

case class CreditCard() {
  val cardNumber: String = ""
}

case class Charge(creditCard: CreditCard, amount: Double) {
  def combine(other: Charge): Charge =
    if (creditCard == other.creditCard)
      Charge(creditCard, amount + other.amount)
    else
      throw new Exception("다른 카드와 금액을 합칠 수 없습니다")
}

class Cafe {
  def buyCoffee(creditCard: CreditCard): (Coffee, Charge) = {
    val cup = new Coffee()
    (cup, Charge(creditCard, cup.price))
  }

  def buyCoffees(creditCard: CreditCard, n: Int): (List[Coffee], Charge) = {
    val coffees = List.fill(n)(Coffee
    val a = coffees.map(buyCoffee(_))
    val b = a.unzip
    val result = (b._1, b._2.reduce(_.combine(_)))
    result
  }
}
