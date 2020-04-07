package fpis.parser

import fpis.parser.impl.{JSON, Reference, Sliceable}
import fpis.parser.impl.ReferenceTypes.Parser

object Main extends App {

  // 9. 파서 조합기 라이브러리
  // 대수(인터페이스 같은?)를 먼저 설계하고 구현은 나중에 하자. 9.6에서 한다.

  // 9.1 대수의 설계: 첫시도
  def p1[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    char('a') // a 문자 하나를 매칭해서 인식하는 파서
  }
  // 파서를 실행하는 run 함수
//  println(Sliceable.run(p1(Sliceable))("aaa"))







  def p2[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    string("abra") // abc 문자열을 매칭해서 인식하는 파서
  }
//  println(Sliceable.run(p2(Sliceable))("abra"))






  def p3[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    //or(string("abra"), string("cadabra")) // or를 이용해서 abra 또는 cadabra 문자열을 인식하는 파서 : | 연산자로 정의
    "abra" | "cadabra"
  }
//  println(Sliceable.run(p3(Sliceable))("cadabra"))






  def p4[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    listOfN(3, "ab" | "cad") // string("ab")는 "ab"로 쓸 수 있도록 암묵적 변환을 구현, ab cad가 3개 반복되는 것을 인식
  }
//  println(Sliceable.run(p4(Sliceable))("ababcad"))





  // 9.2 가능한 대수 하나
  def p6[Parser[+_]](P: Parsers[Parser]) = {
    import P._
//    many(char('a'))
    map(many(char('a')))(_.size) // 여러개의 a를 인식하고 결과 값을 map 함수로 변환 (크기)
  }
//  println(Sliceable.run(p6(Sliceable))("aaaaabb"))




  // map 법칙을 Laws에 만들자.




  // 9.2.1 슬라이싱과 비지 않은 되풀이
  def p7[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    slice(many(char('a') | char('b'))) // many로 매칭되는 것의 리스트를 만들어주지만 그냥 합쳐진 문자열이 나오는 것이 더 편리하게 쓸 수 있다.
  }
//  println(Sliceable.run(p7(Sliceable))("aaba"))





  def p8[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    many1(char('a')) // 하나 이상 a가 나와야 매칭된다. many 는 없어도 매칭이 됨
  }
//  println(Sliceable.run(p8(Sliceable))("aaba"))





  def p9[Parser[+_]](P: Parsers[Parser]) = {
    import P._
//    product(string("abra"), string("cadabra")) // 앞에 파서가 나오고 뒤에 파서가 매칭되어야 함 : ** 연산자로 정의
    "abra" ** "cadabra"
  }
//  println(Sliceable.run(p9(Sliceable))("abracadabra"))






  def p10[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    char('a').many.slice.map(_.length) ** char('b').many1.slice.map(_.length) // 0개 이상의 a가 나오고 다음에 1개 이상의 b가 나오는것에 매칭
  }
  // println(Sliceable.run(p10(Sliceable))("aabbbb")) // many1에서 에러!!

  // 연습문제 9.1
  // def map2[A,B,C](p: Parser[A], p2: => Parser[B])(f: (A,B) => C): Parser[C]

  // 9.3 문맥 민감성의 처리
  // flatMap 이 있으면 문맥 민감 파싱이 가능하다. 파서 두개를 조합할 때 두 파서가 분리된 것이 아니고 이전 파서의 결과를 다음 파서가 쓸 수 있다.
  // 연습문제 9.6
  // regex 만들기
  // 연습문제 9.7
  // flatMap 만들기

  // 9.4 JSON 파서 작성
  // 연습 문제 9.9
  val json: Parser[JSON] = JSON.jsonParser(Reference)
  val data = """
{
  "Company name" : "Microsoft Corporation",
  "Ticker"  : "MSFT",
  "Active"  : true,
  "Price"   : 30.66,
  "Shares outstanding" : 8.38e9,
  "Related companies" : [ "HPQ", "IBM", "YHOO", "DELL", "GOOG" ]
}
"""
//  println(Reference.run(json)(data))

  // 9.5 오류 보고
  def p11[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    val spaces = " ".many
    "abra" ** spaces ** "cadabra"
  }
//  println(Sliceable.run(p11(Sliceable))("abra cAdabra"))

  def p12[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    val spaces = " ".many
    label("첫번째 말")("abra") ** spaces ** label("두번째 말")("cadabra")
  }
//  println(Sliceable.run(p12(Sliceable))("abra cAdabra"))

  for {
    user1: Either[Throwable, User] <- findUser(1)
    email = getUserEmail(user1)

    result: Either[Throwable, Boolean] <- register(1, email)
  }
  // 9.5.2 오류의 중첩
  def p13[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    val spaces = " ".many
    scope("강력한 주문") {
      label("첫번째 말")("abra") ** spaces ** label("두번째 말")("cadabra")
    }
  }
//  println(Sliceable.run(p13(Sliceable))("abra cAdabra"))

  // 9.5.3 문기와 역추적의 제어
  def p14[Parser[+_]](P: Parsers[Parser]) = {
    import P._
    val spaces = " ".many
    // attempt는 앞쪽에 매칭에서 바로 실패하지 않고 "abra" 매칭 후에 뒤에 "abra"가 더올지 "cadabra!"가 올지 결정한다?
    (attempt("abra" ** spaces ** "abra") ** "cadabra") | ("abra" ** spaces ** "cadabra!")
  }

  println(Sliceable.run(p14(Sliceable))("aaa"))
//  println(Sliceable.run(p14(Sliceable))("abra cadabra!"))

  // 9.6 대수의 구현
  // 9.6.1 가능한 구현 하나
  // Parser를 run 함수의 구현이라고 가정
  // type Parser[+A] = String => Either[ParseError,A]
  // 9.6.2 파서들의 순차 실행
  // 9.6.3 파서에 이름표 붙이기
  // 9.6.4 실패의 극복과 역추적
  // 9.6.5 문맥 민감 파싱

  // 요약
  // 이전에 봤던 것들과 대수가 비슷하다.
  // 앞으로 이것들의 공통 구조를 추상화 하는 방법을 배워보자.
  // 함수적 라이브러리를 스스로 설계해보고 싶은 마음이 들었기를 기대한다 ㅋㅋㅋ
}
