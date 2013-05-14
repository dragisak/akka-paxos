organization := "com.github.dragisak"

name := "akka-paxos"

version := "0.2.0"

scalaVersion := "2.10.1"

libraryDependencies ++= Seq(
        "com.typesafe.akka" %%  "akka-actor"       % "2.1.4",
        "com.typesafe.akka" %%  "akka-slf4j"       % "2.1.4",
        "ch.qos.logback"    %   "logback-classic"  % "1.0.12",
        "com.typesafe.akka" %%  "akka-testkit"     % "2.1.2"    % "test",
        "org.scalatest"     %%  "scalatest"        % "1.9.1"    % "test"
)

