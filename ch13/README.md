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

