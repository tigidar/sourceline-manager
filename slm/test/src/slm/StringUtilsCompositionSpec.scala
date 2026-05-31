package slm

import munit.FunSuite

/** Demonstrates that a small primitive set on `SourceLine` (take/drop,
  * takeWhile/dropWhile, indexWhere/indexOfSlice, exists/forall/count/find,
  * startsWith/endsWith/contains, intersperse) plus the existing monoid
  * (`++`, `empty`) and HOFs (`map`, `filter`, `reverse`, `foldLeft`)
  * suffice to express the relevant Apache Commons `StringUtils`
  * operations as derived combinators.
  *
  * Each test defines the StringUtils-equivalent as a local `def` so the
  * derivation is visible at the call site — no new methods are added to
  * `SourceLine` itself.
  */
class StringUtilsCompositionSpec extends FunSuite:

  // Predicate used by trim/strip — a Token is "blank" if it carries no
  // visible content. Defined here, not on Token, because "blank" is a
  // domain choice the caller makes (e.g., whether `Indent` counts).
  val isBlank: Token => Boolean =
    case Token.Value(v) => v.forall(_.isWhitespace)
    case Token.Indent   => true
    case Token.Ref(_)   => false

  // --- left / right / mid / substring -----------------------------------

  test("left(n) = take(n)"):
    def left(s: SourceLine, n: Int): SourceLine = s.take(n)
    val s = SourceLine.fromWords("a", "b", "c", "d")
    assertEquals(left(s, 2), SourceLine.fromWords("a", "b"))
    assertEquals(left(s, 10), s) // overflow returns whole
    assertEquals(left(s, 0), SourceLine.empty)

  test("right(n) = drop(length - n)"):
    def right(s: SourceLine, n: Int): SourceLine =
      s.drop(math.max(0, s.length - n))
    val s = SourceLine.fromWords("a", "b", "c", "d")
    assertEquals(right(s, 2), SourceLine.fromWords("c", "d"))
    assertEquals(right(s, 10), s)
    assertEquals(right(s, 0), SourceLine.empty)

  test("mid(pos, len) = drop(pos).take(len)"):
    def mid(s: SourceLine, pos: Int, len: Int): SourceLine =
      s.drop(pos).take(len)
    val s = SourceLine.fromWords("a", "b", "c", "d", "e")
    assertEquals(mid(s, 1, 3), SourceLine.fromWords("b", "c", "d"))

  test("substring(start, end) = drop(start).take(end - start)"):
    def substring(s: SourceLine, start: Int, end: Int): SourceLine =
      s.drop(start).take(end - start)
    val s = SourceLine.fromWords("a", "b", "c", "d")
    assertEquals(substring(s, 1, 3), SourceLine.fromWords("b", "c"))

  // --- trim / strip ------------------------------------------------------

  test("trim() = dropWhile(blank) ∘ reverse ∘ dropWhile(blank) ∘ reverse"):
    def trim(s: SourceLine): SourceLine =
      s.dropWhile(isBlank).reverse.dropWhile(isBlank).reverse
    val s = SourceLine.fromTokens(
      Token.Indent,
      Token.Value("  "),
      Token.Value("code"),
      Token.Value("  "),
      Token.Indent
    )
    assertEquals(trim(s).tokens, Vector(Token.Value("code")))
    assertEquals(trim(SourceLine.value("code")), SourceLine.value("code"))
    assertEquals(trim(SourceLine.empty), SourceLine.empty)

  test("strip(predicate) is trim with caller-chosen predicate"):
    def strip(s: SourceLine, p: Token => Boolean): SourceLine =
      s.dropWhile(p).reverse.dropWhile(p).reverse
    val s         = SourceLine.fromWords("x", "a", "b", "x")
    val stripXs   = strip(s, _ == Token.Value("x"))
    assertEquals(stripXs, SourceLine.fromWords("a", "b"))

  // --- chomp / chop ------------------------------------------------------

  test("chop = dropRight(1) — but expressed via take(length - 1)"):
    def chop(s: SourceLine): SourceLine =
      if s.isEmpty then s else s.take(s.length - 1)
    val s = SourceLine.fromWords("a", "b", "c")
    assertEquals(chop(s), SourceLine.fromWords("a", "b"))
    assertEquals(chop(SourceLine.empty), SourceLine.empty)

  test("chomp(suffix) removes trailing suffix iff endsWith(suffix)"):
    def chomp(s: SourceLine, suffix: SourceLine): SourceLine =
      if s.endsWith(suffix) then s.take(s.length - suffix.length) else s
    val s   = SourceLine.fromWords("a", "b", "EOL")
    val eol = SourceLine.value("EOL")
    assertEquals(chomp(s, eol), SourceLine.fromWords("a", "b"))
    assertEquals(chomp(SourceLine.fromWords("a", "b"), eol),
                 SourceLine.fromWords("a", "b"))

  // --- repeat / wrap -----------------------------------------------------

  test("repeat(n) = foldLeft over empty"):
    def repeat(s: SourceLine, n: Int): SourceLine =
      (0 until n).foldLeft(SourceLine.empty)((acc, _) => acc ++ s)
    val s = SourceLine.fromWords("ha")
    assertEquals(repeat(s, 3),
                 SourceLine.fromWords("ha", "ha", "ha"))
    assertEquals(repeat(s, 0), SourceLine.empty)

  test("wrap(token) = token +: this :+ token"):
    def wrap(s: SourceLine, t: Token): SourceLine = t +: s :+ t
    val s = SourceLine.value("body")
    assertEquals(
      wrap(s, Token.Value("\"")).tokens,
      Vector(Token.Value("\""), Token.Value("body"), Token.Value("\""))
    )

  // --- padLeft / padRight / center --------------------------------------

  test("padLeft(width, padToken) prepends padding to reach width"):
    def padLeft(s: SourceLine, width: Int, pad: Token): SourceLine =
      if s.length >= width then s
      else s.prependAll(Vector.fill(width - s.length)(pad))
    val s = SourceLine.fromWords("x")
    assertEquals(
      padLeft(s, 3, Token.Value("0")).tokens,
      Vector(Token.Value("0"), Token.Value("0"), Token.Value("x"))
    )
    assertEquals(padLeft(s, 0, Token.Value("0")), s)

  test("padRight(width, padToken) appends padding to reach width"):
    def padRight(s: SourceLine, width: Int, pad: Token): SourceLine =
      if s.length >= width then s
      else s.appendAll(Vector.fill(width - s.length)(pad))
    val s = SourceLine.fromWords("x")
    assertEquals(
      padRight(s, 3, Token.Value(".")).tokens,
      Vector(Token.Value("x"), Token.Value("."), Token.Value("."))
    )

  test("center(width, pad) splits padding L/R"):
    def center(s: SourceLine, width: Int, pad: Token): SourceLine =
      val deficit = math.max(0, width - s.length)
      val left    = deficit / 2
      val right   = deficit - left
      s.prependAll(Vector.fill(left)(pad))
        .appendAll(Vector.fill(right)(pad))
    val s = SourceLine.value("x")
    assertEquals(
      center(s, 5, Token.Value(".")).tokens,
      Vector(Token.Value("."), Token.Value("."),
             Token.Value("x"),
             Token.Value("."), Token.Value("."))
    )

  // --- countMatches / containsAny / defaultIfEmpty ----------------------

  test("countMatches(token) = count(_ == token)"):
    def countMatches(s: SourceLine, t: Token): Int =
      s.count(_ == t)
    val s = SourceLine.fromWords("a", "b", "a", "c", "a")
    assertEquals(countMatches(s, Token.Value("a")), 3)
    assertEquals(countMatches(s, Token.Value("z")), 0)

  test("countMatchesSlice(other) by repeated indexOfSlice"):
    def countSlice(s: SourceLine, sub: SourceLine): Int =
      if sub.isEmpty then 0
      else
        @annotation.tailrec
        def loop(rest: SourceLine, acc: Int): Int =
          val i = rest.indexOfSlice(sub)
          if i < 0 then acc
          else loop(rest.drop(i + sub.length), acc + 1)
        loop(s, 0)
    val s   = SourceLine.fromWords("ab", "ab", "x", "ab")
    val sub = SourceLine.value("ab")
    assertEquals(countSlice(s, sub), 3)

  test("containsAny(tokens*) = exists(t => containsToken(t))"):
    def containsAny(s: SourceLine, ts: Token*): Boolean =
      ts.exists(t => s.exists(_ == t))
    val s = SourceLine.fromWords("foo", "bar")
    assert(containsAny(s, Token.Value("zzz"), Token.Value("bar")))
    assert(!containsAny(s, Token.Value("zzz")))

  test("defaultIfEmpty(default) = if isEmpty then default else this"):
    def defaultIfEmpty(s: SourceLine, default: SourceLine): SourceLine =
      if s.isEmpty then default else s
    val d = SourceLine.value("default")
    assertEquals(defaultIfEmpty(SourceLine.empty, d), d)
    assertEquals(defaultIfEmpty(SourceLine.value("x"), d),
                 SourceLine.value("x"))

  // --- appendIfMissing / prependIfMissing -------------------------------

  test("appendIfMissing(suffix) = if endsWith then this else this ++ suffix"):
    def appendIfMissing(s: SourceLine, suffix: SourceLine): SourceLine =
      if s.endsWith(suffix) then s else s ++ suffix
    val semi = SourceLine.value(";")
    assertEquals(
      appendIfMissing(SourceLine.value("stmt"), semi),
      SourceLine.fromWords("stmt", ";")
    )
    assertEquals(
      appendIfMissing(SourceLine.fromWords("stmt", ";"), semi),
      SourceLine.fromWords("stmt", ";")
    )

  test("prependIfMissing(prefix) symmetric"):
    def prependIfMissing(s: SourceLine, prefix: SourceLine): SourceLine =
      if s.startsWith(prefix) then s else prefix ++ s
    val sigil = SourceLine.value("$")
    assertEquals(
      prependIfMissing(SourceLine.value("name"), sigil),
      SourceLine.fromWords("$", "name")
    )

  // --- replace ----------------------------------------------------------

  test("replaceAll(target, repl) at token level = map"):
    def replaceAll(s: SourceLine, target: Token, repl: Token): SourceLine =
      s.map(t => if t == target then repl else t)
    val s = SourceLine.fromWords("a", "x", "b", "x")
    assertEquals(
      replaceAll(s, Token.Value("x"), Token.Value("Y")),
      SourceLine.fromWords("a", "Y", "b", "Y")
    )

  test("replaceSlice(target, repl) splices via indexOfSlice + splitAt"):
    def replaceSlice(
        s: SourceLine,
        target: SourceLine,
        repl: SourceLine
    ): SourceLine =
      if target.isEmpty then s
      else
        @annotation.tailrec
        def loop(rest: SourceLine, acc: SourceLine): SourceLine =
          val i = rest.indexOfSlice(target)
          if i < 0 then acc ++ rest
          else
            val (before, after) = rest.splitAt(i)
            loop(after.drop(target.length), acc ++ before ++ repl)
        loop(s, SourceLine.empty)
    val s   = SourceLine.fromWords("a", "b", "c", "a", "b", "d")
    val tgt = SourceLine.fromWords("a", "b")
    val rep = SourceLine.fromWords("Z")
    assertEquals(replaceSlice(s, tgt, rep),
                 SourceLine.fromWords("Z", "c", "Z", "d"))
    // identity when target absent
    assertEquals(
      replaceSlice(s, SourceLine.fromWords("nope"), rep),
      s
    )

  // --- abbreviate -------------------------------------------------------

  test("abbreviate(maxWidth, marker) = take(width - marker.length) ++ marker"):
    def abbreviate(
        s: SourceLine,
        maxWidth: Int,
        marker: SourceLine
    ): SourceLine =
      if s.length <= maxWidth then s
      else s.take(math.max(0, maxWidth - marker.length)) ++ marker
    val s   = SourceLine.fromWords("a", "b", "c", "d", "e", "f")
    val dot = SourceLine.value("…")
    assertEquals(abbreviate(s, 4, dot),
                 SourceLine.fromWords("a", "b", "c", "…"))
    assertEquals(abbreviate(SourceLine.fromWords("a", "b"), 4, dot),
                 SourceLine.fromWords("a", "b"))

  // --- join (collection level) -----------------------------------------

  test("join(lines, sepLine) = lines reduced with sep insertion"):
    def joinLines(lines: Seq[SourceLine], sep: SourceLine): SourceLine =
      lines match
        case Nil     => SourceLine.empty
        case head +: tail =>
          tail.foldLeft(head)((acc, next) => acc ++ sep ++ next)
    val parts = Seq(
      SourceLine.value("a"),
      SourceLine.value("b"),
      SourceLine.value("c")
    )
    assertEquals(
      joinLines(parts, SourceLine.value(",")),
      SourceLine.fromWords("a", ",", "b", ",", "c")
    )

  test("intersperse(sep) lifts join to the Token level"):
    val line = SourceLine.fromWords("a", "b", "c")
    assertEquals(
      line.intersperse(Token.Value(",")),
      SourceLine.fromWords("a", ",", "b", ",", "c")
    )
    assertEquals(SourceLine.empty.intersperse(Token.Value(",")),
                 SourceLine.empty)
    assertEquals(SourceLine.value("a").intersperse(Token.Value(",")),
                 SourceLine.value("a"))

  // --- isBlank / isNotBlank --------------------------------------------

  test("isBlank = forall(blank)"):
    def isBlankLine(s: SourceLine): Boolean = s.forall(isBlank)
    assert(isBlankLine(SourceLine.empty))
    assert(isBlankLine(SourceLine.indentToken))
    assert(isBlankLine(SourceLine.fromTokens(Token.Indent, Token.Value(" "))))
    assert(!isBlankLine(SourceLine.fromTokens(Token.Indent, Token.Value("x"))))

  // --- Primitive sanity checks (verify the additions themselves) -------

  test("take/drop are dual: take(n) ++ drop(n) == self"):
    val s = SourceLine.fromWords("a", "b", "c", "d", "e")
    (0 to s.length).foreach: n =>
      assertEquals(s.take(n) ++ s.drop(n), s, s"failed at n=$n")

  test("splitAt(n) == (take(n), drop(n))"):
    val s = SourceLine.fromWords("a", "b", "c", "d")
    (0 to s.length).foreach: n =>
      assertEquals(s.splitAt(n), (s.take(n), s.drop(n)))

  test("indexWhere returns -1 when no match, else first match index"):
    val s = SourceLine.fromTokens(
      Token.Value("a"),
      Token.Indent,
      Token.Value("b"),
      Token.Indent
    )
    assertEquals(s.indexWhere(_ == Token.Indent), 1)
    assertEquals(s.lastIndexWhere(_ == Token.Indent), 3)
    assertEquals(s.indexWhere(_ == Token.Value("z")), -1)

  test("indexOfSlice / startsWith / endsWith / contains"):
    val s   = SourceLine.fromWords("a", "b", "c", "d")
    val mid = SourceLine.fromWords("b", "c")
    assertEquals(s.indexOfSlice(mid), 1)
    assert(s.startsWith(SourceLine.fromWords("a", "b")))
    assert(s.endsWith(SourceLine.fromWords("c", "d")))
    assert(s.contains(mid))
    assert(!s.contains(SourceLine.fromWords("x", "y")))
    // empty is prefix/suffix of everything
    assert(s.startsWith(SourceLine.empty))
    assert(s.endsWith(SourceLine.empty))
