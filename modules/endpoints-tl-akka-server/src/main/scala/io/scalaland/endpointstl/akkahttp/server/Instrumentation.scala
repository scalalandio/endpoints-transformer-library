package io.scalaland.endpointstl.akkahttp.server

import akka.http.scaladsl.model.{ HttpRequest, StatusCode, StatusCodes }
import akka.http.scaladsl.server.{ Directive, Directives, ExceptionHandler, PathMatcher }
import akka.http.scaladsl.server.Directives.{ extractRequestContext, handleExceptions, mapResponse }

import scala.util.control.NonFatal

trait Instrumentation {
  final def path[L](pm: TracedPathMatcher[L]): Directive[L] =
    pathMatcher(Directives.path[L])(pm)

  final def pathPrefix[L](pm: TracedPathMatcher[L]): Directive[L] =
    pathMatcher(Directives.pathPrefix[L])(pm)

  private def pathMatcher[L](nativeDirective: PathMatcher[L] => Directive[L])(pm: TracedPathMatcher[L]): Directive[L] =
    measureAs(nativeDirective(pm.underlying), pm.name)

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  final def measureAs[L](nativeDirective: Directive[L], measurePath: String): Directive[L] = {
    implicit val v = nativeDirective.ev
    extractRequestContext.flatMap { ctx =>
      val instrumentationCtx = createInstrumentationContext
      mapResponse { response =>
        updateMetrics(ctx.request, measurePath, response.status, instrumentationCtx)
        response
      } & nativeDirective & handleExceptions {
        ExceptionHandler {
          case NonFatal(e) =>
            updateMetrics(ctx.request, measurePath, StatusCodes.InternalServerError, instrumentationCtx)
            // Propagate exception to the other handlers in the chain
            throw e
        }
      }
    }
  }

  type InstrumentationContext

  protected def createInstrumentationContext: InstrumentationContext

  protected def updateMetrics(req:        HttpRequest,
                              path:       String,
                              statusCode: StatusCode,
                              timer:      InstrumentationContext): Unit
}

object Instrumentation {

  object Noop extends Instrumentation {
    type InstrumentationContext = Unit
    protected def createInstrumentationContext: Unit = ()
    protected def updateMetrics(req: HttpRequest, path: String, statusCode: StatusCode, ictx: Unit): Unit = ()
  }

  // scalastyle:off structural.type
  def instance[IC](create: => IC)(update: (HttpRequest, String, StatusCode, IC) => Unit): Instrumentation {
    type InstrumentationContext = IC
  } = new Instrumentation {
    type InstrumentationContext = IC

    protected def createInstrumentationContext: IC = create

    protected def updateMetrics(req: HttpRequest, path: String, statusCode: StatusCode, ictx: IC): Unit =
      update(req, path, statusCode, ictx)
  }
  // scalastyle:on structural.type
}
