package io.scalaland.endpointstl.akkahttp.client

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }

trait Instrumentation {
  // will be called just before the HTTP request is sent
  def beforeRequest(req: HttpRequest, path: String): Unit

  // will be called just after HTTP response is received
  // `responseAndDuration` == None means that request failed
  // failure are just IO errors, it does not assume any status codes logic. So if we received any response no matter
  // of status code that method will be called
  def afterResponse(req: HttpRequest, path: String, responseAndDurationMillis: Option[(HttpResponse, Long)]): Unit
}

object Instrumentation {

  object Noop extends Instrumentation {
    def beforeRequest(req: HttpRequest, path: String): Unit = ()
    def afterResponse(req: HttpRequest, path: String, responseAndDurationMillis: Option[(HttpResponse, Long)]): Unit =
      ()
  }

  def instance(
    before: (HttpRequest, String) => Unit
  )(
    after: (HttpRequest, String, Option[(HttpResponse, Long)]) => Unit
  ): Instrumentation = new Instrumentation {
    def beforeRequest(req: HttpRequest, path: String): Unit = before(req, path)
    def afterResponse(req: HttpRequest, path: String, responseAndDurationMillis: Option[(HttpResponse, Long)]): Unit =
      after(req, path, responseAndDurationMillis)
  }
}
