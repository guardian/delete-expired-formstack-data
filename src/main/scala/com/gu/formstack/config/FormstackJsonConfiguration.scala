package com.gu.formstack.config

import io.circe.generic.extras.Configuration

trait FormstackJsonConfiguration {

  // Used by the @ConfiguredJsonCodec macro.
  // JSON fields in Formstack API response have snake_case member names.
  implicit val snakeCaseMemberNames: Configuration = Configuration.default.withSnakeCaseMemberNames
}
