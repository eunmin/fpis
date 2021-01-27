# 모노이드

- 오직 대수에 의해서만 정의되는 간단한 구조인 모노이드(monoid)에 대해 알아본다.

## 모노이드란 무엇인가?

- `"foo" + "bar"`는 `"foobar"`이고 항등원은 빈 문자열이다.
- 문자열 항등원과 문자열을 합치면 그 문자열이 나온다. `"" + s"`는 `s`와 같다.  
- 문자열 `r`, `s`, `t`는 `(r + s) + t`로 하나 `r + (s + t)`로 하나 같다. 결합 법칙을 만족한다.
  ```kotlin
    "foo" + "bar"
    // "foobar"

    "foo" + ""
    // "foo"
    // "": identity

    ("One" + "Two") + "Three"
    // "OneTwoThree"

    "One" + ("Two" + "Three")
    // "OneTwoThree"
  ```
- 정수 덧셈과 곱셈도 똑같다.
  ```kotlin
  1 + 2
  // 3

  1 + 0
  // 1
  // 0: identity

  (1 + 2) + 3
  // 6

  1 + (2 + 3)
  // 6


  2 * 3
  // 6

  2 * 1
  // 2
  // 1: identity

  (2 * 3) * 4
  // 24

  2 * (3 * 4)
  // 24
  ```
- 부울 연산자 `&&`는 결합적이고 항등원은 `true`다.
  ```kotlin
  true && false
  // false

  true && true
  // true
  // true: identity

  (true && true) && false
  // false

  true && (true && false)
  // false
  ```
- 부울 연산자 `||`는 결합적이고 항등원은 `false`다.
  ```kotlin
  true || true
  // true

  true || false
  // true
  // false: identity

  (true || true) || false
  // true

  true || (true || false)
  // true
  ```
- 이런 종류의 대수를 모노이드라고 한다. 결합 법칙과 항등 법칙을 합쳐 모노이드 법칙이라고 한다.
  ```kotlin
  interface Monoid<A> {
      fun op(a: A, b: A): A
      fun zero(): A
  }
  ```
- 위의 모노이드 정의가 실제 어떻게 동작하는지는 구현에 따라 다르다.
- `String` 모노이드 인스턴스
  ```kotlin
  object StringInstances {
      val monoid = object : Monoid<String> {
          override fun op(a: String, b: String): String = a + b
          override fun zero(): String = ""
      }
  }
  ```
- `List` 모노이드 인스턴스
  ```kotlin
  object ListInstances {
      fun <A> monoid() = object : Monoid<List<A>> {
          override fun op(a: List<A>, b: List<A>): List<A> = a + b
          override fun zero(): List<A> = listOf()
      }
  }
  ```
- `Int` 모노이드 인스턴스
  ```kotlin
  ```
- `Boolean` 모노이드 인스턴스
  ```kotlin
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
  // 교환 법칙은 성립하지 않는다.
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
  def foldMapV[A, B](as: IndexedSeq[A], m: Monoid[B])(f: A => B): B =
     if (as.length == 0)
       m.zero
     else if (as.length == 1)
       f(as(0))
     else {
       val (l, r) = as.splitAt(as.length / 2)
       m.op(foldMapV(l, m)(f), foldMapV(r, m)(f))
     }
  ```
- [어려운 연습문제] 7장에서 만든 라이브러리로 병렬 foldMap을 구현하라. 
- [어려운 연습문제] foldMap을 이용해서 주어진 IndexedSeq[Int]가 정렬되어 있는지 점검하라.

## 예제 : 병렬 파싱

- 주어진 String의 단어 수를 병렬로 세는 예제
  ```scala
  "lorem ipsum dolor sit amet, "
  
  // 반씩 짤라서 개수를 세면 병렬로 처리할 수 있지만 가운데 문자열이 짤려버린다.
  // 위 예제를 반으로 자르면 "lorem ipsum do"과 "lor sit amet, "가 되는데 
  // 이렇게 되면 중간에 "do", "lor"이 잘려서 각각 단어로 잘못 세진다.
  // 이것을 방지하기 위한 데이터 구조를 만들어보자.
  ```
- 단어 개수에 대한 대수적 자료 형식
  ```scala
  sealed trait WC
  case class Stub(chars: String) extends WC
  case class Part(lStub: String, words: Int, rStub: String) extends WC
  
  // 가장 왼쪽 문자열과 오른쪽 문자열은 잘려진 문자열일 수 도 있기 때문에 단어로 세지 않고 그대로 가지고 있는다.
  // 공백으로 구분되서 단어가 확실한 것은 words에 카운트 한다.
  // Part("lorem", 1, "do") 
  // Part("lor", 2, "") 
  ```
