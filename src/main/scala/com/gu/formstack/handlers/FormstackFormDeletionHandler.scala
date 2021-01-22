package com.gu.formstack.handlers

import cats.implicits._
import com.amazonaws.services.lambda.runtime.{ Context, RequestStreamHandler }
import com.gu.formstack.Logging
import com.gu.formstack.config.Config
import com.gu.formstack.services.FormstackFormDeleter

import java.io.{ InputStream, OutputStream }

object FormstackFormDeletionHandler extends RequestStreamHandler with Logging {

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    (for {
      config <- Config.fromEnvironmentVariables()
      _ = logger.info(show"executing form deletions with config ${config.hideSecrets}")
      _ <- Logging.configureLogging(config, context)
      formDeleter = FormstackFormDeleter.fromConfig(config)
      _ <- formDeleter.deleteExpiredForms()
    } yield ()).fold(
      err => logger.error("error in form deletion process", err),
      _ => ()
    )
  }
}
