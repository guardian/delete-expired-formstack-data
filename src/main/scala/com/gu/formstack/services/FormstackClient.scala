package com.gu.formstack.services

import cats.implicits._
import com.gu.formstack.Logging
import com.gu.formstack.config.{ Config, FormstackJsonConfiguration }
import io.circe.Decoder.Result
import io.circe._
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.parser.decode

import java.net.http.HttpClient.Version
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{ HttpClient, HttpRequest, HttpResponse }
import java.net.{ URI, URLEncoder }
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter
import java.time.{ Duration, LocalDateTime, ZoneId, ZonedDateTime }
import scala.util.Try

// Formstack client used to model a subset of the endpoints documented in:
// https://developers.formstack.com/reference#api-overview
private[services] class FormstackClient(
  client: HttpClient,
  accessToken: String, // https://developers.formstack.com/reference#api-overview
  encryptionPassword: String // https://help.formstack.com/hc/en-us/articles/360019518371-Data-Encryption
) extends Logging {

  import FormstackClient._

  private def baseGetRequestBuilder(url: String): HttpRequest.Builder = {
    HttpRequest.newBuilder().uri(URI.create(url)).GET().accessToken(accessToken)
  }

  private def baseGetRequest(url: String): Try[HttpRequest] = {
    Try(baseGetRequestBuilder(url).build())
  }

  private def baseDeleteRequestBuilder(url: String): HttpRequest.Builder = {
    HttpRequest.newBuilder().uri(URI.create(url)).DELETE().accessToken(accessToken)
  }

  private def baseDeleteRequest(url: String): Try[HttpRequest] = {
    Try(baseDeleteRequestBuilder(url).build())
  }

  private def checkStatusCode(request: HttpRequest, response: HttpResponse[String]): Try[Unit] = {
    Either.cond(response.statusCode == 200, (), StatusCodeError(request, response.statusCode, response.body)).toTry
  }

  private def sendRequest[A: Decoder](request: HttpRequest): Try[A] = {
    for {
      response <- Try(client.send(request, BodyHandlers.ofString))
      _ <- checkStatusCode(request, response)
      body <- decode[A](response.body).toTry
    } yield {
      body
    }
  }

  def getForms(page: Int): Try[Forms] = {
    for {
      request <- baseGetRequest(s"https://www.formstack.com/api/v2/form.json?page=$page&per_page=100")
      _ = logger.debug(s"${request.method} ${request.uri}")
      forms <- sendRequest[Forms](request)
    } yield {
      forms
    }
  }

  def deleteForm(form: Form): Try[DeleteEntityResponse] = {
    for {
      request <- baseDeleteRequest(s"https://www.formstack.com/api/v2/form/${form.id}.json")
      _ = logger.debug(s"${request.method} ${request.uri}")
      response <- sendRequest[DeleteEntityResponse](request)
    } yield {
      response
    }
  }

  def getSubmissions(form: Form, page: Int, maxTime: ZonedDateTime): Try[Submissions] = {
    for {
      maxTimeEST <- formatDateTimeQueryParam(maxTime)
      request <- Try {
        baseGetRequestBuilder(
          // Maximum of 100 supported per page.
          s"https://www.formstack.com/api/v2/form/${form.id}/submission.json?page=$page&per_page=100&max_time=$maxTimeEST"
        ).encryptionPassword(encryptionPassword).build()
      }
      _ = logger.debug(s"${request.method} ${request.uri}")
      submissions <- sendRequest[Submissions](request)
    } yield {
      submissions
    }
  }

  def deleteSubmission(submission: Submission): Try[DeleteEntityResponse] = {
    for {
      request <- baseDeleteRequest(s"https://www.formstack.com/api/v2/submission/${submission.id}.json")
      _ = logger.debug(s"${request.method} ${request.uri}")
      response <- sendRequest[DeleteEntityResponse](request)
    } yield {
      response
    }
  }
}

object FormstackClient extends FormstackJsonConfiguration {

