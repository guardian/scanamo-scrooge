name := "scanamo-scrooge"
organization := "com.gu"

scalaVersion := "2.12.1"
crossScalaVersions := Seq("2.11.8",  scalaVersion.value)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % scalaVersion.value,
	"com.twitter" %% "scrooge-core" % "4.12.0",
	"org.apache.thrift" % "libthrift" % "0.9.2",
  "com.gu" %% "scanamo" % "0.8.2",
  "org.typelevel" %% "macro-compat" % "1.1.1",
  compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.patch),
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.4" % Test,
  "com.gu" %% "content-atom-model" % "2.4.26" % Test
)

scroogeThriftOutputFolder in Test := sourceManaged.value / "thrift"

doctestMarkdownEnabled := true
doctestDecodeHtmlEntities := true
doctestWithDependencies := false
doctestTestFramework := DoctestTestFramework.ScalaTest

homepage := Some(url("https://github.com/guardian/scanamo-scrooge"))
licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))
publishMavenStyle := true
publishArtifact in Test := false
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
