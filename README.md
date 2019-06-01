# Endpoints Transformer Library

Opinionated endpoints extension which:

 * adds error algebra `E`
 * uses TTFI and MTL to let you pick the `F[_]` what you want to use to both implement server and get the results with client
 * provides some simple interfaces you could to to implement instrumentation
 * for Akka HTTP provides also a testkit helping you run the client against implementation
 
## This is an early draft

so please, treat it as RFC. You can check the [`example`](modules/endpoints-tl-akka-testkit/src/test/scala/example).
