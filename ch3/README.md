# 함수적 자료구조

## 함수적 자료구조의 정의

- 순수 함수로 조작되는 자료구조로 immutable 하다.
- 두 리스트를 연결하면 새로운 리스트가 나온다.
- 단일 연결 목록(singly linked list)의 예
  ```scala
  sealed trait List[+A]
  case object Nil extends List[Nothing]
  case class Cons[+A](head: A, tail: List[A]) extends List[A]

  object List {
    def sum(ints: List[Int]): Int = ints match {
      case Nil => 0
      case Cons(x, xs) => x + sum(xs)
    }

    def product(ds: List[Double]): Double = ds match {
      case Nil => 1.0
      case Cons(0.0, _) => 0.0
      case Cons(x, xs) => x * product(xs)
    }

    def apply[A](as: A*): List[A] =
      if(as.isEmpty) Nil
      else Cons(as.head, apply(as.tail: _*))
  }
  ```
- `trait`은 인터페이스 구문이고 필요하다면 메서드 구현도 담을 수 있다.
- `sealed`는 인터페이스의 모든 구현이 이 안에 있어야 한다는 뜻이다.
- `case`로 시작하는 두 줄은 List가 가질 수 있는 타입을 뜻한다.
- `+A`는 covariant로 Dog이 Animal의 하위 타입이면 List[Dog]도 List[Animal]의 하위 타입으로
  다룬다는 뜻이다. Nothing은 모든 형식의 하위 형식이기 때문에 List[Nothing]은 List[Int] 하위 형식이다.
- 리스트 생성 예제
  ```scala
  val ex1: List[Double] = Nil
  val ex2: List[Int] = Cons(1, Nil)
  val ex3: List[String] = Cons("a", Cons("b", Nil))
  ```
- `sum`, `product`는 패턴 부합을 사용한다.

## 패턴 부합

- 스칼라에서 자료 형식 선언과 그 형식의 같은 이름의 `object`를 선언하고 그 안에 자료 형식을 다루는 함수를
  넣는 경우가 많은데 이 때 객체를 동반(companion) 객체라고 한다.
- `sum`, `product`는 패턴 부합을 사용한다.
  ```scala
  def sum(ints: List[Int]): Int = ints match {
    case Nil => 0
    case Cons(x, xs) => x + sum(xs)
  }

  def product(ds: List[Double]): Double = ds match {
    case Nil => 1.0
    case Cons(0.0, _) => 0.0
    case Cons(x, xs) => x * product(xs)
  }
  ```
- 몇 가지 패턴 매칭의 예
  ```scala
  List(1,2,3) match { case _ => 42 } // 42
  List(1,2,3) match { case Cons(h, _) => h } // 1
  List(1,2,3) match { case Cons(_, t) => t } // List(2,3)
  List(1,2,3) match { case Nil => 42 } // MatchError
  List(1,2,3) match { case Cons(x, Cons(y, _)) => x + y } // 3
  ```
- 스칼라의 가변 인수 함수
  ```scala
  def apply[A](as: A*): List[A] =
      if (as.isEmpty) Nil
      else Cons(as.head, apply(as.tail: _*))
  ```
- as는 `Seq[A]`로 넘어온다. `_*`라는 특별한 타입 형식으로 가변 인수에 `Seq[A]`를 전달할 수 있게 해준다.

## 함수적 자료구조의 자료 공유

- 불변 데이터를 쓰면서 리스트를 변경할 필요가 있다면 새로운 리스트를 만들어야한다. 이때 자료 전체를 복사하지
  않고 자료를 공유해서 준다. 예를 들면 List의 첫 요소를 제거해야 한다면 `Cons(x, xs)`에서 xs를 리턴하면
  된다.

### 자료 공유의 효율성

- `tail`과 `dropWhile`을 구현하는 연습문제
  ```scala
  def drop[A](l: List[A], n: Int): List[A] = ???
  def dropWhile[A](l: List[A], f: A => Boolean): List[A] = ???
  ```
- 자료 공유를 이용하면 더 효율적인 연산을 할 수 있다.
  ```scala
  def append[A](a1: List[A], a2: List[A]): List[A] =
    a1 match {
      case Nil => a2
      case Cons(h, t) => Cons(h, append(t, a2))
    }
  ```
- 위 예제에서 `a1`과 `a2`를 더할 때 `a1`의 항목만 `a2`에 붙여 주면 되기 때문에 `a1` 개수의 시간이 걸린다.
  만약 두개의 배열을 이용해서 모든 항목을 결과 배열에 복사해야 한다면 `a1`, `a2` 길이에 시간이 걸린다.
- 하지만 마지막 항목을 제외한 모든 항목을 리턴하는 `init` 같은 함수는 `tail` 처럼 상수 시간으로 구현할
  수 없다.
  ```scala
  def init[A](l: List[A]): List[A]
  ```
- 스칼라에 Vector 자료 구조는 상수 시간의 임의 접근 갱신, head, tail, init, 항목 추가를 지원한다.

### 고차 함수를 위한 형식 추론 개선

- `dropWhile` 함수를 쓸 때 `f`에 넘기는 인자의 형식을 지정해야 한다.
  ```scala
  val xs: List[Int] = List(1,2,3,4,5)
  val ex1 = dropWhile(xs, (x: Int) => x < 4)
  ```
