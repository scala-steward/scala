package com.cmartin.utils

import com.cmartin.learn.common.ComponentLogging

import scala.util.matching.Regex

/*
  http get: http -v https://search.maven.org/solrsearch/select\?q\=g:"com.typesafe.akka"%20AND%20a:"akka-actor_2.13"%20AND%20v:"2.5.25"%20AND%20p:"jar"\&rows\=1\&wt\=json
 */

object DependencyAnalyzer
  extends App
    with ComponentLogging {

  import Logic._

  import scala.io.Source

  val pattern = raw"(^[a-z][a-z0-9-_\.]+):([a-z0-9-_\.]+):([0-9A-Za-z-\.]+)".r

  def parseDependencies(filename: String): Seq[Either[String, Dep]] = {
    Source
      .fromFile(filename)
      .getLines()
      .map(parseDepLine)
      .toSeq

    //TODO java.io.FileNotFoundException
    // TODO close resources with ZIO utility to avoid resource leaks, bufferedSource.close
  }

  def parseDepLine(line: String): Either[String, Dep] = {
    val matches = pattern.matches(line)
    log.debug(s"reading dependency candidate: $line matches regex? $matches")

    if (matches) {
      val regexMatch: Regex.Match = pattern.findAllMatchIn(line).next()
      Right(Dep(regexMatch.group(1), regexMatch.group(2), regexMatch.group(3)))
    } else {
      Left(line)
    }
  }

  /*
     E X E C U T I O N
   */

  val httpManager = HttpManager()


  val parsedDeps: Seq[Either[String, Dep]] =
    parseDependencies("dep-analyzer/src/main/resources/deps.log")

  val validDeps: Seq[Dep] =
    parsedDeps
      .collect {
        case Right(dep) => dep
      }

  val validRate: Double = 100.toDouble * validDeps.size / parsedDeps.size
  log.debug(s"valid rate: $validRate")


  val finalDeps = validDeps
    .filterNot(_.group.startsWith("com.cmartin"))


  httpManager.checkDependencies(finalDeps)


  //https://search.maven.org/solrsearch/select\?q\=g:"com.typesafe.akka"%20AND%20a:"akka-actor_2.13"%20AND%20v:"2.5.25"%20AND%20p:"jar"\&rows\=1\&wt\=json
  //val uri = raw"https://search.maven.org/solrsearch/select?q=g:com.typesafe.akka%20AND%20a:akka-actor_2.13%20AND%20v:2.5.25%20AND%20p:jar&rows=1&wt=json"

}

