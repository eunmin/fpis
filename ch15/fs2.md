# FS2

## 스트림 만들기

```scala
import fs2.Stream

val s0 = Stream.empty
// s0: Stream[fs2.package.Pure, Nothing] = Stream(..)
val s1 = Stream.emit(1)
// s1: Stream[Nothing, Int] = Stream(..)
val s1a = Stream(1,2,3) // variadic
// s1a: Stream[Nothing, Int] = Stream(..) // variadic
val s1b = Stream.emits(List(1,2,3)) // accepts any Seq
// s1b: Stream[Nothing, Int] = Stream(..)
```

순수한 스트림은 리스트나 벡터로 바꿀 수 있다.

```scala
s1.toList
// res0: List[Int] = List(1)
s1.toVector
// res1: Vector[Int] = Vector(1)
```

스트림은 리스트 함수 같은 함수가 많이 있다. 아래는 몇가지 예다.

```scala
(Stream(1,2,3) ++ Stream(4,5)).toList
// res2: List[Int] = List(1, 2, 3, 4, 5)
Stream(1,2,3).map(_ + 1).toList
// res3: List[Int] = List(2, 3, 4)
Stream(1,2,3).filter(_ % 2 != 0).toList
// res4: List[Int] = List(1, 3)
Stream(1,2,3).fold(0)(_ + _).toList
// res5: List[Int] = List(6)
Stream(None,Some(2),Some(3)).collect { case Some(i) => i }.toList
// res6: List[Int] = List(2, 3)
Stream.range(0,5).intersperse(42).toList
// res7: List[Int] = List(0, 42, 1, 42, 2, 42, 3, 42, 4)
Stream(1,2,3).flatMap(i => Stream(i,i)).toList
// res8: List[Int] = List(1, 1, 2, 2, 3, 3)
Stream(1,2,3).repeat.take(9).toList
// res9: List[Int] = List(1, 2, 3, 1, 2, 3, 1, 2, 3)
Stream(1,2,3).repeatN(2).toList
// res10: List[Int] = List(1, 2, 3, 1, 2, 3)
```

순수한 스트림만 봤는데 스트림 안에 효과가 있을 수 있다.

```scala
import cats.effect.IO

val eff = Stream.eval(IO { println("BEING RUN!!"); 1 + 1 })
// eff: Stream[IO, Int] = Stream(..)
```



```scala
def eval[F[_],A](f: F[A]): Stream[F,A]
```



```scala
eff.toList
// error: value toList is not a member of fs2.Stream[cats.effect.IO,Int]
// val rb = eff.compile.drain // purely for effects
//    
```



```scala
eff.compile.toVector.unsafeRunSync()
// BEING RUN!!
// res12: Vector[Int] = Vector(2)	
```



```scala
val ra = eff.compile.toVector // gather all output into a Vector
// ra: IO[Vector[Int]] = <function1> // gather all output into a Vector
val rb = eff.compile.drain // purely for effects
// rb: IO[Unit] = <function1> // purely for effects
val rc = eff.compile.fold(0)(_ + _) // run and accumulate some result
// rc: IO[Int] = <function1>
```



```scala
ra.unsafeRunSync()
// BEING RUN!!
// res13: Vector[Int] = Vector(2)
rb.unsafeRunSync()
// BEING RUN!!
rc.unsafeRunSync()
// BEING RUN!!
// res15: Int = 2
rc.unsafeRunSync()
// BEING RUN!!
// res16: Int = 2
```

## 청크

## 기본 스트림 연산

## 에러 처리

## 리소스 획득

## 예제 (스트림 만들기)

## 상태있는 스트림 변환

## 예제 (스트림 변환)

## 동시성

## 예제 (동시성)

## 인터럽트

## 외부 세계와 통신하기

## 리엑티브 스트림

## 더 배울거리

## 부록: 어떻게 스트림 동작을 멈출 수 있을까?

