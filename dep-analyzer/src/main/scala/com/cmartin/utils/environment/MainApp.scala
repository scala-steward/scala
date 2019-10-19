package com.cmartin.utils.environment

import com.cmartin.learn.common.{ComponentLogging, Utils}
import zio.{App, UIO}

object MainApp
  extends App
    with ComponentLogging {

  val filename = "dep-analyzer/src/main/resources/deps2.log"

  override def run(args: List[String]): UIO[Int] = {

    log.info(s"Running MainApp")
    val program =
      for {
        lines <- getLinesFromFile(filename)
        t0 <- UIO(System.currentTimeMillis())
        parsedLines <- parseLines(lines)
        validDeps <- filterValid(parsedLines)
        t <- UIO(System.currentTimeMillis() - t0)
        _ <- logDepCollection(parsedLines)
      } yield (validDeps, t)


    val programLive = program.provide(FileManagerLive)

    val result = unsafeRun(programLive.either)

    result match {
      case Left(error) =>
        log.error(s"An error occurred: $error")
        UIO(1)

      case Right(list) =>
        val prettyList = Utils.prettyPrint(list._1)
        //log.info(s"lines: $prettyList, nano time: ${list._2}")
        UIO(0)
    }
  }
}
