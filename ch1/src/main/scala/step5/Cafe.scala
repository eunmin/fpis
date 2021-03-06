package step5

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
    val purchases: List[(Coffee, Charge)] = List.fill(n)(buyCoffee(creditCard))
    val (coffees, charges) = purchases.unzip
    (coffees, charges.reduce((c1, c2) => c1.combine(c2)))
  }
}
