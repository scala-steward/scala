package com.cmartin.zio

import com.cmartin.utils.Domain.Gav
import com.cmartin.utils.ZioLearn.{MyDomainException, MyExceptionTwo}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio._

class ZioLearnSpec extends AnyFlatSpec with Matchers with DefaultRuntime {

  "An unfailling UIO effect" should "return a computation" in {
    val program = for {
      r1     <- UIO.effectTotal(0)
      result <- UIO.effectTotal(r1 + 1)
    } yield result

    val result = unsafeRun(program)

    result shouldBe 1
  }

  "A fallible Task effect" should "throw a FiberFailure when an exception occurs" in {
    val program = for {
      r1     <- UIO(0)
      result <- Task(r1 / r1)
    } yield result

    an[FiberFailure] should be thrownBy unsafeRun(program)
  }

  it should "throw a FiberFailure containing a String when a exception occurs" in {
    val expectedMessage = "error-message"
    val program = for {
      r1     <- UIO(0)
      result <- Task(r1 / r1).mapError(_ => expectedMessage)
    } yield result

    val failure = the[FiberFailure] thrownBy unsafeRun(program)

    failure.cause.failureOption.map { message =>
      message shouldBe expectedMessage
    }
  }

  /* probes the function 'ZIO.either' */
  it should "should return a Left containing an exception" in {
    val program = for {
      a <- Task(1)
      b <- Task(a / 0)
    } yield b

    val result = unsafeRun(program.either)
    result shouldBe Symbol("left")
    result.swap.map(_ shouldBe an[ArithmeticException])
  }

  // Throwable => MyDomainException
  it should "refine a failure with a custom exception" in {
    import com.cmartin.utils.ZioLearn.refineError

    val program: Task[Int] = for {
      r1     <- Task.effect(0)
      result <- Task.effect(1 / r1)
    } yield result

    val programRefined: IO[MyDomainException, Int] = program.refineOrDie(refineError())

    val result = unsafeRun(programRefined.either)

    result.swap.map(_ shouldBe a[MyExceptionTwo])
  }

  it should "map None to a String into the error channel" in {
    val none: Option[Int]        = None
    val noneZio: IO[Unit, Int]   = ZIO.fromOption(none)
    val program: IO[String, Int] = noneZio.mapError(_ => "mapped error")

    a[FiberFailure] should be thrownBy unsafeRun(program)
  }

  it should "return a Left with an string error" in {
    val none: Option[Int]        = None
    val noneZio: IO[Unit, Int]   = ZIO.fromOption(none)
    val program: IO[String, Int] = noneZio.mapError(_ => "mapped error")

    val result: Either[String, Int] = unsafeRun(program.either)

    result shouldBe Left("mapped error")
  }

  it should "exclude a sequence of groups" in {
    import ZioLearnSpec._

    val exclusionList = List("group-2", "group-4")
    val result        = artifacts.filterNot(dep => exclusionList.contains(dep.group))

    result shouldBe filteredArtifacts
  }

  it should "repeat 6 times exponentially from 10 millis" in {
    import zio.clock._
    import zio.console._
    import zio.duration._

    val policy =
      Schedule.exponential(10.milliseconds) &&
        //Schedule.spaced(3.seconds)
        Schedule.recurs(5)

    val program: ZIO[Clock with Console, Nothing, (Duration, Int)] = (for {
      _ <- putStrLn("zio console message")
      //_ <- sleep(1.second)
    } yield ()) repeat policy

    val result: (Duration, Int) = unsafeRun(program)

    info(result._1.toMillis.toString)

  }

  it should "compile" in {
    trait DomainError
    case class ErrorOne(m: String) extends DomainError
    case class ErrorTwo(m: String) extends DomainError

    val program: ZIO[Any, DomainError, Int] =
      for {
        _      <- Task(1 / 0).mapError(_ => ErrorOne("error-one"))
        result <- Task(1 / 0).mapError(_ => ErrorTwo("error-two"))
      } yield result

    val result = unsafeRun(program.either)

    result shouldBe Left(ErrorOne("error-one"))
  }
}

object ZioLearnSpec {
  val artifacts: List[Gav] = List(
    Gav("group-1", "a11", "v11"),
    Gav("group-1", "a12", "v12"),
    Gav("group-2", "a21", "v21"),
    Gav("group-3", "a31", "v31"),
    Gav("group-4", "a4", "v41")
  )

  val filteredArtifacts: List[Gav] = List(
    Gav("group-1", "a11", "v11"),
    Gav("group-1", "a12", "v12"),
    Gav("group-3", "a31", "v31")
  )
}
