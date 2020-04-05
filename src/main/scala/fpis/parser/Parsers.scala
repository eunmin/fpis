package fpis.parser

import java.util.regex.Pattern

import fpis.testing.{Gen, Prop}

import scala.util.matching.Regex

trait Parsers[Parser[+_]] { self =>

  // 9.1 대수의 설계: 첫 시도
  // 파서를 실행
  def run[A](p: Parser[A])(input: String): Either[ParseError,A]

  // 문자 하나와 매칭
  def char(c: Char): Parser[Char] = string(c.toString) map (_.charAt(0))

  // 문자열과 매칭 - 일반 문자열을 Parser[String]으로 만들어준다.
  implicit def string(s: String): Parser[String]

  // 둘 중 하나에 매칭
  def or[A](p1: Parser[A], p2: => Parser[A]): Parser[A]

  // Parser[A]를 ParserOps[A]로 만들어 Parser[A]가 ParserOps 함수를 쓸 수 있게 한다. (연산자)
  implicit def operators[A](p: Parser[A]) = ParserOps[A](p)

  implicit def asStringParser[A](a: A)(implicit f: A => Parser[String]): ParserOps[String] = ParserOps(f(a))

  // N 번 반복되는 것을 표현 - listOfN(3, "ab" | "cad"), "ababcad" => List("ab", "ab", "cad")
  def listOfN[A](n: Int, p: Parser[A]): Parser[List[A]] =
    if (n <= 0) succeed(List())
    else map2(p, listOfN(n-1, p))(_ :: _)

  // 9.2 가능한 대수 하나
  // 0개 이상에 매칭
  def many[A](p: Parser[A]): Parser[List[A]] = map2(p, many(p))(_ :: _) or succeed(List())

  // 파서의 결과 값을 바꾼다.
  // map(many(char('a')))(_.size) , a 개수 만큼 매칭하고 그 개수를 파서의 결과로 한다 Parser[Int]
  def map[A,B](a: Parser[A])(f: A => B): Parser[B] = flatMap(a)(f andThen succeed)

  // 법칙을 코드에 문서화 하자
  object Laws {
    def equal[A](p1: Parser[A], p2: Parser[A])(in: Gen[String]): Prop =
      Prop.forAll(in)(s => run(p1)(s) == run(p2)(s))

    def mapLaw[A](p: Parser[A])(in: Gen[String]): Prop =
      equal(p, p.map(a => a))(in)
  }

  // 입력 문자와는 무관하게 항상 a 값을 돌려주는 함수 - unit 같은것?
  def succeed[A](a: A): Parser[A]

  def defaultSucceed[A](a: A): Parser[A] = string("") map (_ => a)

  // 9.2.1 슬라이싱과 비지 않은 되풀이
  // run(slice(('a' | 'b').many))("aaba") => Right("aaba")
  def slice[A](p: Parser[A]): Parser[String]

  // 1개 이상에 매칭
  def many1[A](p: Parser[A]): Parser[List[A]] = map2(p, many(p))(_ :: _)

  // p가 성공하면 p2를 수행할 수 있는 대수 (** 연산자로 정의)
  def product[A,B](p: Parser[A], p2: => Parser[B]): Parser[(A,B)] = flatMap(p)(a => map(p2)(b => (a,b)))

  // 연습문제 9.1
  def map2[A,B,C](p: Parser[A], p2: => Parser[B])(f: (A,B) => C): Parser[C] = for { a <- p; b <- p2 } yield f(a,b)

  // 9.3 문맥 민감성의 처리

  // 연습문제 9.6
  implicit def regex(r: Regex): Parser[String]

  // 연습문제 9.7
  def flatMap[A,B](p: Parser[A])(f: A => Parser[B]): Parser[B]

  // label("first magic word")("abra") 와 같이 에러가 발생했을 때 메시지를 출력하기 위해 라벨을 만든다.
  def label[A](msg: String)(p: Parser[A]): Parser[A]

  // label은 중첩된 오류를 보여 줄 수 없다.
  // 예를들어 label("first magic word")("abra") ** " ".many ** label("second magic word")("cadabra")
  // 에서 ("abra cAdabra")라고 했을 때 메시지가 분리되어 버려 알기 어렵다.
  // 아래는 두개를 하나로 묶어 메시지를 표시할 수 있는 대수다.
  // scope("magic speel") {
  //   "abra" ** spaces ** "cadabra"
  // }
  def scope[A](msg: String)(p: Parser[A]): Parser[A]

  def attempt[A](p: Parser[A]): Parser[A]

  def skipL[B](p: Parser[Any], p2: => Parser[B]): Parser[B] = map2(slice(p), p2)((_,b) => b)

  def skipR[A](p: Parser[A], p2: => Parser[Any]): Parser[A] = map2(p, slice(p2))((a,b) => a)

