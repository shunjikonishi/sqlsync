name := "sqlsync"

version := "1.0-SNAPSHOT"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "com.novus" %% "salat" % "1.9.9",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",
  "com.google.code.gson" % "gson" % "2.2.2",
  "se.radley" %% "play-plugins-salat" % "1.2",
  "org.apache.velocity" % "velocity" % "1.7",
  "log4j" % "log4j" % "1.2.17",
  "commons-lang" % "commons-lang" % "2.6",
  "org.apache.httpcomponents" % "httpclient" % "4.2.4"
)

play.Project.playScalaSettings
