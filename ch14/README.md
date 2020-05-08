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



### 변이 가능 참조의 대수

### 변이 가능 상태 동작의 실행

### 변이 가능 배열

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

```

```

[연습문제] `qs` 함수를 만들어라

```

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

### 부수 효과로 간주되는 것은 무엇인가?

## 요약

