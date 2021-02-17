package com.gu.formstack.config

import com.gu.formstack.Logging
import com.gu.formstack.services.FormstackClient.Form

import java.time.temporal.TemporalAmount
import java.time.{ Period, ZoneId, ZonedDateTime }
import scala.util.Try

class FormstackGuardianMetadata extends Logging {

  // Some forms are used as templates to create other forms.
  private def isFormATemplate(form: Form): Boolean = {
    form.name.toLowerCase.contains("template")
  }

  // Some forms want to be kept for a long time to e.g. gather data over many years.
  private def isFormLongstanding(form: Form): Boolean = {
    form.name.contains("_RP")
  }

  // i.e. should we delete the form, or form submissions if they have expired?
  def isFormGDPREligible(form: Form): Boolean = {
    !isFormATemplate(form) && !isFormLongstanding(form)
  }

  val submissionExpiryPeriod: TemporalAmount = Period.ofDays(390)

  def isFormExpired(form: Form): Try[Boolean] = {
    for {
      zoneId <- Try(ZoneId.of(form.timezone))
      localExpiryTime = form.lastSubmissionTime
        .map(_.plusDays(390))
        .orElse(form.updated.map(_.plusDays(30)))
        .getOrElse(form.created.plusDays(30))
      zonedExpiryTime = localExpiryTime.atZone(zoneId)
    } yield {
      zonedExpiryTime.isBefore(ZonedDateTime.now)
    }
  }
}
