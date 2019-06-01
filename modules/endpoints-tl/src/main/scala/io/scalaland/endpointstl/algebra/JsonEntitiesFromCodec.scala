package io.scalaland.endpointstl.algebra

import endpoints.algebra.{ Codec, Documentation }

trait JsonEntitiesFromCodec extends endpoints.algebra.JsonEntitiesFromCodec with Responses {

  override def jsonResponse[A](docs: Documentation)(implicit codec: Codec[String, A]): Response[A] =
    jsonResponse[A](defaultStatusCode, docs)
  def jsonResponse[A](statusCode: Int, docs: Documentation)(implicit codec: Codec[String, A]): Response[A]
}
