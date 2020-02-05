package step4

import org.scalatest.FunSuite

class CafeTest extends FunSuite {
  test("한잔 구입하기 테스트") {
    val creditCard = new CreditCard

    val result = new Cafe().buyCoffee(creditCard)

    assertResult((new Coffee(), Charge(creditCard, new Coffee().price)))(result)
  }

  test("여러잔 구입하기 테스트") {
    val creditCard = new CreditCard

    val result = new Cafe().buyCoffees(creditCard, 2)

    assertResult(2)(result._1.size)
  }
}
