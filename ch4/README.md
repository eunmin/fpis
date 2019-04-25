# 예외를 이용하지 않은 오류 처리

- 예외를 던지는 것은 부수 효과다
- 오류를 값으로 돌려주고 오류 처리/복구 패턴을 추상화 하자
- 예외의 장점인 오류 처리 논리의 통합(consolidation of error-handling logic)도 유지할 수 있다
  - 예외 처리가 여기저기 널려 있지 않는 것
- 표준 라이브러리에 있는 Option과 Either를 만들어 볼거다.

## 예외의 장단점

- 참조 투명성을 해치는 예외
  ```scala
  def failingFn(i: Int): Int = {
    val y: Int = throw new Exception("fail!")
    try {
      val x = 42 + 5
      x + y
    }
    catch { case e: Exception => 43}
  }
  ```
- 실행하면 예외가 발생
  ```
  failingFn(12)

  Exception in thread "main" java.lang.Exception: fail!
  	at Main$.failingFn(Main.scala:3)
  	at Main$.delayedEndpoint$Main$1(Main.scala:11)
  	at Main$delayedInit$body.apply(Main.scala:1)
  ...
  ```
- `y`를 치환 해보자
  ```scala
  def failingFn2(i: Int): Int = {
    try {
      val x = 42 + 5
      x + ((throw new Exception("fail!")): Int)
    }
    catch { case e: Exception => 43}
  }
  ```
- 다시 실행해보면
  ```
  failingFn2(12)

  43
  ```
- 참조 투명하지 않다. 이 코드를 추론하기 위해서는 전역적 문맥을 이해해야한다.
- 예외는 참조 투명성을 위반하고 문맥 의존성을 도입한다.
- 예외는 타입에 안전하지 않다. `Int => Int`만 보고 예외를 처리해야하는지 알 수 없다.
  - Checked Exception 은 호출 쪽에 보일러 플레이트가 늘어난다.
  - 고차 함수에는 적용할 수 없다.
    ```scala
    def map[A,B](l: List[A])(f: A => B): List[B]
    ```
  - `f`는 어떤 예외를 던지는지 알 수 없고 던지는 예외 별로 `map`을 모두 만들 수 도 없다.

## 예외의 가능한 대안들

- 다음은 평균을 구하는 함수다. 빈 목록을 입력하면 동작하지 않기 때문에 부분 함수다.
  ```scala
  def mean(xs: Seq[Double]): Double =
    if (xs.isEmpty)
      throw new ArithmeticException("mean of empty list!")
    else xs.sum / xs.length
  ```
- 예외의 대안으로 첫번째 `0.0 / 0.0`을 돌려주거나 경계 값 또는 `null`을 돌려줄 수 있다.
- 이런 방식은 오류를 소리 없이 전파하는 문제가 있다.
- 호출 쪽에서 진짜 값을 받았는지 항상 체크해야한다.
- 다형적 타입을 리턴한다면 경계 값을 결정할 수 없을 때도 있다. `A` 타입의 경계 값은 정할 수 없다.
  `null`도 정할 수 없는 것은 `Double`이나 `Int` 같은 기본 형을 리턴할 수도 있기 때문이다.
- 호출 쪽에 특별한 규칙을 요구한다. 따라서 일반화 적으로 처리해야하는 고차 함수에 전달하기 어렵다.
- 다른 방법은 호출 쪽에서 직접 지정하는 방식이다.
  ```scala
  def mean_1(xs: Seq[Double], onEmpty: Double): Double =
    if (xs.isEmpty) onEmpty
    else xs.sum / xs.length
  ```
- 만약 오류일 때 다른 처리를 해야한다면 이 방법도 쓸 수 없다.

## Option 자료 형식

- 함수가 항상 답을 내지 못하는 것을 함수로 표현하는 방법으로 `Option`을 사용한다.
- 호출자에게 오류 처리 전략을 미루는 것으로 생각할 수 있다.
  ```scala
  sealed trait Option[+A]
  case class Some[+A](get: A) extends Option[A]
  case object None extends Option[Nothing]
  ```
- 이를 이용해 다시 `mean`을 고쳐보자.
  ```scala
  def mean(xs: Seq[Double]): Option[Double] =
    if (xs.isEmpty) None
    else Some(xs.sum / xs.length)
  ```
- `mean`은 모든 입력 값에 대해 출력을 하는 완전 함수다.

### Option의 사용 패턴

- 표준 함수는 Option을 다음과 같은 곳에 쓴다.
  - Map에서 키를 찾는 함수는 Option을 돌려준다.
  - 목록 또는 반복 가능한 자료에 `headOption`과 `lastOption`은 빈 값에 Option을 리턴해 동작한다.

#### Option에 대한 기본적인 함수들

