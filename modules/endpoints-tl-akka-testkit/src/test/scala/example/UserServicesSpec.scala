package example

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import cats.data.{ EitherT, NonEmptyList }
import cats.mtl.implicits._
import io.scalaland.endpointstl.akkahttp.client.{ Error, ServiceError }
import io.scalaland.endpointstl.akkahttp.testkit.ScalatestEndpointsTest
import io.scalaland.endpointstl.FutureToFromF
import io.scalaland.endpointstl.akkahttp.server.ServerConfig
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ ExecutionContext, Future }

class UserServicesSpec extends WordSpec with ScalatestEndpointsTest with ScalaFutures {

  class UserFixture extends EndpointsFixture {

    val serverConfig = ServerConfig[AppError](
      errorCodes = {
        case AppError.UserNotFound(uid)               => s"User $uid not found" -> StatusCodes.NotFound
        case AppError.InvalidUser(_, errors)          => errors.toList.mkString(", ") -> StatusCodes.BadRequest
        case AppError.DecodingError(details, message) => s"$details: $message" -> StatusCodes.BadRequest
      },
      notFoundError = _ => AppError.UserNotFound(None),
      decodingError = (details, message) => AppError.DecodingError(details, message)
    )

    implicit def futureToFromF[E]: FutureToFromF[EitherT[Task, E, ?], E] = new FutureToFromF[EitherT[Task, E, ?], E] {
      type F[A] = EitherT[Task, E, A]

      def deferFuture[A](future: => Future[A]) =
        EitherT.right[E](Task.deferFuture(future))

      def deferFutureAction[A](action: ExecutionContext => Future[A]) =
        EitherT.right[E](Task.deferFutureAction(action))

      def unsafeToFuture[A](fa: F[A]) = fa.value.runToFuture
    }

    val client = new UserServicesClient[EitherT[Task, Error[AppError], ?]](defaultEndpointSettings())
    val server = new UserServicesServer[EitherT[Task, AppError, ?]](serverConfig)

    def routes: Route = server.routes
  }

  "POST /users" should {

    "fail to create invalid user" in new UserFixture {
      val user = User(UUID.randomUUID, "", "")

      val result = client.CreateUser.v1(user).value.runToFuture.futureValue

      assert(
        result === Left(
          ServiceError(AppError.InvalidUser(user, NonEmptyList.of("name cannot be empty", "surname cannot be empty")))
        )
      )
    }

    "create user if it is ok" in new UserFixture {
      val user = User(UUID.randomUUID, "John", "Smith")

      val result = client.CreateUser.v1(user).value.runToFuture.futureValue

      assert(result === Right(user))
    }
  }

  "GET /users/{uid}" should {

    "return user not found if user doesn't exists" in new UserFixture {
      val user = User(UUID.randomUUID, "John", "Smith")

      val result = client.FetchUser.v1(user.uid).value.runToFuture.futureValue

      assert(result === Left(ServiceError(AppError.UserNotFound(Some(user.uid)))))
    }

    "return existing user" in new UserFixture {
      val user = User(UUID.randomUUID, "John", "Smith")
      server.users += (user.uid -> user)

      val result = client.FetchUser.v1(user.uid).value.runToFuture.futureValue

      assert(result === Right(user))
    }
  }
}
