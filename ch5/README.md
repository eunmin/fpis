# 엄격성과 나태성

- 리스트 자료구조를 함수로 처리할 때 비효율적일 수 있다.
  ```scala
  List(1,2,3,4).map(_ + 10).filter(_ % 2 == 0).map(_ * 3)
  ```
- 위 예제에서 각 처리 단계 마다 새로운 목록이 생긴다.
- `while`문으로 작성할 수 있지만 고수준의 합성 스타일을 유지하는 것이 바람직한 방식이다.
- 비엄격성(non-strictness 또는 lzniness)를 이용하면 이런 루프 융합이 가능하다.
- 비엄격성이 함수적 프로그램의 효율성과 모듈성을 개선하는 근본적인 기법임을 볼 수 있다.

## 엄격한 함수와 엄격하지 않은 함수

- 엄격하지 않다는 것은 함수가 하나 이상의 인수를 평가하지 않을 수 도 있다는 것을 말한다.
- 대부분 언어는 엄격한 함수가 기본이고 스칼로도 기본적으로는 엄격한 함수가 기본이다.
  ```scala
  def square(x: Double): Double = x * x
  ```
- 위 함수에서 `square(sys.error("failure"))`를 호출 하면 `square`를 수행하기도 전에 예외가
  발생한다.
- `&&`와 `||`는 비엄격하게 동작한다.
  ```scala
  scala> false && { println("!!"); true } // !!가 출력되지 않는다.
  res0: Boolean = false

  scala> true || { println("!!"); false } // !!가 출력되지 않는다.
  res1: Boolean = true
  ```
- `if`도 역시 비엄격하게 동작한다.
  ```scala
  val result = if (input.isEmpty) sys.error("empty input") else input
  ```
- `if`가 인자가 3개인 함수(조건, 참 값, 거짓 값)로 보면 조건에 따라 참 값 또는 거짓 값은 평가되지 않는다.
- 인자를 함수를 감싸서 넘겨 비엄격하게 만들 수 있다.
  ```scala
  def if2[A](cond: Boolean, onTrue: () => A, onFlase: () => A): A =
    if (cond) onTrue() else onFlase()

  if2(a < 22,
    () => println("a"),
    () => println("b")
  )
  ```
- 표현식에서 평가되지 않은 형태를 성크(thunk)라고 부른다. 스칼라에서는 이런 형태를 문법적으로 지원한다.
  ```scala
  def if3[A](cond: Boolean, onTrue: => A, onFlase: => A): A =
    if (cond) onTrue else onFlase

  if3(false, sys.error("fail"), 3) // 3
  ```
- 평가되지 않은 채로 전달된 함수는 참조할 때마다 평가되고 기본적으로 캐싱하지 않는다.
  ```scala
  def maybeTwice(b: Boolean, i: => Int) = if (b) i + i else 0

  scala> val x = maybeTwice(true, { println("hi"); 1 + 41 })
  hi
  hi
  x: Int = 84
  ```
- 캐싱하려면 `lazy`로 선언된 곳에 할당하면 된다.
  ```scala
  def maybeTwice2(b: Boolean, i: => Int) = {
    lazy val j = i
    if (b) j + j else 0
  }

  val x = maybeTwice2(true, { println("hi"); 1 + 41 })
  scala> val x = maybeTwice2(true, { println("hi"); 1 + 41 })
  hi
  x: Int = 84
  ```
- `lazy`는 `lazy` 선언 우변 평가를 우변이 처음 참조될 때가지 지연하고 캐싱한다.
- 스칼라는 비엄격 함수의 인수는 값으로 전달이 아니고 이름으로 전달된다.
- 어떤 표현식의 평가가 무한히 실행되거나 오류를 던진다면 그런 표현식을 종료되지 않은 표현식 또는 바닥으로
  평가되는 표현식이라고 하는데 이렇게 평가되는 모든 x에 대해 f(x)가 바닥으로 평가되면 이런 함수 f가
  엄격한 함수라고 한다. ???

## 확장 에제: 게으른 목록

- 게으른(lazy) 목록 또는 스트림이라고 부르는 자료구조로 함수적 프로그램의 효율성과 모듈성을 개선할 수 있다.
- 아래는 `Stream`의 간단한 정의다.
  ```scala
  sealed trait Stream[+A]
  case object Empty extends Stream[Nothing]
  case class Cons[+A](h: () => A, t: () => Stream[A]) extends Stream[A]

  object Stream {
    def cons[A](hd: =>A, tl: => Stream[A]): Stream[A] = {
      lazy val head = hd
      lazy val tail = tl
      Cons(() => head, () => tail)
    }

    def empty[A]: Stream[A] = Empty // 리턴타입이 Stream[A]인 것이 타입 추론에 도움이 된다.

    def apply[A](as: A*):Stream[A] =
      if (as.isEmpty) empty else cons(as.head, apply(as.tail: _*))
  }
  ```
