# 외부 효과와 입출력

IO 모나드를 만들어 부수 효과를 순수 함수적 방법으로 처리해보자. 또 효과 있는 계산의 서술을 순수 함수를 이용해서 계산하고, 그 서성을 개별적인 해석기를 이용해서 실행함하는 기법을 알아보자. (Embeded DSL)

## 효과의 추출

부수 효과를 가진 프로그램

```scala
case class Player(name: String, score: Int)

def contest(p1: Player, p2: Player): Unit =
  if (p1.score > p2.score)
    println(s"${p1.name} is the winner!")
  else if (p2.score > p1.score)
    println(s"${p2.name} is the winner!")
  else
    println("It's a draw.")
```

위 함수에서 승자 계산 논리를 분리한 `winner` 함수를 빼내보자.

```scala
def winner(p1: Player, p2: Player): Option[Player] =
  if (p1.score > p2.score) Some(p1)
  else if (p2.score > p1.score) Some(p2)
  else None

def contest(p1: Player, p2: Player): Unit =
  winner(p1, p2) match {
    case Some(Player(name, _)) => println(s"$name is the winner!")
    case None => println("It's a draw.")
  }
```

`contest` 함수를 조금 더 리팩토링 해보자.

```scala
def winnerMsg(p: Option[Player]): String =
  p match {
    case Some(Player(name, _)) => s"$name is the winner!"
    case None => "It's a draw."
  }

def contest(p1: Player, p2: Player): Unit =
  println(winnerMsg(winner(p1, p2)))
```

이제 부수효과는 가장 외각 계층에만 존재한다. `A => B` 형식의 불순 함수가 있을 때 항상 `A => D` 형식의 순수 함수(서술)과 `D => B` 형식의 불순 함수(해석기)로 분리할 수 있다.

## 간단한 입출력 형식

```scala
trait IO { def run: Unit }

def PrintLine(msg: String): IO =
  new IO { def run = println(msg) }

def contest(p1: Player, p2: Player): IO =
  PrintLine(winnerMsg(winner(p1, p2)))
```

이제 `contest` 함수는 순수한 함수다. `contest` 함수는 부수 효과가 없고 부수 효과를 표현하는 표현식(서술)만 있다.

그리고 부수 효과는`PrintLine ` 에 구현(해석기)되어 있는 `IO` `run` 함수가 수행한다.

* 이 `IO` 형식이 참조 투명성의 요구 조건을 기계적으로 만족하는 것 외에 어떤 가치를 가지고 있을까? 이 부분은 주관적이다.

이 형식으로 정의 할 수 있는 연산을 알아보자.

```scala
trait IO { self => 
  def run: Unit
  def ++(io: IO): IO = new IO {
    def run = { self.run; io.run }
  }
}
object IO {
  def empty: IO = new IO { def run = () }
}
```

위 정의는 `IO` 가 모노이드를 형성한다는 점이다. `empty`는 항등원이고 `++`가 결합 연산이다. 예를들어 `List[IO]` 가 있다면 이것을 하나의 `IO` 로 축약할 수 있다. 

중요한 점은 다양한 프로그램을 표현할 수 있는 작은 언어와 그에 대한 해석기를 만드는 것이다. 

### 입력 효과의 처리

앞에서 만든 `IO` 타입은 입력 `IO` 를 처리하지 못한다. `run` 을 실행해도 값을 얻을 수 없기 때문이다. 다음 예제를 보자.

```scala
def fahrenheitToCelsius(f: Double): Double =
  (f - 32) * 5.0 / 9.0

def converter: Unit = {
  println("Enter a temperature in degrees Fahrenheit: ")
  val d = readLine.toDouble
  println(fahrenheitToCelsius(d))
} 
```

`converter` 리턴 값을 `IO` 로 만들어보자.

```scala
def converter: IO = {
  val prompt: IO = PrintLine("Enter a temperature in degrees Fahrenheit: ")
  // ???
}
```

지금 `IO` 타입으로는 불가능하다. `IO` 타입을 값을 받을 수 있고 연속으로 조합 가능하게 만들 수 있는 타입으로 바꿔보자.

```scala
sealed trait IO[A] { self =>
  def run: A
  def map[B](f: A => B): IO[B] =
    new IO[B] { def run = f(self.run) }
  def flatMap[B](f: A => IO[B]): IO[B] =
    new IO[B] { def run = f(self.run).run }
}
```

타입 형식을 보면 `IO` 는 모나드다.

```scala
object IO extends Monad[IO] {
  def unit[A](a: => A): IO[A] = new IO[A] { def run = a }
  def flatMap[A,B](fa: IO[A])(f: A => IO[B]) = fa flatMap f
  def apply[A](a: => A): IO[A] = unit(a) // IO { 어쩌고 } 형태로 쓰기 위한 도우미 함수
}
```

