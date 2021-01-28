package com.gu.formstack.config

import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredCodec
import io.circe.parser.decode

import java.io.InputStream
import scala.io.Source
import scala.util.Try

case class Config(
  formstackAccountId: String,
  formstackAccessToken: String,
  formstackEncryptionPassword: String,
  isDryRun: Boolean = true,
  logLevel: String = "INFO"
) {
  def hideSecrets: Config = copy(formstackAccessToken = "*****", formstackEncryptionPassword = "*****")
}

object Config {

  implicit val configJsonConfiguration: Configuration = Configuration.default.withDefaults

  // Using semi auto functionality as when using the @ConfiguredJsonCode macro,
  // couldn't find a way to bring the configJsonConfiguration into scope.
  implicit val configCodec: Codec[Config] = deriveConfiguredCodec

  def fromInputStream(input: InputStream): Try[Config] = {
    for {
      data <- Try(Source.fromInputStream(input).mkString)
      config <- decode[Config](data).toTry
    } yield config
  }
}
