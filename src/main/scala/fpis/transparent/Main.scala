package fpis.transparent

object Main extends App {
  case class Foo(s: String)

  val b = Foo("hello") == Foo("hello")
  val c = Foo("hello") eq Foo("hello")
  println(s"$b $c")

  // 참조 투명하지 않다. 생성자에는 모두 부수 효과가 있다.
  val x = Foo("hello")
  val b2 = x == x
  val c2 = x eq x
  println(s"$b2 $c2")

  // 일반적인 참조 투명성의 정의
  // 프로그램 p 에서 표현식 e의 모든 출현을 e의 평가 결과로 치환해도 p의 의미에 아무런 영향을 미치지 않는다면 p에 관해 표현식 e는 참조에 투명하다.

  def timesTwo(x :Int) = {
    if (x < 0) println("Got a negative number")
    x * 2
  }

  // timesTwo(1) 과 2를 치환하면 원래 의미가 바뀌었다고 할 수 있다.
  // 하지만 println를 다른 부분에서 부수 효과를 관측할 가능성은 아주 낮기 때문에 참조 투명하다고 할 수도 있다.
  // 우리가 결정하는 선택이다.
  // 확실한 것은 프로그램의 정확성이 의존하는 효과들을 추적하는 것이다.
}
