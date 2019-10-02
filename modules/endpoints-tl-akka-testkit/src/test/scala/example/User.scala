package example

import java.util.UUID

import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto._

final case class User(uid: UUID, name: String, surname: String)
object User {
  implicit val decoder: Decoder[User]          = deriveDecoder[User]
  implicit val encoder: Encoder.AsObject[User] = deriveEncoder[User]
}