- [연습문제] WC에 대한 모노이드 인스턴스를 만들어라.
  ```scala
  val wcMonoid: Monoid[WC] = new Monoid[WC] {
    val zero = Stub("")

    def op(a: WC, b: WC) = (a, b) match {
      // op(Stub("a"), Stub("b")) = Stub("ab") 
      case (Stub(c), Stub(d)) => Stub(c + d)
  
      // op(Stub("a"), Part("b", 1, "c")) = Part("ab", 1, "c")
      case (Stub(c), Part(l, w, r)) => Part(c + l, w, r)
  
      // op(Part("b", 1, "c"), Stub("d")) = Part("b", 1, "cd")
      case (Part(l, w, r), Stub(c)) => Part(l, w, r + c)
  
      // op(Part("a", 1, "b"), Part("c", 1, "d")) = Part("a", 3, "d")
      case (Part(l1, w1, r1), Part(l2, w2, r2)) =>
        Part(l1, w1 + (if ((r1 + l2).isEmpty) 0 else 1) + w2, r2)
    }
  }
  ```
- [연습문제] WC 모노이드로 String 단어 개수를 세는 함수를 구현하라.
  ```scala
  def count(s: String): Int = {
    def wc(c: Char): WC =
      if (c.isWhitespace)
        Part("", 0, "")
      else
        Stub(c.toString)

    def unstub(s: String) = s.length min 1
  
    // foldMapV는 반씩 짤라서 모노이드 op를 한다. wc로 Char -> WC 로 바꾼다.
    foldMapV(s.toIndexedSeq, wcMonoid)(wc) match {
      // 최종 결과인 WC를 숫자로 바꾼다.
      case Stub(s) => unstub(s)
      case Part(l, w, r) => unstub(l) + w + unstub(r)
    }
  }
  ``` 

## 접을 수 있는 자료구조

- List, Tree, Stream, IndexedSeq는 모두 접을 수 있다. 어떤 자료 형태라도 젒을 수 있는 형식을 표현할 수 있다.
  ```scala
  // F 는 타입 생성자이고 higher-kinded type이라고 한다.
  trait Foldable[F[_]] {
    def foldRight[A, B](as: F[A])(z: B)(f: (A, B) => B): B
    def foldLeft[A, B](as: F[A])(z: B)(f: (B, A) => B): B
    def foldMap[A, B](as: F[A])(f: A => B)(mb: Monoid[B]): B
    def concatenate[A](as: F[A])(m: Monoid[A]): A =
      foldLeft(as)(m.zero)(m.op)
  }
  ```
  
- [연습문제] Foldable[List], Foldable[IndexedSeq], Foldable[Stream]를 구현하시오.
  ```scala
  object ListFoldable extends Foldable[List] {
    override def foldRight[A, B](as: List[A])(z: B)(f: (A, B) => B) =
      as.foldRight(z)(f)
    override def foldLeft[A, B](as: List[A])(z: B)(f: (B, A) => B) =
      as.foldLeft(z)(f)
    override def foldMap[A, B](as: List[A])(f: A => B)(mb: Monoid[B]): B =
      foldLeft(as)(mb.zero)((b, a) => mb.op(b, f(a)))
  }
  
  object IndexedSeqFoldable extends Foldable[IndexedSeq] {
    import Monoid._
    override def foldRight[A, B](as: IndexedSeq[A])(z: B)(f: (A, B) => B) =
      as.foldRight(z)(f)
    override def foldLeft[A, B](as: IndexedSeq[A])(z: B)(f: (B, A) => B) =
      as.foldLeft(z)(f)
    override def foldMap[A, B](as: IndexedSeq[A])(f: A => B)(mb: Monoid[B]): B =
      foldMapV(as, mb)(f)
  }
  
  object StreamFoldable extends Foldable[Stream] {
    override def foldRight[A, B](as: Stream[A])(z: B)(f: (A, B) => B) =
      as.foldRight(z)(f)
    override def foldLeft[A, B](as: Stream[A])(z: B)(f: (B, A) => B) =
      as.foldLeft(z)(f)
  }
  ```
  
- [연습문제] Tree의 Foldable 인스턴스를 구현하시오.
  ```scala
  sealed trait Tree[+A]
  case class Leaf[A](value: A) extends Tree[A]
  case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]
  
  object TreeFoldable extends Foldable[Tree] {
    override def foldMap[A, B](as: Tree[A])(f: A => B)(mb: Monoid[B]): B = as match {
      case Leaf(a) => f(a)
      case Branch(l, r) => mb.op(foldMap(l)(f)(mb), foldMap(r)(f)(mb))
    }
    override def foldLeft[A, B](as: Tree[A])(z: B)(f: (B, A) => B) = as match {
      case Leaf(a) => f(z, a)
      case Branch(l, r) => foldLeft(r)(foldLeft(l)(z)(f))(f)
    }
    override def foldRight[A, B](as: Tree[A])(z: B)(f: (A, B) => B) = as match {
      case Leaf(a) => f(a, z)
      case Branch(l, r) => foldRight(l)(foldRight(r)(z)(f))(f)
    }
  }
  ```
  