  def fromConfig(config: Config): FormstackClient = {
    val httpClient = HttpClient.newBuilder().version(Version.HTTP_2).connectTimeout(Duration.ofSeconds(20)).build()
    new FormstackClient(
      httpClient,
      config.formstackAccessToken,
      config.formstackEncryptionPassword
    )
  }

  // Format for date time query parameters and date time fields returned in JSON response bodies.
  private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  // Documentation specifies timezone and format, date time query parameters should have.
  // https://developers.formstack.com/reference#form-id-submission-get
  def formatDateTimeQueryParam(dateTime: ZonedDateTime): Try[String] = {
    for {
      dateTimeEasternTime <- Try(
        dateTime.withZoneSameInstant(ZoneId.of("America/New_York"))
      )
      param <- Try(dateTimeEasternTime.format(dateTimeFormatter))
    } yield {
      URLEncoder.encode(param, StandardCharsets.UTF_8)
    }
  }

  // Instance provided by circe only decodes local date times with format yyyy-MM-ddTHH:mm:ss
  // Required to create codecs for any case classes that include LocalDateTime fields.
  implicit val localDateTimeCodec: Codec[LocalDateTime] =
    new Codec[LocalDateTime] {
      override def apply(c: HCursor): Result[LocalDateTime] = {
        for {
          string <- Decoder.decodeString(c)
          dateTime <- Either
            .catchNonFatal(LocalDateTime.parse(string, dateTimeFormatter))
            .leftMap(err => DecodingFailure(s"LocalDateTime ${err.getMessage}", c.history))
        } yield dateTime
      }
      override def apply(a: LocalDateTime): Json =
        Json.fromString(a.format(dateTimeFormatter))
    }

  // Generic delete response returned by Formstack when an entity is deleted.
  @ConfiguredJsonCodec case class DeleteEntityResponse(id: String, success: Int)

  // Only includes the fields required by this application.
  @ConfiguredJsonCodec case class Form(
    id: String,
    name: String,
    created: LocalDateTime,
    updated: Option[LocalDateTime],
    lastSubmissionTime: Option[LocalDateTime],
    timezone: String
  )

  // Only includes the fields required by this application.
  @ConfiguredJsonCodec case class Forms(forms: List[Form], total: Int)

  // Only includes the fields required by this application.
  // timestamp not strictly required, but useful to verify (via logs) only submissions before a certain time are getting deleted.
  // Also, the documentation implies timestamp is implicitly a zoned date time (America/New_York),
  // however, it's not modelled as such since there'd be no functional change, but would require the creation of more custom codecs.
  @ConfiguredJsonCodec case class Submission(id: String, timestamp: LocalDateTime)

  // Only includes the fields required by this application.
  @ConfiguredJsonCodec case class Submissions(
    submissions: List[Submission],
    pages: Int
  )

  // Generic error response returned by Formstack
  @ConfiguredJsonCodec case class ApiError(error: String)

  case class StatusCodeError(
    request: HttpRequest,
    statusCode: Int,
    body: String
  ) extends Exception(s"${request.method} $statusCode ${request.uri} $body")

  def isSubmissionDecryptionError(err: StatusCodeError): Boolean = {
    decode[ApiError](err.body).fold(_ => false, _.error == "An error occurred while decrypting the submissions")
  }

  // Extends HttpRequest.Builder to add methods used for authenticating API requests to Formstack.
  private implicit class FormstackAuthOps(builder: HttpRequest.Builder) {

    // https://developers.formstack.com/reference#api-overview
    def accessToken(accessToken: String): HttpRequest.Builder = {
      builder.header("Authorization", s"Bearer $accessToken")
    }

    // https://developers.formstack.com/reference#form-id-submission-get
    def encryptionPassword(encryptionPassword: String): HttpRequest.Builder = {
      builder.header("X-FS-ENCRYPTION-PASSWORD", encryptionPassword)
    }
  }
}
