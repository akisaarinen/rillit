import sbt._
import Keys._

object build extends Build {
    val sharedSettings = Defaults.defaultSettings ++ Seq(
      organization := "fi.akisaarinen",
      version := "0.1.0",
      scalaVersion := "2.10.0-RC5",
      //scalacOptions ++= Seq("-Xlog-free-terms", "-Ymacro-debug-lite"),
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _)
    )

    lazy val core = Project(
        id = "rillit",
        base = file("core"),
        settings = sharedSettings ++ Seq(
          publishTo := Some(Resolver.file("file", new File("../rillit-gh-pages/maven")))
        )
    )

    lazy val example = Project(
        id = "example",
        base = file("example"),
        settings = sharedSettings,
        dependencies = Seq(core)
    )
}
