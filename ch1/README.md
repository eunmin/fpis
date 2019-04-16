# 함수형 프로그래밍이란 무엇인가?

- 함수형 프로그래밍이란 프로그램을 순수 함수로만 구축한다는 것
- 부수 효과의 예
  - 변수를 수정한다.
  - 자료구조를 제자리에서 수정한다. (Modify a data structure in place)
  - 객체의 필드를 설정한다.
  - 예외를 던지거나 오류를 내면서 실행을 중단한다.
  - 콘솔에 출력하거나 사용자의 입력을 읽어들인다.
  - 파일에 기록하거나 파일에서 읽어들인다.
  - 화면에 그린다.
- 프로그래밍을 작성하는 방식의 제약이지 표현 가능한 프로그램의 종류를 제약하는 것이 아니다.
- 순수 함수로 프로그램을 작성하면 모듈성(modularity)이 증가한다.
- 버그가 생길 여지가 휠신 적다.

## FP의 이점: 간단한 예제 하나

### 부수 효과가 있는 프로그램

```scala
class Cafe {
  def buyCoffee(creditCard: CreditCard): Coffee = {
    val cup = new Coffee()
    creditCard.charge(cup.price)
    cup
  }
}
```

- `charge`는 실제 신용 카드 회사에 원격 요청으로 결제를 하는 함수라고 하자.
- 부수 효과가 있기 때문에 테스트 하기 어렵다. (`CreditCard`의 Mock을 만들면???)
- `CreditCard`에 신용 카드 결제 로직이 있는 도메인 모델링 관점에서 좋지 않다.
- 결제를 하는 `Payments` 객체를 만들고 함수의 인자로 넘기도록 리팩토링
  ```scala
  class Cafe {
    def buyCoffee(creditCard: CreditCard, p: Payments): Coffee = {
      val cup = new Coffee()
      p.charge(creditCard, cup.price)
      cup
    }
  }
  ```

- 테스트를 위해 `Payments`를 인터페이스로 만들고 Mock을 만들면 된다.
- `Mock`을 만드는 것은 어렵다. `charge` 호출 후에 `Payments` 내부 상태에 대한 변화 검사도 할 필요가
  있다. (Spy 같은 것을 만들어야 하는데 그냥 만들기 힘드니 Mockito 같은 프레임워크를 써야겠다.)
- `buyCoffee` 재사용하기 어렵다. 만약 `buyCoffee`를 여러번 호출 한다고 하면 카드 결제가 여러번
  발생하고 매번 수수료가 발생할 것이다.
- `buyCoffee`가 단순하기 때문에 대금을 누적하는 `buyCoffee` 새로운 함수를 만들면 된다.
- 만약 로직이 복잡하다면 코드의 재사용 능력과 합성 능력에 해가 될 수 있다.

### 함수적 해법: 부수 효과의 제거

- 함수적 해법은 `buyCoffee`가 `Coffee`와 청구건 하나를 돌려주도록 만든다. 부수 효과는 외부 어딘가에서
  처리한다.
  ```scala
  class Cafe {
    def buyCoffee(creditCard: CreditCard): (Coffee, Charge) = {
      val cup = new Coffee()
      (cup, Charge(creditCard, cup.price))
    }
  }
  ```
- `Charge`는 다음과 같이 생겼다. 같은 카드로 결제하면 금액을 모아주는 `combine` 함수가 있다.
  ```scala
  case class Charge(creditCard: CreditCard, amount: Double) {
    def combine(other: Charge): Charge =
      if (creditCard == other.creditCard)
        Charge(creditCard, amount + other.amount)
      else
        throw new Exception("다른 카드와 금액을 합칠 수 없습니다")
  }
  ```
- 다음은 여러 잔 커피를 사는 `buyCoffees` 함수를 보자.
  ```scala
  def buyCoffees(creditCard: CreditCard, n: Int): (List[Coffee], Charge) = {
    val purchases: List[(Coffee, Charge)] = List.fill(n)(buyCoffee((creditCard)))
    val (coffees, charges) = purchases.unzip
    (coffees, charges.reduce((c1, c2) => c1.combine(c2)))
  }
  ```
