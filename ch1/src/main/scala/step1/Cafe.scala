package step1

case class Coffee() {
  val price: Double = 5000
}

case class CreditCard() {
  val cardNumber: String = ""

  def charge(price: Double): Unit = ??? // 네트워크로 실제 카드사에 결제를 요청한다
}

class Cafe {
  def buyCoffee(creditCard: CreditCard): Coffee = {
    val cup = new Coffee()
    creditCard.charge(cup.price)
    cup
  }
}

