package slm

import munit.FunSuite

class SourceFileSpec extends FunSuite:

  // Helper to create test lines
  def line(s: String): SourceLine = SourceLine.fromText(s)
  def lineWords(words: String*): SourceLine = SourceLine.fromWords(words*)

  // --- Construction Tests ---

  test("SourceFile.empty creates empty file"):
    val file = SourceFile.empty
    assert(file.isEmpty)
    assertEquals(file.lineCount, 0)

  test("SourceFile.apply with single line"):
    val file = SourceFile(line("hello world"))
    assertEquals(file.lineCount, 1)
    assertEquals(file.headLine, Some(line("hello world")))

  test("SourceFile.apply with multiple lines"):
    val file = SourceFile(line("a"), line("b"), line("c"))
    assertEquals(file.lineCount, 3)

  test("SourceFile.fromLines creates from varargs"):
    val file = SourceFile.fromLines(line("x"), line("y"))
    assertEquals(file.lineCount, 2)

  test("SourceFile.fromLinesVec creates from vector"):
    val vec = Vector(line("a"), line("b"))
    val file = SourceFile.fromLinesVec(vec)
    assertEquals(file.lines, vec)

  test("SourceFile.fromStrings creates lines from strings"):
    val file = SourceFile.fromStrings("val x = 1", "val y = 2")
    assertEquals(file.lineCount, 2)
    assertEquals(file.headLine.map(_.render()), Some("val x = 1"))

  test("SourceFile.fromMultiLine splits text"):
    val file = SourceFile.fromMultiLine("a b\nc d\ne f")
    assertEquals(file.lineCount, 3)
    assertEquals(file.headLine.map(_.render()), Some("a b"))

  test("SourceFile.value creates single-value file"):
    val file = SourceFile.value("test")
    assertEquals(file.lineCount, 1)
    assertEquals(file.headLine.flatMap(_.head), Some(Token.Value("test")))

  test("SourceFile.singleToken creates file with one token"):
    val file = SourceFile.singleToken(Token.Indent)
    assertEquals(file.lineCount, 1)

  // --- Predicate Tests ---

  test("isEmpty returns true for empty file"):
    assert(SourceFile.empty.isEmpty)

  test("isEmpty returns false for non-empty file"):
    assert(!SourceFile.value("x").isEmpty)

  test("nonEmpty is opposite of isEmpty"):
    val empty = SourceFile.empty
    val nonEmpty = SourceFile.value("x")
    assertEquals(empty.nonEmpty, !empty.isEmpty)
    assertEquals(nonEmpty.nonEmpty, !nonEmpty.isEmpty)

  test("lineCount returns correct count"):
    assertEquals(SourceFile.empty.lineCount, 0)
    assertEquals(SourceFile.fromStrings("a", "b", "c").lineCount, 3)

  // --- Access Tests ---

  test("headLine returns Some for non-empty file"):
    val file = SourceFile.fromStrings("first", "second")
    assertEquals(file.headLine.map(_.render()), Some("first"))

  test("headLine returns None for empty file"):
    assertEquals(SourceFile.empty.headLine, None)

  test("lastLine returns Some for non-empty file"):
    val file = SourceFile.fromStrings("first", "second")
    assertEquals(file.lastLine.map(_.render()), Some("second"))

  test("lastLine returns None for empty file"):
    assertEquals(SourceFile.empty.lastLine, None)

  test("getLine returns Some for valid index"):
    val file = SourceFile.fromStrings("a", "b", "c")
    assertEquals(file.getLine(1).map(_.render()), Some("b"))

  test("getLine returns None for negative index"):
    val file = SourceFile.fromStrings("a", "b")
    assertEquals(file.getLine(-1), None)

  test("getLine returns None for out-of-bounds index"):
    val file = SourceFile.fromStrings("a", "b")
    assertEquals(file.getLine(5), None)

  // --- Token Operations on Last Line ---

  test("lastToken returns last token of last line"):
    val file = SourceFile.fromStrings("a b", "c d")
    assertEquals(file.lastToken, Some(Token.Value("d")))

  test("lastToken returns None for empty file"):
    assertEquals(SourceFile.empty.lastToken, None)

  test("appendToken adds token to last line"):
    val file = SourceFile.fromStrings("hello")
    val updated = file.appendToken(Token("world"))
    assertEquals(
      updated.lastLine.map(_.tokens),
      Some(Vector(Token.Value("hello"), Token.Value("world")))
    )

  test("appendToken creates new line if file is empty"):
    val file = SourceFile.empty
    val updated = file.appendToken(Token("new"))
    assertEquals(updated.lineCount, 1)
    assertEquals(updated.lastToken, Some(Token.Value("new")))

  test("prependTokenToLast adds token to beginning of last line"):
    val file = SourceFile.fromStrings("world")
    val updated = file.prependTokenToLast(Token("hello"))
    assertEquals(
      updated.lastLine.map(_.tokens),
      Some(Vector(Token.Value("hello"), Token.Value("world")))
    )

  test(":+ operator appends token to last line"):
    val file = SourceFile.fromStrings("a")
    val updated = file :+ Token("b")
    assertEquals(
      updated.lastLine.map(_.tokens),
      Some(Vector(Token.Value("a"), Token.Value("b")))
    )

  test("+: operator prepends token to last line"):
    val file = SourceFile.fromStrings("b")
    val updated = Token("a") +: file
    assertEquals(
      updated.lastLine.map(_.tokens),
      Some(Vector(Token.Value("a"), Token.Value("b")))
    )

  // --- Line Operations ---

  test("appendLine adds line to end"):
    val file = SourceFile.fromStrings("a")
    val updated = file.appendLine(line("b"))
    assertEquals(updated.lineCount, 2)
    assertEquals(updated.lastLine.map(_.render()), Some("b"))

  test("prependLine adds line to beginning"):
    val file = SourceFile.fromStrings("b")
    val updated = file.prependLine(line("a"))
    assertEquals(updated.lineCount, 2)
    assertEquals(updated.headLine.map(_.render()), Some("a"))

  test("appendLines adds multiple lines to end"):
    val file = SourceFile.fromStrings("a")
    val updated = file.appendLines(Vector(line("b"), line("c")))
    assertEquals(updated.lineCount, 3)
    assertEquals(updated.lastLine.map(_.render()), Some("c"))

  test("prependLines adds multiple lines to beginning"):
    val file = SourceFile.fromStrings("c")
    val updated = file.prependLines(Vector(line("a"), line("b")))
    assertEquals(updated.lineCount, 3)
    assertEquals(updated.headLine.map(_.render()), Some("a"))

  test(":+ operator with SourceLine appends line"):
    val file = SourceFile.fromStrings("a")
    val updated = file :+ line("b")
    assertEquals(updated.lineCount, 2)

  test("+: operator with SourceLine prepends line"):
    val file = SourceFile.fromStrings("b")
    val updated = line("a") +: file
    assertEquals(updated.lineCount, 2)
    assertEquals(updated.headLine.map(_.render()), Some("a"))

  // --- Combine Operations ---

  test("++ concatenates two files"):
    val file1 = SourceFile.fromStrings("a", "b")
    val file2 = SourceFile.fromStrings("c", "d")
    val combined = file1 ++ file2
    assertEquals(combined.lineCount, 4)
    assertEquals(combined.renderLines(), Vector("a", "b", "c", "d"))

  test("|+| is alias for ++"):
    val file1 = SourceFile.fromStrings("a")
    val file2 = SourceFile.fromStrings("b")
    assertEquals(file1 |+| file2, file1 ++ file2)

  test("combine is alias for ++"):
    val file1 = SourceFile.fromStrings("a")
    val file2 = SourceFile.fromStrings("b")
    assertEquals(file1.combine(file2), file1 ++ file2)

  test("++ with empty returns original"):
    val file = SourceFile.fromStrings("a")
    assertEquals(file ++ SourceFile.empty, file)
    assertEquals(SourceFile.empty ++ file, file)

  test("joinLines combines last line with first line"):
    val file1 = SourceFile.fromStrings("hello")
    val file2 = SourceFile.fromStrings("world")
    val joined = file1.joinLines(file2)
    assertEquals(joined.lineCount, 1)
    assertEquals(joined.headLine.map(_.render()), Some("hello world"))

  test("joinLines preserves other lines"):
    val file1 = SourceFile.fromStrings("a", "b")
    val file2 = SourceFile.fromStrings("c", "d")
    val joined = file1.joinLines(file2)
    assertEquals(joined.lineCount, 3)
    assertEquals(joined.renderLines(), Vector("a", "b c", "d"))

  test("|++| is alias for joinLines"):
    val file1 = SourceFile.fromStrings("a")
    val file2 = SourceFile.fromStrings("b")
    assertEquals(file1 |++| file2, file1.joinLines(file2))

  test("combineAll combines multiple files"):
    val files = List(
      SourceFile.fromStrings("a"),
      SourceFile.fromStrings("b"),
      SourceFile.fromStrings("c")
    )
    val combined = SourceFile.combineAll(files)
    assertEquals(combined.lineCount, 3)

  test("combineAll on empty returns empty"):
    val combined = SourceFile.combineAll(List.empty)
    assert(combined.isEmpty)

  // --- Transformation Tests ---

  test("indentAll indents all lines"):
    val file = SourceFile.fromStrings("a", "b")
    val indented = file.indentAll()
    assert(indented.lines.forall(_.head == Some(Token.Indent)))

  test("indentAll with multiple indents"):
    val file = SourceFile.fromStrings("code")
    val indented = file.indentAll(2)
    assertEquals(
      indented.headLine.map(_.tokens.take(2)),
      Some(Vector(Token.Indent, Token.Indent))
    )

  test("map transforms each line"):
    val file = SourceFile.fromStrings("a", "b")
    val mapped = file.map(_.indent())
    assert(mapped.lines.forall(_.head == Some(Token.Indent)))

  test("flatMap expands lines"):
    val file = SourceFile.fromStrings("a", "b")
    val expanded = file.flatMap(l => SourceFile(l, SourceLine.value("---")))
    assertEquals(expanded.lineCount, 4)

  test("filter keeps matching lines"):
    val file = SourceFile(
      SourceLine.value("keep"),
      SourceLine.empty,
      SourceLine.value("also")
    )
    val filtered = file.filter(_.nonEmpty)
    assertEquals(filtered.lineCount, 2)

  test("filterNot removes matching lines"):
    val file = SourceFile(
      SourceLine.value("a"),
      SourceLine.empty,
      SourceLine.value("b")
    )
    val filtered = file.filterNot(_.isEmpty)
    assertEquals(filtered.lineCount, 2)

  test("dropEmpty removes empty lines"):
    val file = SourceFile(
      SourceLine.value("a"),
      SourceLine.empty,
      SourceLine.value("b"),
      SourceLine.empty
    )
    val dropped = file.dropEmpty
    assertEquals(dropped.lineCount, 2)

  test("reverse reverses line order"):
    val file = SourceFile.fromStrings("a", "b", "c")
    val reversed = file.reverse
    assertEquals(reversed.renderLines(), Vector("c", "b", "a"))

  test("take takes first n lines"):
    val file = SourceFile.fromStrings("a", "b", "c")
    val taken = file.take(2)
    assertEquals(taken.lineCount, 2)
    assertEquals(taken.renderLines(), Vector("a", "b"))

  test("drop drops first n lines"):
    val file = SourceFile.fromStrings("a", "b", "c")
    val dropped = file.drop(1)
    assertEquals(dropped.lineCount, 2)
    assertEquals(dropped.renderLines(), Vector("b", "c"))

  test("slice gets lines in range"):
    val file = SourceFile.fromStrings("a", "b", "c", "d")
    val sliced = file.slice(1, 3)
    assertEquals(sliced.lineCount, 2)
    assertEquals(sliced.renderLines(), Vector("b", "c"))

  test("mapTokens transforms all tokens in all lines"):
    val file = SourceFile.fromStrings("a", "b")
    val mapped = file.mapTokens {
      case Token.Value(v) => Token.Value(v.toUpperCase)
      case other          => other
    }
    assertEquals(mapped.renderLines(), Vector("A", "B"))

  // --- Line Modification Tests ---

  test("updateLine modifies specific line"):
    val file = SourceFile.fromStrings("a", "b", "c")
    val updated = file.updateLine(1, _.indent())
    assertEquals(updated.getLine(1).flatMap(_.head), Some(Token.Indent))

  test("updateLine returns unchanged for invalid index"):
    val file = SourceFile.fromStrings("a", "b")
    val updated = file.updateLine(10, _.indent())
    assertEquals(updated, file)

  test("updateLastLine modifies last line"):
    val file = SourceFile.fromStrings("a", "b")
    val updated = file.updateLastLine(_ :+ Token("!"))
    assertEquals(updated.lastLine.map(_.render()), Some("b !"))

  test("updateLastLine on empty returns empty"):
    val updated = SourceFile.empty.updateLastLine(_.indent())
    assert(updated.isEmpty)

  test("updateFirstLine modifies first line"):
    val file = SourceFile.fromStrings("a", "b")
    val updated = file.updateFirstLine(_ :+ Token("!"))
    assertEquals(updated.headLine.map(_.render()), Some("a !"))

  test("updateFirstLine on empty returns empty"):
    val updated = SourceFile.empty.updateFirstLine(_.indent())
    assert(updated.isEmpty)

  // --- Interleaving and Zipping Tests ---

  test("intersperse adds separator between lines"):
    val file = SourceFile.fromStrings("a", "b", "c")
    val separator = SourceLine.value("---")
    val interspersed = file.intersperse(separator)
    assertEquals(interspersed.lineCount, 5)
    assertEquals(
      interspersed.renderLines(),
      Vector("a", "---", "b", "---", "c")
    )

  test("intersperse with single line returns unchanged"):
    val file = SourceFile.fromStrings("a")
    val interspersed = file.intersperse(SourceLine.value("---"))
    assertEquals(interspersed, file)

  test("intersperse with empty returns empty"):
    val interspersed = SourceFile.empty.intersperse(SourceLine.value("---"))
    assert(interspersed.isEmpty)

  test("zipWith combines lines pairwise"):
    val file1 = SourceFile.fromStrings("a", "b")
    val file2 = SourceFile.fromStrings("1", "2")
    val zipped = file1.zipWith(file2)(_ ++ _)
    assertEquals(zipped.renderLines(), Vector("a 1", "b 2"))

  test("zipWith truncates to shorter"):
    val file1 = SourceFile.fromStrings("a", "b", "c")
    val file2 = SourceFile.fromStrings("1", "2")
    val zipped = file1.zipWith(file2)(_ ++ _)
    assertEquals(zipped.lineCount, 2)

  test("zipWithIndex adds line numbers"):
    val file = SourceFile.fromStrings("a", "b")
    val indexed = file.zipWithIndex
    assertEquals(indexed.headLine.map(_.render()), Some("0:  a"))
    assertEquals(indexed.lastLine.map(_.render()), Some("1:  b"))

  // --- Folding Tests ---

  test("foldLeft accumulates over lines"):
    val file = SourceFile.fromStrings("a", "b", "c")
    val result = file.foldLeft(0)((acc, _) => acc + 1)
    assertEquals(result, 3)

  test("foldRight accumulates over lines"):
    val file = SourceFile.fromStrings("a", "b", "c")
    val result =
      file.foldRight(Vector.empty[String])((line, acc) => line.render() +: acc)
    assertEquals(result, Vector("a", "b", "c"))

  test("foldLines combines all lines"):
    val file = SourceFile.fromStrings("a", "b", "c")
    val result = file.foldLines(_ ++ _)
    assertEquals(result.map(_.render()), Some("a b c"))

  test("foldLines on empty returns None"):
    val result = SourceFile.empty.foldLines(_ ++ _)
    assertEquals(result, None)

  // --- Searching Tests ---

  test("find returns first matching line"):
    val file = SourceFile(
      SourceLine.value("a"),
      SourceLine.fromWords("b", "b"),
      SourceLine.fromWords("c", "c", "c")
    )
    val found = file.find(_.length > 1)
    assertEquals(found.map(_.length), Some(2))

  test("find returns None when no match"):
    val file = SourceFile.fromStrings("a", "b")
    val found = file.find(_.length > 5)
    assertEquals(found, None)

  test("exists returns true when any match"):
    val file = SourceFile(
      SourceLine.value("a"),
      SourceLine.fromWords("b", "b")
    )
    assert(file.exists(_.length > 1))

  test("exists returns false when no match"):
    val file = SourceFile.fromStrings("a", "b")
    assert(!file.exists(_.length > 5))

  test("forall returns true when all match"):
    val file = SourceFile.fromStrings("a", "b")
    assert(file.forall(_.nonEmpty))

  test("forall returns false when any doesn't match"):
    val file = SourceFile(SourceLine.value("a"), SourceLine.empty)
    assert(!file.forall(_.nonEmpty))

  test("count returns matching count"):
    val file = SourceFile(
      SourceLine.value("a"),
      SourceLine.fromWords("b", "b"),
      SourceLine.fromWords("c", "c", "c")
    )
    assertEquals(file.count(_.length > 1), 2)

  test("indexWhere returns first matching index"):
    val file = SourceFile(
      SourceLine.value("a"),
      SourceLine.fromWords("b", "b"),
      SourceLine.fromWords("c", "c", "c")
    )
    assertEquals(file.indexWhere(_.length > 1), 1)

  test("indexWhere returns -1 when no match"):
    val file = SourceFile.fromStrings("a", "b")
    assertEquals(file.indexWhere(_.length > 5), -1)

  // --- Rendering Tests ---

  test("render produces multi-line string"):
    val file = SourceFile.fromStrings("a b", "c d")
    val rendered = file.render()
    assertEquals(rendered, "a b\nc d")

  test("render with custom line separator"):
    val file = SourceFile.fromStrings("a", "b")
    val rendered = file.render(lineSeparator = ";")
    assertEquals(rendered, "a;b")

  test("render with indents"):
    val file = SourceFile(SourceLine.fromTokens(Token.Indent, Token("code")))
    val rendered = file.render(indentStr = "    ")
    assertEquals(rendered, "     code")

  test("renderLines returns vector of rendered lines"):
    val file = SourceFile.fromStrings("a b", "c d")
    assertEquals(file.renderLines(), Vector("a b", "c d"))

  // --- Equality Tests ---

  test("equal files are equal"):
    val file1 = SourceFile.fromStrings("a", "b")
    val file2 = SourceFile.fromStrings("a", "b")
    assertEquals(file1, file2)

  test("different files are not equal"):
    val file1 = SourceFile.fromStrings("a", "b")
    val file2 = SourceFile.fromStrings("a", "c")
    assertNotEquals(file1, file2)

  // --- Monoid Laws ---
  // Quantified over generated SourceFiles in SourceFileMonoidLawsSpec.

  // --- Builder Tests ---

  test("builder creates empty file when build immediately"):
    val file = SourceFile.builder.build
    assert(file.isEmpty)

  test("builder.value adds value token"):
    val file = SourceFile.builder.value("hello").build
    assertEquals(file.lineCount, 1)
    assertEquals(file.headLine.map(_.render(separator = "")), Some("hello"))

  test("builder.newLine creates new line"):
    val file = SourceFile.builder
      .value("a")
      .newLine
      .value("b")
      .build
    assertEquals(file.lineCount, 2)

  test("builder.ref adds ref token"):
    val file = SourceFile.builder.ref("name").build
    assertEquals(file.headLine.flatMap(_.head), Some(Token.Ref("name")))

  test("builder.indent adds indent token"):
    val file = SourceFile.builder.indent.value("code").build
    assertEquals(file.headLine.flatMap(_.head), Some(Token.Indent))

  test("builder.token adds arbitrary token"):
    val file = SourceFile.builder.token(Token.Value("x")).build
    assertEquals(file.headLine.flatMap(_.head), Some(Token.Value("x")))

  test("builder.line adds complete line"):
    val l = SourceLine.fromWords("hello", "world")
    val file = SourceFile.builder.value("start").line(l).build
    assertEquals(file.lineCount, 2)
    assertEquals(file.lastLine.map(_.render()), Some("hello world"))

  test("builder.file appends entire file"):
    val other = SourceFile.fromStrings("a", "b")
    val file = SourceFile.builder.value("start").file(other).build
    assertEquals(file.lineCount, 3)

  test("builder fluent chain"):
    val file = SourceFile.builder
      .value("def")
      .value("foo")
      .value("=")
      .newLine
      .indent
      .value("42")
      .build
    assertEquals(file.lineCount, 2)
    assertEquals(file.headLine.map(_.render(separator = "")), Some("deffoo="))