새 `IO` 타입으로 위 예제를 만들어보자.

```scala
  def ReadLine: IO[String] = IO { readLine }
  def PrintLine(msg: String):IO[Unit] = IO { println(msg) }

  def converter2: IO[Unit] = for {
    _ <- PrintLine("Enter a temperature in degrees Fahrenheit: ")
    d <- ReadLine.map(_.toDouble)
    _ <- PrintLine(fahrenheitToCelsius(d).toString)
  } yield ()
```

`converter` 함수는 부수효과가 없고 참조 투명한 서술이다. 다음 `IO` 를 쓴 예제다.

```scala
val echo: IO[Unit] = ReadLine.flatMap(PrintLine) // 한 줄 읽어서 출력

val readInt: IO[Int] = ReadLine.map(_.toInt) // 한 줄 읽어서 Int로 바꿈

val readInts: IO[(Int,Int)] = readInt ** readInt // 두 줄 읽어서 Int로 바꿈, ** 는 map2(a,b)((_,_))

val read10Lines: IO[List[String]] = replicateM(10)(ReadLine) // 열 줄 읽기, replicateM(3)(fa)는 sequence(List(fa,fa,fa))
```

추가 모나드 함수

```scala
// F[A]에 있는 A 값이 cond 조건에 만족하면 doWhile을 계속 반복한다
def doWhile[A](a: F[A])(cond: A => F[Boolean]): F[Unit] = for {
  a1 <- a
  ok <- cond(a1)
  _ <- if (ok) doWhile(a)(cond) else unit(())
} yield ()

// F 효과를 무한히 반복한다. A 값은 중요하지 않다. 
def forever[A,B](a: F[A]): F[B] = {
  lazy val t: F[B] = forever(a)
  a flatMap (_ => t)
}

// 스트림 A를 효과가 있는 F[B] 타입으로 fold 한다.
def foldM[A,B](l: Stream[A])(z: B)(f: (B,A) => F[B]): F[B] =
  l match {
    case h #:: t => f(z,h) flatMap (z2 => foldM(t)(z2)(f)) // #:: 는 스트림 쪼개기
    case _ => unit(z)
  }

// 스트림 A를 효과가 있는 F[B] 타입으로 fold 하지만 효과의 결과는 받지 않는다. (Unit)
def foldM_[A,B](l: Stream[A])(z: B)(f: (B,A) => F[B]): F[Unit] = 
  skip { foldM(l)(z)(f)} // skip ???

// 스트림 A에 각 항목에 효과가 있는 f를 적용한다.
def foreachM[A](l: Stream[A])(f: A => F[Unit]): F[Unit] =
  foldM_(l)(())((u,a) => skip(f(a)))
```

`q` 를 입력하면 종료하고 숫자를 입력하면 팩토리얼을 구하는 팩토리얼 REPL 예제

```scala
def factorial(n: Int): IO[Int] = for {
  acc <- ref(1)
  _ <- foreachM (1 to n toStream) (i => acc.modify(_ * i).skip)
  result <- acc.get
} yield result

def factorialREPL: IO[Unit] = sequence_(
  IO { println(helpString) },
  doWhile { IO { readLine } } { line => 
    when (line != "q") { for { 
      n <- factorial(line.toInt)
      _ <- IO { println("factorial: " + n) }
    } yield () }
  }
)
```

함수형 프로그래밍에서 이런 방식을 자주 사용하지 않지만 명령형 스타일로 만들 수 있다는 것을 보여준다.

### 단순한 IO 형식의 장단점

`IO` 모나드로 프로그래밍 하다보면 명령식 프로그래밍과 같은 어려움을 겪게된다. 그래서 함수형 프로그래머들은 다른 방식을 더 선호한다. (15장에서 다룸)

하지만 `IO` 모나드는 다음과 같은 좋은 점이 있다.

* IO 계산이 값이기 때문에 재사용 할 수 있다.
* 표현식의 수정없이 해석기를 바꿀 수 있다. `converter` 를 비동기로 만든다고 해도 `converter` 함수를 고칠 필요가 없다.

문제점도 있다. 

* StackOverflow가 발생한다.
* `IO[A]` 로 추론할 수 있는 것이 별로 없다. 아무것도 안 할 수도 있고 영원이 멈춰 있을 수도 있다.
* `IO` 는 동시성이나 비동기 연산에 대해 아무것도 모른다.

## StackOverflowError 방지

아래 예제는 `flatMap` 구조상 스택오버플로우 에러가 난다.

