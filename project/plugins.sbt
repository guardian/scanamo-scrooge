addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "19.3.0")

addSbtPlugin("com.localytics" % "sbt-dynamodb" % "2.0.3")

addSbtPlugin("com.github.tkawachi" % "sbt-doctest" % "0.10.0")

resolvers += "jgit-repo" at "https://download.eclipse.org/jgit/maven"

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "1.4.1")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.13")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.1.2")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.8.2")
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.3.2")


