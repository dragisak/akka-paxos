organization := "com.github.dragisak"

name := "akka-paxos"

version := "0.5.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.4.1"

libraryDependencies ++= Seq(
        "com.typesafe.akka" %%  "akka-actor"       % akkaVersion,
        "com.typesafe.akka" %%  "akka-slf4j"       % akkaVersion    % "test",
        "ch.qos.logback"    %   "logback-classic"  % "1.1.3"        % "test",
        "com.typesafe.akka" %%  "akka-testkit"     % akkaVersion    % "test",
        "org.scalatest"     %%  "scalatest"        % "2.2.6"        % "test"
)

scalacOptions ++= Seq(
        "-language:postfixOps",
        "-deprecation",
        "-Xfatal-warnings",
        "-Xlint"
)

// show elapsed time
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
