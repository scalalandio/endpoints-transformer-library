import sbt._
import Settings._

lazy val root = project.root
  .setName("endpoints-transformer-library")
  .setDescription("Build for endpoints transformer library")
  .configureRoot
  .aggregate(core, akkaClient, akkaServer)

lazy val core = project.from("endpoints-tl")
  .setName("endpoints-tl")
  .setDescription("Endpoints algebra extended with error algebra based on TTFI")
  .setInitialImport("io.scalaland.etl.endpointstl._")
  .configureModule

lazy val akkaClient = project.from("endpoints-tl-akka-client")
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

lazy val akkaServer = project.from("endpoints-tl-akka-server")
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

addCommandAlias("fullTest", ";test;fun:test;it:test;scalastyle")
addCommandAlias("fullCoverageTest", ";coverage;test;coverageReport;coverageAggregate;scalastyle")
addCommandAlias("relock", ";unlock;reload;update;lock")