- `buyCoffee`를 재사용해 만들었다는 점에 주목하라.
- 이 함수는 Mock 없이 테스트할 수 있다.
- 이제 `Cafe` 클래스는 실제 결제가 어떻게 되는지 모른다. 물론 어디선가 `Payments` 클래스가 필요하다.
- 비즈니스 논리를 잘 분리했다.
- `Charge`는 일반 값이기 때문에 여러 다양한 카드로 결제한 `Charge`를 받아서 같은 카드끼리 결제한
  `Charge`로 묶어 줄 수 있다.
  ```scala
  def coalesce(charges: List[Charge]): List[Charge] =
    charges.groupBy(_.creditCard).values.map(_.reduce(_ combine _)).toList
  ```
- 이 함수는 역시 재사용 할 수 있고 Mock 없이 테스트 할 수 있다.

#### 실제 세계는 어떻게 해야 할까?

- 부수 효과가 있는 임의의 함수에 변환을 통해 외부 계층으로 밀어낸다. (IO 모나드)
- 부수 효과 계층은 얇은 계층으로 만든다.
- 지역적으로 선언된 자료가 외부에 참조 되지 않는다면 변이가 가능하다.

## 순수 함수란 구체적으로 무엇인가?

- 함수형 프로그래밍은 추론하기 더 쉽다.
- A => B 함수는 A 타입의 a 값에 의존해 B 타입의 b 값을 낸다. 다른 일을 하지 않는다.
- 순수 함수라고 부르지만 이 책에서는 그냥 함수라고 부르겠다.
- `+`는 순수 함수다. 자바 `String`의 `length` 메서드도 순수 함수다.
- 함수에 표현식 외에 다른 외부 효과가 없어 표현식의 결과와 바꿔 쓸 수 있다면 참조 투명하다고 한다.
  `2 + 3`은 `5`다.

## 참조 투명성, 순수성, 그리고 치환 모형

- 참조 투명성의 정의가 원래 `buyCoffee` 예제에 적용되는지 보자.
  ```scala
  def buyCoffee(creditCard: CreditCard): Coffee = {
    val cup = new Coffee()
    creditCard.charge(cup.price)
    cup
  }
  ```
- 위 코드의 결과는 `new Coffee()`와 같다. 만약 참조 투명하려면 `p(buyCoffee(aliceCreditCard))`와
  `p(new Coffee())`가 같아야 하지만 `buyCoffee(aliceCreditCard)`는 실제 신용카드 결제를 하기 때문에
  같지 않다.
- 참조 투명하다면 표현식과 결과 값을 바꿔쓸 수 있기 때문에 값으로 바꿔가면서 추론 할 수 있다. (치환 모형)
- 참조 투명한 예제 하나를 더 보자.
  ```scala
  val x = "Hello, World"
  val r1 = x.reverse
  val r2 = x.reverse
  ```
- 위 예제를 `x`가 가리키는 값으로 바꾸면 아래와 같다.
  ```scala
  val r1 = "Hello, World".reverse
  val r2 = "Hello, World".reverse
  ```
- 결과에 영향을 미치지 않기 때문에 참조 투명하다. `r1`, `r2` 역시 참조 투명하기 때문에 `r1`, `r2`가
  다른 곳에 나오면 치환 해서 추론 할 수 있다.
- 아래는 참조 투명하지 않은 예다.
  ```scala
  val x = new StringBuiler("Hello")
  val y = x.append(", World")
  val r1 = y.toString
  val r2 = y.toString
  ```
- `y`가 나오는 부분을 치환해 보자.
  ```scala
  val x = new StringBuiler("Hello")
  val r1 = x.append(", World").toString
  val r2 = x.append(", World").toString
  ```
- `r1`와 `r2`는 달라진다. `append`는 순수 함수가 아니다.
- 치환 모형은 코드를 추론하기 쉽게 한다. 순수 함수는 모듈적이고 합성 가능하다.
- 부수 효과에 대한 관심사 분리로 도메인 논리의 재사용성이 높아진다.

## 요약

- 참조 투명성, 치환 모형, FP는 추론이 쉽고 모듈성을 높여준다. 
