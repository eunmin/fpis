# 순수 함수적 병렬성

- 병렬 및 비동기 계산을 위한 순수 함수적 라이브러리를 구축
- 순수 함수적 라이브러리 설계 문제에 대한 접근 방식을 배운다.
- 계산의 서술과 실행을 분리한다.
- `parMap`이라는 조합기를 만들어 함수 `f`를 한 컬렉션의 모든 항목에 동시에 적용할 수 있다.
  ```scala
  val outpuList = parMap(inputList)(f)
  ```
- 만들어가면서 대수적 추론에 역점을 둔다. API를 특정 법칙을 따르는 하나의 대수로 서술 술 할 수 있다는 것을
  소개한다.
- 부수 효과를 허용하지 않도록 만들자.

## 자료 형식과 함수의 선택

- "병렬 계산을 생성할 수 있어야 한다"는 것을 구현하기 위해 간단한 병렬 계산 예제를 보자.
  ```scala
  def sum(ints: Seq[Int]): Int =
      ints.foldLeft(0)((a,b) => a + b)
  ```
- 이 함수는 병렬화 하기 쉽도록 `foldLeft` 대신 분할 정복(divid-and-conquer)으로 다음과 같이 만들 수 있다.
  ```scala
  def sum(ints: IndexedSeq[Int]): Int = // IndexedSeq는 Vector와 비슷하게 임의 접근을 할 수 있어 효율적인 splitAt이 가능하다.
    if (ints.size <= 1)
      ints.headOption getOrElse 0
    else {
      val (l, r) = ints.splitAt(ints.length / 2)
      sum(l) + sum(r)
    }
  ```

### 병렬 계산을 위한 자료 형식 하나

- 병렬 계산을 표현하고 결과를 담을 수 있는 컨테이너 타입 `Par[A]`를 만들자. 그리고 필요한 함수는 아래와 같다.
```scala
trait Par[A]

object Par {
  def unit[A](a: => A): Par[A] = ???

  def get[A](a: Par[A]): A = ???
}
```
- 방금 만든 타입으로 `sum`을 다시 표현해보자.
```scala
def sum(ints: IndexedSeq[Int]): Int =
  if (ints.size <= 1)
    ints.headOption getOrElse 0
  else {
    val (l, r) = ints.splitAt(ints.length / 2)
    val sumL: Par[Int] = Par.unit(sum(l))
    val sumR: Par[Int] = Par.unit(sum(r))
    Par.get(sumL) + Par.get(sumR)
  }
```
- 위 예제를 병렬로 실행하겠다고 한다면 `unit`에 있는 `sum`이 별도의 스레드에서 실행되고 즉시 반환 해야한다. 그리고 `get`에서 실행이 끝나기를 기다려야한다. 
- 그런데 `get`에서 기다리면 병렬로 실행이되는가?

### 병렬 계산의 조합

- `get`을 호출하지 않는다면...
  ```scala
  def sum(ints: IndexedSeq[Int]): Par[Int] =
    if (ints.size <= 1)
      Par.unit(ints.headOption getOrElse 0)
    else {
      val (l, r) = ints.splitAt(ints.length / 2)
      Par.map2(sum(l), sum(r))(_ + _)
    }
  ```
  ```scala
  def map2[A,B,C](a: Par[A], b: Par[B])(f: (A, B) => C): Par[C] = ???
  ```
- `map2`의 `A`와 `B`는 엄격해야 할까? 게을러야 할까? `map2`를 추적해보면서 알아보자.
  ```scala
  sum(IndexedSeq(1,2,3,4))

  Par.map2(
    sum(IndexedSeq(1,2)),
    sum(IndexedSeq(3,4)))(_ + _)

  Par.map2(
    Par.map2(
      sum(IndexedSeq(1)),
      sum(IndexedSeq(2)))(_ + _),
    sum(IndexedSeq(3,4)))(_ + _)

  Par.map2(
    Par.map2(
      Par.unit(1),
      Par.unit(2))(_ + _),
    sum(IndexedSeq(3,4)))(_ + _)

  Par.map2(
    Par.map2(
      Par.unit(1),
      Par.unit(2))(_ + _),
    Par.map2(
      sum(IndexedSeq(3)),
      sum(IndexedSeq(4)))(_ + _))(_ + _)
  ```
- `map2`가 엄격하다면 안자에 `sum`을 만날때 마다 평가해야한다. 결국 엄격하게 평가하면 왼쪽 절반이 엄격하게
  구축되고 나서 오른쪽 절반이 구축되는 문제가 있다. `sum(IndexedSeq(1,2))`가 다 전개되고 나서
  `sum(IndexedSeq(3,4))`가 시작
