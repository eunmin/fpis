# 적용성 함수자와 순회 가능 함수자

- 적용성 함수자 (applicative functor)
- 순회 가능 함수자 (traversable functor)

## 모나드의 일반화

- `sequnce`와 `traverse`의 일반화
  ```scala
  def sequence[A](laf: List[F[A]]): F[List[A]] =
    traverse(lfa)(fa => fa)
  
  def traverse[A,B](as: List[A])(f: A => F[B]): F[List[B]] =
    as.foldRight(unit(List[B]()))((a, mbs) => map2(f(a), mbs)(_ :: _))
  ```
- 모나드의 많은 조합기들을 `unit`과 `map2`로 정의할 수 있다. (`flatMap`이 없어도 됨)
- 만약 `unit`과 `map2`를 기본 연산으로 둔다면 또 다른 추상을 얻을 수 있고 그것을 Applicative funtor라고 한다.
- 모나드보다 덜 강력하지만, 이런 제한이 득이 된다.

## Applicative 특질

- Applicative를 인터페이스로 만들어보자.
  ```scala
  trait Applicative[F[_]] extends Functor[F] {
    def map2[A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] = ???
    def unit[A](a: => A): F[A]
  
    def map[A,B](fa: F[A])(f: A => B): F[B] =
      map2(fa, unit(()))((a, _) => f(a))
      
    def traverse[A,B](as: List[A])(f: A => F[B]): F[List[B]] =
      as.foldRight(unit(List[B]()))((a, mbs) => map2(f(a), mbs)(_ :: _))
  }
  ```
- `map2`와 `unit`으로 `map`을 얻을 수 있기 때문에 모든 Applicative Functor는 Functor다.
- [연습문제] `def sequence[A](fas: List[F[A]]): F[List[A]]` 구현하기
  ```scala
  def sequence[A](fas: List[F[A]]): F[List[A]] =
    traverse(fas)(fa => fa)
  ```
- [연습문제] `def replicateM[A](n: Int, fa: F[A]): F[List[A]] ` 구현하기
  ```scala
  def replicateM[A](n: Int, fa: F[A]): F[List[A]] =
      sequence(List.fill(n)(fa))
  ```
- [연습문제] `def product[A,B](fa: F[A], fb: F[B]): F[(A,B)]` 구현하기
- [연습문제: 어려움] Applicative를 `unit`과 `apply`를 이용해서 `map2`와 `map`을 구현하라
  ```scala
  trait Applicative[F[_]] extends Functor[F] {
    def apply[A,B](fab: F[A => B])(fa: F[A]): F[B]
    def unit[A](a: => A): F[A]
  
    def map[A,B](fa: F[A])(f: A => B): F[B] =
      apply(unit(f))(fa)
  
    def map2[A,B,C](fa: F[A], fb: F[B])(f: (A, B) => C): F[C] =
      apply(map(fa)(f.curried))(fb)
  }
  ```
- [연습문제] `apply`는 `map3`, `map4` 같은 것을 만들기 쉽다. 구현해봐라.
  ```scala
  def map3[A,B,C,D](fa: F[A],
                    fb: F[B],
                    fc: F[C])(f: (A,B,C) => D): F[D]
  def map3[A,B,C,D,E](fa: F[A],
                      fb: F[B],
                      fc: F[C],
                      fd: F[D])(f: (A,B,C,D) => E): F[E]
  ```
