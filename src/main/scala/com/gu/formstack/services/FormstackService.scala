package com.gu.formstack.services

import cats.Monoid
import cats.implicits._
import com.gu.formstack.config.{ Config, FormstackJsonConfiguration }
import com.gu.formstack.services.FormstackClient.{ DeleteEntityResponse, Form, Forms, Submission, Submissions }
import io.circe.generic.extras.ConfiguredJsonCodec

import java.time.ZonedDateTime
import scala.util.Try

class FormstackService(
  client: FormstackClient,
  isDryRun: Boolean // i.e. if true, only HTTP requests with no side-effects will be executed.
) {

  import FormstackService._

  private def checkNotDryRun: Try[Unit] =
    Either.cond(!isDryRun, (), DryRun).toTry

  def deleteForm(form: Form): Try[DeletionResult] = {
    (for {
      _ <- checkNotDryRun
      response <- client.deleteForm(form)
    } yield {
      DeletionResult.nonDryRun(response)
    }).recover {
      case DryRun => DeletionResult.dryRun(DeleteEntityResponse(form.id, success = 0))
    }
  }

  def deleteSubmission(submission: Submission): Try[DeletionResult] = {
    (for {
      _ <- checkNotDryRun
      response <- client.deleteSubmission(submission)
    } yield {
      DeletionResult.nonDryRun(response)
    }).recover {
      case DryRun => DeletionResult.dryRun(DeleteEntityResponse(submission.id, success = 0))
    }
  }

  // Paginate through the pages of forms,
  // combining the results of applying a function to each page.
  def forEachPageOfForms[A: Monoid](f: Forms => Try[A]): Try[A] = {
    val pagination = FormPaginator
      .init[A]
      .iterateUntilM(paginator => {
        // The function to execute per page of forms.
        for {
          forms <- client.getForms(paginator.nextPage)
          data <- f(forms)
        } yield {
          paginator.next(forms, data)
        }
      })(_.isComplete) // The function to determine if pagination should stop.

    for {
      result <- pagination
    } yield result.data
  }

  // Paginate through the pages of submissions,
  // combining the results of applying a function to each page.
  def forEachPageOfSubmissions[A: Monoid](
    form: Form,
    submissionsBefore: ZonedDateTime
  )(f: Submissions => Try[A]): Try[A] = {
    val pagination = SubmissionPaginator
      .init[A]
      .iterateUntilM(paginator => {
        // The function to execute per page of submissions.
        for {
          submissions <- client.getSubmissions(form, paginator.nextPage, submissionsBefore)
          data <- f(submissions)
        } yield {
          paginator.next(submissions, data)
        }
      })(_.isComplete) // The function to determine if pagination should continue.

    for {
      result <- pagination
    } yield result.data
  }
}

object FormstackService extends FormstackJsonConfiguration {

  def fromConfig(config: Config): FormstackService = {
    val client = FormstackClient.fromConfig(config)
    new FormstackService(client, config.isDryRun)
  }

  // Encoder type class created so deletion results can be easily rendered in log entries
  @ConfiguredJsonCodec case class DeletionResult(response: DeleteEntityResponse, isDryRun: Boolean)

  object DeletionResult {

    def dryRun(response: DeleteEntityResponse): DeletionResult = {
      DeletionResult(response, isDryRun = true)
    }

    def nonDryRun(response: DeleteEntityResponse): DeletionResult = {
      DeletionResult(response, isDryRun = false)
    }
  }

  // Used to fail any for-comprehensions attempting to delete an entity when dry run is configured to true.
  private case object DryRun extends Exception

  private case class FormPaginator[A](
    nextPage: Int,
    forms: Int, // number of forms encountered so far in the pagination
    totalForms: Option[Int], // unknown until the first request is made
    data: A
  ) {
    def next(forms: Forms, data: A)(implicit M: Monoid[A]): FormPaginator[A] = FormPaginator(
      nextPage = nextPage + 1,
      forms = this.forms + forms.forms.size,
      totalForms = totalForms.orElse(Some(forms.total)),
      data = this.data.combine(data)
    )

    // Unfortunately form pagination doesn't have the concept of a page.
    // Instead, pagination termination has to be inferred by the number of forms encountered.
    def isComplete: Boolean = forms >= totalForms.getOrElse(Int.MaxValue)
  }

  private object FormPaginator {
    def init[A: Monoid]: FormPaginator[A] = FormPaginator(
      nextPage = 1,
      forms = 0,
      totalForms = None,
      data = Monoid[A].empty
    )
  }

  case class SubmissionPaginator[A](
    nextPage: Int,
    totalPages: Option[Int], // unknown until the first request is made
    data: A
  ) {
    def next(submissions: Submissions, data: A)(implicit M: Monoid[A]): SubmissionPaginator[A] = SubmissionPaginator(
      nextPage = nextPage + 1,
      totalPages = totalPages.orElse(Some(submissions.pages)),
      data = this.data.combine(data)
    )

    def isComplete: Boolean = nextPage > totalPages.getOrElse(Int.MaxValue)
  }

  private object SubmissionPaginator {
    def init[A: Monoid]: SubmissionPaginator[A] =
      SubmissionPaginator(
        nextPage = 1,
        totalPages = None,
        data = Monoid[A].empty
      )
  }
}
