import sbt._
import Settings._

lazy val root = project.root
  .setName("endpoints-transformer-library")
  .setDescription("Build for endpoints transformer library")
  .configureRoot
  .aggregate(core, akkaClient, akkaServer, akkaTestkit)

lazy val core = project
  .from("endpoints-tl")
  .setName("endpoints-tl")
  .setDescription("Endpoints algebra extended with error algebra based on TTFI")
  .setInitialImport("io.scalaland.etl.endpointstl._")
  .configureModule

lazy val circeUtils = project
  .from("endpoints-tl-circe-utils")
  .setName("endpoints-tl-circe-utils")
  .setDescription("Utils for turning CirceCodec into Codec from the outside of algebra")
  .setInitialImport("io.scalaland.etl.endpointstl.circe._")
  .configureModule
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.endpointsCirce,
      Dependencies.endpointsJsonSchemaCirce,
    )
  )

lazy val akkaClient = project
  .from("endpoints-tl-akka-client")
  .setName("endpoints-tl-akka-http-client")
  .setDescription("Akka HTTP client based for Endpoints with error algebra and TTFI")
  .setInitialImport("io.scalaland.etl.endpointstl._", "io.scalaland.etl.endpointstl.akkahttp.client._")
  .configureModule
  .compileAndTestDependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.endpointsAkkaHttpClient
    )
  )

lazy val akkaServer = project
  .from("endpoints-tl-akka-server")
  .setName("endpoints-tl-akka-http-server")
  .setDescription("Akka HTTP server based for Endpoints with error algebra and TTFI")
  .setInitialImport("io.scalaland.etl.endpointstl._", "io.scalaland.etl.endpointstl.akkahttp.server._")
  .configureModule
  .compileAndTestDependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.endpointsAkkaHttpServer
    )
  )

lazy val akkaTestkit = project
  .from("endpoints-tl-akka-testkit")
  .setName("endpoints-tl-akka-http-testkit")
  .setDescription("Scalatest utilities for testing Endpoints Akka Http client with Endpoints Akka Http server")
  .setInitialImport("io.scalaland.etl.endpointstl._", "io.scalaland.etl.endpointstl.akkahttp.testkit._")
  .configureModule
  .configureTests()
  .compileAndTestDependsOn(core, circeUtils, akkaClient, akkaServer)
  .settings(
    libraryDependencies ++= testDeps,
    libraryDependencies ++= Seq(
      Dependencies.circeGeneric % Test,
      Dependencies.endpointsCirce % Test,
      Dependencies.endpointsJsonSchemaCirce % Test,
      Dependencies.monix % Test
    )
  )

addCommandAlias("fullTest", ";test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate;scalastyle")
