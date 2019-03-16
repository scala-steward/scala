package com.cmartin.learn.refined

import org.scalatest.EitherValues._
import org.scalatest._

class RefinedSpec extends FlatSpec with Matchers {

  it should "validate a positive integer" in {
    val a: Int = 1
    val res = validatePositiveInt(a)

    res.right.get.value shouldBe a
  }

  it should "reject a negative integer" in {
    val a: Int = -1
    val res = validatePositiveInt(a)

    res.left.value.contains(Constants.PredicateFailed) shouldBe true
  }

  it should "validate an even positive integer" in {
    val a: Int = 16
    val res = validateEvenPositive(a)

    res.right.get.value shouldBe a
  }

  it should "reject an odd positive integer" in {
    val a: Int = 17
    val res = validateEvenPositive(a)

    res.left.value.contains(Constants.PredicateFailed) shouldBe true
  }

  it should "validate a well known port" in {
    val a: Int = Constants.Port22
    val res = validateWellKnownPort(a)

    res.right.get.value shouldBe a
  }

  it should "reject a non well known port" in {
    val expected = None
    val a: Int = Constants.Port2222
    val res = validateWellKnownPort(a)

    res.toOption shouldBe expected
  }

  it should "validate an user port" in {
    val a: Int = Constants.Port8080
    val res = validateUserPort(a)

    res.right.get.value shouldBe a
  }

  it should "reject a non user port" in {
    val expected = None
    val a: Int = Constants.Port22
    val res = validateUserPort(a)

    res.toOption shouldBe expected
  }

  it should "validate a network port" in {
    val expectedPorts = List(Constants.Port22, Constants.Port2222)
    val resList = expectedPorts map (validateNetworkPort(_))
    val pairs = resList zip expectedPorts

    resList forall (_.isRight) shouldBe true
    pairs forall (p => p._1.right.get.value == p._2)
  }

}

object Constants {
  val PredicateFailed = "failed"
  val Port22 = 22
  val Port2222 = 2222
  val Port8080 = 8080
  val zipCode28020 = "28020"
}