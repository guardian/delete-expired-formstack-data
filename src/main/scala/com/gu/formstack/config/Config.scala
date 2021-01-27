package com.gu.formstack.config

import cats.data.{ Validated, ValidatedNel }
import cats.implicits._
import io.circe.generic.JsonCodec

import scala.util.Try

// Encoder type class created so config can be easily rendered in log entries
@JsonCodec case class Config(
  formstackAccountId: String,
  formstackAccessToken: String,
  encryptionPassword: String,
  isDryRun: Boolean,
  logLevel: String
) {
  def hideSecrets: Config = copy(formstackAccessToken = "*****", encryptionPassword = "*****")
}

object Config {

  private trait EnvironmentVariableParser[A] {
    def parse(name: String): Either[String, A]
  }

  private implicit val stringParser: EnvironmentVariableParser[String] = (name: String) => {
    val value = System.getenv(name)
    Either.cond(value.nonEmpty, value, s"environment variable $name not set")
  }

  private implicit val booleanParser: EnvironmentVariableParser[Boolean] = (name: String) => {
    for {
      raw <- stringParser.parse(name)
      bool <- Either.catchNonFatal(raw.toBoolean).leftMap(_ => s"environment variable $name is not a valid boolean")
    } yield bool
  }

  private def validateEnvironmentVariable[A: EnvironmentVariableParser](name: String): ValidatedNel[String, A] = {
    val result = implicitly[EnvironmentVariableParser[A]].parse(name)
    Validated.fromEither(result).toValidatedNel
  }

  def fromEnvironmentVariables(): Try[Config] = {
    (
      validateEnvironmentVariable[String]("FORMSTACK_ACCOUNT_ID"),
      validateEnvironmentVariable[String]("FORMSTACK_ACCESS_TOKEN"),
      validateEnvironmentVariable[String]("FORMSTACK_ENCRYPTION_PASSWORD"),
      validateEnvironmentVariable[Boolean]("IS_DRY_RUN"),
      validateEnvironmentVariable[String]("LOG_LEVEL")
    ).mapN(Config.apply).leftMap(errs => new RuntimeException(errs.toList.mkString(" and "))).toEither.toTry
  }
}
