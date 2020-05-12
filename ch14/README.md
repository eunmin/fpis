# 지역 효과와 변이 가능 상태

## 순수 함수적 변이 가능 상태

함수형 프로그래밍은 변경 가능한 상태를 쓸 수 없다는 것은 아니다.

다만 참조 투명성과 순수성을 지키면 지역적으로 변경하는 것은 문제가 되지 않는다.

```
참조 투명성과 순수성의 정의

만일 모든 프로그램 p에 대해 표현식 e의 모든 출현을 e의 평가 결과로 치환해도 p의 의미에 아무런 영향을 미치지 않는다면, 그 표현식 e는 참조에 투명하다

만일 표현식 f(x)가 참조 투명한 모든 x에 대해 참조 투명하다면, 함수 f는 순수하다.
```

다음 함수는 순수 함수다.

```scala
def quicksort(xs: List[Int]): List[Int] = if (xs.isEmpty) xs else {
  val arr = xs.toArray
  def swap(x: Int, y: Int) = {
    val tmp = arr(x)
    arr(x) = arr(y)
    arr(y) = tmp
  }
  def partition(n: Int, r: Int, pivot: Int) = {
    val pivotVal = arr(pivot)
    swap(pivot, r)
    var j = n
    for (i <- n until r) if (arr(i) < pivotVal) {
      swap(i,j)
      j += 1
    }
    swap(j, r)
    j
  }
  def qs(n: Int, r: Int): Unit = if (n < r) {
    val pi = partition(n,r,n+(r-n)/2)
    qs(n, pi - 1)
    qs(pi + 1, r)
  }
  qs(0, arr.length - 1)
  arr.toList
}
```

위 함수는 외부에서 보기에 순수하기 때문에 순수함수다. 성능상 더 좋기도 하다. 원칙적으로 구현에서 지역 부수 효과를 사용해 순수 함수를 만드는 것에는 아무런 문제가 없다.

## 부수 효과를 지역 범위로 한정하는 자료 형식

`quicksort` 안에서 사용한 상태가 외부에서 관찰된다면 문제가 발생할 수 있다. 그래서 안전하게 스칼라 타입 시스템으로 변이를 지역으로 한정하는 자료 구조를 만들어보자.

### 범위 있는 변이를 위한 작은 언어

State 모나드(`S => (A,S)`)는 실제 mutation을 하는 것이 아니기 때문에 쓸 수 없다. 그래서 안전하게 실행될 수 있는 효과를 가지는 새로운 모나드 형식을 만들어보자.

```scala
sealed trait ST[S,A] { self =>
	protected def run(s: S): (A,S)
  def map[B](f: A => B): ST[S,B] = new ST[S,B] {
    def run(s: S) = {
      val (a, s1) = self.run(s)
      (f(a), s1)
    }
  }
  def flatMap[B](f: A => ST[S,B]): ST[S,B] = new ST[S,B] {
    def run(s: S) = {
      val (a, s1) = self.run(s)
      f(a).run(s1)
    }
  }
}

object ST {
  def apply[S,A](a: => A) = {
    lazy val memo = a
    new ST[S,A] {
    	def run(s: S) = (memo, s)
    }
  }
}
```



### 변이 가능 참조의 대수

```scala
sealed trait STRef[S,A] {
  protected var cell: A // 변수
  def read: ST[S,A] = ST(cell)
  def write(a: A): ST[S,Unit] = new ST[S,Unit] {
    def run(s: S) = {
      cell = a // 변수 할당
      ((), s)
    }
  }
}

object STRef {
  def apply[S,A](a: A): ST[S, STRef[S,A]] = ST(new STRef[S,A] {
    var cell = a
  })
}
```



```scala
val p: ST[Nothing, STRef[Nothing,Int]] = for {
  r1 <- STRef[Nothing,Int](1)
  r2 <- STRef[Nothing,Int](1)
  x <- r1.read
  y <- r2.read
  _ <- r1.write(y + 1)
  _ <- r2.write(x + 1)
  a <- r1.read
  b <- r2.read
} yield (a,b)
```

`p` 는 실행할 수 없다. `ST` 의 `run` 이 실행하는 함수인데 `protected` 로 되어 있기 때문이다.

### 변이 가능 상태 동작의 실행

```scala
trait RunableST[A] {
  def apply[S]: ST[S,A]
}

val p = new RunableST[(Int,Int)] {
  def apply[S] = for {
    r1 <- STRef(1)
    r2 <- STRef(2)
    x <- r1.read
    y <- r2.read
    _ <- r1.write(y + 1)
    _ <- r2.write(x + 1)
    a <- r1.read
    b <- r2.read
  } yield (a,b)
}
```

```scala
object ST {
  def apply[S,A](a: => A) = {
    lazy val memo = a
    new ST[S,A] {
    	def run(s: S) = (memo, s)
    }
  }
  def runST[A](st: RunableST[A]): A =
    st.apply[Unit].run(())._1 // ST의 run을 실행 할 수 있다.
}
```

이제 위 `p` 를 실행할 수 있다.

```scala
val r = ST.runST(p)
r: (Int, Int) = (3,2)
```

`RunableST` 로 `STRef` 를 꺼낼 수 는 없기 때문에 내부 상태는 밖으로 나갈 수 없다.