- [연습문제] Foldable[Option] 인스턴스를 작성하라.
  ```scala
  object OptionFoldable extends Foldable[Option] {
    override def foldMap[A, B](as: Option[A])(f: A => B)(mb: Monoid[B]): B =
      as match {
        case None => mb.zero
        case Some(a) => f(a)
      }
    override def foldLeft[A, B](as: Option[A])(z: B)(f: (B, A) => B) = as match {
      case None => z
      case Some(a) => f(z, a)
    }
    override def foldRight[A, B](as: Option[A])(z: B)(f: (A, B) => B) = as match {
      case None => z
      case Some(a) => f(a, z)
    }
  }
  ```

- [연습문제] Foldable을 List로 만드는 toList를 구현하라
  ```scala
  def toList[A](as: F[A]): List[A] =
      foldRight(as)(List[A]())(_ :: _)
  ```
  
## 모노이드 합성

- 모노이드의 진정한 능력은 합성 능력에서 비롯된다. 예를 들어 A와 B가 모노이드면 (A, B) 역시 모노이드다.
- [연습문제] A.op 와 B.op 가 결합 적이면 다음 함수에 대한 op 구현은 결합적임을 증명하라.
  ```scala
  def productMonoid[A,B](A: Monoid[A], B: Monoid[B]): Monoid[(A, B)] =
      new Monoid[(A, B)] {
        def op(x: (A, B), y: (A, B)) =
          (A.op(x._1, y._1), B.op(x._2, y._2))
        val zero = (A.zero, B.zero)
      }
  ```
  
### 좀 더 복잡한 모노이드 합성

- Map의 값이 모노이드면 그 Map을 합치는 모노이드가 있다.
  ```scala
  def mapMergeMonoid[K,V](V: Monoid[V]): Monoid[Map[K, V]] =
      new Monoid[Map[K, V]] {
        def zero = Map[K,V]()
        def op(a: Map[K, V], b: Map[K, V]) =
          (a.keySet ++ b.keySet).foldLeft(zero) { (acc,k) =>
            acc.updated(k, V.op(a.getOrElse(k, V.zero),
                                b.getOrElse(k, V.zero)))
          }
      }
  ```
- 이런 모노이드가 있으면 더 복잡한 모노이드를 조합할 수 있다.
  ```scala
   val M: Monoid[Map[String, Map[String, Int]]] = 
     mapMergeMonoid(mapMergeMonoid(intAddtion))
  
  val m1 = Map("o1" -> Map("i1" -> 1, "i2" -> 2))
  val m2 = Map("o1" -> Map("i2" -> 3))
  val m3 = Mp.op(m1, m2)
  // Map(o1 -> Map(i1 -> 1, i2 -> 5))
  ```
- [연습문제] 결과가 모노이드인 함수들에 대한 모노이드 인스턴스를 작성하라.
  ```scala
  def functionMonoid[A,B](B: Monoid[B]): Monoid[A => B] =
      new Monoid[A => B] {
        def op(f: A => B, g: A => B) = a => B.op(f(a), g(a))
        val zero: A => B = a => B.zero
      }
  
  val M = functionMonoid(intAddtion)
  val M2 = M.op(Math.abs, Math.round) // a => intAddtion.op(Math.abs(a), Math.round(b)) 
  ```
- [연습문제] 다음과 같은 `bag`을 구현하라
  ```scala
  bag(Vector("a", "rose", "is", "a", "rose"))
  // Map[String,Int] = Map(a -> 2, rose -> 2, is -> 1)
  
  def bag[A](as: IndexedSeq[A]): Map[A, Int] =
      foldMapV(as, mapMergeMonoid[A, Int](intAddition))((a: A) => Map(a -> 1))
  ```

### 모노이드 합성을 이용한 순회 융합

- 모노이드를 합성하면 여러 계산을 함께 할 수 있다. 예를 들어 아래는 길이를 구하면서 합을 구할 수 있다.
  ```scala
  val m = productMonoid(intAddition, intAddition)
  val p = listFoldable.foldMap(List(1,2,3,4))(a => (1, a))(m) // (1+1+1+1,1+2+3+4)
  val mean = p._1 / p._2.toDouble // 2.5
  ```