- `flatMap`으로 `map2`를 구현할 수 있기 때문에 모든 `Monad`는 `Applicative`다.
  ```scala
  trait Monad[F[_]] extends Applicative[F] {
    def flatMap[A,B](ma: F[A])(f: A => F[B]): F[B] =
      join(map(ma)(f))
    
    override def apply[A,B](mf: F[A => B])(ma: F[A]): F[B] =
      flatMap(mf)(f => map(ma)(f))
  
    override def map[A,B](m: F[A])(f: A => B): F[B] =
        flatMap(m)(a => unit(f(a)))
  
    override def map2[A,B,C](ma: F[A], mb: F[B])(f: (A, B) => C): F[C] =
        flatMap(ma)(a => map(mb)(b => f(a, b)))
  
    def join[A](mma: F[F[A]]): F[A] = flatMap(mma)(ma => ma)
 
    def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C] =
      a => flatMap(f(a))(g)   
  }
  ```
 - 이제 모나드는 `unit`을 구현해야하고`flatMap`을 오버라이드 하던지 `join`과 `map`을 오버라이드 하던지 해야한다.
 
 ## 모나드와 Applicative Functor 차이
 
 - 모나드의 기본 연산은
   - `unit`과 `flatMap` 또는
   - `unit`과 `compose` 또는
   - `unit`과 `map`, `join`
   이다.
   
 - `join`이나 `flatMap`은 `map2`나 `unit`으로 구현할 수 없다. 그렇기 때문에 모나드는 Applicative 보다 추가적인 기능이 있다.
 
 ### Applicative Option과 Option 모나드
 
- 두개의 Map의 결과(Option 타입)를 그냥 합친다면 `map2`를 쓰면 된다.
  ```scala
  val F: Applicative[Option] = ???
  
  val depts: Map[String, String] = ???
  val salaries: Map[String, Double] = ???
  
  val o: Option[String] =
    F.map2(depts.get("Alice"), salaries.get("Alice"))(
      (dept, salary) => s"Alice in $dept makes $salary per year"
    )
  ```
- 만약 어떤 Option 값이 다른 Option 값의 결과에 의존적이라면 `flatMap`과 `join`이 필요하다.
  ```scala
  val idsByName: Map[String, Int] = ???
  val depts: Map[Int, String] = ???
  val salaries: Map[Int, Double] = ???
 
  val o: Option[String] =
    idsByName.get("Bob").flatMap { id =>
      F.map2(depts.get(id), salaries.get(id))(
        (dept, salary) => s"Bob in $dept makes $salary per year"
      ) 
    }
  ```
- [쉬어가는 페이지] FP에서 Option, Par, List 같은 것을 효과(Effect)라고 부르기도 한다. 컨텍스트는 어떤 다른 일을 하기 때문이다. 모나딕 효과 어플리커티브 효과
 
### 적용성 Parser 대 Parser 모나드
 
- 아래 파일을 파싱하는 Parser는 다음과 같다.
  ```text
  1/1/2010, 25
  2/1/2010, 28
  3/1/2010, 42
  ```
  ```scala
  case class Row(date: Date, temperature: Double)
  
  val F: Applicative[Parser] = ???
  val d: Parser[Date] = ???
  val temp: Parser[Date] = ???
  
  val row: Parser[Row] = F.map2(d, temp)(Row(_,_))
  val rows: Parser[List[Row]] = row.sep("\n")
  ```
- 만약 아래 처럼 해더를 파싱해야 어느 열이 온도인지 날짜인지 알 수 있다면 `flatMap`이 필요하다.
  ```text
  # Temperature, Date
  25, 1/1/2020
  28, 2/1/2020
  42, 3/1/2020
  ```
  ```scala
  case class Row(date: Date, temperature: Double)
  
  val F: Monad[Parser] = ???
  val d: Parser[Date] = ???
  val temp: Parser[Double] = ???
  
  val header: Parser[Parser[Row]] = ???
  val rows: Parser[List[Row]] =
    F.flatMap (header) { row => row.sep("\n") }
  ```
- Applicative는 효과를 차례로 적용할 뿐이다. 모나드는 이전 효과의 결과에 따라 동적으로 선택할 수 있다.
- Applicative는 문맥 자유 계산을 형성하는 반면 모나드는 문맥 민감 계산이 가능하다.
- 모나드는 효과를 일급으로 만든다. 효과를 인터프리테이션하는 시점에 생성할 수 있다. ?   

## Applicative Fuctor의 장점
 
- 최소한의 가정으로 `traverse` 같은 것을 만드는 것이 좋다. 항상 모나드여야 한다면 모나드 마다 `traverse`를 따로 만들어야한다.
- Applicative 효과의 해석기가 더 유연하다. flatMap은 파서를 동적으로 생성한다는 것이고 해석기가 할 수 있는 일이 제한한다.
- Applicative functor는  합성할 수 있지만 Monad는 일반적으로 합성할 수 없다.
 