- 만약 두 인수를 다음과 같이 두 그룹으로 나눠서 만들면 스칼라가 타입을 추론할 수 있게 된다. (커링 버전)
  ```scala
  def dropWhile[A](as: List[A])(f: A => Boolean): List[A] =
    as match {
      case Cons(h, t) if f(h) => dropWhile(t)(f)
      case _ => as
    }
  ```
- 이제 익명 함수 인자 타입을 지정하지 않고 쓸 수 있다.
  ```scala
  val xs: List[Int] = List(1,2,3,4,5)
  val ex1 = dropWhile(xs)(x => x < 4)
  ```
- 이렇게 형식 추론이 최대로 일어나도록 함수 인수들을 적절한 순서로 묶는 경우가 많다.

## 목록에 대한 재귀와 고차 함수로의 일반화

- `sum`과 `product`는 유사하기 때문에 표현식을 함수로 추출해서 적용해보자.
  ```scala
  def foldRight[A, B](as: List[A], z: B)(f: (A, B) => B): B =
   as match {
     case Nil => z
     case Cons(h, t) => f(h, foldRight(t, z)(f))
   }

  def sum2(ns: List[Int]) =
    foldRight(ns, 0)((x, y) => x + y)

  def product2(ns: List[Double]) =
    foldRight(ns, 1.0)(_ * _)
  ```

- 익명 함수를 위한 밑줄 표기법 (적당히 사용할 것)
  ```scala
  _ + _ // (x,y) => x + y
  _ * 2 // x => x * 2
  _.head // xs => xs.head
  _ drop _ // (xs,n) => xs.drop(n)
  ```

- `foldRight`의 축약 과정
  ```scala
  foldRight(Cons(1, Cons(2, Cons(3, Nil))), 0)((x,y) => x + y)
  1 + foldRight(Cons(2, Cons(3, Nil)), 0)((x,y) => x + y)
  1 + (2 + foldRight(Cons(3, Nil), 0)((x,y) => x + y))
  1 + (2 + (3 + foldRight(Nil, 0)((x,y) => x + y)))
  1 + (2 + (3 + (0)))
  6
  ```

- `foldRight`로 구현된 `product`는 0.0을 만났을 때 바로 멈추는가? 이 문제에 대해서는 5장에서 다룬다.
- `foldLeft`를 꼬리 재귀적으로 만들어라.

### 그 외의 목록 조작 함수들

```scala
def map[A,B](as: List[A])(f: A => B): List[B]

def filter[A](as: List[A])(f: A => Boolean): List[A]

def flatMap[A,B](as: List[A])(f: A => List[B]): List[B]

// flatMap(List(1,2,3))(i => List(i,i)) = List(1,1,2,2,3,3)
```

- `flatMap`을 이용해서 `filter`를 구현하기
- 리스트 두개를 받아서 하나로 합치는 `zipWith`를 만들어 봐라.
- 스칼라 표준 라이브러리에도 List가 있는데 `Cons`는 `::`라서 `1 :: 2 :: Nil`로 리스트를 만들고
  `List(1,2)`로 만들 수 도 있다. 또 `case Cons(h,t)`는 `case h :: t`로 쓸 수 있다.
- 그외 다양한 리스트 표준 라이브러리가 있다. 자세한 것은 API 목록을 참조
  ```scala
  def take(n: Int): List[A]

  def takeWhile(f: A => Boolean): List[A]

  def forall(f: A => Boolean): Boolean // 모든 요소가 f를 만족할 때 true

  def exists(f: A => Boolean): Boolean
  ```
- `scanLeft`, `scanRight`는 `foldLeft`, `foldRight`와 비슷하지만 최종 결과가 아니고 부분 결과
  List를 돌려준다.

### 단순 구성요소들로 목록 함수를 조립할 때의 효율성 손실

- 이런 범용 함수로 다양한 일을 할 수 있지만 구현이 항상 효율적인 것은 아니다. 같은 입력을 여러번 훑거나
  빠르게 종료하기 위해 재귀 루프를 만들어야 할 수 도 있다.

## 트리

- List는 대수적 자료 형식(Algebraic Data Type)라고 부른다. ADT는 합 타입(인자)과 곱 타입(extends)으로 표현된다.
- 곱하기(타입1 x 타입2) : 타입1, 타입2 조합 가능한 모든 페어
- 더하기(타입1 + 타입2) : 타입1, 타입2 둘 중 하나
- 스칼라의 튜플도 ADT와 동일한 방식으로 작동한다.
  ```scala
  val p = ("Bob", 42)
  p._1
  p._2
  p match { case (a,b) => b }
  ```
- `("Bob", 42)`는 `(String, Int)` 쌍 타입이며 `Tuple2[String, Int]` 타입과 같다.
- 대수적 자료 형식으로 이진 트리 자료 구조를 정의해 보자.
  ```scala
  sealed trait Tree[+A]
  case class Leaf[A](value: A) extends Tree[A]
  case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]
  ```
- ADT는 내부 구조를 모두 `public`으로 노출하므로 캡슐화를 위반한다고 생각할 수 있지만 데이터 변경이
  불가능하기 때문에 문제가 되지 않는다.