  def opt[A](p: Parser[A]): Parser[Option[A]] = p.map(Some(_)) or succeed(None)

  def whitespace: Parser[String] = "\\s*".r

  def digits: Parser[String] = "\\d+".r

  def thru(s: String): Parser[String] = (".*?"+Pattern.quote(s)).r

  def quoted: Parser[String] = string("\"") *> thru("\"").map(_.dropRight(1))

  def escapedQuoted: Parser[String] = token(quoted label "string literal")

  def doubleString: Parser[String] = token("[-+]?([0-9]*\\.)?[0-9]+([eE][-+]?[0-9]+)?".r)

  def double: Parser[Double] = doubleString map (_.toDouble) label "double literal"

  def token[A](p: Parser[A]): Parser[A] = attempt(p) <* whitespace

  def sep[A](p: Parser[A], p2: Parser[Any]): Parser[List[A]] = sep1(p,p2) or succeed(List())

  def sep1[A](p: Parser[A], p2: Parser[Any]): Parser[List[A]] = map2(p, many(p2 *> p))(_ :: _)

  def opL[A](p: Parser[A])(op: Parser[(A,A) => A]): Parser[A] = map2(p, many(op ** p))((h,t) => t.foldLeft(h)((a,b) => b._1(a,b._2)))

  def surround[A](start: Parser[Any], stop: Parser[Any])(p: => Parser[A]) = start *> p <* stop

  def eof: Parser[String] = regex("\\z".r).label("unexpected trailing characters")

  def root[A](p: Parser[A]): Parser[A] = p <* eof

  // 중위 연산자들
  case class ParserOps[A](p: Parser[A]) {
    // or 에 대한 연산자 정의
    def |[B>:A](p2: => Parser[B]): Parser[B] = self.or(p,p2)
    // or 도 쓸 수 있도록
    def or[B>:A](p2: => Parser[B]): Parser[B] = self.or(p,p2)
    def map[B](f: A => B): Parser[B] = self.map(p)(f)
    def many = self.many(p) // char('a').many.map(_.size) 형식으로 쓸 수 있다.
    def many1 = self.many1(p)
    def slice: Parser[String] = self.slice(p)
    def **[B](p2: => Parser[B]): Parser[(A,B)] = self.product(p,p2)
    def product[B](p2: => Parser[B]): Parser[(A,B)] = self.product(p,p2)
    def flatMap[B](f: A => Parser[B]): Parser[B] = self.flatMap(p)(f)
    def label(msg: String): Parser[A] = self.label(msg)(p)
    def scope(msg: String): Parser[A] = self.scope(msg)(p)
    def *>[B](p2: => Parser[B]) = self.skipL(p, p2)
    def <*(p2: => Parser[Any]) = self.skipR(p, p2)
    def token = self.token(p)
    def sep(separator: Parser[Any]) = self.sep(p, separator)
    def sep1(separator: Parser[Any]) = self.sep1(p, separator)
    def as[B](b: B): Parser[B] = self.map(self.slice(p))(_ => b)
    def opL(op: Parser[(A,A) => A]): Parser[A] = self.opL(p)(op)
  }


}

case class Location(input: String, offset: Int = 0) {

  lazy val line = input.slice(0,offset+1).count(_ == '\n') + 1
  lazy val col = input.slice(0,offset+1).lastIndexOf('\n') match {
    case -1 => offset + 1
    case lineStart => offset - lineStart
  }

  def toError(msg: String): ParseError =
    ParseError(List((this, msg)))

  def advanceBy(n: Int) = copy(offset = offset+n)

  def currentLine: String =
    if (input.length > 1) input.lines.drop(line-1).next
    else ""

  def columnCaret = (" " * (col-1)) + "^"
}

case class ParseError(stack: List[(Location,String)] = List()) {
  def push(loc: Location, msg: String): ParseError =
    copy(stack = (loc,msg) :: stack)

  def label[A](s: String): ParseError =
    ParseError(latestLoc.map((_,s)).toList)

  def latest: Option[(Location,String)] =
    stack.lastOption

  def latestLoc: Option[Location] =
    latest map (_._1)

  override def toString =
    if (stack.isEmpty) "no error message"
    else {
      val collapsed = collapseStack(stack)
      val context =
        collapsed.lastOption.map("\n\n" + _._1.currentLine).getOrElse("") +
          collapsed.lastOption.map("\n" + _._1.columnCaret).getOrElse("")
      collapsed.map { case (loc,msg) => loc.line.toString + "." + loc.col + " " + msg }.mkString("\n") +
        context
    }

  def collapseStack(s: List[(Location,String)]): List[(Location,String)] =
    s.groupBy(_._1).
      view.
      mapValues(_.map(_._2).mkString("; ")).
      toList.sortBy(_._1.offset)

  def formatLoc(l: Location): String = l.line + "." + l.col
}