### 모든 Applicative Functor가 Monad는 아니다.
 
#### Applicative 스트림
 
- 스트림은 `map2`와 `unit`을 정의 할 수 있지만 `flatMap[A,B](ma: F[A])(f: A => F[B]): F[B]`은 정의 할 수 없다.
  ```scala
  val streamApplicative = new Applicative[Stream] {

    def unit[A](a: => A): Stream[A] =
      Stream.continually(a) // The infinite, constant stream

    override def map2[A,B,C](a: Stream[A], b: Stream[B])( // Combine elements pointwise
                    f: (A,B) => C): Stream[C] =
      a zip b map f.tupled
  }
  ```

#### 유효성 점검: 오류를 누적하는 Either 변종

- [연습문제] Either 모나드 작성하라
  ```scala
  def eitherMonad[E]: Monad[({type f[x] = Either[E, x]})#f] =
    new Monad[({type f[x] = Either[E, x]})#f] {
      def unit[A](a: => A): Either[E, A] = Right(a)
      override def flatMap[A,B](eea: Either[E, A])(f: A => Either[E, B]) = eea match {
        case Right(a) => f(a)
        case Left(b) => Left(b)
      }
    }
  ```
- `flatMap`으로 연속적인 validation을 수행하는 경우 어느 단계에서 `Left`가 나온다면 그 이후는 진행하지 않는다. 하지만 `mapN`으로 처리하면 각 validtion
  결과에 상관 없이 모두 처리한다.
- 여러 개의 오류를 보고하는 Either
  ```scala
  sealed trait Validation[+E, +A]
  case class Failure[E](head: E, tail: Vector[E]) extends Validation[E, Nothing]
  case class Success[A](a: A) extends Validation[Nothing, A]
  
  def validationApplicative[E]: Applicative[({type f[x] = Validation[E,x]})#f] =
      new Applicative[({type f[x] = Validation[E,x]})#f] {
        def unit[A](a: => A) = Success(a)
        override def map2[A,B,C](fa: Validation[E,A], fb: Validation[E,B])(f: (A, B) => C) =
          (fa, fb) match {
            case (Success(a), Success(b)) => Success(f(a, b))
            case (Failure(h1, t1), Failure(h2, t2)) =>
              Failure(h1, t1 ++ Vector(h2) ++ t2)
            case (e@Failure(_, _), _) => e
            case (_, e@Failure(_, _)) => e
          }
      }
  ```
  
- 웹 양식 검증의 예
  ```scala
  case class WebForm(name: String, brithdate: Date, phoneNumber: String)
  
  def validName(name: String): Validation[String, String] =
    if(name != "") Success(name)
    else Failure("Name cannot be emtpy")
  
  def validBirthdate(birthdate: String): Validation[String, Date] = 
    try {
      import java.text._
      Success((new SimpleDateFormat("yyyy-MM-dd")).parse(brithdate))
    } catch {
      Failure("Birthdate must be in the form yyy-MM-dd")
    }
  
  def validPhone(phoneNumber: String): Validation[String, String] = 
    if (phoneNumber.matches("[0-9]{10}"))
      Success(phoneNumber)
    else Failure("Phone number must be 10 digits")
  
  def validWebForm(name: String, birthdate: String, phone: String): Validation[String, WebForm] = 
     map3(
       validName(name),
       validBirthdate(birthdate),
       validPhone(phoneNumber))(
    WebForm(_,_,_))
  ```
 
## Applicative Functor 법칙
 
### 왼쪽, 오른쪽 항등법칙

- 먼저 Applicative는 Functor기 때문에 Functor 법칙을 따라야한다.
  ```scala
  map(v)(id) == v
  map(map(v)(g))(f) == map(v)(f compose g)
  ```
- `map2`는 왼쪽, 오른쪽 항등법칙을 따른다.
  ```scala
  map2(unit(()), fa)((_,a) => a) == fa
  map2(fa, unit(()))((a,_) => a) == fa
  ```
  
### 결합법칙

