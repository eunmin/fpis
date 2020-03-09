# 순수 함수적 상태

- 이번 장은 난수 발생 예제로 임의의 상태가 있는 API 순수 함수적으로 만드는데 쓰이는 기본 패턴에 대해 알아본다.

## 부수 효과를 이용한 난수 발생

- 스칼라는 `scala.util.Random` 클래스에 있는 API로 난수를 만들 수 있다.
  ```scala
  val rng = new scala.util.Random

  rng.nextDouble // 0.005657172616017725
  rng.nextDouble // 0.5921687459641743
  rng.nextInt // -1186006268
  rng.nextInt(10) // 6
  ```
- 같은 함수를 불렀을 때 다른 값이 나오는 것으로 `rng` 안에 상태가 있다고 볼 수 있다. 이 함수는 참조
  투명하지 않다.
- 참조 투명하지 않은 함수는 여러가지 어려움이 있지만 그 중에 테스트에 대해 살펴보자. 만약 1에서 6사이의
  값을 돌려주는 주사위 굴리는 함수를 만든다고 치고 아래 처럼 잘 못 만들어서 0에서 5사이의 값이 나온다고
  가정하자.
  ```scala
  def rollDie: Int = {
    val rng = new scala.util.Random
    rng.nextInt(6)
  }
  ```
- 이 함수는 여섯번 중 다섯번은 테스트에 성공한다. 하지만 실패를 재현할 수 없다.
- 만약 난수 발생기를 인자로 전달한다고 해도 시드 값과 내부 상태를 알아야 재현 할 수 있기 때문에 어렵다.
  ```scala
  def rollDie(rng: scala.util.Random): Int = rng.nextInt(6)
  ```

## 순수 함수적 난수 발생

- 참조 투명성을 갖도록 만드려면 상태를 부수 효과 대신 명시적인 리턴 값으로 돌려 주면 된다.
  ```scala
  trait RNG {
    def nextInt: (Int, RNG)
  }
  ```
- 새 상태를 가진 `RNG`를 사용할지 말지는 `nextInt` 호출자가 결정한다. `RNG`가 어떻게 동작하는지
  노출하지 않는다는 점에서 여전히 캡슐화를 유지한다.
- 아래는 구현 예제다. 구현 내용은 중요하지 않고(linear congruential generator) 새로운 난수 발생기를 돌려준다는 점이 중요하다.
  ```scala
  case class SimpleRNG(seed: Long) extends RNG {
    def nextInt: (Int, RNG) = {
      val newSeed = (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL
      val nextRNG = SimpleRNG(newSeed)
      val n = (newSeed >>> 16).toInt
      (n, nextRNG)
    }
  }
  ```
- 아래는 사용 예다. 같은 난수 발생기로 함수를 부르면 항상 같은 값이 나온다.
  ```scala
  val rng = SimpleRNG(42)
  val (n1, rng2) = rng.nextInt
  n1 // 16159453

  val (n2, rng3) = rng2.nextInt
  n2 // -1281479697

  val (n3, rng4) = rng2.nextInt
  n3 // -1281479697
  ```

## 상태 있는 API를 순수하게 만들기

- 난수 발생기에서 쓴 방법은 다른 곳에도 쓸 수 있다.
  ```scala
  class Foo {
    private var s: FooState = ??
    def bar: Bar
    def baz: Int
  }
  ```
- `bar`과 `baz`가 `Foo`의 `s` 상태를 변경한다면 아래 처럼 만들 수 있다.
  ```scala
  trait Foo {
    def bar: (Bar, Foo)
    def baz: (Int, Foo)
  }
  ```
- 만약 앞의 예제에서 `RNG`를 재사용 한다면 같은 값이 나올 것이다. 
  ```scala
  def randomPair(rng: RNG): (Int, Int) = {
    val (i1, _) = rng.nextInt
    val (i2, _) = rng.nextInt
    (i1, i2)
  }
  ```
- 만약 안에서 다른 값을 내야한다면 아래 처럼 할 수 있을 것이다.
  ```scala
  def randomPair2(rng: RNG): ((Int, Int), RNG) = {
    val (i1, rng2) = rng.nextInt
    val (i2, rng3) = rng2.nextInt
    ((i1, i2), rng3)
  }
  ```
- 이 패턴은 반복되기 때문에 추출할 수 있는 부분이 있는지 살펴보자.
- [연습문제] `RNG.nextInt`가 `0`이상 `Int.MaxValue` 이하 난수가 생성되도록 함수를 작성하라.
  ```scala
  def nonNegativeInt(rng: RNG): (Int, RNG) = {
    val (i, rng2) = rng.nextInt
    (Math.abs(i), rng2)
  }
  ```
