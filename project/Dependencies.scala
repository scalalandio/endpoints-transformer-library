import sbt._

import Dependencies._

object Dependencies {

  // scala version
  val scalaOrganization = "org.scala-lang"
  val scalaVersion      = "2.12.8"

  // build tools version
  val scalaFmtVersion = "1.5.1"

  // libraries versions
  val catsVersion       = "1.6.0"
  val catsEffectVersion = "1.3.0"
  val catsMtlVersion    = "0.4.0"
  val endpointsVersion  = "0.9.0"
  val specs2Version     = "4.5.1"

  // resolvers
  val resolvers = Seq(
    Resolver sonatypeRepo "public",
    Resolver typesafeRepo "releases"
  )

  // functional libraries
  val cats                     = "org.typelevel" %% "cats-core"                   % catsVersion
  val catsEffect               = "org.typelevel" %% "cats-effect"                 % catsEffectVersion
  val catsMtl                  = "org.typelevel" %% "cats-mtl-core"               % catsMtlVersion
  // endpoints
  val endpoints                = "org.julienrf"  %% "endpoints-algebra"           % endpointsVersion
  val endpointsCirce           = "org.julienrf"  %% "endpoints-algebra-circe"     % endpointsVersion
  val endpointsJsonSchemaCirce = "org.julienrf"  %% "endpoints-json-schema-circe" % endpointsVersion
  val endpointsAkkaHttpClient  = "org.julienrf"  %% "endpoints-akka-http-client"  % endpointsVersion
  val endpointsAkkaHttpServer  = "org.julienrf"  %% "endpoints-akka-http-server"  % endpointsVersion
  // testing
  val spec2Core                = "org.specs2"    %% "specs2-core"                 % specs2Version
  val spec2Mock                = "org.specs2"    %% "specs2-mock"                 % specs2Version
  val spec2Scalacheck          = "org.specs2"    %% "specs2-scalacheck"           % specs2Version
}

trait Dependencies {

  val scalaOrganizationUsed = scalaOrganization
  val scalaVersionUsed = scalaVersion

  val scalaFmtVersionUsed = scalaFmtVersion

  // resolvers
  val commonResolvers = resolvers

  val mainDeps = Seq(cats, catsEffect, catsMtl, endpoints)

  val testDeps = Seq(spec2Core, spec2Mock, spec2Scalacheck)

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