- 기존의 리스트와 거의 비슷하지만 성크를 인자로 받는 다는 점이 다르다.
- 아래는 `Stream`에서 첫번째 항목을 가져오는 함수다.
  ```scala
  sealed trait Stream[+A] {
    def headOption: Option[A] = this match {
      case Empty => None
      case Cons(h, t) => Some(h())
    }
  }
  ```

### 스트림의 메모화를 통한 재계산 피하기

- `Cons`노드가 평가되면 그 값은 캐싱해두는 것이 좋다.
  ```scala
  val x = Cons(() => expensive(x), tl)
  val h1 = x.headOption
  val h2 = x.headOption // 다시 계산할 필요가 없다.
  ```
- 이런 문제는 스마트 생성자라고 부르는 곳에서 메모화한다. 관례로 생성자의 첫 글자를 소문자로 바꾼 함수를
  이름을 사용한다.
  ```scala
  def cons[A](hd: =>A, tl: => Stream[A]): Stream[A] = {
    lazy val head = hd
    lazy val tail = tl
    Cons(() => head, () => tail)
  }
  ```
- 그리고 `Stream.apply`에서 `cons`를 써서 쉽게 만들 수 있게 한다.

### 스트림의 조사를 위한 보조 함수들

- [연습문제] `Stream`을 `List`로 바꿔 평가를 강제하는 `toList`를 만들어보자.
  ```scala
  def toList: List[A]
  ```
- [연습문제] `take(n)`과 `drop(n)`을 만들어보자.
- [연습문제] `takeWhile`을 만들어보자.
  ```scala
  def takeWhile(p: A => Boolean): Stream[A]
  ```

## 프로그램 서술과 평가의 분리

- 일급 함수는 계산을 본문에 담고 있지만 계산은 인수들이 전달 되어야 실행된다.
- `Option`은 오류가 발생했다는 사실을 담고 있고 무엇을 수행하는 가는 분리된 관심사다.
- `Stream`은 순차열을 생성하는 계산을 구축하되 계산 단계의 실행은 필요할 때까지 미룰 수 있다.
- 일반적으로 나태성으로 표현식의 서술과 평가를 분리할 수 있다. 더 큰 서술에서 필요한 부분 만 평가 할 수 있다.
- 다음은 `exist`의 예다.
  ```scala
  def exists(p: A => Boolean): Boolean = this match {
    case Cons(h, t) => p(h()) || t().exists(p)
    case _ => false
  }
  ```
- 만약 `p(h())`가 `true`면 `||` 뒤는 평가하지 않는다.
- `exist`는 `foldRight`으로 만들 수도 있다.
  ```scala
  def foldRight[B](z : => B)(f: (A, => B) => B): B =
    this match {
      case Cons(h, t) => f(h(), t().foldRight(z)(f))
      case _ => z
    }

  def exists(p: A => Boolean): Boolean =
    foldRight(false)((a, b) => p(a) || b)
  ```
- `foldRight`로 만든 `exists`는 `b` 값이 사용되지 않으면 인수 위치에서 평가되지 않기 때문에
  첫번째로 조건에 맞는 항목이 있으면 거기까지만 실행된다.
- `foldRight`의 엄격한 버전으로는 조기 종료를 할 수 없다.
- [연습문제] 모든 항목이 조건을 만족하는지 체크하는 `forAll`을 만들어보시오.
  ```scala
  def forAll(p: A => Boolean): Boolean
  ```
- [연습문제] `foldRight`로 `takeWhile`을 만드시오.
- [연습문제] `foldRight`으로 `headOption`을 만드시오.
- [연습문제] `foldRight`으로 `map`, `filter`, `append`, `flatMap`을 만드시오. `append`는
  인수에 대해 엄격하지 않아야한다.
- 이 함수들은 전체 결과를 생성하지 않는다. 그래서 중간 결과를 완전히 만들지 않아도 함수를 연이어 호출 할 수 있다.
- 아래 예제를 추적해보자.
  ```scala
  Stream(1,2,3,4).map(_ + 10).filter(_ % 2 == 0).toList
  ```
  ```scala
  sealed trait Stream[+A] {
    def headOption: Option[A] = this match {
      case Empty => None
      case Cons(h, t) => Some(h())
    }
    def toList: List[A] = this match {
      case Empty => Nil
      case Cons(h, t) => h() :: t().toList
    }
    def foldRight[B](z : => B)(f: (A, => B) => B): B =
      this match {
        case Cons(h, t) => f(h(), t().foldRight(z)(f))
        case Empty => z
      }
    def map[B](f: A => B): Stream[B] =
      foldRight(Stream.empty[B])((a, b) => Stream.cons(f(a), b))

    def filter(f: A => Boolean): Stream[A] =
      foldRight(Stream.empty[A])((a, b) => if (f(a)) Stream.cons(a, b) else b)
  }
  
  object Stream {
    def cons[A](hd: => A, tl: => Stream[A]): Stream[A] = {
      lazy val head = hd
      lazy val tail = tl
      Cons(() => head, () => tail)
    }

    def empty[A]: Stream[A] = Empty

    def apply[A](as: A*):Stream[A] =
      if (as.isEmpty) empty else cons(as.head, apply(as.tail: _*))
  }
  ```
  ```scala
  Stream(1,2,3,4).map(_ + 10).filter(_ % 2 == 0).toList
  cons(11, Stream(2,3,4).map(_ + 10)).filter(_ % 2 == 0).toList
  Stream(2,3,4).map(_ + 10).filter(_ % 2 == 0).toList
  cons(12, Stream(3,4).map(_ + 10)).filter(_ % 2 == 0).toList
  12 :: Stream(3,4).map(_ + 10).filter(_ % 2 == 0).toList
  12 :: cons(13, Stream(4).map(_ + 10)).filter(_ % 2 == 0).toList
  12 :: Stream(4).map(_ + 10).filter(_ % 2 == 0).toList
  12 :: cons(14, Stream().map(_ + 10)).filter(_ % 2 == 0).toList
  12 :: 14 :: Stream().map(_ + 10).filter(_ % 2 == 0).toList
  12 :: 14 :: List()
  ```
