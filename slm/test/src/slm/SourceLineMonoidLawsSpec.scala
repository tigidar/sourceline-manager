package slm

import org.scalacheck.Gen

class SourceLineMonoidLawsSpec extends MonoidLawsSuite[SourceLine]("SourceLine"):
  val empty: SourceLine                     = SourceLine.empty
  def combine(x: SourceLine, y: SourceLine) = x ++ y
  val gen: Gen[SourceLine]                  = Generators.genSourceLine