- `map3`로 결합법칙을 살펴보자. `map3`를 `map2`로 구현했다면 `a,b,c` 중 `a,b`를 먼저 `map2`하고 `c`를 하거나 `b,c`를 `map2`하고 `a`를 적용할 
  수 있다.
- `map2`를 `product`로 나타내고 `accoc`이라는 함수로 결합법칙을 표현해보자.
  ```scala
  def product[A,B](fa: F[A], fb: F[B]): F[(A,B)] = 
    map2(fa, fb)((_, _)) // 그냥 튜플로 묶는다.
  
  def assoc[A,B,C](p: (A,(B,C))): ((A,B),C) = 
    p match { case (a, (b, c)) => ((a, b), c)}
  
  product(product(fa,fb),fc)) == map(product(fa,product(fb,fc)))(assoc) // 튜플로 묶인 타입을 맞추기 위해 assoc을 적용
  ```  

### 곱의 자연성 법칙

```scala
val F: Applicative[Option] = ??? 

case class Employee(name: String, id: Int)
case class Pay(rate: Double, hoursPerYear: Double)

def format(e: Option[Employee], pay: Option[Pay]): Option[String] =
  F.map2(e, pay) { (e, pay) =>
    s"${e.name} makes ${pay.rate * pay.hoursPerYear}"   
  }

val e: Option[Employee] = ???
val pay: Option[Pay] = ???
format(e, pay)
```

- `format`을 일반화하기 위해 `Option[Employee]` 대신 `Option[String]`을 받는 식으로 리팩토링

```scala
def format(e: Option[String], pay: Option[Double]): Option[String] =
  F.map2(e, pay) { (e, pay) =>
    s"$e makes $pay"   
  }

val e: Option[Employee] = ???
val pay: Option[Pay] = ???
format(
  F.map(e)(_name),
  F.map(pay)(pay => pay.rate * pay.hoursPerYear)
)
```

- Applicative 효과는 `map2`로 값을 결합하기 전에 할 수 도 있고 후에 할 수도 있다. (자연성 법칙)
  ```scala
  map2(a,b)(productF(f,g)) == product(map(a)(f), map(b)(g))
  
  def productF[I,O,I2,O2](f: I => O, g: I2 => O2): (I,I2) => (O,O2) =
    (i,i2) => (f(i), g(i2))
  ```
- [연습문제] Applicative 두개를 product 하는 함수를 구현하라.
  ```scala
  def product[G[_]](G: Applicative[G]): Applicative[({type f[x] = (F[x], G[x])})#f] = ???
  ```
  
- [연습문제] `F[_]`와 `G[_]`가 Applicative Functor 면 `F[G[_]]` 도 Applicative Functor 이다.
  ```scala
  def compose[G[_]](G: Applicative[G]): Applicative[({type f[x] = F[G[X]]})#f]
  ``` 
  
## 순회 가능 함수자 (Traversable Functor)
 
- 앞에서 `traverse`, `sequnce`는 `flatMap`에 의존하지 않는다는 것을 알아봤다.
  ```scala
  def traverse[A,B](as: List[A])(f: A => F[B]): F[List[B]]
  def sequence[A](fas: List[F[A]]): F[List[A]]
  ```
- 여기서 `List`를 추상화 하면 어떻게 될까?
- [연습문제] `Map`에 대한 `sequnce`를 만들어라.
  ```scala
  def sequenceMap[K,V](ofa: Map[K,F[V]]): F[Map[K,V]] =
    (ofa foldLeft unit(Map.empty[K,V])) { case (acc, (k, fv)) =>
      map2(acc, fv)((m, v) => m + (k -> v))
    }
  ```
- 새로운 추상인 `Traverse`를 만들어보자. 
  ```scala
  trait Traverse[F[_]] {
    def traverse[G[_]:Applicative,A,B](fa: F[A])(f: A => G[B]): G[F[B]] =
      sequence(map(fa)(f))
    def sequence[G[_]:Applicative,A](fma: F[G[A]]): G[F[A]] =
      traverse(fma)(ma => ma)
  }
  ```
