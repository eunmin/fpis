# 모노이드

- 오직 대수에 의해서만 정의되는 간단한 구조인 모노이드(monoid)에 대해 알아본다.

## 모노이드란 무엇인가?

- `"foo" + "bar"`는 `"foobar"`이고 항등원은 빈 문자열이다.
- 문자열 항등원과 문자열을 합치면 그 문자열이 나온다. `"" + s"`는 `s`와 같다.  
- 문자열 `r`, `s`, `t`는 `(r + s) + t`로 하나 `r + (s + t)`로 하나 같다. 결합 법칙을 만족한다.
- 정수도 똑같다. 다만 항등원이 0이다.
- 부울 연산자 &&는 결합적이고 항등원은 `true`다.
- 부울 연산자 ||는 결합적이고 항등원은 `false`다.
- 이런 종류의 대수를 모노이드라고 한다. 결합 법칙과 항등 법칙을 합쳐 모노이드 법칙이라고 한다.
   
```scala
trait Monoid[A] {
  def op(a1: A, a2: A): A
  def zero: A
}
```

- 위의 모노이드 정의가 실제 어떻게 동작하는지는 구현에 따라 다르다.
- 아래는 `String`에 대한 모노이드 인스턴스다.
  ```scala
  val stringMonoid = new Monoid[String] {
    override def op(a1: String, a2: String): String = a1 + a2
    override def zero: String = ""
  }
  ```
- 아래는 `List`에 대한 모노이드 인스턴스다.
  ```scala
  def listMonoid[A] = new Monoid[List[A]] {
    override def op(a1: List[A], a2: List[A]): List[A] = a1 ++ a2
    override def zero: List[A] = Nil
  }
  ```
 
- [연습문제] 아래 타입, 연산자의 모노이드 인스턴스를 만들어라
  ```scala
  val intAddition: Monoid[Int] = new Monoid[Int] {
    override def op(a1: Int, a2: Int): Int = a1 + a2
    override def zero: Int = 0
  }
  
  val intMultiplication: Monoid[Int] = new Monoid[Int] {
    override def op(a1: Int, a2: Int): Int = a1 * a2
    override def zero: Int = 1
  }
  
  val booleanOr: Monoid[Boolean] = new Monoid[Boolean] {
    override def op(a1: Boolean, a2: Boolean): Boolean = a1 || a2
    override def zero: Boolean = false
  }
  
  val booleanAnd: Monoid[Boolean] = new Monoid[Boolean] {
    override def op(a1: Boolean, a2: Boolean): Boolean = a1 && a2
    override def zero: Boolean = true
  }
  ```

- [연습문제] Option에 대한 모노이드 인스턴스를 만들어라.
  ```scala
  def optionMonoid[A]: Monoid[Option[A]] = new Monoid[Option[A]] {
    override def op(a1: Option[A], a2: Option[A]): Option[A] = a1 orElse a2
    override def zero: Option[A] = None
  }
  ```
  
- [연습문제] 인수와 반환값의 타입이 같은 함수를 자기함수(endofunction)이라고 한다. 자기 함수를 위한 모노이드 인스턴스를 만들어라.
  ```scala
  def endoMonoid[A]: Monoid[A => A] = new Monoid[A => A] {
    def op(f: A => A, g: A => A) = f compose g
    val zero = (a: A) => a
  }
  ```
- [연습문제] Prop를 이용해서 모노이드 법칙을 검사해보시오.
  ```scala
  def monoidLaws[A](m: Monoid[A], gen: Gen[A]): Prop =
    forAll(for {
      x <- gen
      y <- gen
      z <- gen
    } yield (x, y, z))(p =>
      m.op(p._1, m.op(p._2, p._3)) == m.op(m.op(p._1, p._2), p._3)) &&
    forAll(gen)((a: A) =>
      m.op(a, m.zero) == a && m.op(m.zero, a) == a)
  ``` 
  
- 모노이드만 가지고 유용한 것을 만들 수 있을까?
 
 ## 모노이드를 이용한 목록 접기
 
- 목록에 있는 `foldRight`와 `foldLeft`에 A 타입과 B 타입이 같다면 모노이드에 잘 맞는다.
  ```scala
  def foldRight(z: A)(f: (A, A) => A): A
  def foldLeft(z: A)(f: (A, A) => A): A
  ``` 
- 만약 문자열 목록이 있을 때 `stringMonoid`의 `op`와 `zero`만 넘겨주면 모든 문자열을 하나로 합칠 수 있다.
  ```scala
  val words = List("Hic", "Est", "Index")
  words.foldRight(stringMonoid.zero)(stringMonoid.op)
  ```
- 일반화해서 함수 `concatenate`를 만들 수 있다.
  ```scala
  def concatenate[A](as: List[A], m: Monoid[A]): A = 
    as.foldLeft(m.zero)(m.op)
  ```
- 목록에 있는 타입과 모노이드 타입이 안맞는다면 아래 함수로 맞출 수 있다.
  ```scala
  def foldMap[A,B](as: List[A], m: Monoid[B])(f: A => B): B =
    as.foldLeft(m.zero)((b, a) => m.op(b, f(a)))
  ```

## 결합법칙과 병렬성

- `foldLeft`나 `foldRight`는 왼쪽이나 오른쪽으로 접을 수 있다. 
  ```scala
  op(a, op(b, op(c, d)))
  
  op(op(op(a, b), c), d)
  ```
- 균형 잡기(balanced fold)는 아래 처럼 접을 수 있다. 또 병렬로 실행할 수 도 있다.
  ```scala
  op(op(a, b), op(c, d))
  ```
- [연습문제] IndexedSeq 자료구조에 대한 foldMap을 구현하라.
  ```scala
  def foldMapV(v: IndexedSeq[A], m: Monoid[B])(f: A => B): B
  ```
- [연습문제] 7장에서 만든 라이브러리로 병렬 foldMap을 구현하라. 
- [연습문제] foldMap을 이용해서 주어진 IndexedSeq[Int]가 정렬되어 있는지 점검하라.

## 예제 : 병렬 파싱

- 주어진 String의 단어 수를 병렬로 세는 예제
  ```scala
  "lorem ipsum dolor sit amet, "
  ```
- 단어 개수에 대한 대수적 자료 형식
  ```scala
  sealed trait WC
  case class Stub(chars: String) extends WC
  case class Part(lStub: String, words: Int, rStub: String) extends WC
  ```
- [연습문제] WC에 대한 모노이드 인스턴스를 만들어라.

## 접을 수 있는 자료구조

## 모노이드 합성