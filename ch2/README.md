# 스칼라로 함수형 프로그래밍 시작하기

## 스칼라 언어의 소개: 예제 하나

```scala
object MyModule {
  def abs(n: Int): Int =
    if (n < 0) -n
    else n

  private def formatAbs(x: Int) = {
    val msg = "The absolute value of %d is %d"
    msg.format(x, abs(x))
  }

  def main(args: Array[String]): Unit =
    println(formatAbs(-42))
}
```

- 스칼라는 `static` 메서드가 없고 `object`로 표현되는 싱글톤 객체에 `static`에 해당하는 메서드를
  정의한다.
- `main`은 프로그램 시작점이다.
- `Unit`은 값이 없음을 표현한다.
- 함수의 마지막 값이 리턴 값이다.
- 함수 본문은 `=` 다음에 오고 여러 줄인 경우 `{}`로 묶을 수 있다.

## 프로그램의 실행

- `scalac`로 컴파일 하면 클래스 파일이 나온다. `java`나 `scala`로 실행할 수 있다.
  ```
  > scalac MyModule.scala
  > scala MyMoudle
  ```
- `scala`로 실행하면 프로그램을 바로 실행할 수 있다.
  ```
  > scala MyMoudle.scala
  ```
- `REPL`에서 실행 할 수 있다.
  ```scala
  > scala
  scala> :load MyModule.scala
  scala> MyModule.abs(-42)
  ```

## 모듈, 객체, 이름공간

- `MyModule`은 `abs`가 속한 이름공간(namespace)이다.
- 스칼라의 모든 값은 객체다.
- 이름공간을 제공하는 것이 주된 목적인 객체를 모듈이라고 부른다.
- `2 + 1`에 `+`는 특별한 연산자가 아니고 중의 표기법을 가진 메서드를 호출하는 것이다. `2.(+1)`과 같다.
- `import` 구문으로 현재 범위로 가져올 수 있다. 그럼 객체 이름을 생략할 수 있다.
  ```scala
  scala> import MyModule.abs
  scala> abs(-42)
  ```
- 객체에 있는 모든 멤버를 범위로 가져오려면 아래와 같이 한다.
  ```scala
  import MyModule._
  ```

## 고차 함수: 함수를 함수에 전달

- 스칼라에서 함수는 값이다. 이런 함수를 고차 함수라고 한다.

### 잠깐 곁가지: 함수적으로 루프 작성하기

- 아래는 `factorial` 함수다.
  ```scala
  def factorial(n: Int): Int = {
    def go(n:Int, acc: Int): Int =
      if (n <= 0) acc
      else go(n - 1, n * acc)

    go(n, 1)
  }
  ```

- 보통 루프용 보조 함수는 `go`, `loop` 이름을 많이 쓴다.
- 꼬리 재귀를 사용하면 스칼라 컴파일러는 `while` 구문으로 변환한다.
- 꼬리 재귀가 아닌 경우는 재귀를 하고 나서 다른 작업을 한 후 리턴하는 경우다. `1 + go(n -1, n * acc)`
- `@tailrec` 어노테이션을 쓰면 꼬리 재귀를 `while` 구문으로 변환하는 작업을 확신 할 수
  있다. 만약 꼬리 재귀가 아니라면 컴파일 오류를 발생한다. ??? 안되는 것 같은데
  ```scala
  import scala.annotation.tailrec

  def factorial(n: Int): Int = {
    @tailrec
    def go(n:Int, acc: Int): Int =
      if (n <= 0) acc
      else go(n - 1, n * acc)

    go(n, 1)
  }
  ```

- 피보나치 수 함수 작성하기
  ```scala
  def fib(n: Int): Int =
    n match {
      case 0 => 0
      case 1 => 1
      case n => fib(n - 2) + fib(n - 1)
    }
  ```

### 첫번째 고차 함수 작성

- `factorial` 함수를 사용하는 코드를 만들어 보자.
  ```scala
  private def formatAbs(x: Int) = {
    val msg = "The absolute value of %d is %d"
    msg.format(x, abs(x))
  }

  private def formatFactorial(n: Int) = {
    val msg = "The factorial of %d is %d"
    msg.format(n, factorial(n))
  }

  def main(args: Array[String]): Unit = {
    println(formatAbs(-42))
    println(formatFactorial(7))
  }
  ```
