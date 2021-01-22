package com.gu.formstack.services

import cats.implicits._
import com.gu.formstack.config.{ Config, FormstackGuardianMetadata }
import com.gu.formstack.services.FormstackClient.{ Form, StatusCodeError, isSubmissionDecryptionError }
import com.gu.formstack.Logging

import java.time.ZonedDateTime
import scala.util.Try

class FormstackSubmissionDeleter(val service: FormstackService, metadata: FormstackGuardianMetadata) extends Logging {

  private def deleteSubmissionsInForm(form: Form): Try[Unit] = {
    service.forEachPageOfSubmissions(
      form,
      submissionsBefore = ZonedDateTime.now.minus(metadata.submissionExpiryPeriod)
    ) { page =>
      logger.debug(show"${page.submissions.size} submission(s) returned in page: ${page.submissions}")

      page.submissions.traverse { submission =>
        logger.info(show"deleting submission $submission")
        service.deleteSubmission(submission).map { response =>
          logger.info(show"submission $submission deleted: $response")
        }
      }.map(_ => ())
    }
  }

  def deleteExpiredSubmissions(): Try[Unit] = {
    service.forEachPageOfForms { page =>
      page.forms
        .filter(metadata.isFormGDPREligible)
        .traverse { form =>
          logger.info(show"deleting expired submissions in form $form")
          deleteSubmissionsInForm(form).map(_ => logger.info(show"expired submissions in form $form deleted")).recover {
            // It has been observed that getting the submissions for some forms always returns a 500.
            // Whilst this issue is being resolved with Formstack, recover from this error
            // so that it doesn't short circuit the traversal
            // and prevent the submissions of other forms from being deleted.
            case err: StatusCodeError if isSubmissionDecryptionError(err) =>
              logger.warn(show"submission decryption error occurred for form $form")
          }
        }
        .map(_ => ())
    }
  }
}

object FormstackSubmissionDeleter {

  def fromConfig(config: Config): FormstackSubmissionDeleter = {
    val service = FormstackService.fromConfig(config)
    val metadata = new FormstackGuardianMetadata
    new FormstackSubmissionDeleter(service, metadata)
  }
}
