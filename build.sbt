name := "scanamo-scrooge"
organization := "com.gu"

scalaVersion := "2.12.16"

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
	"com.twitter" %% "scrooge-core" % "22.4.0",
	"org.apache.thrift" % "libthrift" % "0.10.0",
  "org.scanamo" % "scanamo_2.12" % "1.0.0-M9",
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
  "org.scalatest" %% "scalatest" % "3.2.12" % Test,
  "org.scalatest" %% "scalatest-funspec" % "3.2.12" % Test,
  "org.scalacheck" %% "scalacheck" % "1.16.0" % Test,
  "com.gu" %% "content-atom-model" % "3.0.2" % Test
)

// Necessary because of a conflict between catz, imported by scanamo 1.0.0M9, and scanamo-scrooge
dependencyOverrides += "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.4"

Test / scroogeThriftOutputFolder := sourceManaged.value / "thrift"

doctestMarkdownEnabled := true
doctestDecodeHtmlEntities := true
doctestTestFramework := DoctestTestFramework.ScalaTest

homepage := Some(url("https://github.com/guardian/scanamo-scrooge"))
licenses := Seq("Apache V2" -> url("https://www.apache.org/licenses/LICENSE-2.0.html"))
publishMavenStyle := true
Test / publishArtifact := false

scmInfo := Some(ScmInfo(
  url("https://github.com/guardian/scanamo-scrooge"),
  "scm:git:git@github.com:guardian/scanamo-scrooge.git"
))

pomExtra := {
  <developers>
    <developer>
      <id>philwills</id>
      <name>Phil Wills</name>
      <url>https://github.com/philwills</url>
    </developer>
  </developers>
}

import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  ReleaseStep(action = Command.process("publishSigned", _)),
  setNextVersion,
  commitNextVersion,
  ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
  pushChanges
)



