organization := "com.github.dragisak"

name := "akka-paxos"

version := "0.3.0"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
        "com.typesafe.akka" %%  "akka-actor"       % "2.2.0-RC1",
        "com.typesafe.akka" %%  "akka-slf4j"       % "2.2.0-RC1"    % "test",
        "ch.qos.logback"    %   "logback-classic"  % "1.0.12"       % "test",
        "com.typesafe.akka" %%  "akka-testkit"     % "2.2.0-RC1"    % "test",
        "org.scalatest"     %%  "scalatest"        % "1.9.1"        % "test"
)

