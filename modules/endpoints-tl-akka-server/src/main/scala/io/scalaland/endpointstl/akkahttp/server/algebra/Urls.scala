package io.scalaland.endpointstl.akkahttp.server.algebra

import endpoints.algebra.Documentation
import endpoints.Tupler

/** Based on [endpoints.akkahttp.server.Urls].
  *
  * Adds support for metricPath.
  */
trait Urls extends endpoints.algebra.Urls with endpoints.akkahttp.server.Urls {

  private sealed trait MetricPath { val metricPath: String }
  object MetricPath {
    def url[A](url: Url[A], value: String): Url[A] = new Url(url.directive) with MetricPath {
      val metricPath = value
    }
    def path[A](path: Path[A], value: String): Path[A] = new Path(path.pathPrefix) with MetricPath {
      val metricPath = value
    }

    @SuppressWarnings(Array("org.wartremover.warts.ToString"))
    def apply(url: Url[_]): String = url match {
      case wm: MetricPath => wm.metricPath
      case _ => url.toString
    }
  }

  override def urlWithQueryString[A, B](path: Path[A],
                                        qs:   QueryString[B])(implicit tupler: Tupler[A, B]): Url[tupler.Out] = {
    val url = super.urlWithQueryString(path, qs)
    MetricPath.url(url, MetricPath(path))
  }

  override def segment[A](name: String, docs: Documentation)(implicit s: Segment[A]): Path[A] = {
    val path = super.segment[A](name, docs)
    MetricPath.path(path, "$" + (if (name.nonEmpty) name else "dynamic"))
  }

  override def staticPathSegment(segment: String): Path[Unit] = {
    val path = super.staticPathSegment(segment)
    MetricPath.path(path, segment)
  }

  override def chainPaths[A, B](first: Path[A], second: Path[B])(implicit tupler: Tupler[A, B]): Path[tupler.Out] = {
    val path = super.chainPaths(first, second)
    MetricPath.path(path, Seq(MetricPath(first), MetricPath(second)).filter(_.nonEmpty).mkString("/"))
  }
}
