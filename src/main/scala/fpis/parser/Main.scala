package fpis.parser

import fpis.parser.impl.{JSON, Reference}
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
}
