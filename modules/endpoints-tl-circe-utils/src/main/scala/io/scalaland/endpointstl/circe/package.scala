package io.scalaland.endpointstl

import endpoints.algebra.circe.CirceCodec
import endpoints.algebra.Codec
import io.circe.parser

package object circe {

  def summonCodec[A: CirceCodec]: Codec[String, A] = new Codec[String, A] {
    def encode(from: A):      String               = CirceCodec[A].encoder(from).noSpaces
    def decode(from: String): Either[Exception, A] = parser.decode[A](from)(CirceCodec[A].decoder)
  }
}
