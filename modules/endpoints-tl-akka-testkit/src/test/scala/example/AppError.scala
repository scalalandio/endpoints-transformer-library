package example

import java.util.UUID

import cats.data.NonEmptyList
import io.circe.{ Decoder, ObjectEncoder }
import io.circe.generic.semiauto._

sealed trait AppError extends Product with Serializable
object AppError {
  final case class UserNotFound(uid:      Option[UUID]) extends AppError
  final case class InvalidUser(user:      User, errors: NonEmptyList[String]) extends AppError
  final case class DecodingError(details: String, exception: String) extends AppError

  implicit val decoder: Decoder[AppError]       = deriveDecoder[AppError]
  implicit val encoder: ObjectEncoder[AppError] = deriveEncoder[AppError]
}
