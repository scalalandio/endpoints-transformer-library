package example

import akka.stream.Materializer
import cats.effect.Sync
import cats.mtl.ApplicativeHandle
import endpoints.akkahttp.client.EndpointsSettings
import io.scalaland.endpointstl
import io.scalaland.endpointstl.FutureToFromF
import io.scalaland.endpointstl.akkahttp.client.{ Error, Instrumentation }
import io.scalaland.endpointstl.circe._

class UserServicesClient[F[_]](
  settings:        EndpointsSettings,
  instrumentation: Instrumentation = Instrumentation.Noop
)(implicit F:      Sync[F],
  E:               ApplicativeHandle[F, Error[AppError]],
  FutureToFromF:   FutureToFromF[F, Error[AppError]],
  materializer:    Materializer)
    extends endpointstl.akkahttp.client.algebra.Endpoints[F, AppError](settings, summonCodec[AppError], instrumentation)
    with UserServices
