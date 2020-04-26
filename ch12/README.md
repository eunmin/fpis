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
 
 ...
 
 ## Applicative Fuctor의 장점
 
 - 최소한의 가정으로 `traverse` 같은 것을 만드는 것이 좋다. 항상 모나드여야 한다면 모나드 마다 `traverse`를 따로 만들어야한다.
 - Applicative 효과의 해석기가 더 유연하다. flatMap은 파서를 동적으로 생성한다는 것이고 해석기가 할 수 있는 일이 제한한다.
 - Applicative functor는  합성할 수 있지만 Monad는 일반적으로 합성할 수 없다.
 
 ### 모든 Applicative Functor가 Monad는 아니다.
 
 #### Applicative 스트림
 
 #### 유효성 점검: 오류를 누적하는 Either 변종
 
 ## Applicative Functor 법칙
 
 ## 순회 가능 함수자 (Traversable Functor)
 
 ## Traverse의 용도
 
 ### 모노이드에서 Applicative Functor로
 
 ### 상태가 있는 순회
 
 ### Traverse 구조의 조합
 
 ### 순회의 융합
 
 ### 중첩된 순회
 
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
 