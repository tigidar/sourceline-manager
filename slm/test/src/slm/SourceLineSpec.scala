package slm

import munit.FunSuite

class SourceLineSpec extends FunSuite:

  // --- Token Tests ---

  test("Token.apply creates Value token"):
    val token = Token("hello")
    assertEquals(token, Token.Value("hello"))

  test("Token.ref creates Ref token"):
    val token = Token.ref("varName")
    assertEquals(token, Token.Ref("varName"))

  test("Token.empty creates empty Value token"):
    assertEquals(Token.empty, Token.Value(""))

  test("Token.asLine converts token to single-token SourceLine"):
    val token = Token("test")
    val line = token.asLine
    assertEquals(line.length, 1)
    assertEquals(line.head, Some(Token.Value("test")))

  // --- SourceLine Construction Tests ---

  test("SourceLine.empty creates empty line"):
    val line = SourceLine.empty
    assert(line.isEmpty)
    assertEquals(line.length, 0)

  test("SourceLine.apply creates single-token line"):
    val line = SourceLine(Token("word"))
    assertEquals(line.length, 1)
    assertEquals(line.head, Some(Token.Value("word")))

  test("SourceLine.fromWords creates line from multiple strings"):
    val line = SourceLine.fromWords("val", "x", "=", "42")
    assertEquals(line.length, 4)
    assertEquals(
      line.tokens,
      Vector(
        Token.Value("val"),
        Token.Value("x"),
        Token.Value("="),
        Token.Value("42")
      )
    )

  test("SourceLine.fromText splits text by separator"):
    val line = SourceLine.fromText("one two three")
    assertEquals(line.length, 3)
    assertEquals(
      line.tokens,
      Vector(
        Token.Value("one"),
        Token.Value("two"),
        Token.Value("three")
      )
    )

  test("SourceLine.fromText with custom separator"):
    val line = SourceLine.fromText("a,b,c", ",")
    assertEquals(line.length, 3)
    assertEquals(
      line.tokens,
      Vector(
        Token.Value("a"),
        Token.Value("b"),
        Token.Value("c")
      )
    )

  test("SourceLine.fromTokens creates line from varargs"):
    val line = SourceLine.fromTokens(
      Token.Value("def"),
      Token.Indent,
      Token.Ref("fn")
    )
    assertEquals(line.length, 3)

  test("SourceLine.fromTokensVec creates line from vector"):
    val tokens = Vector(Token.Value("a"), Token.Value("b"))
    val line = SourceLine.fromTokensVec(tokens)
    assertEquals(line.tokens, tokens)

  test("SourceLine.value creates single value line"):
    val line = SourceLine.value("hello")
    assertEquals(line.tokens, Vector(Token.Value("hello")))

  test("SourceLine.ref creates single ref line"):
    val line = SourceLine.ref("myVar")
    assertEquals(line.tokens, Vector(Token.Ref("myVar")))

  test("SourceLine.indentToken creates single indent line"):
    val line = SourceLine.indentToken
    assertEquals(line.tokens, Vector(Token.Indent))

  test("SourceLine.spaced intersperses tokens with spaces"):
    val line = SourceLine.spaced(Token("a"), Token("b"), Token("c"))
    assertEquals(
      line.tokens,
      Vector(
        Token.Value("a"),
        Token.Value(" "),
        Token.Value("b"),
        Token.Value(" "),
        Token.Value("c")
      )
    )

  test("SourceLine.spaced with empty input returns empty"):
    val line = SourceLine.spaced()
    assert(line.isEmpty)

  // --- Predicates Tests ---

  test("isEmpty returns true for empty line"):
    assert(SourceLine.empty.isEmpty)

  test("isEmpty returns false for non-empty line"):
    assert(!SourceLine.value("x").isEmpty)

  test("nonEmpty returns false for empty line"):
    assert(!SourceLine.empty.nonEmpty)

  test("nonEmpty returns true for non-empty line"):
    assert(SourceLine.value("x").nonEmpty)

  test("length returns correct count"):
    val line = SourceLine.fromWords("a", "b", "c")
    assertEquals(line.length, 3)

  // --- Access Tests ---

  test("head returns Some for non-empty line"):
    val line = SourceLine.fromWords("first", "second")
    assertEquals(line.head, Some(Token.Value("first")))

  test("head returns None for empty line"):
    assertEquals(SourceLine.empty.head, None)

  test("last returns Some for non-empty line"):
    val line = SourceLine.fromWords("first", "second")
    assertEquals(line.last, Some(Token.Value("second")))

  test("last returns None for empty line"):
    assertEquals(SourceLine.empty.last, None)

  test("tokens returns underlying vector"):
    val expected = Vector(Token.Value("x"))
    val line = SourceLine.value("x")
    assertEquals(line.tokens, expected)

  // --- Prefix Operations Tests ---

  test("prefix adds token to beginning"):
    val line = SourceLine.value("world")
    val prefixed = line.prefix(Token("hello"))
    assertEquals(
      prefixed.tokens,
      Vector(Token.Value("hello"), Token.Value("world"))
    )

  test("+: operator prepends token"):
    val line = SourceLine.value("world")
    val prefixed = Token("hello") +: line
    assertEquals(
      prefixed.tokens,
      Vector(Token.Value("hello"), Token.Value("world"))
    )

  test("prependAll adds multiple tokens to beginning"):
    val line = SourceLine.value("c")
    val prefixed = line.prependAll(Vector(Token("a"), Token("b")))
    assertEquals(
      prefixed.tokens,
      Vector(
        Token.Value("a"),
        Token.Value("b"),
        Token.Value("c")
      )
    )

  // --- Postfix Operations Tests ---

  test("postfix adds token to end"):
    val line = SourceLine.value("hello")
    val postfixed = line.postfix(Token("world"))
    assertEquals(
      postfixed.tokens,
      Vector(Token.Value("hello"), Token.Value("world"))
    )

  test(":+ operator appends token"):
    val line = SourceLine.value("hello")
    val postfixed = line :+ Token("world")
    assertEquals(
      postfixed.tokens,
      Vector(Token.Value("hello"), Token.Value("world"))
    )

  test("appendAll adds multiple tokens to end"):
    val line = SourceLine.value("a")
    val appended = line.appendAll(Vector(Token("b"), Token("c")))
    assertEquals(
      appended.tokens,
      Vector(
        Token.Value("a"),
        Token.Value("b"),
        Token.Value("c")
      )
    )

  // --- Combine Operations Tests ---

  test("++ concatenates two lines"):
    val line1 = SourceLine.fromWords("a", "b")
    val line2 = SourceLine.fromWords("c", "d")
    val combined = line1 ++ line2
    assertEquals(combined.length, 4)
    assertEquals(
      combined.tokens,
      Vector(
        Token.Value("a"),
        Token.Value("b"),
        Token.Value("c"),
        Token.Value("d")
      )
    )

  test("|+| is alias for ++"):
    val line1 = SourceLine.value("a")
    val line2 = SourceLine.value("b")
    assertEquals(line1 |+| line2, line1 ++ line2)

  test("combine is alias for ++"):
    val line1 = SourceLine.value("a")
    val line2 = SourceLine.value("b")
    assertEquals(line1.combine(line2), line1 ++ line2)

  test("++ with empty line returns original"):
    val line = SourceLine.value("x")
    assertEquals(line ++ SourceLine.empty, line)
    assertEquals(SourceLine.empty ++ line, line)

  test("combineAll combines multiple lines"):
    val lines = List(
      SourceLine.value("a"),
      SourceLine.value("b"),
      SourceLine.value("c")
    )
    val combined = SourceLine.combineAll(lines)
    assertEquals(
      combined.tokens,
      Vector(
        Token.Value("a"),
        Token.Value("b"),
        Token.Value("c")
      )
    )

  test("combineAll on empty returns empty"):
    val combined = SourceLine.combineAll(List.empty)
    assert(combined.isEmpty)

  // --- Transformation Tests ---

  test("indent adds single indent by default"):
    val line = SourceLine.value("code")
    val indented = line.indent()
    assertEquals(indented.tokens, Vector(Token.Indent, Token.Value("code")))

  test("indent adds multiple indents"):
    val line = SourceLine.value("code")
    val indented = line.indent(3)
    assertEquals(
      indented.tokens,
      Vector(
        Token.Indent,
        Token.Indent,
        Token.Indent,
        Token.Value("code")
      )
    )

  test("indent with 0 returns unchanged"):
    val line = SourceLine.value("code")
    val indented = line.indent(0)
    assertEquals(indented, line)

  test("map transforms each token"):
    val line = SourceLine.fromWords("a", "b")
    val mapped = line.map {
      case Token.Value(v) => Token.Value(v.toUpperCase)
      case other          => other
    }
    assertEquals(mapped.tokens, Vector(Token.Value("A"), Token.Value("B")))

  test("flatMap expands tokens"):
    val line = SourceLine.fromWords("a", "b")
    val expanded = line.flatMap { t =>
      SourceLine.fromTokens(t, Token.Value("-"))
    }
    assertEquals(
      expanded.tokens,
      Vector(
        Token.Value("a"),
        Token.Value("-"),
        Token.Value("b"),
        Token.Value("-")
      )
    )

  test("filter keeps matching tokens"):
    val line = SourceLine.fromTokens(
      Token.Value("keep"),
      Token.Indent,
      Token.Value("also")
    )
    val filtered = line.filter {
      case Token.Value(_) => true
      case _              => false
    }
    assertEquals(
      filtered.tokens,
      Vector(Token.Value("keep"), Token.Value("also"))
    )

  test("reverse reverses token order"):
    val line = SourceLine.fromWords("a", "b", "c")
    val reversed = line.reverse
    assertEquals(
      reversed.tokens,
      Vector(
        Token.Value("c"),
        Token.Value("b"),
        Token.Value("a")
      )
    )

  // --- Folding Tests ---

  test("foldLeft accumulates from left"):
    val line = SourceLine.fromWords("a", "b", "c")
    val result = line.foldLeft("") { (acc, token) =>
      token match
        case Token.Value(v) => acc + v
        case _              => acc
    }
    assertEquals(result, "abc")

  test("foldRight accumulates from right"):
    val line = SourceLine.fromWords("a", "b", "c")
    val result = line.foldRight("") { (token, acc) =>
      token match
        case Token.Value(v) => v + acc
        case _              => acc
    }
    assertEquals(result, "abc")

  // --- Rendering Tests ---

  test("render produces string with default settings"):
    val line = SourceLine.fromWords("val", "x", "=", "1")
    val rendered = line.render()
    assertEquals(rendered, "val x = 1")

  test("render with indent uses indent string"):
    val line = SourceLine.fromTokens(Token.Indent, Token.Value("code"))
    val rendered = line.render(indentStr = "    ")
    assertEquals(rendered, "     code")

  test("render with custom separator"):
    val line = SourceLine.fromWords("a", "b", "c")
    val rendered = line.render(separator = "")
    assertEquals(rendered, "abc")

  test("render handles Ref tokens"):
    val line = SourceLine.fromTokens(Token.Value("val"), Token.Ref("name"))
    val rendered = line.render()
    assertEquals(rendered, "val ${name}")

  test("renderTokens returns vector of strings"):
    val line = SourceLine.fromTokens(
      Token.Indent,
      Token.Value("x"),
      Token.Ref("y")
    )
    val tokens = line.renderTokens()
    assertEquals(tokens, Vector("  ", "x", "${y}"))

  test("renderTokens with custom indent"):
    val line = SourceLine.fromTokens(Token.Indent, Token.Value("x"))
    val tokens = line.renderTokens(indentStr = "\t")
    assertEquals(tokens, Vector("\t", "x"))

  // --- Equality Tests ---

  test("equal lines are equal"):
    val line1 = SourceLine.fromWords("a", "b")
    val line2 = SourceLine.fromWords("a", "b")
    assertEquals(line1, line2)

  test("different lines are not equal"):
    val line1 = SourceLine.fromWords("a", "b")
    val line2 = SourceLine.fromWords("a", "c")
    assertNotEquals(line1, line2)

  // --- Monoid Laws ---
  // Quantified over generated SourceLines in SourceLineMonoidLawsSpec.
