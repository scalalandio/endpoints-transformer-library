package io.scalaland.endpointstl.akkahttp.server

import akka.http.scaladsl.model.StatusCode

final case class ServerConfig[E](errorCodes:    E => (String, StatusCode),
                                 notFoundError: String => E,
                                 decodingError: (String, String) => E)