- `Option`은 `List`와 비슷하다. 그래서 실제 `List`에서 본 함수가 있다.
- 이번 예제에서는 함수를 동반 객체가 아닌 `trait` 본문에 넣어서 `obj.fn(arg1)` 또는 `obj fn arg1`
  스타일로 쓰겠다. (객체지향적 스타일)
  ```scala
  sealed trait Option[+A] {
    def map[B](f: A => B): Option[B]                 // None이 아니면 f를 적용
    def flatMap[B](f: A => Option[B]): Option[B]     // None이 아니면 실패할 수도 있는 f를 적용
    def getOrElse[B >: A](default: => B): B          // None이면 default를 준다.
    def orElse[B >: A](ob: => Option[B]): Option[B]  // None이면 다른 Option을 준다.
    def filter(f: A => Boolean): Option[A]           // f에 만족하지 않으면 Some을 None으로 바꿈
  }
  ```
- 인자에 쓰인 `default: => B`는 인자가 쓰여야 평가가된다.
- `B >: A`는 `B`는 반드시 `A`의 상위 타입이어야 한다는 제약이다. `Option[+A]`를 쓰려면 이렇게 해야한다.

#### 기본적인 Option 함수들의 용례

- 위에 말한 고차함수는 익숙해져야 하기 때문에 `Option`이 나타나면 무조건 패턴 매칭하지 말고 적당한
  고차 함수가 있는지 살펴보자.
- `map`은 아래 처럼 쓴다. 결과가 None이면 `f`를 부르지 않는다.
  ```scala
  case class Employee(name: String, department: String)

  def lookupByName(name: String): Option[Employee] = ???

  val joeDepartment: Option[String] =
    lookupByName("Joe").map(_.department)
  ```
- `flatMap`은 변환 함수가 실패할 수 도 있다는 점을 빼면 `map`과 같다.
  ```scala
  case class Employee(name: String, department: String, manager: Option[Employee])

  lookupByName("Joe").flatMap(_.manager)
  ```
- `getOrElse`의 예
  ```scala
  lookupByName("Joe").map(_.department).getOrElse("Default Dept.")
  ```
- 연습문제 `variance` 함수를 `flatMap`으로 구현하라.
  ```scala
  def variance(xs: Seq[Double]): Option[Double] =
    mean(xs).flatMap(m => mean(xs.map(x => math.pow(x - m, 2))))
  ```
- `filter`의 사용 예
  ```scala
  val dept: String =
    lookupByName("Joe").
      map(_.department).
      filter(_ != "Accounting").
      getOrElse("Default Dept")
  ```
- 고차 함수로 매번 `None`을 점검할 필요가 없어 오류 처리 논리 통합을 할 수 있다.
- `Option[A]`는 `A`가 아니기 때문에 오류를 항상 처리해줘야한다. 아니면 컴파일 에러가 발생한다.

### 예외 지향적 API의 Option 함성과 승급, 감싸기

- `Option`을 사용하기 시작하면 코드 전체에 `Option`이 번지게 될 것이라는 우려를 하지만 일반 함수를
  `Option`에 대해 처리할 수 있는 함수로 승급시킬(`lift`) 수 있기 때문에 어렵지 않다.
- `lift`는 `A => B` 함수를 `Option[A] => Option[B]`로 바꾸는 것과 같다.
  ```scala
  def lift[A,B](f: A => B): Option[A] => Option[B] = _ map f

  def absO: Option[Double] => Option[Double] = lift(math.abs)
  ```
- 보험료 계산 함수의 예
  ```scala
  def insuranceRateQuote(age: Int, numberOfSpeedingTickets: Int): Double = ???

  def Try[A](a: => A): Option[A] =
    try Some(a)
    catch { case e: Exception => None }

  def parseInsuranceRateQuote(age: String, numberOfSpeedingTickets: String): Option[Double] = {
    val optAge: Option[Int] = Try(age.toInt)
    val optTickets: Option[Int] = Try(numberOfSpeedingTickets.toInt)
    insuranceRateQuote(optAge, optTickets) // ???
  }
  ```
- `insuranceRateQuote`를 `lift` 해야하는데 `insuranceRateQuote`가 `(A, B) => C`다.
- 이를 위해 `map2`가 있으면 된다. `map2`는 두 인자 중 하나라도 `None`이면 전체가 `None`이다.
  ```scala
  def map2[A,B,C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C]
  ```
- `map2`로 위 예제를 완성해보자.
  ```scala
  def parseInsuranceRateQuote(age: String, numberOfSpeedingTickets: String): Option[Double] = {
    val optAge: Option[Int] = Try(age.toInt)
    val optTickets: Option[Int] = Try(numberOfSpeedingTickets.toInt)
    map2(optAge, optTickets)(insuranceRateQuote)
  }
  ```
- `map2`는 인자 두개를 받지만 여러개를 받을 수 있는 `sequence`도 만들어 볼 수 있다. (연습문제 4.4)
  `sequence`는 `Option` 목록을 받아서 `Option`으로 되어 있는 값을 리턴한다. 인자 목록에 하나라도
  None이 있으면 결과는 None이다.
  ```scala
  def sequence[A](a: List[Option[A]]): Option[List[A]] = ???
  ```
