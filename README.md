# sourceline-manager

**A composable algebraic data type for source-code generation in Scala 3.**
Build source files from typed, immutable values — not string interpolation.

> ## 🚧 UNDER CONSTRUCTION 🚧
>
> **This project is a work in progress and is not yet usable.** Nothing here is
> guaranteed to work, compile, or stay stable. APIs, types, and documentation
> may change or disappear without notice, and the published coordinates below
> are **not yet released**. Do not depend on this library yet — treat everything
> in this README as a statement of intent, not a promise.

[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE.md)
[![Scala 3](https://img.shields.io/badge/Scala-3.8.3-red.svg)](https://www.scala-lang.org/)
[![Platforms](https://img.shields.io/badge/platforms-JVM%20%7C%20JS%20%7C%20Native-green.svg)](#platform-support)

- **groupId:** `no.virtual-architect`
- **artifactId:** `sourceline-manager`
- **version:** `0.2.0-SNAPSHOT`
- **base package:** `slm`
- **repository:** https://github.com/tigidar/sourceline-manager

## Overview

Code generators that grow beyond a handful of templates hit the limits of
plain string interpolation: indentation becomes accidental, variable
references and free text are indistinguishable, and refactors turn into
fragile substring surgery.

`slm` (sourceline-manager) treats source code as **data**. A small algebra
of three layered types — `Token`, `SourceLine`, `SourceFile` — captures the
structure that string templating throws away. Rendering to text is the
*last* step, parameterized by indent strings and separators, never baked
into the data.

The result is a foundation library that downstream generators can rely on:
transformations are total functions over the ADT and never touch character
data; composition is associative with an identity (both layers form
**monoids**, enforced by property-based tests); variable holes (`Token.Ref`)
survive every transformation; and the same value algebra runs on **JVM,
Scala.js, and Scala Native** from a single source tree.

## Features

- Three-layer ADT: `Token` (`Value | Indent | Ref`) → `SourceLine` → `SourceFile`.
- Private primary constructors and a vocabulary of **smart constructors**
  (`SourceLine.value`, `.ref`, `.fromWords`, `.fromText`, `.spaced`, …) that
  communicate intent, not just shape.
- **Operator algebra**: `++` / `|+|` / `combine` for stacking,
  `|++|` / `joinLines` for horizontal merge of last-with-first.
- Symmetric prefix/postfix and `+:` / `:+` operators on both layers.
- Combinators: `indent`, `mapTokens`, `intersperse`, `zipWith`,
  `zipWithIndex`, `foldLines`, `updateLine`, `dropEmpty`, …
- Total accessors — `head`, `last`, `getLine`, `lastToken` return `Option`.
- Pluggable **rendering** with configurable indent string, token separator,
  and line separator. References render as `${name}`.
- **`derives CanEqual`** — accidental cross-type comparisons fail to compile.
- A fluent **`SourceFileBuilder`** for incremental DSL-style construction.
- Monoid laws verified with ScalaCheck on every platform.
- Zero runtime dependencies; pure `scala.collection.immutable`.

## Installation

Maven coordinates:

```
no.virtual-architect::sourceline-manager::0.2.0-SNAPSHOT
```

### Mill

```scala
def mvnDeps = Seq(
  mvn"no.virtual-architect::sourceline-manager::0.2.0-SNAPSHOT"
)
```

### sbt

```scala
libraryDependencies += "no.virtual-architect" %%% "sourceline-manager" % "0.2.0-SNAPSHOT"
```

### scala-cli

```scala
//> using dep no.virtual-architect::sourceline-manager::0.2.0-SNAPSHOT
```

## Quick start

```scala
import slm.*

// 1. Fluent builder
val script = SourceFile.builder
  .value("#!/usr/bin/env bash").newLine
  .value("set -eu").newLine.newLine
  .value("echo").value("'hello'")
  .build

script.render()
// #!/usr/bin/env bash
// set -eu
//
// echo 'hello'

// 2. Value algebra with a named reference
val signature = SourceLine.fromWords("val", "greeting", "=")
val body      = SourceLine.fromTokens(
  Token.Value("s\"hello, "), Token.Ref("name"), Token.Value("\"")
).indent()

val snippet = SourceFile(signature, body)
snippet.render()
// val greeting =
//    s"hello,  ${name} "

// 3. Compose files monoidally
val header   = SourceFile.fromStrings("// generated — do not edit", "")
val combined = header ++ snippet
```

`SourceLine` and `SourceFile` are immutable values — every operation returns a
new instance, so they are safe to share, cache, and combine in any order.

## API tour

### `Token`

```scala
enum Token derives CanEqual:
  case Value(value: String)   // literal text
  case Indent                 // one logical indent step
  case Ref(value: String)     // named reference, renders as ${value}
```

Smart constructors: `Token("hello")` → `Value("hello")`, `Token.ref("x")` →
`Ref("x")`, `Token.empty` → `Value("")`, `token.asLine` → `SourceLine(token)`.

### `SourceLine`

A sequence of `Token`s representing one logical line.

```scala
SourceLine.empty
SourceLine.fromWords("val", "x", "=", "42")
SourceLine.fromText("a,b,c", separator = ",")
SourceLine.fromTokens(Token.Value("def"), Token.Indent, Token.Ref("fn"))
SourceLine.value("hello")                 // single Value
SourceLine.ref("varName")                 // single Ref
SourceLine.indentToken                    // single Indent
SourceLine.spaced(Token("a"), Token("b")) // tokens with " " between
```

Operations grouped by intent:

- **Access:** `isEmpty`, `length`, `head`, `last`, `tokens`.
- **Prefix / postfix:** `prefix` / `+:`, `postfix` / `:+`, `prependAll`, `appendAll`.
- **Combine (monoid):** `++`, `|+|`, `combine`, `SourceLine.combineAll`.
- **Transform:** `indent(n)`, `map`, `flatMap`, `filter`, `reverse`.
- **Slice / search:** `take`, `drop`, `splitAt`, `takeWhile`, `indexWhere`,
  `indexOfSlice`, `find`, `exists`, `forall`, `startsWith`, `endsWith`, `contains`.
- **Join / fold:** `intersperse`, `foldLeft`, `foldRight`.
- **Render:** `render(indentStr, separator)`, `renderTokens(indentStr)`.

### `SourceFile`

A sequence of `SourceLine`s. The operational vocabulary lifts one level up.

```scala
SourceFile.empty
SourceFile(line1, line2, line3)
SourceFile.fromStrings("val x = 1", "val y = 2")
SourceFile.fromMultiLine("a\nb\nc")
SourceFile.value("test")
SourceFile.builder
```

Highlights of the operator set:

- `file1 ++ file2` — vertical stack (aliases: `|+|`, `combine`).
- `file1 |++| file2` / `joinLines` — merge `file1`'s last line with
  `file2`'s first line, then continue. Distinct from `++` because horizontal
  merge is not vertical stack.
- `file :+ token` / `token +: file` — operate on the **last** line.
- `file :+ line` / `line +: file` — operate at the file level.
- `indentAll(n)`, `mapTokens(f)`, `dropEmpty`, `intersperse(sepLine)`,
  `zipWith`, `zipWithIndex`.
- `updateLine(i, f)`, `updateFirstLine(f)`, `updateLastLine(f)` — index-safe.
- `render(indentStr, tokenSeparator, lineSeparator)`, `renderLines(…)`.

### `SourceFileBuilder`

A convenience facade over the immutable model for incremental, fluent
construction. `build` is the only operation that exits the builder;
everything else returns a new builder. The canonical API remains the value
algebra — the builder is purely sugar.

```scala
val file = SourceFile.builder
  .value("def").value("foo").value("=").newLine
  .indent.value("42")
  .build
```

## Design notes

The library is small on purpose. Four decisions shape the entire surface.

**1. Source code is data, not strings.** A `String`-templating generator
loses information the moment a fragment is concatenated. `slm` keeps three
things distinguishable at the type level: literal text (`Token.Value`),
indentation (`Token.Indent`), and named holes (`Token.Ref`). Rendering is
one pure function per layer; the rendering parameters (indent string, token
separator, line separator) are arguments and never leak into the model.
Substitution, indentation, and renaming are walks over the ADT, not regex
over text.

**2. Functional domain design.** Immutable values everywhere — `Token` is
an `enum`, `SourceLine` and `SourceFile` are `final case class`es. Primary
constructors are private; the public surface is a vocabulary of smart
constructors that name intent. Operators encode algebra: `++` is the
canonical combine, with `|+|` and `combine` as semantically identical
aliases. `|++|` / `joinLines` is deliberately a separate operator because
horizontal merge is a distinct operation from vertical stacking. Total
functions where possible — `head`, `last`, `getLine`, `lastToken` return
`Option`; `indent(0)` is identity; `joinLines` handles either side empty
without exception. Monoid laws (left identity, right identity,
associativity) are quantified over generated values in the test suite.

**3. Cross-platform via a single source of truth.** One source tree
compiles for all three platforms. The library uses only
`scala.collection.immutable`, `Option`, `String`, and `Int` — no
reflection, no `java.io`, no `java.util.concurrent`, no `js.Dynamic`.
Portability is met by *not having a platform surface*, not by the build
juggling sources. The same MUnit suite acts as a portability conformance
test that must pass on JVM, JS, and Native.

**4. Scala version policy.** The build is wired for `Cross[]`
cross-Scala-version publishing from day one, but the matrix is pinned to a
single version (`3.8.3`) until the next LTS ships. When that happens,
adding the new version is a one-line change to `build.mill`. If a
version-specific divergence ever appears, it goes in a `src-<version>/`
directory, not in preprocessor-style conditionals.

## Platform support

| Platform     | Scala            | Notes                              |
|--------------|------------------|------------------------------------|
| JVM          | 3.8.3            | `slm.jvm[3.8.3]`                   |
| Scala.js     | 3.8.3 / JS 1.20  | `slm.js[3.8.3]`                    |
| Scala Native | 3.8.3 / SN 0.5   | `slm.native[3.8.3]`                |

All platforms share one source tree and one test suite. New Scala versions are
added by appending to `V.scalaVersions` in `build.mill`.

## Building from source

The build is driven by [Mill](https://mill-build.com/) 1.1.2 and tests run on
[MUnit](https://scalameta.org/munit/) with `munit-scalacheck` for the
property-based monoid laws.

```bash
mill slm.jvm[3.8.3].compile
mill slm.jvm[3.8.3].test
mill slm.js[3.8.3].test
mill slm.native[3.8.3].test

mill __.compile          # everything, all platforms
mill __.test             # all tests, all platforms
```

### Adding a new Scala version (when 3.9 LTS ships)

Edit `build.mill`:

```scala
val scalaVersions = Seq("3.8.3", "3.9.0")  // append the new entry
```

Then `mill __.compile` and `mill __.test`. No other changes are required.

### Nix devshell (optional)

A `flake.nix` provides Mill, scala-cli, an OpenJDK, and the Scala Native
toolchain (clang, libcxxClang, boehm-gc, zlib):

```bash
nix develop
mill __.test
```

## Project layout

```
sourceline-manager/
├── build.mill                      Mill build (Cross + per-platform modules)
├── .mill-version                   pinned Mill 1.1.2
├── flake.nix                       optional Nix devshell
├── deps/
│   └── Dependencies.mill           test-only dependencies (munit, munit-scalacheck)
├── slm/
│   ├── src/slm/                    shared sources (JVM + JS + Native)
│   │   ├── SourceLine.scala
│   │   └── SourceFile.scala
│   └── test/src/slm/               shared MUnit suite + ScalaCheck laws
├── docs/adr/                       architecture decision records
└── LICENSE.md
```

The `slm/jvm/`, `slm/js/`, `slm/native/` directories are created on demand by
Mill as module-anchor dirs. They never contain source.

## License

Released under the Apache License 2.0. See [`LICENSE.md`](LICENSE.md) for the
full text.
