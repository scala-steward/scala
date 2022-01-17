package com.cmartin.zio

import com.cmartin.utils.Domain.Gav
import com.cmartin.utils.ZioLearn.{MyDomainException, MyExceptionTwo}
import com.cmartin.utils.config.ConfigHelper
import com.cmartin.utils.config.ConfigHelper._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import zio._
import zio.config.{ReadError, _}

class ZioLearnSpec extends AnyFlatSpec
    with Matchers {

  import ZioLearnSpec._

  val runtime = Runtime.default

  "An non fallible UIO effect" should "return a computation" in {
    val program = for {
      r1     <- UIO.succeed(0)
      result <- UIO.succeed(r1 + 1)
    } yield result

    val result = runtime.unsafeRun(program)

    result shouldBe 1
  }

  "A fallible Task effect" should "throw a FiberFailure when an exception occurs" in {
    val program = for {
      r1     <- UIO(0)
      result <- Task(r1 / r1)
    } yield result

    an[FiberFailure] should be thrownBy runtime.unsafeRun(program)
  }

  it should "throw a FiberFailure containing a String when a exception occurs" in {
    val expectedMessage = "error-message"
    val program         = for {
      r1     <- UIO(0)
      result <- Task(r1 / r1).orElseFail(expectedMessage)
    } yield result

    val failure = the[FiberFailure] thrownBy runtime.unsafeRun(program)

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

    val result = runtime.unsafeRun(program.either)
    result shouldBe Symbol("left")
    result.swap.map(_ shouldBe an[ArithmeticException])
  }

  // Throwable => MyDomainException
  it should "refine a failure with a custom exception" in {
    import com.cmartin.utils.ZioLearn.refineError

    val program: Task[Int] = for {
      r1     <- Task.attempt(0)
      result <- Task.attempt(1 / r1)
    } yield result

    val programRefined: IO[MyDomainException, Int] =
      program.refineOrDie(refineError())

    val result = runtime.unsafeRun(programRefined.either)

    result.swap.map(_ shouldBe a[MyExceptionTwo])
  }

  it should "map None to a String into the error channel" in {
    val none: Option[Int]                 = None
    val noneZio: IO[Option[Nothing], Int] = ZIO.fromOption(none)
    val program: IO[String, Int]          = noneZio.orElseFail("mapped error")

    a[FiberFailure] should be thrownBy runtime.unsafeRun(program)
  }

  it should "return a Left with an string error" in {
    val none: Option[Int]                 = None
    val noneZio: IO[Option[Nothing], Int] = ZIO.fromOption(none)
    val program: IO[String, Int]          = noneZio.mapError(_ => "mapped error")

    val result: Either[String, Int] = runtime.unsafeRun(program.either)

    result shouldBe Left("mapped error")
  }

  it should "exclude a sequence of groups" in {
    import ZioLearnSpec._

    val exclusionList = List("group-2", "group-4")
    val result        = artifacts.filterNot(dep => exclusionList.contains(dep.group))

    result shouldBe filteredArtifacts
  }

  it should "repeat 6 times exponentially from 10 millis" in {
    import zio._

    val policy =
      Schedule.exponential(10.milliseconds) &&
        // Schedule.spaced(3.seconds)
        Schedule.recurs(5)

    val program =
      (for {
        _ <- UIO(Console.printLine("zio console message"))
        // _ <- sleep(1.second)
      } yield ()) repeat policy

    // TODO
    // val result: (Duration, Int) = runtime.unsafeRun(program)

    // info(result._1.toMillis.toString)

  }

  it should "return a domain error inside fiber failure" in {
    trait DomainError
    case class ErrorOne(m: String) extends DomainError
    case class ErrorTwo(m: String) extends DomainError

    val program: ZIO[Any, DomainError, Int] =
      for {
        _      <- Task(1 / 1).orElseFail(ErrorOne("error-one"))
        result <- Task(1 / 0).orElseFail(ErrorTwo("error-two"))
      } yield result

    val failure = the[FiberFailure] thrownBy runtime.unsafeRun(program)

    failure.cause.failureOption.map { ex => ex shouldBe ErrorTwo("error-two") }
  }

  it should "return a domain error inside Left (either)" in {
    trait DomainError
    case class ErrorOne(m: String) extends DomainError
    case class ErrorTwo(m: String) extends DomainError

    val program: ZIO[Any, DomainError, Int] =
      for {
        _      <- Task(1 / 0).orElseFail(ErrorOne("error-one"))
        result <- Task(1 / 0).orElseFail(ErrorTwo("error-two"))
      } yield result

    val result = runtime.unsafeRun(program.either)

    result shouldBe Left(ErrorOne("error-one"))
  }

  "Zio Config" should "read the configuration from a Map" in {
    val expectedConfig: AppConfig = AppConfig(filename, exclusions)
    val configMap: Map[String, String] = Map(
      "FILENAME"   -> filename,
      "EXCLUSIONS" -> exclusions
    )

    val configLayer: Layer[ReadError[String], AppConfig] =
      ZConfig.fromMap(configMap, ConfigHelper.configDescriptor)

    val program = getConfig[AppConfig]

    val result = runtime.unsafeRun(
      program.provideLayer(configLayer)
    )

    result shouldBe expectedConfig
  }

  it should "fail when trying to retrieve a missing property" in {
    val configMap: Map[String, String]                   = Map.empty //  Map("FILENAME" -> filename)
    val configLayer: Layer[ReadError[String], AppConfig] =
      ZConfig.fromMap(configMap, ConfigHelper.configDescriptor)

    val program = getConfig[AppConfig]

    val resultEither = runtime.unsafeRun(
      program.provideLayer(configLayer).either
    )

    info(s"$resultEither")

    resultEither.isLeft shouldBe true
    resultEither.left.map { error =>
      error shouldBe a[ReadError.ZipErrors[_]]
      error.size shouldBe 2
    }
  }

  //TODO
  //   TypesafeConfig.fromHoconFile(file, descriptor)
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

  val filename   = "dependencies.data"
  val exclusions = "dep-exclusion-1"

}