```scala
val p = IO.forever(PrintLine("Still going..."))

def flatMap[B](f: A => IO[B]): IO[B] = 
  new IO[B] { def run = f(self.run).run } // run 안에서 self.run 을 부르는데 이 함수가 종료되어야 run이 종료된다. forever를 호출할 때 self.run은 종료되지 않는다. 꼬리 재귀가 아니다.
```

### 제어의 흐름을 자료 생성자로 구체화

`flatMap` 을 그냥 `FlatMap(x,k)` 자료형으로 만들면 해석기를 꼬리재귀로 만들 수 있다.

```scala
sealed trait IO[A] {
  def flatMap[B](f: A => IO[B]): IO[B] =
    FlatMap(this, f)
  def map[B](f: A => B): IO[B] =
    flatMap(f andThen (Return(_)))
}

case class Return[A](a: A) extends IO[A]
case class Suspend[A](resume: () => A) extends IO[A]
case class FlatMap[A, B](sub: IO[A], k: A => IO[B]) extends IO[B]
```

새로 만든 `IO` 로 `PrintLine` 을 만들어보자.

```scala
def printLine(s: String): IO[Unit] = 
  Suspend(() => println(s))

def p = IO.forever(printLine("Still goging..."))

// FlatMap(Suspend(() => println("Still goging...")),
//   _ => FlatMap(Suspend(() => println("Still goging...")),
//                ...
//   )
// )
```

이제 이 구조를 순회하면서 효과를 수행하는 해석기는 꼬리재귀로 만들 수 있다.

```scala
@annotation.tailrec def run[A](io: IO[A]): A = io match {
  case Return(a) => a
  case Suspend(r) => r()
  case FlatMap(x, f) => x match {
    case Return(a) => run(f(a)) // 꼬리재귀
    case Suspend(r) => run(f(r())) // 꼬리재귀
    case FlatMap(y, g) => run(y flatMap (a => g(a) flatMap f)) // 꼬리재귀
  }
}
```

위 구조는 일종의 코루틴이라고 할 수 있다. 또 `run` 같은 함수를 트램펄린 이라고 부른다.

### 트램펄린 적용: 스택 넘침에 대한 일반적 해법

위에서 만든 `IO` 는 꼭 `IO` 에만 쓸 수 있는 것은 아니다. 스택 넘침 문제를 해결할 수 있는 일반적인 해법이다.

```scala
val f = (x: Int) => x
val g = List.fill(100000)(f).foldLeft(f)(_ compose _)
g(42)
// 스택오버플로우 에러
```

이 문제를 앞에서 만든 `IO` 로 해결할 수 있다.

```scala
val f = (x: Int) => Return(x)
val g = List.fill(100000)(f).foldLeft(f) {
  (a, b) => x => Suspend(() => ()).flatMap {
    _ => a(x).flatMap(b)
  }
}
run(g(42))
// 스택오버플루우 문제가 없음
```

따라서 `IO` 는 입출력을 위한 모나드가 아니고 꼬리 호출을 위한 모나드다. 그래서 이름을 바꿔보자.

```scala
sealed trait TailRec[A] {
  def flatMap[B](f: A => TailRec[B]): TailRec[B] =
    FlatMap(this, f)
  def map[B](f: A => B): TailRec[B] =
    flatMap(f andThen (Return(_)))
}

case class Return[A](a: A) extends TailRec[A]
case class Suspend[A](resume: () => A) extends TailRec[A]
case class FlatMap[A, B](sub: TailRec[A], k: A => TailRec[B]) extends TailRec[B]
```

## 좀 더 정교한 IO 형식

위에서 만든 `TailRec` 는 결국 중첩된 `FlatMap` 안에 있는 `Suspend` 구조에 `resume` 함수를 실행하는 것이다. `resume` 함수는 `() => A` 인데(`Function0` 타입) 이것은 그냥 `A` 값을 주는 것 외에 아무런 정보가 없다. 비동기로 실행할 방법도 없다. 그래서 `resume` 함수를 `() => A` 대신 `Par[A]` 로 바꾸면 비동기로 실행할 수 있다. 그런 형식을 `TailRec` 를 조금 고쳐 `Aysnc` 라고 하자.

```scala
sealed trait Async[A] {
  def flatMap[B](f: A => Async[B]): Async[B] =
    FlatMap(this, f)
  def map[B](f: A => B): Async[B] =
    flatMap(f andThen (Return(_)))
}

case class Return[A](a: A) extends Async[A]
case class Suspend[A](resume: Par[A]) extends Async[A]
case class FlatMap[A, B](sub: Async[A], k: A => Async[B]) extends Async[B]
```

이제 `Aysnc` 를 실행할 `run` 를 만들어보자.

