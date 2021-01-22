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
      _ <- Logging.configureLogging(config, context)
      _ = logger.info(show"executing form deletions with config ${config.hideSecrets}")
      formDeleter = FormstackFormDeleter.fromConfig(config)
      _ <- formDeleter.deleteExpiredForms()
    } yield ()).fold(
      err => {
        logger.error("error deleting expired forms", err)
        // It's important to throw this exception, so that the invocation will be recorded as an error.
        // Invocation retries and alarms being triggered are determined by invocation errors;.
        // See https://docs.aws.amazon.com/lambda/latest/dg/invocation-async.html and template.yaml (respectively)
        // for more details.
        throw err
      },
      _ => logger.info("expired forms successfully deleted")
    )
  }
}
