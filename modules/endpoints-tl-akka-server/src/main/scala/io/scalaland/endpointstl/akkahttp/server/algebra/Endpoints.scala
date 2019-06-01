package io.scalaland.endpointstl.akkahttp.server.algebra

import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import cats._
import cats.effect.Sync
import cats.implicits._
import endpoints._
import endpoints.algebra.{ Codec, Documentation }
import io.scalaland.endpointstl.akkahttp.server.{ Instrumentation, ServerConfig }
import io.scalaland.endpointstl.FutureToFromF

import scala.language.higherKinds

/** Based on [endpoints.akkahttp.server.Endpoints].
  *
  * Adds support for error algebra result to clients and uses F[A] instead of Future[A].
  */
abstract class Endpoints[F[_], E](
  val serverConfig:    ServerConfig[E],
  val errorCodec:      Codec[String, E],
  val instrumentation: Instrumentation = Instrumentation.Noop
)(implicit
  F:             Sync[F],
  FutureToFromF: FutureToFromF[F, E])
    extends io.scalaland.endpointstl.algebra.Endpoints[E]
    with io.scalaland.endpointstl.algebra.JsonEntitiesFromCodec
    with Urls
    with akkahttp.server.Methods {

  import serverConfig._

  def jsonRequest[A](docs: Documentation)(implicit codec: Codec[String, A]): Directive1[A] =
    textRequest(docs).flatMap { from =>
      codec.decode(from) match {
        case Right(value)    => provide[A](value)
        case Left(throwable) => errorRouting(decodingError(from, throwable.getMessage)).toDirective[Tuple1[A]]
      }
    }

  def jsonResponse[A](statusCode: Int, docs: Documentation)(implicit codec: Codec[String, A]): F[A] => Route =
    result =>
      onSuccess(FutureToFromF.unsafeToFuture(result.map(codec.encode))) {
        case Right(value) =>
          complete(
            (statusCode,
             List(headers.`Content-Type`(MediaTypes.`application/json`)),
             HttpEntity(MediaTypes.`application/json`, value))
          )
        case Left(error) => errorRouting(error)
    }

  type RequestHeaders[A] = Directive1[A]

  type Request[A] = Directive1[A]

  type RequestEntity[A] = Directive1[A]

  type Response[A] = F[A] => Route

  case class Endpoint[A, B](request: Request[A], response: Response[B]) {

    def implementedBy(implementation: A => F[B]): Route = request { arguments =>
      response(implementation(arguments))
    }

    def implementedByF[G[_]](implementation: A => G[B])(implicit nt: G ~> F): Route = request { arguments =>
      response(nt(implementation(arguments)))
    }

    def returns(b: B): Route = implementedBy { _ =>
      F.pure(b)
    }
  }

  /* ************************
      REQUESTS
  ************************* */

  def emptyRequest: RequestEntity[Unit] = convToDirective1(pass)

  def textRequest(docs: Documentation): RequestEntity[String] = {
    val um: FromRequestUnmarshaller[String] = implicitly
    entity[String](um)
  }

  implicit lazy val reqEntityInvFunctor: InvariantFunctor[RequestEntity] = directive1InvFunctor

  /* ************************
      HEADERS
  ************************* */

  def emptyHeaders: RequestHeaders[Unit] = convToDirective1(pass)

  def header(name: String, docs: Documentation): RequestHeaders[String] =
    headerValueByName(name)

  def optHeader(name: String, docs: Documentation): RequestHeaders[Option[String]] =
    optionalHeaderValueByName(name)

  implicit lazy val reqHeadersInvFunctor: InvariantFunctor[RequestHeaders] = directive1InvFunctor
  implicit lazy val reqHeadersSemigroupal: endpoints.Semigroupal[RequestHeaders] =
    new endpoints.Semigroupal[RequestHeaders] {
      def product[A, B](fa: Directive1[A], fb: Directive1[B])(implicit tupler: Tupler[A, B]): Directive1[tupler.Out] =
        joinDirectives(fa, fb)
    }

  /* ************************
      RESPONSES
  ************************* */

  val errorRouting: E => StandardRoute = error => {
    val (_, statusCode) = errorCodes(error)
    // TODO: add loggers for first element
    complete(statusCode -> errorCodec.encode(error))
  }

  def emptyResponse(statusCode: Int, docs: Documentation): Response[Unit] =
    result =>
      onSuccess(FutureToFromF.unsafeToFuture(result)) { _ =>
        complete(statusCode -> HttpEntity.Empty)
    }

  def textResponse(statusCode: Int, docs: Documentation): Response[String] =
    result =>
      onSuccess(FutureToFromF.unsafeToFuture(result)) {
        case Right(value) => complete(statusCode -> value)
        case Left(error)  => errorRouting(error)
    }

  def wheneverFound[A](response: Response[A], notFoundDocs: Documentation): Response[Option[A]] =
    result =>
      onSuccess(FutureToFromF.unsafeToFuture(result)) {
        case Right(Some(a)) => response(F.pure(a))
        case Right(None)    => errorRouting(notFoundError(notFoundDocs.getOrElse("Entity not found")))
        case Left(error)    => errorRouting(error)
    }

  def request[A, B, C, AB, Out](
    method:            Method,
    url:               Url[A],
    entity:            RequestEntity[B] = emptyRequest,
    headers:           RequestHeaders[C] = emptyHeaders
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] = {

    val methodDirective = convToDirective1(Directives.method(method))
    // we use Directives.pathPrefix to construct url directives, so now we close it
    val urlDirective =
      joinDirectives(url.directive, convToDirective1(pathEndOrSingleSlash))
    val directiveWithEntityAndHeaders =
      joinDirectives(joinDirectives(joinDirectives(methodDirective, urlDirective), entity), headers)

    instrumentation.measureAs(directiveWithEntityAndHeaders, MetricPath(url))
  }

  def endpoint[A, B](
    request:     Request[A],
    response:    Response[B],
    summary:     Documentation = None,
    description: Documentation = None,
    tags:        List[String] = Nil
  ): Endpoint[A, B] = Endpoint(request, response)

  lazy val directive1InvFunctor: InvariantFunctor[Directive1] = new InvariantFunctor[Directive1] {
    def xmap[From, To](f: Directive1[From], map: From => To, contramap: To => From): Directive1[To] = f.map(map)
  }
}
