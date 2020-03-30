package fpis.parser

import fpis.parser.impl.{JSON, Reference, Sliceable}
import fpis.parser.impl.ReferenceTypes.Parser

object Main extends App {

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
  println(Reference.run(json)(data))

  def charParser[Parser[+_]](P: Parsers[Parser]): Parser[Char] = {
    import P._
    char('a') | char('b')
  }

  println(Reference.run(charParser(Reference))("a"))
  println(Reference.run(charParser(Reference))("b"))

  def p2[Parser[+_]](P: Parsers[Parser]): Parser[String] = {
    import P._
    slice(("a" | "b").many)
  }

  println(Sliceable.run(p2(Sliceable))("aaba"))

  // 0개 이상의 'a' 다음에 'b'가 오는 것에 매칭되는 파서
  def p3[Parser[+_]](P: Parsers[Parser]): Parser[(Int, Int)] = {
    import P._
    char('a').many.slice.map(_.size) ** char('b').many1.slice.map(_.size)
  }

  println(Sliceable.run(p3(Sliceable))("aaba"))
}
