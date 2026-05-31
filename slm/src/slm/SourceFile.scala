package slm

final case class SourceFile(
    lines: Vector[SourceLine]
) derives CanEqual:

  // --- Predicates ---

  def isEmpty: Boolean = lines.isEmpty

  def nonEmpty: Boolean = lines.nonEmpty

  def lineCount: Int = lines.length

  // --- Access ---

  def headLine: Option[SourceLine] = lines.headOption

  def lastLine: Option[SourceLine] = lines.lastOption

  def getLine(index: Int): Option[SourceLine] =
    if index >= 0 && index < lines.length then Some(lines(index))
    else None

  // --- Token operations on last line ---

  def lastToken: Option[Token] =
    lastLine.flatMap(_.last)

  def appendToken(token: Token): SourceFile =
    lines.lastOption match
      case Some(lastLine) =>
        copy(lines.init :+ (lastLine :+ token))
      case None =>
        copy(Vector(SourceLine(token)))

  def prependTokenToLast(token: Token): SourceFile =
    lines.lastOption match
      case Some(lastLine) =>
        copy(lines.init :+ (token +: lastLine))
      case None =>
        copy(Vector(SourceLine(token)))

  // Binary operator: append token to last line
  def :+(token: Token): SourceFile =
    appendToken(token)

  // Binary operator: prepend token to last line
  def +:(token: Token): SourceFile =
    prependTokenToLast(token)

  // --- Line operations ---

  def appendLine(line: SourceLine): SourceFile =
    copy(lines :+ line)

  def prependLine(line: SourceLine): SourceFile =
    copy(line +: lines)

  def appendLines(newLines: Vector[SourceLine]): SourceFile =
    copy(lines ++ newLines)

  def prependLines(newLines: Vector[SourceLine]): SourceFile =
    copy(newLines ++ lines)

  // Binary operator: append line
  def :+(line: SourceLine): SourceFile =
    appendLine(line)

  // Binary operator: prepend line
  def +:(line: SourceLine): SourceFile =
    prependLine(line)

  // --- Combine operations ---

  def ++(other: SourceFile): SourceFile =
    copy(lines ++ other.lines)

  def |+|(other: SourceFile): SourceFile =
    this ++ other

  def combine(other: SourceFile): SourceFile =
    this ++ other

  // Combine last line of this with first line of other
  def joinLines(other: SourceFile): SourceFile =
    (lines.lastOption, other.lines.headOption) match
      case (Some(last), Some(first)) =>
        copy(lines.init :+ (last ++ first)) ++ SourceFile(other.lines.tail)
      case (None, _) => other
      case (_, None) => this

  // Binary operator for joinLines
  def |++|(other: SourceFile): SourceFile =
    joinLines(other)

  // --- Transformation ---

  def indentAll(nrOfIndents: Int = 1): SourceFile =
    copy(lines.map(_.indent(nrOfIndents)))

  def map(f: SourceLine => SourceLine): SourceFile =
    copy(lines.map(f))

  def flatMap(f: SourceLine => SourceFile): SourceFile =
    SourceFile(lines.flatMap(line => f(line).lines))

  def filter(p: SourceLine => Boolean): SourceFile =
    copy(lines.filter(p))

  def filterNot(p: SourceLine => Boolean): SourceFile =
    copy(lines.filterNot(p))

  def dropEmpty: SourceFile =
    filter(_.nonEmpty)

  def reverse: SourceFile =
    copy(lines.reverse)

  def take(n: Int): SourceFile =
    copy(lines.take(n))

  def drop(n: Int): SourceFile =
    copy(lines.drop(n))

  def slice(from: Int, until: Int): SourceFile =
    copy(lines.slice(from, until))

  // Transform all tokens in all lines
  def mapTokens(f: Token => Token): SourceFile =
    copy(lines.map(_.map(f)))

  // --- Line modification ---

  def updateLine(index: Int, f: SourceLine => SourceLine): SourceFile =
    if index >= 0 && index < lines.length then
      copy(lines.updated(index, f(lines(index))))
    else this

  def updateLastLine(f: SourceLine => SourceLine): SourceFile =
    if lines.nonEmpty then copy(lines.init :+ f(lines.last))
    else this

  def updateFirstLine(f: SourceLine => SourceLine): SourceFile =
    if lines.nonEmpty then copy(f(lines.head) +: lines.tail)
    else this

  // --- Interleaving and zipping ---

  def intersperse(separator: SourceLine): SourceFile =
    if lines.length <= 1 then this
    else
      val interspersed =
        lines.flatMap(line => Vector(line, separator)).dropRight(1)
      copy(interspersed)

  def zipWith(other: SourceFile)(
      f: (SourceLine, SourceLine) => SourceLine
  ): SourceFile =
    copy(lines.zip(other.lines).map { case (a, b) => f(a, b) })

  def zipWithIndex: SourceFile =
    copy(lines.zipWithIndex.map { case (line, idx) =>
      Token.Value(s"$idx: ") +: line
    })

  // --- Folding ---

  def foldLeft[B](z: B)(op: (B, SourceLine) => B): B =
    lines.foldLeft(z)(op)

  def foldRight[B](z: B)(op: (SourceLine, B) => B): B =
    lines.foldRight(z)(op)

  def foldLines(
      op: (SourceLine, SourceLine) => SourceLine
  ): Option[SourceLine] =
    lines.reduceOption(op)

  // --- Searching ---

  def find(p: SourceLine => Boolean): Option[SourceLine] =
    lines.find(p)

  def exists(p: SourceLine => Boolean): Boolean =
    lines.exists(p)

  def forall(p: SourceLine => Boolean): Boolean =
    lines.forall(p)

  def count(p: SourceLine => Boolean): Int =
    lines.count(p)

  def indexWhere(p: SourceLine => Boolean): Int =
    lines.indexWhere(p)

  // --- Rendering (for interpretation) ---

  def render(
      indentStr: String = "  ",
      tokenSeparator: String = " ",
      lineSeparator: String = "\n"
  ): String =
    lines
      .map(_.render(indentStr, tokenSeparator))
      .mkString(lineSeparator)

  def renderLines(
      indentStr: String = "  ",
      tokenSeparator: String = " "
  ): Vector[String] =
    lines.map(_.render(indentStr, tokenSeparator))