- 추적 결과에서 보듯이 `map`과 `filter`가 번갈아 가며 실행된다.
- 스트림을 일급 루프라고 부르기도 한다.
- `find`라는 함수도 `filter`로 구현할 수 있다.
  ```scala
  def find(p: A => Boolean): Option[A] =
    filter(p).headOption
  ```
- 메모리 사용량도 줄일 수 있다. 자세한 내용은 4부에서 다룬다.

## 무한 스트림과 공재귀

- 스트림으로 무한 스트림도 만들 수 있다.
  ```scala
  val ones: Stream[Int] = Stream.cons(1, ones)

  ones.take(5).toList // List(1,1,1,1,1)

  ones.exists(_ % 2 != 0) // true

  ones.map(_ + 1).exists(_ % 2 == 0)

  ones.takeWhile(_ == 1)

  ones.forAll(_ != 1)

  ones.forAll(_ == 1) // 무한히 계산하다 스택 오버플로우가 난다.
  ```
- [연습문제] 무한 값을 만드는 `constant` 함수를 만들어보자.
  ```scala
  def constant[A](a: A): Stream[A]
  ```
- [연습문제] `n`에서 시작해서 무한이 1씩 증가하는 함수를 만들어보자.
  ```scala
  def from(n: Int): Stream[Int]
  ```
- [연습문제] 무한 피보나치를 만들어보자.
- [연습문제] 초기 값과 생성 산술 함수를 받아 무한 스트림을 만드는 `unfold` 함수를 만들어보자.
  ```scala
  def unfold[A,S](z: S)(f: S => Option[(A,S)]): Stream[A]
  ```
- `Option`은 종료 시점이 있는 경우에 `None`을 넘겨서 종료 시킬 수 있다.
- `unfold`는 공재귀 함수의 예다.
- 재귀 함수는 자료를 소비하지만 공재귀 함수는 자료를 생산한다.
- 공재귀를 보호되는 재귀(guarded recursion)이라고 부르고 생산성을 공종료(cotermination)이라고
  부른다.
- [연습문제] `unfold`로 `fibs`, `from`, `constant`, `ones`를 만들어라.
- [연습문제] `unfold`로 `map`, `take`, `takeWhile`, `zipWith`, `zipAll`을 만들어라
  `zipAll`은 스트림에 항목이 있는 한 계속 순회해햐한다. 스트림이 소진 여부는 `Option`으로 지정한다.
  ```scala
  def zipAll[B](s2: Stream[B]): Stream[(Option[A], Option[B])]
  ```
- [연습문제] 주어진 인수의 스트림으로 시작하는지 여부를 판단하는 `startsWith`를 구현하라.
  ```scala
  def startsWith[A](s: Stream[A]): Boolean
  ```
- [연습문제] `unfold`로 `tails`를 만들어라. `tails`는 `Stream(1,2,3)`을 받으면
  `Stream(Stream(1,2,3), Stream(2,3), Stream(3), Stream())`를 리턴한다.
  ```scala
  def tails: Stream[Stream[A]]
  ```
- 3장 끝에 나왔던 `hasSubsequence`는 위 함수로 만들 수 있다.
  ```scala
  def hasSubsequence[A](s: Stream[A]): Boolean =
    tails exists (_ startsWith s)
  ```
- 위 함수는 효율성을 유지한다.
- [연습문제] `tails`를 일반화한 `scanRight` 함수를 만들어라.
  ```scala
  scala> Stream(1,2,3).scanRight(0)(_ + _).toList
  res0: List[Int] = List(6,5,3,0)
  ```

## 요약

- 비엄격성은 서술과 평가를 분리함으로서 모듈성을 증가시킨다.
- 그러면 표현식의 서술을 재사용할 수 있다.
