import sbt._
import Dependencies._

object Dependencies {

  // scala version
  val scalaOrganization  = "org.scala-lang"
  val scalaVersion       = "2.12.10"
  val crossScalaVersions = Seq("2.12.10")

  // build tools version
  val scalaFmtVersion = "1.5.1"

  // libraries versions
  val akkaVersion       = "2.5.25"
  val akkaHttpVersion   = "10.1.10"
  val catsVersion       = "2.0.0"
  val catsEffectVersion = "2.0.0"
  val catsMtlVersion    = "0.7.0"
  val circeVersion      = "0.12.1"
  val endpointsVersion  = "0.10.1"
  val monixVersion      = "3.0.0"
  val scalatestVersion  = "3.0.5"

  // resolvers
  val resolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver typesafeRepo "releases"
  )

  // functional libraries
  val cats                     = "org.typelevel"     %% "cats-core"                   % catsVersion
  val catsEffect               = "org.typelevel"     %% "cats-effect"                 % catsEffectVersion
  val catsMtl                  = "org.typelevel"     %% "cats-mtl-core"               % catsMtlVersion
  val monix                    = "io.monix"          %% "monix"                       % monixVersion
  // serialization
  val circeGeneric             = "io.circe"          %% "circe-generic-extras"        % circeVersion
  // endpoints
  val endpoints                = "org.julienrf"      %% "endpoints-algebra"           % endpointsVersion
  val endpointsCirce           = "org.julienrf"      %% "endpoints-algebra-circe"     % endpointsVersion
  val endpointsJsonSchemaCirce = "org.julienrf"      %% "endpoints-json-schema-circe" % endpointsVersion
  val endpointsAkkaHttpClient  = "org.julienrf"      %% "endpoints-akka-http-client"  % endpointsVersion
  val endpointsAkkaHttpServer  = "org.julienrf"      %% "endpoints-akka-http-server"  % endpointsVersion
  // testing
  val akkaStreamTestkit        = "com.typesafe.akka" %% "akka-stream-testkit"         % akkaVersion
  val akkaHttpTestkit          = "com.typesafe.akka" %% "akka-http-testkit"           % akkaHttpVersion
  val scalatest                = "org.scalatest"     %% "scalatest"                   % scalatestVersion
  // compiler plugins
  val kindProjector            = "org.typelevel"     %% "kind-projector"              % "0.10.3"
}

trait Dependencies {

  val scalaOrganizationUsed = scalaOrganization
  val scalaVersionUsed = scalaVersion
  val crossScalaVersionsUsed = crossScalaVersions

  val scalaFmtVersionUsed = scalaFmtVersion

  // resolvers
  val commonResolvers = resolvers

  val mainDeps = Seq(cats, catsEffect, catsMtl, endpoints)

  val testDeps = Seq(scalatest, akkaHttpTestkit, akkaStreamTestkit)

  implicit final class ProjectRoot(project: Project) {

    def root: Project = project in file(".")
  }

  implicit final class ProjectFrom(project: Project) {

    private val commonDir = "modules"

    def from(dir: String): Project = project in file(s"$commonDir/$dir")
  }

  implicit final class DependsOnProject(project: Project) {

    private val testConfigurations = Set("test", "fun", "it")
    private def findCompileAndTestConfigs(p: Project) =
      (p.configurations.map(_.name).toSet intersect testConfigurations) + "compile"

    private val thisProjectsConfigs = findCompileAndTestConfigs(project)
    private def generateDepsForProject(p: Project) =
      p % (thisProjectsConfigs intersect findCompileAndTestConfigs(p) map (c => s"$c->$c") mkString ";")

    def compileAndTestDependsOn(projects: Project*): Project =
      project dependsOn (projects.map(generateDepsForProject): _*)
  }
}