- [연습문제] `0`이상 `1`미만의 `Double` 난수를 발생하는 함수를 만들어라.
  ```scala
  def double(rng: RNG): (Double, RNG) = {
    val (i, rng2) = nonNegativeInt(rng)
    (i.toDouble / Int.MaxValue.toDouble, rng2)
  }
  ```
- [연습문제] 앞의 함수를 재사용해서 아래 함수를 만들어라.
  ```scala
  def intDouble(rng: RNG): ((Int, Double), RNG) = {
    val (n, rng2) = rng.nextInt
    val (d, rng3) = double(rng2)
    ((n,d), rng3)
  }
  
  def doubleInt(rng: RNG): ((Double, Int), RNG)
  def double3(rng: RNG): ((Double, Double, Double), RNG)
  ```
- [연습문제] 난수 목록을 만드는 함수를 만들어라.
  ```scala
  def ints(count: Int)(rng: RNG): (List[Int], RNG)
  ```

## 상태 동작을 위한 더 나은 API

- 앞에 함수는 `RNG => (A, RNG)`로 상태를 변이하는 패턴을 가지고 있기 때문에 `combinator`를 이용해서
  추상화 하면 상태를 계속 전달하지 않아도 된다. 먼저 이 형식의 타입 앨리어스를 만들자.
  ```scala
  type Rand[+A] = RNG => (A, RNG)
  ```
- 이제 `nextInt`를 새로운 형식으로 정의해보자.
  ```scala
  trait RNG {
    def nextInt: (Int, RNG)
  }
  
  val int: Rand[Int] = _.nextInt
  ```
- 여러가지 조합기를 만들어보자. 먼저 값을 받아서 그대로 전달하는 `unit`을 만들어보자.
  ```scala
  def unit[A](a: A): Rand[A] =
    rng => (a, rng)
  ```
- 다음은 상태 동작의 출력을 바꾸지만 상태 자체는 유지하는 `map` 함수다.
  ```scala
  def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    rng => {
      val (a, rng2) = s(rng)
      (f(a), rng2)
    }
  ```
- `map`으로 `nonNegativeEven`를 만들면 아래와 같다.
  ```scala
  def nonNegativeEven: Rand[Int] =
    map(nonNegativeInt)(i => i - i % 2)
  ```
- [연습문제] `map`으로 `double`을 만들어보라.
  ```scala
  def double: Rand[Double] =
    map(nonNegativeInt)(i => i.toDouble / Int.MaxValue.toDouble)
  ```

### 상태 동작들의 조합

- [연습문제] `map`으로는 `intDouble`이나 `doubleInt`를 구현할 수 없다. 그래서 `map2`를 만들어보자.
  ```scala
  def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    rng => {
      val (a, rng2) = ra(rng)
      val (b, rng3) = rb(rng2)
      (f(a,b), rng3)
    }
  ```
- `map2`로 `A`, `B` 타입의 상태 발생 동작이 있다면 함께 발생하게 하는 함수도 만들 수 있다.
  ```scala
  def both[A,B](ra: Rand[A], rb: Rand[B]): Rand[(A,B)] =
    map2(ra, rb)((_, _))
  ```
- `both`로 `intDouble`과 `doubleInt`를 만들어보자.
  ```scala
  def randIntDouble: Rand[(Int, Double)] =
    both(int, double)

  def randDoubleInt: Rand[(Double, Int)] =
    both(double, int)
  ```
- [연습문제] `map2`를 확장해서 상태 변환 여러개를 받을 수 있는 `sequence`를 만들고 `ints`를 다시
  만들어봐라.

### 내포된 상태 동작

- `map`과 `map2`로 할 수 없는 구현이 있는데 `0`부터 `n`미만의 난수를 생성하는 `nonNegativeLessThan`이다.
  ```scala
  def nonNegativeLessThan(n: Int): Rand[Int]
  ```
- 쉬운 방법중 하나는 난수를 `n`으로 나누는 방법이다.
  ```scala
  def nonNegativeLessThan(n: Int): Rand[Int] =
    map(nonNegativeInt) { _ % n }
  ```
- 하지만 `n`으로 나누어 떨어지지 않는다면 난수들이 치우치므로 재귀적으로 난수를 다시 발생시켜야 한다.
  ```scala
  def nonNegativeLessThan(n: Int): Rand[Int] =
    map(nonNegativeInt) { i =>
      val mod = i % n
      if (i + (n-1) - mod >= 0) mod else nonNegativeLessThan(n)(????)
    }
  ```
