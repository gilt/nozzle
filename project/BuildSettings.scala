import sbt._
import Keys._
import java.text.SimpleDateFormat
import java.util.Date

object BuildSettings {
  val VERSION = "0.0.1"

  lazy val basicSettings = Seq(
    version               := VERSION + new SimpleDateFormat("-yyyyMMdd").format(new Date),
    homepage              := Some(new URL("http://dev.gilt.com")),
    organization          := "com.gilt",
    organizationHomepage  := Some(new URL("http://www.gilt.com")),
    description           := "API gateway to publish your services to developers",
    startYear             := Some(2014),
    licenses              := Seq("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scalaVersion          := "2.10.4",
    resolvers             ++= Dependencies.resolutionRepos,
    scalacOptions         := Seq(
      "-encoding", "utf8",
      "-feature",
      "-unchecked",
      "-deprecation",
      "-target:jvm-1.6",
      "-language:postfixOps",
      "-language:implicitConversions",
      "-Xlog-reflective-calls",
      "-Ywarn-adapted-args"
    )
  )

  lazy val nozzleSettings = basicSettings

}
