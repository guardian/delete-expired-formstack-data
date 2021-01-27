package com.gu.formstack

import cats.Show
import ch.qos.logback.classic.{ Level, Logger }
import com.amazonaws.services.lambda.runtime.Context
import com.gu.formstack.config.Config
import com.typesafe.scalalogging.StrictLogging
import io.circe.Encoder
import io.circe.syntax._
import org.slf4j.{ LoggerFactory, MDC }

import scala.util.Try

// Mixin in to classes to provide logging functionality.
trait Logging extends StrictLogging {

  // "show" some data by rendering it as JSON.
  // https://typelevel.org/cats/typeclasses/show.html
  implicit def showFromEncoder[A: Encoder]: Show[A] = Show.show(_.asJson.noSpaces)
}

object Logging {

  private def setLogLevel(rawLevel: String): Try[Unit] = {
    for {
      logger <- Try {
        LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
      }
      level = Level.toLevel(rawLevel, Level.INFO)
      _ = logger.setLevel(level)
    } yield ()
  }

  private def setFormstackAccountId(id: String): Try[Unit] = {
    Try(MDC.put("FormstackAccountId", id))
  }

  private def setAWSRequestId(id: String): Try[Unit] = {
    Try(MDC.put("AWSRequestId", id))
  }

  def configureLogging(config: Config, context: Context): Try[Unit] = {
    for {
      _ <- setLogLevel(config.logLevel)
      _ <- setFormstackAccountId(config.formstackAccountId)
      _ <- setAWSRequestId(context.getAwsRequestId)
    } yield ()
  }
}
