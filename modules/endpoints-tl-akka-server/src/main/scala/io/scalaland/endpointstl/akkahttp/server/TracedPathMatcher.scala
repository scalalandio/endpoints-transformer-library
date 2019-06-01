package io.scalaland.endpointstl.akkahttp.server

import akka.http.scaladsl.server.PathMatcher
import akka.http.scaladsl.server.util.TupleOps.Join

// scalastyle:off method.name
final case class TracedPathMatcher[L](underlying: PathMatcher[L], name: String) {
  def /[R](other: TracedPathMatcher[R])(implicit join: Join[L, R]): TracedPathMatcher[join.Out] =
    TracedPathMatcher(underlying / other.underlying, s"${name}/${other.name}")

  def /[R](other: PathMatcher[R])(implicit join: Join[L, R]): TracedPathMatcher[join.Out] =
    TracedPathMatcher(underlying / other, name + "/$dynamic")
}
// scalastyle:on method.name

object TracedPathMatcher {
  implicit def fromString(path: String): TracedPathMatcher[Unit] =
    TracedPathMatcher(path, path)
}