```scala
new RunableST[STRef[Nothing,Int]] {
  def apply[S] = STRef(1)
}
// error: type mismatch;
// found   : ST[S,STRef[S,Int]]
// required: ST[S,STRef[Nothing,Int]] ...
```

하지만 우회해서 빼낼 수 있지만 `STRef` 의 `read`, `wirte` 에는 접근할 수 없어 안전하다.

### 변이 가능 배열

```scala
sealed abstract class STArray[S,A](implicit manifest: Manifest[A]) {
  protected def value: Array[A]
  def size: ST[S,Int] = ST(value.size)

  def write(i: Int, a: A): ST[S,Unit] = new ST[S,Unit] {
    def run(s: S) = {
      value(i) = a
      ((), s)
    }
  }

  def read(i: Int): ST[S,A] = ST(value(i))

  def freeze: ST[S,List[A]] = ST(value.toList) // 불변형으로 바꿈
}

object STArray {
  def apply[S,A:Manifest](sz: Int, v: A): ST[S,STArray[S,A]] =
    ST(new STArray[S,A]) {
      lazy val value = Array.fill(sz)(v)
    }

  def fromList[S,A:Manifest](xs: List[A]): ST[S,STArray[S,A]] =
    ST(new STArray[S,A]) {
      lazy val value = xs.toArray
    }
}
```



### 순수 함수적 제자리 quicksort

이제 `quicksort` 는 `ST` 자료 형식으로 만들 수 있다. 먼저 `swap`은 아래와 같다.

```scala
def swap[S](i: Int, j: Int): ST[S, Unit] = for {
  x <- read(i)
  y <- read(j)
  _ <- write(i, y)
  _ <- write(j, x)
} yield ()
```

[연습문제] `partition` 함수를 만들어라

```scala
def noop[S] = ST[S,Unit](())

def partition[S](a: STArray[S,Int], l: Int, r: Int, pivot: Int): ST[S,Int] = for {
  vp <- a.read(pivot)
  _ <- a.swap(pivot, r)
  j <- STRef(l)
  _ <- (l until r).foldLeft(noop[S])((s, i) => for {
    _ <- s
    vi <- a.read(i)
    _  <- if (vi < vp) (for {
      vj <- j.read
      _  <- a.swap(i, vj)
      _  <- j.write(vj + 1)
    } yield ()) else noop[S]
  } yield ())
  x <- j.read
  _ <- a.swap(x, r)
} yield x
```

[연습문제] `qs` 함수를 만들어라

```scala
def qs[S](a: STArray[S,Int], l: Int, r: Int): ST[S, Unit] = if (l < r) for {
  pi <- partition(a, l, r, l + (r - l) / 2)
  _ <- qs(a, l, pi - 1)
  _ <- qs(a, pi + 1, r)
} yield () else noop[S]
```

위에서 만든 함수로 다시 만든 `quicksort` 함수는 아래와 같다.

```scala
def quicksort(xs: List[Int]): List[Int] =
	if (xs.isEmpty) xs else ST.runST(new RunableST[List[Int]]) {
    def apply[S] = for {
      arr <- STArray.fromList(xs)
      size <- arr.size
      _ <- qs(arr, 0, size - 1)
      sorted <- arr.freeze
    } yield sorted
  }
```

`ST` 모나드는 자료를 변이하긴 하지만 순수한 함수를 만들 수 있다. 그리고 타입 시스템을 이용해 안전하게 조합할 수 있도록 강제한다.

## 순수성은 문맥에 의존한다

```scala
case class Foo(s: String)

val b = Foo("hello") == Foo("hello") // true

val c = Foo("hello") eq Foo("hello") // false
```

모든 생성자는 부수효과가 있다. `eq` 를 쓰지 않으면 문제가 없다. 문맥에서 이것은 부수 효과가 아니라고 말할 수 있다.

이 책에서 말하는 참조 투명성 정의는 이 점을 고려하지 않는다.

```
좀 더 일반적인 참조 투명성의 정의

만일 어떤 프로그램 p에서 표현식 e의 모든 출현을 e의 평가 결과로 치환해도 p의 의미가 아무런 영향을 미치지 않는다면, 그 프로그램 p 에 관해 표현식 e는 참조에 투명하다.
```

```scala
val v = e
```
e의 참조투명성은 모든 e의 출현을 v로 바꿔도 프로그램의 의미가 변하지 않는다는 것을 말한다.

프로그램의 의미가 바뀌지 않는다는 말은 철학적인 질문이다.

참조투명성을 말할 때는 어떤 문백이 관여한다.

### 부수 효과로 간주되는 것은 무엇인가?

```scala
def timesTwo(x: Int) = {
  if (x < 0) println("Got a negative number")
  x * 2
}
```

위 프로그램은 부수효과가 있다. 하지만 정말 참조 투명한지 이야기하려면 표준 출력을 관측할 필요가 있는지 선택해야한다.

## 요약

자료의 변이가 지역을 벗어나지 않는다면 문제가 없다. 그리고 안전한 지역 변이를 위한 자료 형식을 만들어봤다.

또 부수 효과로 간주 할 것은 프로그래머의 선택이라는 것도 알아봤다.
