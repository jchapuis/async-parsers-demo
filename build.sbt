name := "async-parsers-demo"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
  "org.scala-lang.modules" % "scala-jline" % "2.12.1",
  "com.nexthink" %% "scala-parser-combinators-completion" % "1.1.1-SNAPSHOT",
  "com.nexthink" %% "scala-parser-combinators-completion-async" % "1.1.1-SNAPSHOT",
  "org.json4s" %% "json4s-native" % "3.5.3",
  "org.http4s" % "blaze-http_2.12" % "0.13.0"
)
