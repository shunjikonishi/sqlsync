import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "sqlsync"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
    "com.google.code.gson" % "gson" % "2.2.2",
    "se.radley" %% "play-plugins-salat" % "1.2",
    "org.apache.velocity" % "velocity" % "1.7",
    "log4j" % "log4j" % "1.2.17",
//    "jp.co.flect" % "flectSalesforce" % "1.0",
    "org.apache.httpcomponents" % "httpclient" % "4.2.4"
            
//    "se.radley" % "play-plugins-salat_2.10.0-RC1" % "1.2-SNAPSHOT"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
//    resolvers += "FLECT Repository" at "http://flect.github.io/maven-repo/",
    resolvers += "Typesafe Repository 2" at "http://repo.typesafe.com/typesafe/repo/"
  )

}
