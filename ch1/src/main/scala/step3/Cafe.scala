package step3

case class Coffee() {
  val price: Double = 5000
}

case class CreditCard() {
  val cardNumber: String = ""
}

case class Charge(creditCard: CreditCard, price: Double)

class Cafe {
  def buyCoffee(creditCard: CreditCard): (Coffee, Charge) = {
    val cup = new Coffee()
    (cup, Charge(creditCard, cup.price))
  }
}
