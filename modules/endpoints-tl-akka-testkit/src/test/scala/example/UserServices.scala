package example

import java.util.UUID

import io.scalaland.endpointstl

// scalastyle:off
trait UserServices
    extends endpointstl.algebra.Endpoints[AppError]
    with endpoints.algebra.circe.JsonEntitiesFromCodec
    with endpointstl.algebra.JsonEntitiesFromCodec {

  object CreateUser {

    val v1 = endpoint[User, User](
      post(path / "users", jsonRequest[User](Some("Newly created User"))),
      jsonResponse[User](201, Some("Created user if success")),
      summary     = Some("Create new User"),
      description = Some("Attempts to create new User basing on payload"),
      tags        = List("user")
    )
  }

  object FetchUser {

    val v1 = endpoint[UUID, User](
      get(path / "users" / segment[UUID]("UID")),
      jsonResponse[User](Some("Found user if success")),
      summary     = Some("Fetch existing User"),
      description = Some("Attempts to fetch existing User"),
      tags        = List("user")
    )
  }
}
