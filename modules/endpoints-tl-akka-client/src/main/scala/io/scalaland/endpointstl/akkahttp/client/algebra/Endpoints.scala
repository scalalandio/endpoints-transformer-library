package io.scalaland.endpointstl.akkahttp.client.algebra

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.stream.Materializer
import cats.effect.Sync
import cats.implicits._
import cats.mtl._
import cats.mtl.implicits._
import endpoints._
import endpoints.algebra.{ Codec, Documentation }
import endpoints.akkahttp.client.EndpointsSettings
import io.scalaland.endpointstl.FutureToFromF
import io.scalaland.endpointstl.akkahttp.client._

/** Based on [endpoints.akkahttp.client.Endpoints]
  *
  * Adds support of error algebra and returns F[A] instead of Future[A].
  */
@SuppressWarnings(Array("org.wartremover.warts.Equals"))
abstract class Endpoints[F[_], E](
  val settings:        EndpointsSettings,
  val instrumentation: Instrumentation = Instrumentation.Noop
)(implicit F:          Sync[F],
  E:                   ApplicativeHandle[F, Error[E]],
  FutureToFromF:       FutureToFromF[F, E],
  errorCodec:          Codec[String, E],
  materializer:        Materializer)
    extends io.scalaland.endpointstl.algebra.Endpoints[E]
    with io.scalaland.endpointstl.algebra.JsonEntitiesFromCodec
    with Urls
    with endpoints.akkahttp.client.Methods {

  def jsonRequest[A](docs: Documentation)(implicit codec: Codec[String, A]): RequestEntity[A] =
    (a, req) => req.copy(entity = HttpEntity(ContentTypes.`application/json`, codec.encode(a)))

  def jsonResponse[A](statusCode: Int, docs: Documentation)(implicit codec: Codec[String, A]): Response[A] =
    (uri, response) =>
      FutureToFromF
        .deferFuture(response.entity.toStrict(settings.toStrictTimeout))
        .map(settings.stringContentExtractor)
        .flatMap { entity =>
          codec.decode(entity) match {
            case Right(value) => value.pure[F]
            case Left(error) =>
              errorCodec.decode(entity) match {
                case Right(value) => E.raise(ServiceError(value))
                case Left(_)      => E.raise[A](ClientError.DecodingError(uri.toString, entity, error.getMessage))
              }
          }
      }

  type RequestHeaders[A] = (A, List[HttpHeader]) => List[HttpHeader]

  implicit lazy val reqHeadersInvFunctor: endpoints.InvariantFunctor[RequestHeaders] =
    new InvariantFunctor[RequestHeaders] {
      def xmap[From, To](f:         (From, List[HttpHeader]) => List[HttpHeader],
                         map:       From => To,
                         contramap: To => From): (To, List[HttpHeader]) => List[HttpHeader] =
        (to, headers) => f(contramap(to), headers)
    }

  implicit lazy val reqHeadersSemigroupal: endpoints.Semigroupal[RequestHeaders] =
    new endpoints.Semigroupal[RequestHeaders] {
      def product[A, B](fa: (A, List[HttpHeader]) => List[HttpHeader], fb: (B, List[HttpHeader]) => List[HttpHeader])(
        implicit tupler:    Tupler[A, B]
      ): (tupler.Out, List[HttpHeader]) => List[HttpHeader] =
        (tuplerOut, headers) => {
          val (left, right) = tupler.unapply(tuplerOut)
          val leftResult    = fa(left, headers)
          val rightResult   = fb(right, headers)
          leftResult ++ rightResult
        }
    }

  lazy val emptyHeaders: RequestHeaders[Unit] = (_, req) => req

  case class InvalidHeaderDefinition(parsingResult: ParsingResult) extends RuntimeException

  def header(name: String, docs: Option[String]): (String, List[HttpHeader]) => List[HttpHeader] =
    (value, headers) => createHeader(name, value) :: headers

  def optHeader(name: String, docs: Option[String]): (Option[String], List[HttpHeader]) => List[HttpHeader] =
    (valueOpt, headers) =>
      valueOpt match {
        case Some(value) => createHeader(name, value) :: headers
        case None        => headers
    }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  protected def createHeader(name: String, value: String): HttpHeader =
    HttpHeader.parse(name, value) match {
      case ParsingResult.Ok(header, _) => header
      case x                           => throw InvalidHeaderDefinition(x)
    }

  type Request[A] = A => (Uri, F[HttpResponse])

  type RequestEntity[A] = (A, HttpRequest) => HttpRequest

  implicit lazy val reqEntityInvFunctor: endpoints.InvariantFunctor[RequestEntity] =
    new InvariantFunctor[RequestEntity] {
      def xmap[From, To](f:         (From, HttpRequest) => HttpRequest,
                         map:       From => To,
                         contramap: To => From): (To, HttpRequest) => HttpRequest =
        (to, req) => f(contramap(to), req)
    }

  lazy val emptyRequest: RequestEntity[Unit] = (_, req) => req

  def textRequest(docs: Option[String]): (String, HttpRequest) => HttpRequest =
    (body, request) => request.copy(entity = HttpEntity(body))

  def request[A, B, C, AB, Out](
    method:            Method,
    url:               Url[A],
    entity:            RequestEntity[B],
    headers:           RequestHeaders[C]
  )(implicit tuplerAB: Tupler.Aux[A, B, AB], tuplerABC: Tupler.Aux[AB, C, Out]): Request[Out] =
    (abc: Out) => {
      val (ab, c) = tuplerABC.unapply(abc)
      val (a, b)  = tuplerAB.unapply(ab)
      val uri =
        if (settings.baseUri == Uri("/")) Uri(url.encode(a))
        else Uri(s"${settings.baseUri.path}${url.encode(a)}")

      val request =
        method(entity(b, HttpRequest(uri = uri))).withHeaders(headers(c, List.empty))
      val metricPath = url match {
        case path: Path[_] => path.metricPath
        case _ => uri.toString
      }

      val result = F
        .delay(instrumentation.beforeRequest(request, metricPath))
        .flatMap { _ =>
          measure(FutureToFromF.deferFuture(settings.requestExecutor(request)))
        }
        .handleWith[Error[E]] {
          // catches wrapped errors and fills missing url for errors created by jsonCodec
          case ClientError.DecodingError(_, content, details) =>
            E.raise(ClientError.DecodingError(url = uri.toString, content, details))
          case error => E.raise(error)
        }
        .handleErrorWith { throwable: Throwable =>
          E.raise(ClientError.RequestFailed(request.uri.toString, throwable))
        }
        .flatTap {
          case (response, duration) =>
            F.delay(instrumentation.afterResponse(request, metricPath, Some((response, duration))))

        }
        .handleErrorWith { error =>
          F.delay(instrumentation.afterResponse(request, metricPath, None)).flatMap { _ =>
            F.raiseError(error)
          }
        }
        .map(_._1)

      uri -> result
    }

  type Response[A] = (Uri, HttpResponse) => F[A]

  def emptyResponse(statusCode: Int, docs: Documentation): (Uri, HttpResponse) => F[Unit] =
    (uri, x) =>
      if (x.status.intValue == statusCode) F.unit
      else E.raise[Unit](ClientError.UnexpectedStatusCode(uri.toString, x.status, List(StatusCodes.OK)))

  def textResponse(statusCode: Int, docs: Documentation): (Uri, HttpResponse) => F[String] =
    (uri, x) =>
      if (x.status.intValue == statusCode) {
        FutureToFromF.deferFuture(x.entity.toStrict(settings.toStrictTimeout)).map(settings.stringContentExtractor)
      } else {
        E.raise[String](ClientError.UnexpectedStatusCode(uri.toString, x.status, List(statusCode)))
    }

  def wheneverFound[A](inner:        (Uri, HttpResponse) => F[A],
                       notFoundDocs: Documentation): (Uri, HttpResponse) => F[Option[A]] = {
    case (_, resp) if resp.status.intValue == 404 => F.pure(None)
    case (uri, resp)                              => inner(uri, resp).map(Some(_))
  }

  //#endpoint-type
  type Endpoint[A, B] = A => F[B]
  //#endpoint-type

  def endpoint[A, B](request:     Request[A],
                     response:    Response[B],
                     summary:     Documentation,
                     description: Documentation,
                     tags:        List[String]): Endpoint[A, B] = a => {
    val (uri, respR) = request(a)
    for {
      resp <- respR
      result <- response(uri, resp)
      _ = resp.discardEntityBytes() //Fix for https://github.com/akka/akka-http/issues/1495
    } yield result
  }

  private def measure[T](result: F[T]): F[(T, Long)] = {
    def now() = System.currentTimeMillis()

    val before = now()
    result.map(res => (res, now() - before))
  }
}