- 재밌는 것은 `sequence`인데 `F[G[A]]`를 순서를 바꿔 `G[F[A]]`로 만들어준다.
- [연습문제] `List`, `Option`, `Tree`의 Traverse 인스턴스를 만들어라.
- 각 타입의 의미는
  - `List[Option[A]] => Option[List[A]]`는 리스트 중에 하나라도 None이면 None을 돌려주고 아니면 Some으로 감싼 `List[A]`를 만들어준다.
  - `Tree[Option[A]] => Option[Tree[A]]`는 노드 중에 하나라도 None이면 None을 돌려주고 아니면 Some으로 감싼 `Tree[A]`를 만들어준다.
  - `Map[K,Par[A]] => Par[Map[K,A]]`는 맵 값이 병결 계산을 담고 있다면 전체를 병렬로 평가하는 맵을 만들어준다.
- `foldMap`과 비슷하지만 Traverse는 원래 구조를 보존한다. (순서는 바뀜)

## Traverse의 용도

- [연습문제: 어려움] Traverse의 `traverse`가 `map`의 일반 적인 구현이기 때문에 결국 Traverse는 Functor의 확장이다.
  ```scala
  trait Traverse[F[_]] extends Functor[F] { self =>
    def traverse[M[_]:Applicative,A,B](fa: F[A])(f: A => M[B]): M[F[B]] =
      sequence(map(fa)(f))
    def sequence[M[_]:Applicative,A](fma: F[M[A]]): M[F[A]] =
      traverse(fma)(ma => ma)
  
    type Id[A] = A
  
    val idMonad = new Monad[Id] {
      def unit[A](a: => A) = a
      override def flatMap[A,B](a: A)(f: A => B): B = f(a)
    }
  
    def map[A,B](fa: F[A])(f: A => B): F[B] =
      traverse[Id, A, B](fa)(f)(idMonad)
    ...
  ```
 
### 모노이드에서 Applicative Functor로

- Traverse와 Foldable의 관계는 Applicative와 Monoid 관계와 비슷하다.
- `traverse`로 `foldMap`과 `foldLeft`, `foldRight`를 만들 수 있다. 구현은 생략
  ```scala
  trait Traverse[F[_]] extends Functor[F] with Foldable[F]
  ```
 
### 상태가 있는 순회

- Traverse 추상으로 의미있는 것을 만들어보자.
- State와 traverse로 내부 상태를 유지하면서 콜렉션을 순회하는 코드를 만들 수 있다.
  ```scala
  def traverseS[S,A,B](fa: F[A])(f: A => State[S, B]): State[S, F[B]] =
    traverse[({type f[x] = State[S, x]})#f, A, B](fa)(f)(Monad.stateMonad)
  ```
- 이것을 사용하는 예로 Traverse를 순회하면서 카운트를 증가시키면서 항목, 인덱스 튜플을 만든다. 
  ```scala
  def zipWithIndex_[A](ta: F[A]): F[(A,Int)] =
      traverseS(ta)((a: A) => (for {
        i <- get[Int]
        _ <- set(i + 1)
      } yield (a, i))).run(0)._1
  ```
- 아래는 Traverse를 List로 바꾸는 함수다.
  ```scala
   def toList_[A](fa: F[A]): List[A] =
      traverseS(fa)((a: A) => (for {
        as <- get[List[A]] 
        _  <- set(a :: as) // 앞에다 추가하므로
      } yield ())).run(Nil)._2.reverse // 마지막에 뒤집어준다.
  ```
- 위 두 코드는 비슷하기 때문에 일반화 해서 `mapAccum`을 만들 수 있다.
  ```scala
  def mapAccum[S,A,B](fa: F[A], s: S)(f: (A, S) => (B, S)): (F[B], S) =
    traverseS(fa)((a: A) => (for {
      s1 <- get[S]
      (b, s2) = f(a, s1)
      _  <- set(s2)
    } yield b)).run(s)
  ```
- `foldLeft`를 `mapAccum`으로 만들 수 있다.

### Traverse 구조의 조합
 
