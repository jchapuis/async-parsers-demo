trait GoogleAst {
  case class Term(value: String)

  sealed trait SearchTerm
  case class Include(term: Term) extends SearchTerm
  case class Exclude(term: Term) extends SearchTerm

  sealed trait Op
  case object And extends Op
  case object Or  extends Op

  sealed trait SearchExp
  case class CompositeSearchExp(op: Op, exps: Seq[SearchExp]) extends SearchExp
  case class SimpleSearchExp(term: Term)                      extends SearchExp

  case class Search(exp: Option[SearchExp], terms: Seq[SearchTerm])
}
