name := "sqlsync"

version := "1.0-SNAPSHOT"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  ws,
  jdbc,
  anorm,
  cache,
  "com.novus" %% "salat" % "1.9.9",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41",
  "com.google.code.gson" % "gson" % "2.3.1",
  "se.radley" %% "play-plugins-salat" % "1.5.0",
  "org.apache.velocity" % "velocity" % "1.7",
  "log4j" % "log4j" % "1.2.17",
  "commons-lang" % "commons-lang" % "2.6",
  "org.apache.httpcomponents" % "httpclient" % "4.3.5"
)

scalacOptions += "-feature"