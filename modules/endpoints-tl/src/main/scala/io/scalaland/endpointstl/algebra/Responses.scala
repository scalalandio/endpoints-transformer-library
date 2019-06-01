package io.scalaland.endpointstl.algebra

import endpoints.algebra.Documentation

trait Responses extends endpoints.algebra.Responses {

  protected val defaultStatusCode = 200

  override def emptyResponse(docs: Documentation): Response[Unit] = emptyResponse(defaultStatusCode, docs)
  def emptyResponse(statusCode:    Int, docs: Documentation): Response[Unit]

  override def textResponse(docs: Documentation): Response[String] = textResponse(defaultStatusCode, docs)
  def textResponse(statusCode:    Int, docs: Documentation): Response[String]
}
