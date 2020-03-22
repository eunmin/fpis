package fpis

import java.util.concurrent.{ExecutorService, Executors}

import fpis.parallelism.Par
import fpis.parallelism.Par.Par
import fpis.state.RNG
import fpis.testing.Prop.{Falsified, Passed, Proved, forAll}
import fpis.testing.{Gen, Prop}

object Main extends App {

  // 8.1 속성 기반 검사의 간략한 소개
  // 8.2 자료 형식과 함수의 선택
  // 테스트 코드
  val intList = Gen.listOf(Gen.choose(0, 100)) // 0 부터 99까지 숫자를 만든다 (몇 개?)
  val prop = // 두 개의 속성을 모두 만족해야한다 && 조합
    forAll(intList)(ns => ns.reverse.reverse == ns) && // 속성 1 - 리스트는 뒤집고 뒤집으면 원래 리스트여야한다.
    forAll(intList)(ns => ns.headOption == ns.reverse.lastOption) // 속성 2 - 뒤집은 리스트의 마지막 항목은 원래 항목의 첫번째 항목이어야한다.

  // 테스트 코드 실행
  val maxSize = 10 // 검례 최소화(test case minimization) - 디버깅을 쉽게 하기 위해 테스트 개수를 줄여가면서 실패하는 최소 테스트 개수를 찾는다
  val testCases = 10 // 테스트 개수
  val rng = RNG.Simple(System.currentTimeMillis) // 랜덤 생성기 상태
  val result = prop.run(maxSize, testCases, rng) // 테스트 실행

  // 결과 출력
  result match {
    case Falsified(msg, n) => // 테스트 실패, n은 성공한 테스트 케이스 개수
      println(s"! Falsified after $n passed tests\n $msg")
    case Passed => // 모든 테스트 성공
      println(s"+ OK, passed all tests.")
  }

  // 리스트 크기를 지정할 수 있는 함수가 있으면 좋겠다.
  val intList2 = Gen.listOfN(8, Gen.choose(0, 100))
//  println(intList2.sample.run(rng))

  // flatMap으로 Gen을 연속으로 적용하기
  val ints = Gen.choose(0, 12)
//   println(ints.sample.run(rng))

  val doubleList = ints.flatMap(i => Gen.listOfN(i, Gen.choose(0.0, 0.1)))
  val doubles = for {
    i      <- Gen.choose(0, 12)
    double <- Gen.listOfN(i, Gen.choose(0.0, 0.1))
  } yield double
//  println(doubleList.sample.run(rng))
//  println(doubles.sample.run(rng))

  // 두개의 Gen 중에서 동일한 확률로 한 쪽 Gen을 선택
  val value = Gen.union(Gen.choose(0, 10), Gen.choose(10, 20))
// println(value.sample.run(rng))

  // 두개의 Gen과 확률을 받아서 한 쪽 Gen을 선택
  val value2 = Gen.weighted(
    (Gen.choose(0, 10), 0.1),
    (Gen.choose(10, 20), 0.9)
  )
//  println(value2.sample.run(rng))

  // 연습문제 8.9
  // 어디에서 실패했는지 tag를 달아서 알아내기
  val intList3 = Gen.listOfN(100, Gen.choose(0, 100))
  val prop3 =
    forAll(intList)(ns => ns.reverse.reverse == ns)
      .tag("리스트는 뒤집고 뒤집으면 원래 리스트여야한다.") &&
    forAll(intList)(ns => ns.headOption == ns.reverse.headOption)
      .tag("뒤집은 리스트의 마지막 항목은 원래 항목의 첫번째 항목이어야한다")

//  val result3 = prop3.run(10, 10, RNG.Simple(System.currentTimeMillis))
//
//  result3 match {
//    case Falsified(msg, n) =>
//      println(s"! Falsified after $n passed tests:\n $msg")
//    case Passed =>
//      println(s"+ OK, passed all tests.")
//  }

  // 8.3 검례 최소화
  // 위 예제에서 실패 했지만 생성기의 최대 값을 가진 SGen에 의해 작은 샘플 값이 결과로 출력되었다.
  val gen = Gen.choose(0, 100)
  val sgen = gen.unsized

  // 8.4 라이브러리의 사용과 사용성 개선
  val smallInt  = Gen.choose(-10, 10)
  val maxProp = forAll(Gen.listOf(smallInt)) { ns =>
    val max = ns.max
    !ns.exists(_ > max)
  }
  // ! maxProp는 스택 오버플로우가 발생함

  val intList4 = Gen.listOfN(100, Gen.choose(0, 100))
  val prop4 =
    forAll(intList)(ns => ns.reverse.reverse == ns)
      .tag("리스트는 뒤집고 뒤집으면 원래 리스트여야한다.") &&
      forAll(intList)(ns => ns.headOption == ns.reverse.headOption)
        .tag("뒤집은 리스트의 마지막 항목은 원래 항목의 첫번째 항목이어야한다")

//  Prop.run(prop4)

  // map(unit(1))(_ + 1) == unit(2) 를 증명하기
  val ES: ExecutorService = Executors.newCachedThreadPool
  val p1 = Prop.forAll(Gen.unit(Par.unit(1)))( i =>
    Par.map(i)(_ + 1)(ES).get == Par.unit(2)(ES).get
  )

//  Prop.run(p1)
  val p2 = Prop.check {
    val p = Par.map(Par.unit(1))(_ + 1)
    val p2 = Par.unit(2)
    p(ES).get == p2(ES).get
  }

  def equal[A](p: Par[A], p2: Par[A]): Par[Boolean] =
    Par.map2(p,p2)(_ == _)

  val p3 = Prop.check {
    equal (
      Par.map(Par.unit(1))(_ + 1),
      Par.unit(2)
    ) (ES).get
  }

  val S = Gen.weighted(
    Gen.choose(1,4).map(Executors.newFixedThreadPool) -> .75,
    Gen.unit(Executors.newCachedThreadPool) -> .25)

  def forAllPar[A](g: Gen[A])(f: A => Par[Boolean]): Prop =
    forAll(S.map2(g)((_,_))) { case (s,a) => f(a)(s).get }

  def checkPar(p: Par[Boolean]): Prop =
    forAllPar(Gen.unit(()))(_ => p)

  val p4 = checkPar {
    equal (
      Par.map(Par.unit(1))(_ + 1),
      Par.unit(2)
    )
  }

  // 8.5 고차 함수의 검사와 향후 개선 방향
  // 어려운 연습문제 8.19

  // 8.6 생성기의 법칙
  // Gen에 있는 map은 Par나 다른 형식과 같고 같은 속성을 가진다.
  // def map[A, B](a: Par[A])(f: A => B): Par[B]
  // def map[B](f: A => B): Gen[B]
  // map(x)(id) = x

  // 8.7 요약
  // 함수적 라이브러리 설계를 잘해봤다.
}
