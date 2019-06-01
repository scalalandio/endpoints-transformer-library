package io.scalaland.endpointstl.akkahttp.client

import akka.http.scaladsl.model.StatusCode

sealed trait Error[+E] extends Product with Serializable

sealed trait ClientError extends Error[Nothing]
object ClientError {
  final case class RequestFailed(url:        String, error:      Throwable) extends ClientError
  final case class DecodingError(url:        String, content:    String, details: String) extends ClientError
  final case class UnexpectedStatusCode(url: String, statusCode: StatusCode, expectedStatusCodes: List[StatusCode])
      extends ClientError
}

final case class ServiceError[+E](details: E) extends Error[E]
