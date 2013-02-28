import sbt._
import sbt.Keys._

object AkkaPaxosBuild extends Build {

  lazy val akkapaxos = Project(
    id = "akkapaxos",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "AkkaPaxos",
      organization := "com.dragisak",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.0",
      scalacOptions ++= Seq("-feature", "-deprecation"),
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.0"
    )
  )
}