- `map2` 인수가 병렬적으로 평가된다면 오른쪽 절반을 구축하기 전에 왼쪽 절반이 실행되기 시작한다는 말이다.
- 만약 `map2`가 엄격하지만 즉시 시작 되지 않도록 서술만 가지고 있다고 한다면 서술은 평가되기 전까지 전체 트리를
  가지고 있기 때문에 어떤 경우 많은 공간을 차지하게 된다.
- 결국 `map2`를 게으르게 만들고 인수를 병렬로 실행하는 것이 좋을 것 같다.

### 명시적 분기

- `map2`의 인수를 병렬로 평가하면 항상 좋은 것은 아니다.
  ```scala
  Par.map2(Par.unit(1), Par.unit(2))(_ + _)
  ```
- 위 예제는 계산이 아주 간단하기 때문에 논리적 스레드를 띄워 실행할 필요가 없다.
- 그래서 명시적으로 논리적 스레드를 띄워 실행하는 것을 표현하도록 해보자.
  ```scala
  def sum(ints: IndexedSeq[Int]): Par[Int] =
    if (ints.size <= 1)
      Par.unit(ints.headOption getOrElse 0)
    else {
      val (l, r) = ints.splitAt(ints.length / 2)
      Par.map2(Par.fork(sum(l)), Par.fork(sum(r)))(_ + _)
    }
  ```
  ```scala
  def fork[A](a: => Par[A]): Par[A] = ???
  ```
- 이렇게 하면 `fork`가 즉시 반환 되기 때문에 `map2`의 인수를 엄격하게 해도 된다.
- 이제 결합 수단이 필요하고 비동기적으로 수행할지 아닐지 선택하는 수단도 필요하다 ?????
- `fork`가 있기 때문에 `unit`은 이제 엄격하게 만들어도 문제가 없다. 또 비엄격 버전은 `fork`로 만들수
  있다.
  ```scala
  def unit[A](a: A): Par[A] = ???

  def lazyUnit[A](a: => A): Par[A] = fork(unit(a))
  ```
- 이제 `fork`의 평가를 `fork`에서 할지 `get`에서 할지에 대해 알아보자. (eager vs lazy)
- 이를 결정하기 위해 `fork`와 `get`의 구현에 어떤 정보가 필요한가를 생각해보면 좋다.
- 만약 `fork`에서 평가한다면 `fork`는 스레드 생성 방법에 대해 직간접적으로 알고 있어야 한다.
- 그 말은 스레드 자원(예를 들면 스레드풀)이 전역적으로 접근 가능해야한다는 말이고 좋지 않다.
- 따라서 평가는 `get`에서 하는 것이 더 좋을 것 같다.
- 그렇다면 `fork`는 평가되지 않은 `Par` 인수를 받고 동시적 평가가 필요하다는 정보만 해두면 된다.
- 결국 `Par`는 병렬 계산에 필요한 서술에 가깝다.
- 그래서 `get`는 그냥 담고 있는 값을 주는 함수라기 보다 실행을 하는 역할이라 이름을 `run`으로 바꾸는 것이
  더 좋을 것 같다.
- `Par`는 순수한 자료 구조다. `run`에 따라 동작은 달라진다.

## 표현의 선택

- 지금까지 만든 API는 다음과 같다.
  ```scala
  def unit[A](a: A): Par[A] = ???
  def map2[A,B,C](a: Par[A], b: Par[B])(f: (A, B) => C): Par[C] = ???
  def fork[A](a: => Par[A]): Par[A] = ???
  def lazyUnit[A](a: => A): Par[A] = fork(unit(a))
  def run[A](a: Par[A]): A = ???
  ```
- `unit`은 상수 값을 병렬 계산으로 승격한다.
- `map2`는 두 병렬 계산의 결과를 이항 함수로 조합한다.
- `fork`는 주어진 인수가 동시에 평가될 계산임을 나타낸다. 실행은 되지 않고 `run`을 해야 실행 됨
- `lazyUnit`은 평가되지 않은 인수를 병렬 계산으로 승격한다.
- `run`은 계산을 실행해서 값을 추출한다.
- `run`을 구현하기 위해 구현에 필요한 `java.util.concurrent.ExcutorService`를 먼저 살펴보자.
  ```scala
  class ExecutorService {
    def submit[A](a: Callable[A]): Future[A] = ???
  }
  trait Callable[A] { def call: A }
  trait Future[A] {
    def get: A
    def get(timeout: Long, unit: TimeUnit): A
    def cancel(evenIfRunning: Boolean): Boolean
    def isDone: Boolean
    def isCancelled: Boolean
  }
  ```
- `run`의 가장 쉬운 표현은 다음과 같다.
  ```scala
  def run[A](s: ExecutorService)(a: Par[A]): A
  ```
