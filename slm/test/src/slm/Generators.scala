package slm

import org.scalacheck.Gen

/** ScalaCheck generators for the source-line algebra. Kept small on
  * purpose: laws hold for any inhabitant, so the generators don't
  * need to chase pathological cases — only to cover the shape.
  */
object Generators:

  val genShortString: Gen[String] =
    Gen.alphaNumStr.map(_.take(8))

  val genTokenValue: Gen[Token.Value] =
    genShortString.map(Token.Value.apply)

  val genTokenRef: Gen[Token.Ref] =
    Gen.identifier.map(_.take(8)).map(Token.Ref.apply)

  val genTokenIndent: Gen[Token.Indent.type] =
    Gen.const(Token.Indent)

  val genToken: Gen[Token] =
    Gen.frequency(
      6 -> genTokenValue,
      2 -> genTokenRef,
      1 -> genTokenIndent
    )

  val genSourceLine: Gen[SourceLine] =
    for
      n      <- Gen.choose(0, 6)
      tokens <- Gen.listOfN(n, genToken)
    yield SourceLine.fromTokensVec(tokens.toVector)

  val genSourceFile: Gen[SourceFile] =
    for
      n     <- Gen.choose(0, 4)
      lines <- Gen.listOfN(n, genSourceLine)
    yield SourceFile.fromLinesVec(lines.toVector)
