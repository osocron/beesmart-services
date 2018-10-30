import com.lightbend.lagom.sbt.LagomImport.lagomScaladslApi

organization in ThisBuild := "edu.beesmart"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.12.4"

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.0.4" % Test
val base64 = "net.iharder" % "base64" % "2.3.9"
val jwt = "com.pauldijou" %% "jwt-play-json" % "0.12.1"
val accord = "com.wix" %% "accord-core" % "0.7.2"

lazy val `beesmart-services` = (project in file("."))
  .aggregate(
    `common`,
    `beesmart-auth-api`,
    `beesmart-auth-impl`)

lazy val `common` = (project in file("common"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      lagomScaladslServer,
      jwt,
      accord
    )
  )

lazy val `beesmart-auth-api` = (project in file("beesmart-auth-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi,
      accord
    )
  )
  .dependsOn(`common`)

lazy val `beesmart-auth-impl` = (project in file("beesmart-auth-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra,
      lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      base64,
      jwt
    )
  )
  .dependsOn(`common`, `beesmart-auth-api`)

def commonSettings: Seq[Setting[_]] = Seq(
)