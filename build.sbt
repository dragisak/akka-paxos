organization := "com.github.dragisak"

name := "akka-paxos"

version := "0.6.0"

scalaVersion := "2.11.7"

val akkaVersion = "2.4.1"

libraryDependencies ++= Seq(
        "com.typesafe.akka" %%  "akka-actor"       % akkaVersion,
        "com.typesafe.akka" %%  "akka-slf4j"       % akkaVersion    % Test,
        "ch.qos.logback"    %   "logback-classic"  % "1.1.3"        % Test,
        "com.typesafe.akka" %%  "akka-testkit"     % akkaVersion    % Test,
        "org.scalatest"     %%  "scalatest"        % "2.2.6"        % Test
)

scalacOptions ++= Seq(
        "-language:postfixOps",
        "-deprecation",
        "-Xfatal-warnings",
        "-Xlint"
)

// show elapsed time
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")
