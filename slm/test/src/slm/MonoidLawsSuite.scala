package slm

import munit.ScalaCheckSuite
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll

/** Reusable property-based suite asserting the three monoid laws
  * (left identity, right identity, associativity) quantified over
  * generated inputs.
  *
  * Subclasses supply the algebra (`empty`, `combine`), a generator
  * `gen` for `A`, and a short `name` used to label the property tests.
  *
  * One trait, N implementations: instead of hand-writing six example-
  * based monoid tests across two types, each new monoid instance pays
  * one extension cost (provide `gen` + `empty` + `combine`) and gets
  * three quantified properties for free.
  */
abstract class MonoidLawsSuite[A](name: String) extends ScalaCheckSuite:

  def empty: A

  def combine(x: A, y: A): A

  def gen: Gen[A]

  property(s"$name: empty is left identity"):
    forAll(gen): a =>
      combine(empty, a) == a

  property(s"$name: empty is right identity"):
    forAll(gen): a =>
      combine(a, empty) == a

  property(s"$name: combine is associative"):
    forAll(gen, gen, gen): (a, b, c) =>
      combine(combine(a, b), c) == combine(a, combine(b, c))
