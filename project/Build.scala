import sbt._
import Keys._


object Build extends Build {
  import BuildSettings._
  import Dependencies._

  // configure prompt to show current project
  override lazy val settings = super.settings :+ {
    shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // Root Project
  // -------------------------------------------------------------------------------------------------------------------

  lazy val root = Project("root",file("."))
    .aggregate(core)
    .settings(basicSettings: _*)

  lazy val core = Project("nozzle-core", file("nozzle-core"))
    .settings(nozzleSettings: _*)
    .settings(libraryDependencies ++=
      compile(akkaActor, sprayCan, sprayRouting, sprayClient, akkaSlf4j, logback) ++
      test(specs2)
    )

  // -------------------------------------------------------------------------------------------------------------------
  // Example Projects
  // -------------------------------------------------------------------------------------------------------------------

  lazy val examples = Project("nozzle-examples", file("nozzle-examples"))
    .settings(nozzleSettings: _*)
    .dependsOn(core)
}
