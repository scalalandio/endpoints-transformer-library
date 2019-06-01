package io.scalaland.endpointstl.akkahttp.client.algebra

import endpoints.Tupler
import endpoints.algebra.Documentation

/** Adds support for metricPath.
  */
trait Urls extends endpoints.algebra.Urls with endpoints.akkahttp.client.Urls {

  trait Path[A] extends super.Path[A] {
    def metricPath: String
  }

  override def staticPathSegment(segment: String): Path[Unit] = new Path[Unit] {
    def encode(a: Unit): String = segment
    def metricPath: String = segment
  }

  override def segment[A](name: String, docs: Documentation)(implicit s: Segment[A]): Path[A] =
    new Path[A] {
      def encode(a: A): String = s.encode(a)
      def metricPath: String = "$" + (if (name.nonEmpty) name else "dynamic")
    }

  override def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    new Path[tupler.Out] {
      def encode(ab: tupler.Out): String = {
        val (a, b) = tupler.unapply(ab)
        first.encode(a) + "/" + second.encode(b)
      }
      def metricPath: String =
        first.metricPath + "/" + second.metricPath
    }

  override def urlWithQueryString[A, B](path: Path[A],
                                        qs:   QueryString[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] =
    new Path[tupler.Out] {
      def encode(ab: tupler.Out): String = {
        val (a, b) = tupler.unapply(ab)

        qs.encodeQueryString(b) match {
          case Some(q) => s"${path.encode(a)}?$q"
          case None    => path.encode(a)
        }
      }
      def metricPath: String = path.metricPath
    }
}
