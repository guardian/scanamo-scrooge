// Scrooge relies on an ancient version of thrift that's not on maven central
// Instead, force a slightly more recent version
libraryDependencies += "org.apache.thrift" % "libthrift" % "0.6.1"

addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "4.5.0")

addSbtPlugin("com.localytics" % "sbt-dynamodb" % "1.4.0")

addSbtPlugin("com.github.tkawachi" % "sbt-doctest" % "0.4.0")

resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.2")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.5.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.1.0")
