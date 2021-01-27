package com.gu.formstack.handlers

import com.amazonaws.services.lambda.runtime.{ Context, RequestStreamHandler }

import java.io.{ InputStream, OutputStream }

object FormstackFormDeletionHandler extends RequestStreamHandler {

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    println("executing updated lambda")
  }
}
