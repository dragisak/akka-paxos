organization := "com.github.dragisak"

name := "akka-paxos"

version := "0.4.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
        "com.typesafe.akka" %%  "akka-actor"       % "2.2.3",
        "com.typesafe.akka" %%  "akka-slf4j"       % "2.2.3"    % "test",
        "ch.qos.logback"    %   "logback-classic"  % "1.0.13"   % "test",
        "com.typesafe.akka" %%  "akka-testkit"     % "2.2.3"    % "test",
        "org.scalatest"     %%  "scalatest"        % "1.9.1"    % "test"
)

