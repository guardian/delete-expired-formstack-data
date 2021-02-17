package com.gu.formstack.services

import cats.implicits._
import com.gu.formstack.config.{ Config, FormstackGuardianMetadata }
import com.gu.formstack.services.FormstackClient.Form
import com.gu.formstack.Logging

import scala.util.Try

class FormstackFormDeleter(val service: FormstackService, metadata: FormstackGuardianMetadata) extends Logging {

  private def isFormExpired(form: Form): Boolean = {
    metadata.isFormExpired(form).getOrElse {
      // If we can't determine whether the form has expired,
      // since expired forms are deleted, err on the side of caution and say it hasn't.
      logger.warn(show"unable to determine if form has expired: $form")
      false
    }
  }

  // Returns a list of forms that were deleted
  def deleteExpiredForms(): Try[Unit] = {
    service.forEachPageOfForms { page =>
      logger.debug(show"forms in page: ${page.forms}")

      val formsToDelete =
        page.forms.filter(form => metadata.isFormGDPREligible(form) && isFormExpired(form))
      logger.debug(show"forms in page to delete: $formsToDelete")

      formsToDelete.traverse { form =>
        logger.info(show"deleting form $form")
        service.deleteForm(form).map { response =>
          logger.info(show"form $form deleted: $response")
        }
      }.map(_ => ())
    }
  }
}

object FormstackFormDeleter {

  def fromConfig(config: Config): FormstackFormDeleter = {
    val service = FormstackService.fromConfig(config)
    val metadata = new FormstackGuardianMetadata
    new FormstackFormDeleter(service, metadata)
  }
}