- 하지만 `Future`에 있는 취소기능 같은 것을 쓰지 못하기 때문에 값 대신 `Future`를 주는 것이 더 좋을 것 같다.
- 이를 위해 `Par[A]`와 `run`을 고쳐보자.
  ```scala
  type Par[A] = ExecutorService => Future[A]

  def run[A](s: ExecutorService)(a: Par[A]): Future[A] = a(s)
  ```
- 이제 지금까지 나온 API를 구현해보자.

## API의 정련

- 지금까지 논의로 구현한 `Par`는 다음과 같다.
  ```scala
  object Par {
    type Par[A] = ExecutorService => Future[A]

    def unit[A](a: A): Par[A] = (es: ExecutorService) => UnitFuture(a)

    private case class UnitFuture[A](get: A) extends Future[A] {
      def isDone = true
      def get(timeout: Long, units: TimeUnit) = get
      def isCancelled = false
      def cancel(evenIfRunning: Boolean): Boolean = false
    }

    def map2[A,B,C](a: Par[A], b: Par[B])(f: (A, B) => C): Par[C] =
      (es: ExecutorService) => {
        val af = a(es)
        val bf = b(es)
        UnitFuture(f(af.get, bf.get)) // Timeout이 없음
      }

    def fork[A](a: => Par[A]): Par[A] =
      es => es.submit(new Callable[A] {
        def call = a(es).get
      })

    def lazyUnit[A](a: => A): Par[A] = fork(unit(a))

    def run[A](s: ExecutorService)(a: Par[A]): Future[A] = a(s)
  }
  ```
- 중요한 점은 `Future`는 순수 함수적이지 않으나 `Par`는 순수하다는 점이다.
- [연습문제] Future의 만료시간을 존중하도록 map2를 개선하라
- [연습문제] 일반 함수 `A => B`를 비동기로 평가되도록 변환하는 함수를 `lazyUnit`으로 만들어라
  ```scala
  def asyncF[A,B](f: A => B): A => Par[B]
  ```
- 다른 함수 예를 하나 더 살펴보자. 다음은 병렬 계산을 나타내는 `Par[List[Int]]`을 정렬하는 함수다.
  ```scala
  def sortPar(parList: Par[List[Int]]): Par[List[Int]]
  ```
- 이 함수를 구현하려면 `run`을 하고 정렬을 해서 다시 `Par`로 싸면 되지만 `run`을 하지 않고 구현해보자.
  ```scala
  def sortPar(parList: Par[List[Int]]): Par[List[Int]] =
    map2(parList, unit(()))((a, _) => a.sorted)
  ```
- 이 형태를 가지고 승급 시키는 `map` 함수는 다음과 같이 만들 수 있다.
  ```scala
  def map[A,B](pa: Par[A])(f: A => B): Par[B] =
    map2(pa, unit(()))((a, _) => f(a))
  ```
- `sortPar`는 `map`을 이용해서 만들 수 있다.
  ```scala
  def sortPar(parList: Par[List[Int]]): Par[List[Int]] =
    map(parList)(_.sorted)
  ```
- `map2`로 `map`을 만들 수 있지만 `map`으로는 `map2`를 만들 수 없다.
- `map2`를 일반화하는 `parMap`을 만들어보자.
  ```scala
  def parMap[A,B](ps: List[A])(f: A => B): Par[List[B]] = ???
  ```
- 구현은 `A => Par[B]`인 `asyncF`로 시작 할 수 있다. 리스트의 모든 항목에 `asyncF`를 적용하면 된다.
  ```scala
  def parMap[A,B](ps: List[A])(f: A => B): Par[List[B]] = {
    val fbs: List[Par[B]] = ps.map(asyncF(f))
    ...
  }
  ```
- 다음으로 적용된 결과인 `List[Par[B]]`를 모두 합쳐 `Par[List[B]]`로 취합하는 수단이 필요하다.
- [연습문제] `sequence`로 할 수 있다. `run`은 호출하지 말것
  ```scala
  def sequnce[A](ps: List[Par[A]]): Par[List[A]] = ???
  ```
- 다음은 `sequnce`로 `parMap`을 완성할 수 있다.
  ```scala
  def parMap[A,B](ps: List[A])(f: A => B): Par[List[B]] = {
    val fbs: List[Par[B]] = ps.map(asyncF(f))
    sequnce(fbs)
  }
  ```
- [연습문제] 목록을 병렬로 걸러내는 `parFilter`를 구현하라.
  ```scala
  def parFilter[A](as: List[A])(f: A => Boolean): Par[List[A]] = ???
  ```

