package slm

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

/** Property-based laws for the `SourceLine` primitive operator set.
  *
  * Where `MonoidLawsSuite` quantifies the `(empty, ++)` algebra,
  * this suite quantifies the slicing, search, predicate, pattern,
  * and joining primitives that enable StringUtils-style derivations
  * (see `StringUtilsCompositionSpec`).
  *
  * Together the two suites form the algebraic contract a refactor
  * must preserve: any change to a primitive that breaks one of these
  * laws breaks every derived combinator transitively.
  */
class SourceLinePrimitivesLawsSpec extends ScalaCheckSuite:

  import Generators.{genSourceLine, genToken}

  val genNonNeg: Gen[Int] = Gen.choose(0, 12)

  // --- Slicing: take / drop / splitAt --------------------------------

  property("take(0) == empty"):
    forAll(genSourceLine): s =>
      s.take(0) == SourceLine.empty

  property("take(length) == self"):
    forAll(genSourceLine): s =>
      s.take(s.length) == s

  property("drop(0) == self"):
    forAll(genSourceLine): s =>
      s.drop(0) == s

  property("drop(length) == empty"):
    forAll(genSourceLine): s =>
      s.drop(s.length) == SourceLine.empty

  property("take(n) ++ drop(n) == self  (slicing covers the algebra)"):
    forAll(genSourceLine, genNonNeg): (s, n) =>
      s.take(n) ++ s.drop(n) == s

  property("splitAt(n) == (take(n), drop(n))"):
    forAll(genSourceLine, genNonNeg): (s, n) =>
      s.splitAt(n) == ((s.take(n), s.drop(n)))

  property("take is monotone-idempotent: take(n).take(m) == take(min(n,m))"):
    forAll(genSourceLine, genNonNeg, genNonNeg): (s, n, m) =>
      s.take(n).take(m) == s.take(math.min(n, m))

  property("drop composes: drop(n).drop(m) == drop(n + m)"):
    forAll(genSourceLine, genNonNeg, genNonNeg): (s, n, m) =>
      s.drop(n).drop(m) == s.drop(n + m)

  property("take(n).length == min(n, length)"):
    forAll(genSourceLine, genNonNeg): (s, n) =>
      s.take(n).length == math.min(n, s.length)

  property("drop(n).length == max(0, length - n)"):
    forAll(genSourceLine, genNonNeg): (s, n) =>
      s.drop(n).length == math.max(0, s.length - n)

  // --- Predicate slicing: takeWhile / dropWhile ----------------------

  property("takeWhile(p) ++ dropWhile(p) == self"):
    forAll(genSourceLine, genToken): (s, t) =>
      val p: Token => Boolean = _ == t
      s.takeWhile(p) ++ s.dropWhile(p) == s

  property("takeWhile(p) elements all satisfy p"):
    forAll(genSourceLine, genToken): (s, t) =>
      val p: Token => Boolean = _ == t
      s.takeWhile(p).forall(p)

  property("dropWhile(p).head does not satisfy p"):
    forAll(genSourceLine, genToken): (s, t) =>
      val p: Token => Boolean = _ == t
      s.dropWhile(p).head.forall(h => !p(h))

  property("takeWhile(_ => true) == self"):
    forAll(genSourceLine): s =>
      s.takeWhile(_ => true) == s

  property("takeWhile(_ => false) == empty"):
    forAll(genSourceLine): s =>
      s.takeWhile(_ => false) == SourceLine.empty

  property("dropWhile(_ => true) == empty"):
    forAll(genSourceLine): s =>
      s.dropWhile(_ => true) == SourceLine.empty

  property("dropWhile(_ => false) == self"):
    forAll(genSourceLine): s =>
      s.dropWhile(_ => false) == s

  // --- Search: indexWhere / lastIndexWhere / indexOfSlice ------------

  property("indexWhere(_ => false) == -1"):
    forAll(genSourceLine): s =>
      s.indexWhere(_ => false) == -1

  property("indexWhere(_ => true) is 0 iff non-empty, else -1"):
    forAll(genSourceLine): s =>
      s.indexWhere(_ => true) == (if s.isEmpty then -1 else 0)

  property("indexWhere(p) >= 0  ⇔  exists(p)"):
    forAll(genSourceLine, genToken): (s, t) =>
      val p: Token => Boolean = _ == t
      (s.indexWhere(p) >= 0) == s.exists(p)

  property("lastIndexWhere(p) >= indexWhere(p) when found"):
    forAll(genSourceLine, genToken): (s, t) =>
      val p: Token => Boolean = _ == t
      val first = s.indexWhere(p)
      val last  = s.lastIndexWhere(p)
      first < 0 || last >= first

  property("indexOfSlice(empty) == 0"):
    forAll(genSourceLine): s =>
      s.indexOfSlice(SourceLine.empty) == 0

  property("self.indexOfSlice(self) == 0"):
    forAll(genSourceLine): s =>
      s.indexOfSlice(s) == 0

  property("indexOfSlice round-trip: drop(i).take(sub.length) == sub when found"):
    forAll(genSourceLine, genSourceLine): (s, sub) =>
      val i = s.indexOfSlice(sub)
      i < 0 || s.drop(i).take(sub.length) == sub

  property("(a ++ b).indexOfSlice(b) ∈ [0, a.length]"):
    forAll(genSourceLine, genSourceLine): (a, b) =>
      val i = (a ++ b).indexOfSlice(b)
      i >= 0 && i <= a.length

  // --- Predicates: exists / forall / count / find --------------------

  property("forall(p) == !exists(!p)  (De Morgan)"):
    forAll(genSourceLine, genToken): (s, t) =>
      val p: Token => Boolean = _ == t
      s.forall(p) == !s.exists(x => !p(x))

  property("count(p) + count(!p) == length  (partition)"):
    forAll(genSourceLine, genToken): (s, t) =>
      val p: Token => Boolean = _ == t
      s.count(p) + s.count(x => !p(x)) == s.length

  property("count(p) == filter(p).length"):
    forAll(genSourceLine, genToken): (s, t) =>
      val p: Token => Boolean = _ == t
      s.count(p) == s.filter(p).length

  property("find(p) == filter(p).head"):
    forAll(genSourceLine, genToken): (s, t) =>
      val p: Token => Boolean = _ == t
      s.find(p) == s.filter(p).head

  property("forall(_ => true)  and  !exists(_ => false)"):
    forAll(genSourceLine): s =>
      s.forall(_ => true) && !s.exists(_ => false)

  // --- Pattern: startsWith / endsWith / contains ---------------------

  property("startsWith(empty) and endsWith(empty)"):
    forAll(genSourceLine): s =>
      s.startsWith(SourceLine.empty) && s.endsWith(SourceLine.empty)

  property("self.startsWith(self) and self.endsWith(self)"):
    forAll(genSourceLine): s =>
      s.startsWith(s) && s.endsWith(s)

  property("(a ++ b).startsWith(a)  and  (a ++ b).endsWith(b)"):
    forAll(genSourceLine, genSourceLine): (a, b) =>
      val combined = a ++ b
      combined.startsWith(a) && combined.endsWith(b)

  property("startsWith(p)  ⇔  take(p.length) == p"):
    forAll(genSourceLine, genSourceLine): (s, p) =>
      s.startsWith(p) == (s.take(p.length) == p)

  property("endsWith(suffix)  ⇔  matches the tail slice"):
    forAll(genSourceLine, genSourceLine): (s, suffix) =>
      s.endsWith(suffix) ==
        (suffix.length <= s.length &&
          s.drop(s.length - suffix.length) == suffix)

  property("contains(sub)  ⇔  indexOfSlice(sub) >= 0"):
    forAll(genSourceLine, genSourceLine): (s, sub) =>
      s.contains(sub) == (s.indexOfSlice(sub) >= 0)

  // --- Joining: intersperse ------------------------------------------

  property("empty.intersperse(sep) == empty"):
    forAll(genToken): t =>
      SourceLine.empty.intersperse(t) == SourceLine.empty

  property("singleton.intersperse(sep) == singleton"):
    forAll(genToken, genToken): (a, sep) =>
      SourceLine(a).intersperse(sep) == SourceLine(a)

  property("intersperse(sep).length == 2 * length - 1  (non-empty)"):
    forAll(genSourceLine, genToken): (s, sep) =>
      s.isEmpty || s.intersperse(sep).length == 2 * s.length - 1

  // --- Existing operators: laws that close the algebra ---------------

  property("reverse is an involution: reverse.reverse == self"):
    forAll(genSourceLine): s =>
      s.reverse.reverse == s

  property("reverse is anti-distributive over ++: (a ++ b).reverse == b.reverse ++ a.reverse"):
    forAll(genSourceLine, genSourceLine): (a, b) =>
      (a ++ b).reverse == b.reverse ++ a.reverse

  property("map(identity) == self"):
    forAll(genSourceLine): s =>
      s.map(identity) == s

  property("filter(_ => true) == self  and  filter(_ => false) == empty"):
    forAll(genSourceLine): s =>
      s.filter(_ => true) == s &&
      s.filter(_ => false) == SourceLine.empty