- `formatAbs`와 `formatFactorial`은 거의 비슷하기 때문에 사용하는 함수를 인자로 받는 하나의
  함수로 일반화해 보자.
  ```scala
  def formatResult(name: String, n: Int, f: Int => Int) = {
    val msg = "The %s of %d is %d"
    msg.format(name, n, f(n))
  }

  formatResult("absolute value", -42, abs)
  formatResult("factorial", 7, factorial)
  ```

## 다형적 함수: 형식에 대한 추상

- 위에서 만든 함수는 `Int`라는 특정 타입을 처리하는 단형적 함수다. 여러 타입을 처리할 수 있는 함수를
  다형적 함수라고 부른다.

### 다형적 함수의 예

- 배열에서 처음 찾은 곳의 인덱스를 리턴하는 `findFirst` 함수
  ```scala
  def findFirst(ss: Array[String], key: String): Int = {
    @tailrec
    def loop(n: Int): Int =
      if (n >= ss.length) - 1
      else if (ss(n) == key) n
      else loop(n + 1)

    loop(0)
  }
  ```

- 이 함수는 배열에 들어 있는 타입에 관계 없이 쓸 수 있는 함수기 때문에 다형적 함수로 바꿀 수 있다.
  ```scala
  def findFirst[A](as: Array[A], p: A => Boolean): Int = {
    @tailrec
    def loop(n: Int): Int =
      if (n >= as.length) - 1
      else if (p(as(n))) n
      else loop(n + 1)

    loop(0)
  }
  ```

- 이는 제너릭을 이용한 다형적 함수의 예다. 여기서 `A`는 타입 파라미터다.

### 익명 함수로 고차 함수 호출

- 이름이 없는 함수인 익명 함수로 고차 함수를 호출 할 수 있다.
  ```scala
  findFirst(Array(7, 9, 13), (x: Int) => x == 9)
  ```
- `Array()`는 `Array` 객체에 `apply` 메서드를 부른다.
- 스칼라가 타입을 추론할 수 있는 경우에는 익명함수 매개변수 목록에 타입을 생략할 수 있다.
  ```scala
  (x, y) => x < y
  ```
- 스칼라에서 값으로서의 함수
  ```scala
  val lessThan = new Function2[Int, Int, Boolean] {
    def apply(a: Int, b: Int) = a < b
  }

  val b = lessThan.apply(10, 20)
  val c = lessThan(2, 3)
  ```

## 형식에서 도출된 구현

- 함수의 형식(시그네처)에서 단 한가지 방식으로만 구현할 수 있는 함수가 있다. 부분 적용 함수인데 다음을 보자.
  ```scala
  def partail[A,B,C](a: A, f: (A, B) => C): B => C = ???
  ```
- 이 함수는 일단 B 형식을 인자로 받는 함수를 리턴해야 하기 때문에 아래와 같다.
  ```scala
  def partail[A,B,C](a: A, f: (A, B) => C): B => C =
    (b: B) => ???
  ```
- 그리고 남은 부부은 `f`에 `A`타입 값과 `B`타입 값을 넘긴 결과인 `C`타입을 리턴해야한다.
  ```scala
  def partail[A,B,C](a: A, f: (A, B) => C): B => C =
    (b: B) => f(a, b)
  ```
- 비슷한 함수로 `curry`, `uncurry`, `compose`는 다음과 같다.
  ```scala
  def curry[A,B,C](f: (A, B) => C): A => (B => C) =
    a => b => f(a, b)
  def uncurry[A,B,C](f: A => (B => C)): (A, B) => C =
    (a, b) => f(a)(b)
  def compose[A,B,C](f: B => C, g: A => B): A => C =
    a => f(g(a))
  ```
- 함수 함성 함수는 많이 쓰기 때문에 스칼라에서 제공하고 있고 순서를 반대로 함성하는 `andThen` 함수도 있다.
  ```scala
  val f = (x: Double) => math.Pi / 2 - x
  val cos = f andThen math.sin
  ```
- 종종 다형적 함수는 가능한 구현이 상당히 제한되서 형식을 따라가다 보면 구현에 이르는 경우가 많다.
- 이번 논리들은 대규모 프로그램이나 소규모 프로그램에 동일하게 적용된다.
