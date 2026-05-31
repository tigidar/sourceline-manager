package slm

enum Token derives CanEqual:
  case Value(value: String)
  case Indent
  case Ref(value: String)

  def asLine: SourceLine = SourceLine(this)

object Token:
  def apply(s: String): Token.Value =
    Token.Value(s)

  def ref(value: String): Token.Ref =
    Token.Ref(value)

  val empty: Token.Value = Token.Value("")

final case class SourceLine private (
    line: Vector[Token]
) derives CanEqual:

  // --- Predicates ---

  def isEmpty: Boolean = line.isEmpty

  def nonEmpty: Boolean = line.nonEmpty

  def length: Int = line.length

  // --- Access ---

  def head: Option[Token] = line.headOption

  def last: Option[Token] = line.lastOption

  def tokens: Vector[Token] = line

  // --- Prefix operations (prepend) ---

  def prefix(value: Token): SourceLine =
    copy(value +: line)

  def +:(value: Token): SourceLine =
    prefix(value)

  def prependAll(values: Vector[Token]): SourceLine =
    copy(values ++ line)

  // --- Postfix operations (append) ---

  def postfix(value: Token): SourceLine =
    copy(line :+ value)

  def :+(value: Token): SourceLine =
    postfix(value)

  def appendAll(values: Vector[Token]): SourceLine =
    copy(line ++ values)

  // --- Combine operations ---

  def ++(other: SourceLine): SourceLine =
    SourceLine(line ++ other.line)

  def |+|(other: SourceLine): SourceLine =
    this ++ other

  def combine(other: SourceLine): SourceLine =
    this ++ other

  // --- Transformation ---

  def indent(nrOfIndents: Int = 1): SourceLine =
    copy(
      Vector.fill(nrOfIndents)(Token.Indent) ++ line
    )

  def map(f: Token => Token): SourceLine =
    copy(line.map(f))

  def flatMap(f: Token => SourceLine): SourceLine =
    SourceLine(line.flatMap(t => f(t).line))

  def filter(p: Token => Boolean): SourceLine =
    copy(line.filter(p))

  def reverse: SourceLine =
    copy(line.reverse)

  // --- Slicing (primitive) ---

  def take(n: Int): SourceLine =
    copy(line.take(n))

  def drop(n: Int): SourceLine =
    copy(line.drop(n))

  def splitAt(n: Int): (SourceLine, SourceLine) =
    val (l, r) = line.splitAt(n)
    (SourceLine(l), SourceLine(r))

  def takeWhile(p: Token => Boolean): SourceLine =
    copy(line.takeWhile(p))

  def dropWhile(p: Token => Boolean): SourceLine =
    copy(line.dropWhile(p))

  // --- Search (primitive) ---

  def indexWhere(p: Token => Boolean): Int =
    line.indexWhere(p)

  def lastIndexWhere(p: Token => Boolean): Int =
    line.lastIndexWhere(p)

  def indexOfSlice(other: SourceLine): Int =
    line.indexOfSlice(other.line)

  // --- Predicates ---

  def exists(p: Token => Boolean): Boolean =
    line.exists(p)

  def forall(p: Token => Boolean): Boolean =
    line.forall(p)

  def count(p: Token => Boolean): Int =
    line.count(p)

  def find(p: Token => Boolean): Option[Token] =
    line.find(p)

  // --- Sub-sequence pattern matching ---

  def startsWith(other: SourceLine): Boolean =
    line.startsWith(other.line)

  def endsWith(other: SourceLine): Boolean =
    line.endsWith(other.line)

  def contains(other: SourceLine): Boolean =
    indexOfSlice(other) >= 0

  // --- Joining ---

  def intersperse(sep: Token): SourceLine =
    if line.length <= 1 then this
    else copy(line.flatMap(t => Vector(t, sep)).dropRight(1))

  // --- Folding ---

  def foldLeft[B](z: B)(op: (B, Token) => B): B =
    line.foldLeft(z)(op)

  def foldRight[B](z: B)(op: (Token, B) => B): B =
    line.foldRight(z)(op)

  // --- Rendering (for interpretation) ---

  def render(indentStr: String = "  ", separator: String = " "): String =
    line
      .map {
        case Token.Value(v) => v
        case Token.Indent   => indentStr
        case Token.Ref(r)   => s"$${$r}"
      }
      .mkString(separator)

  def renderTokens(indentStr: String = "  "): Vector[String] =
    line.map {
      case Token.Value(v) => v
      case Token.Indent   => indentStr
      case Token.Ref(r)   => s"$${$r}"
    }

object SourceLine:
  val empty: SourceLine = SourceLine(Vector.empty)

  def apply(t: Token): SourceLine =
    SourceLine(Vector(t))

  def fromWords(s: String*): SourceLine =
    SourceLine(s.toVector.map(Token.Value.apply))

  def fromText(s: String, separator: String = " "): SourceLine =
    SourceLine(s.split(separator).toVector.map(Token.Value.apply))

  def fromTokens(tokens: Token*): SourceLine =
    SourceLine(tokens.toVector)

  def fromTokensVec(tokens: Vector[Token]): SourceLine =
    SourceLine(tokens)

  // Monoid-like combine for collections
  def combineAll(lines: Iterable[SourceLine]): SourceLine =
    lines.foldLeft(empty)(_ ++ _)

  // Smart constructors
  def value(s: String): SourceLine =
    SourceLine(Token.Value(s))

  def ref(name: String): SourceLine =
    SourceLine(Token.Ref(name))

  def indentToken: SourceLine =
    SourceLine(Token.Indent)

  // Convenience for building lines with spaces between tokens
  def spaced(tokens: Token*): SourceLine =
    if tokens.isEmpty then empty
    else
      val spacer = Token.Value(" ")
      val interspersed =
        tokens.toVector.flatMap(t => Vector(t, spacer)).dropRight(1)
      SourceLine(interspersed)
