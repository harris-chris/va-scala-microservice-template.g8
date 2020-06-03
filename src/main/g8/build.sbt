import Dependencies._
import com.typesafe.sbt.packager.docker._

ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.visualalpha"
ThisBuild / organizationName := "Visual Alpha"

lazy val root = (project in file("."))
  .settings(
    name := "$name$",
    libraryDependencies += scalaTest % Test
  )

libraryDependencies := Seq(
  "com.newmotion" %% "akka-rabbitmq" % "5.1.2",
  "com.typesafe.akka" %% "akka-actor" % "2.6.5",
)

trapExit := false

enablePlugins(DockerPlugin, JavaAppPackaging, DockerComposePlugin)

packageName in Docker := "$dockerName$"
version in Docker := "latest"
