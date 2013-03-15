import sbt._
import sbt.Keys._

object AkkaPaxosBuild extends Build {

  val akkaVersion = "2.1.1"

  lazy val akkapaxos = Project(
    id = "akka-paxos",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "AkkaPaxos",
      organization := "com.dragisak",
      version := "0.2-SNAPSHOT",
      scalaVersion := "2.10.1",
      scalacOptions ++= Seq("-feature", "-deprecation"),
      resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %%  "akka-actor"       % akkaVersion,
        "com.typesafe.akka" %%  "akka-slf4j"       % akkaVersion,
        "ch.qos.logback"    %   "logback-classic"  % "1.0.9"
      )
    )
  )
}
