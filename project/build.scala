import sbt._
import Keys._

object build extends Build {
    val sharedSettings = Defaults.defaultSettings ++ Seq(
      organization := "fi.akisaarinen",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.10.0-RC5",
      //scalacOptions ++= Seq("-Xlog-free-terms", "-Ymacro-debug-lite"),
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)
    )

    lazy val core = Project(
        id = "rillit",
        base = file("core"),
        settings = sharedSettings
    )

    lazy val testing = Project(
        id = "rillit-testing",
        base = file("testing"),
        settings = sharedSettings,
        dependencies = Seq(core)
    )
}
