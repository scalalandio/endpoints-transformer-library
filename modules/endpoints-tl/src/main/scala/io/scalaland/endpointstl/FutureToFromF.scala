package io.scalaland.endpointstl

import scala.concurrent.{ ExecutionContext, Future }

trait FutureToFromF[F[_], E] {

  def deferFuture[A](future:       => Future[A]):                  F[A]
  def deferFutureAction[A](action: ExecutionContext => Future[A]): F[A]
  def unsafeToFuture[A](fa:        F[A]):                          Future[Either[E, A]]
}

object FutureToFromF {

  def apply[F[_], E](implicit futureF: FutureToFromF[F, E]): FutureToFromF[F, E] = futureF
}