```scala
@annotation.tailrec
def step[A](async: Async[A]): Async[A] = async match {
  case FlatMap(FlatMap(x,f), g) => step(x flatMap (a => f(a) flatMap g))
	case FlatMap(Return(x), f) => step(f(x))
  case _ => async
}

def run[A](async: Async[A]): Par[A] = step(async) match {
  case Return(a) => Par.unit(a)
  case Suspend(r) => Par.flatMap(r)(a => run(a))
  case FlatMap(x, f) => x match {
    case Suspend(r) => Par.flatMap(r)(a => run(f(a)))
    case _ => sys.error("Imposiible; `step` eliminates these cases")
  }
}
```

이제 `Par` 를  이용해 비동기로 실행할 수 있다. 그럼 더 일반화 해서 `Par[A]` 대신 `F[A]` 면 어떨까? 그런 추상을 `Free` 라고 하자.

```scala
sealed trait Free[F[_],A]
case class Return[F[_],A](a: A) extends Free[F,A]
case class Suspend[F[_],A](resume: F[A]) extends Free[F,A]
case class FlatMap[F[_],A,B](sub: Free[F,A], k: A => Free[F,B]) extends Free[F,B]
```

이제 `TailRec` 과 `Async` 는 `Free` 의 특별한 버전이기 때문에 타입 별칭으로 정의하면 된다.

```scala
type TailRec[A] = Free[Function0, A]
type Async[A] = Free[Par, A]
```

### 자유 모나드

* [연습문제] `Free[F,_]` 에대한 모나드 인스턴스를 만들어라.

  ```scala
  def freeMonad[F[_]]: Monad[({type f[a] = Free[F,a]})#f] =
     new Monad[({type f[a] = Free[F,a]})#f] {
       def unit[A](a: => A) = Return(a)
       def flatMap[A,B](fa: Free[F, A])(f: A => Free[F, B]) = fa flatMap f
     }
  ```

* [연습문제] `Free[Function0, A]` 를 실행하도록 특화된 해석기를 만들어라.

  ```scala
  @annotation.tailrec
    def runTrampoline[A](a: Free[Function0,A]): A = (a) match {
      case Return(a) => a
      case Suspend(r) => r()
      case FlatMap(x,f) => x match {
        case Return(a) => runTrampoline { f(a) }
        case Suspend(r) => runTrampoline { f(r()) }
        case FlatMap(a0,g) => runTrampoline { a0 flatMap { a0 => g(a0) flatMap f } }
      }
    }
  ```

* [어려운 연습문제] `Monad[F]` 가 있을 때 `Free[F,A]` 에대한 일반적인 해석기를 만들어라.

  ```scala
  def run[F[_],A](a: Free[F,A])(implicit F: Monad[F]): F[A] = step(a) match {
    case Return(a) => F.unit(a)
    case Suspend(r) => r
    case FlatMap(Suspend(r), f) => F.flatMap(r)(a => run(f(a)))
    case _ => sys.error("Impossible, since `step` eliminates these cases")
  }
  ```

위에서 본 `Free` 형식을 자유 모나드라고 부른다. 이것은 `A` 형식의 값을 `flatMap`으로 여러개 `F` 층으로 감싼 재귀적 자료 구조다. 그리고 결과를 얻기 위해 해석기는 `F` 를 모두 처리해야한다. 

### 콘솔 입출력만 지원하는 모나드



### 순수 해석기



## 비차단 비동기 입출력



## 범용 IO 형식

입출력을 수행하는 프로그램의 일반적인 방법론은 다음과 같다.

1. 각 연산을 나타내는 case 클래스가 있는 대수적 자료 형식 `F` 을 만든다.
2. 형식 `F` 에 대해 `Free[F,A]` 를 생성해 프로그램을 작성한다.

그러면 `type IO[A] = Free[Par,A]` 를 얻을 수 있고 이 형식은 순차실행과 비동기 실행 모두를 지원한다.

### 그 모든 것의 끝에 있는 주 프로그램

`main` 함수는 `Unit` 을 리턴한다. 비슷하게 `IO[Unit]` 을 리턴하는 `pureMain`을 만들고 내가 만든 프로그램에서는 순수한 `IO` 만 처리할 수 있는 `App` 이라는 추상을 만들어보자.

```scala
abstract class App {
  import java.util.concurrent._
  
  def unsafePerformIO[A](a: IO[A])(pool: ExecutorService): A =
    Par.run(pool)(run(a)(parMonad))
  
  def main(args: Array[String]): Unit = {
    val pool = Executors.fixedThreadPool(8)
    unsafePerformIO(pureMain(args))(pool)
  }
  
  def pureMain(args: IndexedSeq[String]): IO[Unit] // 상속 받는 클래스에서 구현해야한다.
}
```

## IO 형식이 스트림 방식 입출력에 충분하지 않은 이유



## 요약



