package fpis.parser.impl

import SliceableTypes._
import fpis.parser.{Location, ParseError, Parsers}

import scala.util.matching.Regex

object SliceableTypes {

  type Parser[+A] = ParseState => Result[A]

  case class ParseState(loc: Location, isSliced: Boolean) {
    def advanceBy(numChars: Int): ParseState =
      copy(loc = loc.copy(offset = loc.offset + numChars))
    def input: String = loc.input.substring(loc.offset)
    def unslice = copy(isSliced = false)
    def reslice(s: ParseState) = copy(isSliced = s.isSliced)
    def slice(n: Int) = loc.input.substring(loc.offset, loc.offset + n)
  }

  sealed trait Result[+A] {
    def extract(input: String): Either[ParseError,A]
    def slice: Result[String]
    def uncommit: Result[A] = this match {
      case Failure(e,true) => Failure(e,false)
      case _ => this
    }
    def addCommit(isCommitted: Boolean): Result[A] = this match {
      case Failure(e,c) => Failure(e, c || isCommitted)
      case _ => this
    }
    def mapError(f: ParseError => ParseError): Result[A] = this match {
      case Failure(e,c) => Failure(f(e),c)
      case _ => this
    }
    def advanceSuccess(n: Int): Result[A]
  }
  case class Slice(length: Int) extends Result[String] {
    def extract(s: String) = Right(s.substring(0,length))
    def slice = this
    def advanceSuccess(n: Int) = Slice(length+n)
  }
  case class Success[+A](get: A, length: Int) extends Result[A] {
    def extract(s: String) = Right(get)
    def slice = Slice(length)
    def advanceSuccess(n: Int) = Success(get, length+n)
  }
  case class Failure(get: ParseError, isCommitted: Boolean) extends Result[Nothing] {
    def extract(s: String) = Left(get)
    def slice = this
    def advanceSuccess(n: Int) = this
  }

  def firstNonmatchingIndex(s: String, s2: String, offset: Int): Int = {
    var i = 0
    while (i+offset < s.length && i < s2.length) {
      if (s.charAt(i+offset) != s2.charAt(i)) return i
      i += 1
    }
    if (s.length-offset >= s2.length) -1
    else s.length-offset
  }
}

object Sliceable extends Parsers[Parser] {

  def run[A](p: Parser[A])(s: String): Either[ParseError,A] = {
    val s0 = ParseState(Location(s), false)
    p(s0).extract(s)
  }

  def succeed[A](a: A): Parser[A] = s => Success(a, 0)

  def or[A](p: Parser[A], p2: => Parser[A]): Parser[A] =
    s => p(s) match {
      case Failure(e,false) => p2(s)
      case r => r
    }

  override def map[A,B](p: Parser[A])(f: A => B): Parser[B] =
    s => p(s) match {
      case Success(a,n) => Success(f(a),n)
      case Slice(n) => Success(f(s.slice(n).asInstanceOf[A]),n)
      case f@Failure(_,_) => f
    }

  def flatMap[A,B](f: Parser[A])(g: A => Parser[B]): Parser[B] =
    s => f(s.unslice) match {
      case Success(a,n) =>
        g(a)(s.advanceBy(n).reslice(s))
          .addCommit(n != 0)
          .advanceSuccess(n)
      case Slice(n) => g(s.slice(n).asInstanceOf[A])(s.advanceBy(n).reslice(s))
        .advanceSuccess(n)
      case f@Failure(_,_) => f
    }

  def string(w: String): Parser[String] = {
    val msg = "'" + w + "'"
    s => {
      val i = firstNonmatchingIndex(s.loc.input, w, s.loc.offset)
      if (i == -1) { // they matched
        if (s.isSliced) Slice(w.length)
        else            Success(w, w.length)
      }
      else
        Failure(s.loc.advanceBy(i).toError(msg), i != 0)
    }
  }

  def regex(r: Regex): Parser[String] = {
    val msg = "regex " + r
    s => r.findPrefixOf(s.input) match {
      case None => Failure(s.loc.toError(msg), false)
      case Some(m) =>
        if (s.isSliced) Slice(m.length)
        else            Success(m,m.length)
    }
  }

  def scope[A](msg: String)(p: Parser[A]): Parser[A] =
    s => p(s).mapError(_.push(s.loc,msg))

  def label[A](msg: String)(p: Parser[A]): Parser[A] =
    s => p(s).mapError(_.label(msg))

  def fail[A](msg: String): Parser[A] =
    s => Failure(s.loc.toError(msg), true)

  def attempt[A](p: Parser[A]): Parser[A] =
    s => p(s).uncommit

  def slice[A](p: Parser[A]): Parser[String] =
    s => p(s.copy(isSliced = true)).slice

  override def map2[A,B,C](p: Parser[A], p2: => Parser[B])(f: (A,B) => C): Parser[C] =
    s => p(s) match {
      case Success(a,n) => val s2 = s.advanceBy(n); p2(s2) match {
        case Success(b,m) => Success(f(a,b),n+m)
        case Slice(m) => Success(f(a,s2.slice(m).asInstanceOf[B]), n+m)
        case f@Failure(_,_) => f
      }
      case Slice(n) => val s2 = s.advanceBy(n); p2(s2) match {
        case Success(b,m) => Success(f(s.slice(n).asInstanceOf[A],b),n+m)
        case Slice(m) =>
          if (s.isSliced) Slice(n+m).asInstanceOf[Result[C]]
          else Success(f(s.slice(n).asInstanceOf[A],s2.slice(m).asInstanceOf[B]), n+m)
        case f@Failure(_,_) => f
      }
      case f@Failure(_,_) => f
    }

  override def product[A,B](p: Parser[A], p2: => Parser[B]): Parser[(A,B)] =
    map2(p,p2)((_,_))

  override def many[A](p: Parser[A]): Parser[List[A]] =
    s => {
      var nConsumed: Int = 0
      if (s.isSliced) {
        def go(p: Parser[String], offset: Int): Result[String] =
          p(s.advanceBy(offset)) match {
            case f@Failure(e,true) => f
            case Failure(e,_) => Slice(offset)
            case Slice(n) => go(p, offset+n)
            case Success(_,_) => sys.error("sliced parser should not return success, only slice")
          }
        go(p.slice, 0).asInstanceOf[Result[List[A]]]
      }
      else {
        val buf = new collection.mutable.ListBuffer[A]
        def go(p: Parser[A], offset: Int): Result[List[A]] = {
          p(s.advanceBy(offset)) match {
            case Success(a,n) => buf += a; go(p, offset+n)
            case f@Failure(e,true) => f
            case Failure(e,_) => Success(buf.toList,offset)
            case Slice(n) =>
              buf += s.input.substring(offset,offset+n).
                asInstanceOf[A]
              go(p, offset+n)
          }
        }
        go(p, 0)
      }
    }
}