- 그런 문제는 `nonNegativeLessThan`를 부를 때 이 함수의 리턴 값이 `Int`가 아니고 `Rand[Int]`라는 점과
  부를 때 상태를 유지하려면 새로운 `rng`를 넘겨줘야한다. 그래서 `map`을 사용할 수 없고 직접 구현해야한다.
  ```scala
  def nonNegativeLessThan(n: Int): Rand[Int] = { rng =>
    val (i, rng2) = nonNegativeInt(rng)
    val mod = i % n
    if (i + (n-1) - mod >= 0)
      (mod, rng2)
    else nonNegativeLessThan(n)(rng2)
  }
  ```
- 이런 것을 해주는 함수가 있다면 더 좋은데 그것은 `flatMap`이다.
- [연습문제] `flatMap`을 만들고 `nonNegativeLessThan`을 만들어라
  ```scala
  def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (x, rng2) = f(rng)
      g(x)(rng2)
    }
  ```
- [연습문제] `map`과 `map2`를 `flatMap`으로 만들어라
  ```scala
  def map_[A,B](s: Rand[A])(f: A => B): Rand[B] =
    flatMap(s)(x => unit(f(x)))
  ```
- 처음에 나왔던 `rollDie` 함수 오류를 잡아보자.
  ```scala
  def rollDie: Rand[Int] = nonNegativeLessThan(6)
  ```
- 아직도 `0`이 나오지만 이제 `0`이 나오는 생성기를 찾을 수 있다.
  ```scala
  val zero = rollDie(SimpleRNG(5))._1
  ```
- 그리고 이 생성기로 테스트를 만들고 오류를 수정할 수 있다.
  ```scala
  def rollDie: Rand[Int] = map(nonNegativeLessThan(6))(_ + 1)
  ```

## 일반적 상태 동작 자료 형식

- 지금까지 만든 난수 발생 함수들(`unit`, `map`, `map2`, `flatMap`, `sequence`)는 일반화 할 수
  있다.
- `map`은 다음과 같다.
  ```scala
  def map[S,A,B](a: S => (A,S))(f: A => B): S => (B,S)
  ```
- 그리고 `Rand` 대신 더 일반적인 형식을 정의 해보자.
  ```scala
  type State[S,+A] = S => (A,S)
  ```
- `State`는 상태를 유지하는 계산(상태 동작, 상태 전이)를 나타낸다. 심지어는 명령문을 나타낸다고 할 수도 있다.
- 다음 처럼 독립적인 클래스도 만들어도 좋다.
  ```scala
  case class State[S,+A](run: S => (A,S))
  ```
- `Rand`는 `State`로 다시 정의할 수 있다.
  ```scala
  type Rand[A] = State[RNG, A]
  ```
- [연습문제] `unit`, `map2`, `flatMap`, `sequence`를 일반화 하라. `State` 메서드나 동반객체로
  만들어라.

## 순수 함수적 명령식 프로그래밍

- `State`는 상태 동작을 실행하고 결과를 배정하고 다시 다른 상태 동작을 실행하고 결과를 배정하는 방식으로
  동작하고 그것은 명령식 프로그래밍과 비슷하다. 다음 예제를 보자.
  ```scala
  val ns: Rand[List[Int]] =
    int.flatMap(x =>
      int.flatMap(y =>
        ints(x).map(xs =>
          xs.map(_ % y))))
  ```
- 이 코드는 `for` 함축으로 아래처럼 쓸 수 있다. 그리고 더 읽기 쉽다.
  ```scala
  val ns: Rand[List[Int]] = for {
    x <- int
    y <- int
    xs <- ints(x)
  } yield xs.map(_ % y)
  ```
- 명령형 프로그램과 비슷하게 하기 위해 상태를 읽는 `get`과 `set`이 있으면 명령형 프로그래밍의 스타일을
  표현할 수 있다.
  ```scala
  def modify[S](f: S => S): State[S, Unit] = for {
    s <- get
    _ <- set(f(s))
  } yield ()
  ```
- `get`, `set`은 아래와 같이 만들 수 있다.
  ```scala
  def get[S]: State[S, S] = State(s => (s, s))

  def set[S](s: S): State[S, Unit] = State(_ => ((), s))
  ```

- [연습문제] 사탕 판매기 예제 (설명 책 참조)

## 요약

- 순수 함수적 방식으로 상태를 가진 프로그램을 작성하는 패턴에 대해 알아봤다.
