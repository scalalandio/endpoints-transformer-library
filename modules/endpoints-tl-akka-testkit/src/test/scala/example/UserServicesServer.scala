package example

import java.util.UUID

import akka.http.scaladsl.server.Directives._
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.mtl.ApplicativeHandle
import io.scalaland.endpointstl
import io.scalaland.endpointstl.akkahttp.server.{ Instrumentation, ServerConfig }
import io.scalaland.endpointstl.FutureToFromF
import io.scalaland.endpointstl.circe._

import scala.collection.mutable

class UserServicesServer[F[_]](
  serverConfig:    ServerConfig[AppError],
  instrumentation: Instrumentation = Instrumentation.Noop
)(implicit
  F:             Sync[F],
  E:             ApplicativeHandle[F, AppError],
  FutureToFromF: FutureToFromF[F, AppError])
    extends endpointstl.akkahttp.server.algebra.Endpoints[F, AppError](serverConfig,
                                                                       summonCodec[AppError],
                                                                       instrumentation)
    with UserServices {

  val users = mutable.Map.empty[UUID, User]

  val routes = CreateUser.v1.implementedBy { user =>
    (if (user.name.isEmpty) List("name cannot be empty") else Nil) ++
      (if (user.surname.isEmpty) List("surname cannot be empty") else Nil) match {
      case head :: tail =>
        E.raise(AppError.InvalidUser(user, NonEmptyList(head, tail)))
      case Nil =>
        F.delay {
          users += (user.uid -> user)
          user
        }
    }
  } ~ FetchUser.v1.implementedBy { uid =>
    F.defer {
      users.get(uid) match {
        case Some(user) => F.pure(user)
        case None       => E.raise(AppError.UserNotFound(Some(uid)))
      }
    }
  }
}