object SourceFile:
  val empty: SourceFile = SourceFile(Vector.empty)

  def apply(line: SourceLine): SourceFile =
    SourceFile(Vector(line))

  def apply(lines: SourceLine*): SourceFile =
    SourceFile(lines.toVector)

  def fromLines(lines: SourceLine*): SourceFile =
    SourceFile(lines.toVector)

  def fromLinesVec(lines: Vector[SourceLine]): SourceFile =
    SourceFile(lines)

  // Create from raw strings (one line per string)
  def fromStrings(strings: String*): SourceFile =
    SourceFile(strings.toVector.map(s => SourceLine.fromText(s)))

  // Create from a single multi-line string
  def fromMultiLine(text: String, lineSeparator: String = "\n"): SourceFile =
    SourceFile(
      text.split(lineSeparator).toVector.map(s => SourceLine.fromText(s))
    )

  // Monoid-like combine
  def combineAll(files: Iterable[SourceFile]): SourceFile =
    files.foldLeft(empty)(_ ++ _)

  // Smart constructor for single token
  def singleToken(token: Token): SourceFile =
    SourceFile(Vector(SourceLine(token)))

  // Smart constructor for single value
  def value(s: String): SourceFile =
    singleToken(Token.Value(s))

  // Builder pattern support
  def builder: SourceFileBuilder = SourceFileBuilder.empty

// Builder for fluent DSL construction
final case class SourceFileBuilder private (
    current: SourceFile,
    currentLine: SourceLine
):
  def token(t: Token): SourceFileBuilder =
    copy(currentLine = currentLine :+ t)

  def value(s: String): SourceFileBuilder =
    token(Token.Value(s))

  def ref(name: String): SourceFileBuilder =
    token(Token.Ref(name))

  def indent: SourceFileBuilder =
    token(Token.Indent)

  def newLine: SourceFileBuilder =
    copy(
      current = current :+ currentLine,
      currentLine = SourceLine.empty
    )

  def line(l: SourceLine): SourceFileBuilder =
    copy(
      current = current :+ currentLine :+ l,
      currentLine = SourceLine.empty
    )

  def file(f: SourceFile): SourceFileBuilder =
    copy(
      current = (current :+ currentLine) ++ f,
      currentLine = SourceLine.empty
    )

  def build: SourceFile =
    if currentLine.isEmpty then current
    else current :+ currentLine

object SourceFileBuilder:
  val empty: SourceFileBuilder =
    SourceFileBuilder(SourceFile.empty, SourceLine.empty)
