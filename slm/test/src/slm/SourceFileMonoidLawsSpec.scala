package slm

import org.scalacheck.Gen

class SourceFileMonoidLawsSpec extends MonoidLawsSuite[SourceFile]("SourceFile"):
  val empty: SourceFile                     = SourceFile.empty
  def combine(x: SourceFile, y: SourceFile) = x ++ y
  val gen: Gen[SourceFile]                  = Generators.genSourceFile