- 두개의 Traverse를 조합할 때는 순회할 두 Traverse의 길이가 같아야 하기 때문에 왼쪽 또는 오른쪽 길이에 맞춰서 동작하는 조합합수가 각각 필요하다.
  모자른 공간은 None으로 채운다.
  ```scala
   def zipL[A,B](fa: F[A], fb: F[B]): F[(A, Option[B])] =
     (mapAccum(fa, toList(fb)) {
       case (a, Nil) => ((a, None), Nil)
       case (a, b :: bs) => ((a, Some(b)), bs)
     })._1
  
   def zipR[A,B](fa: F[A], fb: F[B]): F[(Option[A], B)] =
     (mapAccum(fb, toList(fa)) {
       case (b, Nil) => ((None, b), Nil)
       case (b, a :: as) => ((Some(a), b), as)
     })._1
  ```
 
### 순회의 융합

- `F[A]`에 대해 `A => M[B]`, `A => N[B]` 함수가 주어졌을 때 `(M[F[B]], N[F[B]])`를 만드는 함수 
```scala
def fuse[M[_],N[_],A,B](fa: F[A])(f: A => M[B], g: A => N[B])
                       (implicit M: Applicative[M], N: Applicative[N]): (M[F[B]], N[F[B]]) =
  traverse[({type f[x] = (M[x], N[x])})#f, A, B](fa)(a => (f(a), g(a)))(M product N)
```
 
### 중첩된 순회

- [연습문제] `Map[K,Option[List[V]]]`와 같이 중첩된 Traverse가 있을 때 모두 순회 해서 V 값을 얻기위해 `compose`함수를 만들어라.
  ```scala
  def compose[G[_]](implicit G: Traverse[G]): Traverse[({type f[x] = F[G[x]]})#f] =
      new Traverse[({type f[x] = F[G[x]]})#f] {
        override def traverse[M[_]:Applicative,A,B](fa: F[G[A]])(f: A => M[B]) =
          self.traverse(fa)((ga: G[A]) => G.traverse(ga)(f))
      }
  ```
 
### 모나드 합성
 
- 중첩된 모나드 `F`와 `G`를 위한 `join[A](mma: F[F[A]]): F[A]`을 구현하려면 `F[G[F[G[A]]]] => F[G[A]]`를 만들어야 한다. `(F : F[G[_]])`
- 일반적으로 얻을 수 없지만 `G`에 대한 Traverse 인스턴스가 있다면 `sequence[G[_]:Applicative,A](fma: F[G[A]]): G[F[A]]` 를 이용해서
   `G[F[_]]`를 `F[G[_]]`로 바꿀 수 있다. 그러면 `F[F[G[G[A]]]]`가 나오므로 인접한 `F`와 `G`를 각각 모나드 인스턴스를 이용해서 결합하면 된다.
- [연습문제: 어려움] 둘 중 하나가 Traversable Functor인 두 모나드의 합성을 구현하라.
   ```scala
   def composeM[G[_],H[_]](implicit G: Monad[G], H: Monad[H], T: Traverse[H]):
       Monad[({type f[x] = G[H[x]]})#f] = new Monad[({type f[x] = G[H[x]]})#f] {
         def unit[A](a: => A): G[H[A]] = G.unit(H.unit(a))
         override def flatMap[A,B](mna: G[H[A]])(f: A => G[H[B]]): G[H[B]] =
           G.flatMap(mna)(na => G.map(T.traverse(na)(f))(H.join))
       }
  ```
- `Traversable Functor`인 모나드는 위에 만든 `composeM`으로 해결 가능하지만 그렇지 않은 모나드의 합성 문제는 모나드마다 합성을 위해 특별하게 작성된 버전을 
  이용해서 해결한다. 이런 것을 모나드 Transformer 라고 부른다.
- 다음은 `Option`을 위한 모나드 변환기다.
  ```scala
  case class OptionT[M[_],A](value: M[Option[A]])(implicit M: Monad[M]) {
    def flatMap[B](f: A => OptionT[M, B]): OptionT[M, B] = 
      OptionT(value flatMap {
        case None => M.unit(None)
        case Some(a) => f(a).value
      })
  }
  ```
- State 모나드도 `Traversable Functor`가 아니기 때문에 합성하려면 `StateT`를 만들어야한다.
 
## 요약
 
- Monoid, Funcotr, Monad, Applicative, Traverse 같은 추상을 알아봤다.
 