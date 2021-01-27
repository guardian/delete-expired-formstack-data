package com.gu.formstack.handlers

import com.amazonaws.services.lambda.runtime.{ Context, RequestStreamHandler }

import java.io.{ InputStream, OutputStream }
import scala.io.Source

object FormstackSubmissionDeletionHandler extends RequestStreamHandler {

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    val input = Source.fromInputStream(input).mkString
    println(s"executing updated lambda: $input")
  }
}