- 이를 이용해 `parseInts`를 만들어보자.
  ```scala
  def parseInts(a: List[String]): Option[List[Int]] =
    sequence(a map (i => Try(i.toInt)))
  ```
- 이 방식은 `map`을 한번 돌고 또 `sequence`를 돌아야 하므로 비효율적이다. 일반적으로 많이 쓰기 때문에
  효율적인 일반 함수로 정의할 수 있다.
  ```scala
  def traverse[A,B](a: List[A])(f: A => Option[B]): Option[List[B]] = ???
  ```
- 그리고 `sequence`는 위에서 정의한 `traverse`로 구현할 수 있다.
- 스칼라에서 승급 함수가 흔히 쓰이기 때문에 `for` 함축 문법이 있다. 이것으로 `map2`를 구현 할 수 있다.
  ```scala
  // 원래 버전
  def map2[A,B,C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] =
      a flatMap (aa =>
        b map   (bb =>
                f(aa, bb)))

  // for 함축 버전
  def map2_for[A,B,C](a: Option[A], b: Option[B])(f: (A, B) => C): Option[C] = for {
    aa <- a
    bb <- b
  } yield f(aa, bb)
  ```

## Either 자료 형식

- 단순히 None 대신 에러에 대한 정보가 필요한 경우 `Either`를 쓸 수 있다.
  ```scala
  sealed trait Either[+E, +A]
  case class Left[+E](value: E) extends Either[E, Nothing]
  case class Right[+A](value: A) extends Either[Nothing, A]
  ```
- `Either` 자료 형식은 둘 중 하나인 값을 가진다.
- 성공 실패 값을 넣을 때는 영어의 옳다와 같은 `Right`를 성공에 `Left`를 실패에 사용한다.
- 스칼라 표준 라이브러리에 있는 `Option`과 `Either`에는 `sequence`, `traverse`, `map2`가 없다.
- `mean` 예제를 `Either`로 다시 만들어 보자.
  ```scala
  def mean(xs: IndexedSeq[Double]): Either[String, Double] =
    if (xs.isEmpty)
      Left("mean of empty list!")
    else
      Right(xs.sum / xs. length)
  ```
- 오류에 스택 정보를 넣을 수 있는데 이럴 때는 `Left`에 `Exception`을 넣으면 된다.
  ```scala
  def safeDiv(x: Int, y: Int): Either[Exception, Int] =
    try Right(x / y)
    catch { case e: Exception => Left(e) }
  ```
- 이것을 좀 추상화 해서 `Try`를 다시 만들어보자.
  ```scala
  def Try[A](a: => A): Either[Exception, A] =
    try Right(a)
    catch { case e: Exception => Left(e) }
  ```
- [연습문제] `Right` 값에 대해 작용하는 `map`, `flatMap`, `orElse`, `map2`를 `Either`에
  구현하라.
  ```scala
  sealed trait Either[+E, +A] {
    def map[B](f: A => B): Either[E, B]
    def flatMap[EE >: E, B](f: A => Either[EE, B]): Either[EE, B]
    def orElse[EE >: E, B](ob: => Either[EE, B]): Either[EE, B]
    def map2[EE >: E, B, C](b: Either[EE, B])(f: (A, B) => C): Either[EE, C]
  }
  ```
- 이런 함수가 있으면 `for` 함축을 쓸 수 있다.
  ```scala
  def parseInsuranceRateQuote(age: String, numberOfSpeedingTickets: String): Either[Exception, Double] = for {
    a       <- Try { age.toInt }
    tickets <- Try { numberOfSpeedingTickets.toInt }
  } yield insuranceRateQuote(a, tickets)
  ```
- [연습문제] `Either`에 대한 `sequence`와 `traverse` 만들기 (하나라도 오류가 있으면 첫번째 오류를 돌려준다)
  ```scala
  def sequence[E, A](es: List[Either[E, A]]): Either[E, List[A]] = ???
  def traverse[E, A, B](as: List[A])(f: A => Either[E, B]): Either[E, List[B]] = ???
  ```
- 다음은 `map2`를 사용한 예다. (`Person` 도메인 모델링)
  ```scala
  case class Person(name: Name, age: Age)
  sealed class Name(val value: String)
  sealed class Age(val value: Int)

  def mkName(name: String): Either[String, Name] =
    if (name == "" || name == null) Left("Name is empty.")
    else Right(new Name(name))

  def mkAge(age: Int): Either[String, Age] =
    if (age < 0) Left("Age is out of range.")
    else Right(new Age(age))

  def mkPerson(name: String, age: Int): Either[String, Person] =
    mkName(name).map2(mkAge(age))(Person(_, _))
  ```
- [연습문제] 여기서 둘다 유효하지 않을 때 두 오류를 모두 보고하게 하려면 어떻게 해야할까?

## 요약

- 이 장에 나온 고차 함수를 사용하면 예외는 거의 사용하지 않아도 된다.
- 다음 장에서는 비엄격성(non-strict)에 대해서 알아본다. (`default: => A`)
