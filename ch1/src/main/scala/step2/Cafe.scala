package step2

case class Coffee() {
  val price: Double = 5000
}

case class CreditCard() {
  val cardNumber: String = ""
}

class Payments {
  def charge(creditCard: CreditCard, price: Double): Unit = ??? // 네트워크로 실제 카드사에 결제를 요청한다
}

class Cafe {
  def buyCoffee(creditCard: CreditCard, p: Payments): Coffee = {
    val cup = new Coffee()
    p.charge(creditCard, cup.price)
    cup
  }
}
