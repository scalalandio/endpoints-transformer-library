package io.scalaland.endpointstl.akkahttp.server

// if we have to pass an error through several layers that only let you communicate through exception
// you can wrap Error with this Wrapper and then easily recover Error
private[server] final case class ErrorWrapper[+E](error: E) extends RuntimeException
