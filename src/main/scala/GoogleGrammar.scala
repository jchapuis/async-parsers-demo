import com.nexthink.utils.parsing.combinator.completion.{AsyncRegexCompletionSupport, TermsParsingHelpers}
import com.typesafe.scalalogging.Logger
import monix.eval.Task

import scala.util.parsing.combinator.Parsers

object GoogleGrammar extends Parsers with AsyncRegexCompletionSupport with TermsParsingHelpers with GoogleAst {
  val logger = Logger("grammar")

  def parseSuggestions(in: Input): Task[ParseResult[String]] = {
    val start = dropAnyWhiteSpace(in)
    if (start.atEnd) {
      Task.eval(Failure("missing some input", start))
    } else {
      remainder(start)
        .split("\\s+")
        .filterNot(_.isEmpty)
        .headOption
        .map(s => {
          if (keywords.contains(s)) {
            Task.eval(Failure("keyword term", start))
          } else if (start.offset + s.length == in.source.length()) {
            Task.eval(Failure("incomplete term", start))
          } else {
            val indexOfParen = Seq(openParen, closeParen).map(s.indexOf).filter(_ > 0)
            if (indexOfParen.nonEmpty) {
              // term embeds parenthesis, end it at first paren
              Task.eval(Success(s, in.drop(start.offset + indexOfParen.min - in.offset)))
            } else {
              Task.eval(Success(s, in.drop(start.offset + s.length - in.offset)))
            }
          }
        })
        .getOrElse(Task.eval(Failure("missing some input", start)))
    }
  }

  def completeSuggestions(in: Input): Task[Completions] = {
    val start = dropAnyWhiteSpace(in)
    if (start.atEnd) {
      Task.eval(Completions.empty)
    } else {
      val remainingInput            = remainder(start)
      val tokens                    = remainingInput.split("\\s+").filterNot(_.isEmpty).toList
      lazy val whiteSpaceToTheRight = remainingInput.length > tokens.head.length
      if (tokens.length != 1 || whiteSpaceToTheRight) {
        Task.eval(Completions.empty)
      } else {
        tokens
          .filterNot(keywords.contains)
          .headOption
          .map(s => {
            logger.debug(s"Completing on $s")
            Google
              .querySuggestions(s)
              .map(list => {
                logger.debug(s"Full completions at ${start.pos}: $list")
                Completions(start.pos, CompletionSet(list.filterNot(_ == s).map(Completion(_))))
              })
          })
          .getOrElse(Task.eval(Completions.empty))
      }
    }
  }

  val incl       = "including"
  val excl       = "excluding"
  val or         = "or"
  val and        = "and"
  val openParen  = "("
  val closeParen = ")"
  val keywords   = Seq(incl, excl, or, and, openParen, closeParen)

  lazy val term       = AsyncParser(parseSuggestions, completeSuggestions) ^^ Term
  lazy val simpleTerm = incl ~>! term ^^ Include | excl ~>! term ^^ Exclude
  lazy val orExp = andExp ~ rep(or ~>! andExp) ^^ {
    case ~(e, Nil)  => e
    case ~(e, exps) => CompositeSearchExp(Or, e +: exps)
  }
  lazy val andExp = exp ~ rep(and ~>! exp) ^^ {
    case ~(e, Nil)  => e
    case ~(e, exps) => CompositeSearchExp(And, e +: exps)
  }
  lazy val exp: AsyncParser[SearchExp] = openParen ~>! orExp <~! closeParen | term ^^ SimpleSearchExp
  lazy val search                      = opt(orExp) ~ rep(simpleTerm) ^^ { case ~(e, terms) => Search(e, terms) }
}
