package com.gu.formstack.handlers

import com.amazonaws.services.lambda.runtime.{ Context, RequestStreamHandler }
import com.gu.formstack.Logging

import java.io.{ InputStream, OutputStream }

object FormstackSubmissionDeletionHandler extends RequestStreamHandler with Logging {

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    println("executing lambda")
//    (for {
//      config <- Config.fromEnvironmentVariables()
//      _ = logger.info(show"executing submission deletions with config ${config.hideSecrets}")
//      _ <- Logging.configureLogging(config, context)
//      submissionDeleter = FormstackSubmissionDeleter.fromConfig(config)
//      _ <- submissionDeleter.deleteExpiredSubmissions()
//    } yield ()).fold(
//      err => logger.error("error deleting expired submissions", err),
//      _ => ()
//    )
  }
}
