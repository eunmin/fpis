# 모나드

## 함수자: map 함수의 일반화

- 앞에서 살펴본 map 함수는 아래와 같다.
  ```scala
  def map[A,B](ga: Gen[A])(f: A => B): Gen[B]
  
  def map[A,B](pa: Parser[A])(f: A => B): Parser[B]
  
  def map[A,B](oa: Option[A])(f: A => B): Option[B]
  ```
- 차이는 자료 형식(Gen, Parser, Option) 뿐이다. 그래서 trait로 표현하면 아래와 같다.
  ```scala
  trait Functor[F[_]] {
    def map[A,B](fa: F[A])(f: A => B): F[B]
  }
  ```
- 다음은 List의 Functor 인스턴스다.
  ```scala
  val listFunctor = new Functor[List] {
    def map[A,B](as: List[A])(f: A => B): List[B] = as map f
  }
  ```
- 이것으로 무엇을 할 수 있을까? `map`으로 만들 수 있는 다른 연산을 찾아보자. `F[(A,B)]`를 (F[A],F[B])`로
  만드는 연산을 만들어보자.
  ```scala
  def distribute[A,B](fab: F[(A, B)]): (F[A], F[B]) =
      (map(fab)(_._1), map(fab)(_._2))
  ```
- 단지 추상이기 때문에 구체적인 의미를 알 수 없다. List, Gen, Option에서 어떻게 동작할까?
- List[(A,B)] 는 (List[A], List[B])가 만들어진다. 이런 연산을 `unzip`이라고 한다. 
- `distribute`의 반대 연산도 만들어보자.
  ```scala
  def codistribute[A,B](e: Either[F[A], F[B]]): F[Either[A, B]] = e match {
    case Left(fa) => map(fa)(Left(_))
    case Right(fb) => map(fb)(Right(_))
  }
  ```
- Gen에 대해 `codistribute`는 어떤 의미인가? `Either[Gen[A], Gen[B]]`는 `Gen[Either[A, B]]`가 된다.
- A, B 타입의 생성기가 있을 때 둘 중에 어떤 것이 주어지는가에 따라 A 또는 B를 생성하는 생성기를 만들 수 있다.
- 추상으로만 유용한 것을 만들 수 있다.

### 함수자의 법칙들

- Functor 같은 추상을 만들 때 구현들이 지켜야할 법칙도 고민해야한다. 이것은 스칼라가 강제하지 않는다.
- 법칙은 추론에 도움이 된다. 만약 `Monoid[A]`와 `Monoid[B]`의 곱으로 `Monoid[(A,B)]`를 만든다면 A, B를 몰라도 
  만들어진 모노이드도 결합법칙을 따른다는 결론을 낼 수 있다.
- map은 다음 법칙을 따른다.
  ```scala
  map(x)(a => a) == x
  ```   
- x 자료구조의 구조를 그대로 보존한다. 

## 모나드: flatMap 함수와 unit 함수의 일반화

- Functor는 그리 매력적이지 않다. `map`으로만 만들 수 있는 것이 많이 없다.
- Monad로는 많은 것을 만들 수 있 코드 중복을 많이 줄 일 수 있다.
- 전에 만든 map2에 대해 살펴보자.
  ```scala
  def map2[A,B,C](fa: Gen[A], fb: Gen[B])(f: (A,B) => C): Gen[C] = 
    fa flatMap(a => fb map (b => f(a,b)))
    
  def map2[A,B,C](fa: Parser[A], fb: Parser[B])(f: (A,B) => C): Parser[C] = 
    fa flatMap(a => fa map (b => f(a,b)))
  
  def map2[A,B,C](fa: Option[A], fb: Option[B])(f: (A,B) => C): Option[C] =
    fa flatMap(a => fb map (b => f(a,b)))
  ```
- 다 똑같다. 그래서 `map2`를 한번만 만들어두면 코드의 중복을 줄일 수 있다. 
  
 ### Monad 특질
 
- `map2`와 같이 공통 연산들을 하나로 정의할 `Mon`이라는 triat를 만들어보자.
  ```scala
  trait Mon[F[_]] {
    def map2[A,B,C](fa: F[A], fb: F[B])(f: (A,B) => C): F[C] =
      fa flatMap(a => fb map (b => f(a,b))) // map과 flatMap이 없어서 컴파일 되지 않는다.
  }
  ```
- map과 flatMap을 추가하자.
  ```scala
  trait Mon[F[_]] {
    def map[A,B](fa: F[A])(f: A => B): F[B]
    def flatMap[A,B](fa: F[A])(f: A => F[B]): F[B]
    def map2[A,B,C](fa: F[A], fb: F[B])(f: (A,B) => C): F[C] =
      fa flatMap(a => fb map (b => f(a,b))) // map과 flatMap이 없어서 컴파일 되지 않는다.
    }
  ```
 - `unit`이 있다면 `map`은 `flatMap`으로 만들 수 있다.
   ```scala
   def map[A,B](fa: F[A])(f: A => B): F[B] =
    flatMap(a => unit(f(a))
   ```
- 모나드는 다음과 같이 정의 할 수 있다.
  ```scala
  trait Monad[M[_]] extends Functor[M] { // Functor에 map이 정의 되어 있기 때문에 구현 가능
    def unit[A](a: => A): M[A]
    def flatMap[A,B](ma: M[A])(f: A => M[B]): M[B]
  
    def map[A,B](ma: M[A])(f: A => B): M[B] =
      flatMap(ma)(a => unit(f(a)))
    def map2[A,B,C](ma: M[A], mb: M[B])(f: (A, B) => C): M[C] =
      flatMap(ma)(a => map(mb)(b => f(a, b)))
  }  
  ```
- `Gen`에 대한 Monad 인스턴스
  ```scala
  object Monad {
    val genMonad = new Monad[Gen] {
      def unit[A](a: => A): Gen[A] = Gen.unit(a)
      override def flatMap[A,B](ma: Gen[A])(f: A => Gen[B]): Gen[B] =
        ma flatMap f
    }
  }
  ```
- [연습문제] Par, Parser, Option, Stream, List 모나드 인스턴스 만들기
  ```scala
  object Monad {
    val parMonad = new Monad[Par] {
      def unit[A](a: => A) = Par.unit(a)
      override def flatMap[A,B](ma: Par[A])(f: A => Par[B]) = Par.flatMap(ma)(f)
    }
  
    def parserMonad[P[+_]](p: Parsers[P]) = new Monad[P] {
      def unit[A](a: => A) = p.succeed(a)
      override def flatMap[A,B](ma: P[A])(f: A => P[B]) = p.flatMap(ma)(f)
    }
  
    val optionMonad = new Monad[Option] {
      def unit[A](a: => A) = Some(a)
      override def flatMap[A,B](ma: Option[A])(f: A => Option[B]) = ma flatMap f
    }
  
    val streamMonad = new Monad[Stream] {
      def unit[A](a: => A) = Stream(a)
      override def flatMap[A,B](ma: Stream[A])(f: A => Stream[B]) = ma flatMap f
    }
  
    val listMonad = new Monad[List] {
      def unit[A](a: => A) = List(a)
      override def flatMap[A,B](ma: List[A])(f: A => List[B]) = ma flatMap f
    }
  }
  ```
  
## 모나드적 조합기

- [연습문제] sequence 조합기와 traverse 조합기를 
  ```scala
  trait Monad[F[_]] extends Functor[F] {
    def unit[A](a: => A): M[A]
    def flatMap[A,B](ma: M[A])(f: A => M[B]): M[B]
  
    def map[A,B](ma: M[A])(f: A => B): M[B] =
      flatMap(ma)(a => unit(f(a)))
  
    def map2[A,B,C](ma: M[A], mb: M[B])(f: (A, B) => C): M[C] =
      flatMap(ma)(a => map(mb)(b => f(a, b)))
  
    def sequence[A](lma: List[F[A]]): F[List[A]] =
      lma.foldRight(unit(List[A]()))((ma, mla) => map2(ma, mla)(_ :: _))
    
    def traverse[A,B](la: List[A])(f: A => F[B]): F[List[B]] =
      la.foldRight(unit(List[B]()))((a, mlb) => map2(f(a), mlb)(_ :: _))
  }
  ```

- [연습문제] Gen과 Parser에 listOfN 이라는 조합기가 있었다. 이것도 Monad의 일반 조합기로 만들 수 있다. 이름은 조금 더 
  일반적인 replicateM 이라고 하자.
  ```scala
  def replicateM[A](n: Int, ma: F[A]): F[List[A]] =
      sequence(List.fill(n)(ma))
  ```
  
- 역시 이전에 product 라는 조합기를 만든적이 있는데 이것도 `map2`로 만들었기 때문에 모나드에 추가하는 것이 좋다.
  ```scala
  def product[A,B](ma: F[A], mb: F[B]): F[(A, B)] = map2(ma, mb)((_, _))
  ```

- [연습문제] `filterM`을 구현하라. `A => F[Boolean]`에 동작하는 점이 일반 filter와 다르다.
  ```scala
  def filterM[A](ms: List[A])(f: A => F[Boolean]): F[List[A]] =
      ms.foldRight(unit(List[A]()))((x,y) =>
        compose(f, (b: Boolean) => if (b) map2(unit(x),y)(_ :: _) else y)(x))
  ```
  
## 모나드 법칙

- `Monad[F]`는 일종의 `Functor[F]`기 때문에 Functor 법칙도 Monad에 성립한다.
- 그 외 법칙은 어떤 것이 있을까?

### 결합법칙

- 세 개의 모나드 값을 하나로 조합할 때 셋 중에 어떤 것을 먼저 조합해야 할까?
- [예제1]
  ```scala
  case class Order(item: Item, quantity: Int)
  case class Item(name: String, price: Double)
  
  val genOrder: Gen[Order] = for {
    name <- Gen.stringN(3) // 길이가 3인 문자열
    price <- Gen.uniform.map(_ * 10) // 0 ~ 10 사이의 균등분포 Double 난수 
    quantity <- Gen.choose(1,100) // 1 ~ 100 사이 난수
  } yield Order(Item(name, price), quantity)
  ```
- [예제2] 예제1과 같은 기능을 하지만 Item을 따로 생성할 수 있도록 분리
  ```scala
  var  genItem: Gen[Item] = for {
    name <- Gen.stringN(3)
    price <- Gen.uniform.map(_ * 10)
  } yield Item(name, price)
  
  var genOrder: Gen[Order] = for {
    item <- genItem
    quantity <- Gen.choose(1,100)
  } yield Order(item, quantity)
  ``` 
- 예제1,예제2는 같은 결과가 나올까? 펼쳐보자.
  ```scala
  Gen.nextString.flatMap(name => 
  Gen.nextDouble.flatMap(price =>
  Gen.nextInt.map(quantity =>
    Order(Item(name, price), quantity)
  )))
  
  Gen.nextString.flatMap(name =>
  Gen.nextint.map(price =>
    Item(name, price))).flatMap(item => 
            Gen.nextInt.map(quantity => 
              Order(item, quantity)))
  ```

- 구현은 다르지만 flatMap이 결합법칙을 만족하기 때문에 같다.
  ```scala
  x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
  ```
  
### 특정 모나드의 결합법칙 성립 증명

- Option에 대해 결합 법칙이 성립할까? (성립한다.)
  ```scala
  None.flatMap(f).faltMap(g) == None.flatMap(a => f(a).flatMap(g))
  
  None == None // None.flatMap 은 None이기 때문에

  Some(v).flatMap(f).flatMap(g) == Some(v).flatMap(a => f(a).flatMap(g))
  f(v).flatMap(g) == (a => f(a).flatMap(g))(v)
  f(v).flatMap(g) == f(v).faltMap(g)
  ```
- 모나드 결합 법칙은 모노이드처럼 직관적이지 않기 때문에 알아보기 힘들다.
  ```scala
   op(op(x, y), z) == op(x, op(y, z))
  
  x.flatMap(f).flatMap(g) == x.flatMap(a => f(a).flatMap(g))
  ```
- [연습문제] 모나드를 합성하는 클라이슬리 화살표를 만들어 표현해보자.
  ```scala
  def compose[A,B,C](f: A => F[B], g: B => F[C]): A => F[C] =
      a => flatMap(f(a))(g)
  
  compose(compose(f, g), h) == compose(f, compose(g, h))
  ```

### 항등법칙

- 모나드의 기본 연산인 `unit`을 항등원으로 모나드 항등 법칙을 표현할 수 있다.
  ```scala
  def unit[A](a: => A): F[A]
  
  compose(f, unit) == f
  compose(unit, f) == f
  
  flatMap(x)(unit) == x // ???
  flatMap(unit(y))(f) == f(y) // ???
  
  x.flatMap(unit) == x
  unit(y).flatMap(f) = f(y)
  
  Some(v).flatMap(unit) = Some(v)
  Some(y).flatMap(f) = f(y) 
  ```
- [연습문제] 다음과 같은`join` 연산을 구현해라
  ```scala
  def join[A](mma: F[F[A]]): F[A] = flatMap(mma)(ma => ma)
  ```
  
## 도대체 모나드란 무엇인가?

- 모나드란 모나드적 조합기들의 최소 집합 중 하나를 결합법칙과 항등법칙을 만족하도록 구현한 것이다.
- 이 정의는 애매하고 동어 반복이다. 
- 감을 잡기위해서 구체적인 모나드 두 개를 살펴보면서 비교해보자.

### 항등 모나드 (Identity 모나드)

```scala
case class Id[A](value: A) {
  def map[B](f: A => B): Id[B] = Id(f(value))
  def flatMap[B](f: A => Id[B]): Id[B] = f(value)
}

Id("Hello, ").flatMap(a =>
Id("monad!").flatMap(b =>
  Id(a + b)))
// Id("Hello, monad!")

for {
  a <- Id("Hello, ")
  b <- Id("Monad!")
} yield a + b
// Id("Hello, monad!")

val a = "Hello, "
val b = "moand!"
a + b
// "Hello, monad!"
```

- 대입 변수 치환을 한다.

### State 모나드와 부분 형식 적용

- 6장에서 살펴본 State를 다시 살펴보자.

```scala
case class State[S, A](run: S => (A, S)) {
  def map[B](f: A => B): State[S, B] =
    State(s => {
      val (a, s1) = run(s)
      (f(a), s1)
    })
  def flatMap[B](f: A => State[S, B]): State[S, B] =
    State(s => {
      val (a, s1) = run(s)
      f(a).run(s1)
    })
}

class StateMonads[S] {
  type StateS[A] = State[S, A]

  // S 타입을 고정한 State 모나드 인스턴스를 생성
  def stateMonad[S] = new Monad[({type lambda[x] = State[S, x]})#lambda] {
    def unit[A](a: => A): State[S, A] = State(s => (a, s))
    override def flatMap[A,B](st: State[S, A])(f: A => State[S, B]): State[S, B] =
      st flatMap f 
  }
  
  def getState[S]: State[S,S] = State(s => (s,s))
  def setState[S](s: S): State[S,Unit] = State(_ => ((),s))

  val F = stateMonad[Int] // S가 Int State 모나드 인스턴스
  
  def zipWithIndex[A](as: List[A]): List[(Int,A)] =
    as.foldLeft(F.unit(List[(Int, A)]()))((acc,a) => for {
      xs <- acc
      n  <- getState
      _  <- setState(n + 1)
    } yield (n, a) :: xs).run(0)._1.reverse
}
```

- Id 모나드 처럼 변수에 바인딩 하는 것은 확실하다. 
- 하지만 줄 사이에서 다른 일이 진행된다.
- 모나드는 각 명령문의 경꼐에서 어떤 일이 일어나는지를 명시한다. 무슨 일이 일어나는지는 모나드 구현에 따라 다르다.
- [어려운 연습문제] Reader 모나드를 구현하라. 

## 요약

- "전에도 모나드가 뭔지 이해했다고 생각했지만, 이제는 정말로 이해한것 같아."