- 더 해볼만한 함수로 `sum`을 더 일반화 하고 최대 값을 병렬로 구해봐라, 또 `List[String]` 문단을
  받고 단어수를 돌려주는 함수를 일반화해봐라, 또 map3, map4, map5를 map2로 만들어봐라.

## API의 대수

- 여기서도 그 형식을 따라가다보면 구현이 저절로 되는 경우가 많다. 구체적 문제 해결보다 형식을 맞추는데
  집중하면 된다. 이 방법은 자연스러운 대수 추론 방식이다. (수학의 증명?)
- 여기서 발생한 법칙을 공식화 해보자.

### map에 관한 법칙

- 다음은 간단한 테스트 코드다.
  ```scala
  map(unit(1))(_ + 1) == unit(2)
  ```
- `unit(1)`에 `_ + 1`을 사상하면 `unit(2)`와 같다를 의미한다. 이 것을 두 `Par` 객체의 `Future`
  결과가 같다면 두 `Par` 객체는 동등하다 라고 말할 수 있다. 다음은 다른 테스트다.
  ```scala
  def equal[A](e: ExecutorService)(p: Par[A], p2: Par[A]): Boolean =
    p(e).get == p2(e).get
  ```
- 위 함수는 일반화 해서 아래처럼 쓸 수 있다.
  ```scala
  map(unit(x))(f) == unit(f(x))
  ```
- `f`를 `identity` 함수로 바꾸면 아래처럼 된다.
  ```scala
  map(unit(x))(f) == unit(f(x))
  map(unit(x))(id) == unit(id(x))
  map(unit(x))(id) == unit(x)
  map(y)(id) == y
  ```
- 이 법칙으로 `map`이 할 수 없는 것들을 알 수 있다. 예로 map은 예외를 던져서 안된다.
- 이 법칙은 반대로도 찹이어야 한다. `map(y)(id) == y` 면 `map(unit(x))(f) == unit(f(x))`
- 이런 것을 2차 법칙 또는 공짜 정리라고 부른다.

### fork에 관한 법칙

- `fork(x) == x` 법칙으로 `fork`는 `x`와 동일한 일을 해야한다.
- 증명해봐라
- FP에서 증명이 중요한 이유는 합성을 통해 재사용 가능한 것을 보장하기 위함이다. `fork` 법칙이 성립하지
  않는다면 `parMap`도 쓸 수 없다.

### 법칙 깨기: 미묘한 버그 하나

- `FixThreadPool`을 1개로 해서 `ExecutorService`를 만들면 동시 실행이 되지 않고 블러킹이 되어
  `fork(x) == x` 법칙이 깨진다. 아래 코드를 실행하면 교착 상태에 빠진다.
  ```scala
  val a = lazyUnit(42 + 1)
  val s = ExecutorService.newFixedThreadPool(1)
  println(Par.equal(s)(a, fork(a)))
  ```
- `fork`의 구현을 보면 알 수 있다. `submit`으로 새 스레드를 만들고 안에서 다시 `a(es).get`시에
  `lazyUnit`으로 만든 `fork(unit(a))`를 불러 유일한 스래드 안에서 다시 스래드를 생성하려고 대기하면서
  교착상태가 된다.
  ```scala
  def fork[A](a: => Par[A]): Par[A] =
    es => es.submit(new Callable[A] {
      def call = a(es).get
    })
  ```
- 교착을 막으려면 `fork`를 아래 스래드를 띄우지 않으면 된다.
  ```scala
  def fork[A](fa: -> Par[A]): Par[A] =
    es => fa(es)
  ```
- 하지만 스래드가 생성되지 않고 그냥 메인 스레드에서 실행되기 때문에 적절한 수정은 아니지만 이 기능도
  계산을 지연시킬 수 있다는 점에서 의미가 있어 이름을 `delay`라고 하고 두면 좋겠다.
  ```scala
  def delay[A](fa: -> Par[A]): Par[A] =
    es => fa(es)
  ```

### 행위자를 이용한 완전 비차단 Par 구현

- 고정 크기의 스래드 풀을 사용하면서 블러킹이 되지 않게 하기 위한 방법을 찾아보자.
- `Future`와 `run`을 다음과 같이 바꿔보자.
  ```scala
  trait Future[+A] {
    private[parallelism] def apply(k: A => Unit): Unit
  }

  // 원래 run
  // def run[A](s: ExecutorService)(a: Par[A]): Future[A] = a(s)

  def run[A](es: ExecutorService)(p: Par[A]): A = {
      val ref = new java.util.concurrent.atomic.AtomicReference[A]
      val latch = new CountDownLatch(1)
      p(es) { a => ref.set(a); latch.countDown }
      latch.await
      ref.get
    }
  ```

## 조합기들을 가장 일반적인 형태로 정련

- 

## 요약
