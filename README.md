ScanamoScrooge [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.gu/scanamo-scrooge_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.gu/scanamo-scrooge_2.11) [![Build Status](https://travis-ci.org/guardian/scanamo-scrooge.svg?branch=master)](https://travis-ci.org/guardian/scanamo-scrooge)
==============

Like [Scanamo](https://github.com/guardian/scanamo) for interacting with DynamoDB and use
[Scrooge](https://github.com/twitter/scrooge) for working with [Thrift](https://thrift.apache.org/)?
Then you'll love ScanamoScrooge!

ScanamoScrooge provides automatic derivation of [DynamoFormat](https://guardian.github.io/scanamo/latest/api/#com.gu.scanamo.DynamoFormat)
type classes for Scrooge generated classes based on Thrift `struct` and `enum` types.

Installation
------------

```scala
libraryDependencies ++= Seq(
  "com.gu" %% "scanamo-scrooge" % "0.1.1"
)sbt

```

Usage
-----

To bring `DynamoFormat`s for Scrooge generated classes into scope, simply

```scala
import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
```

While, there is normally no need to explicitly reference `DynamoFormat`, the following
 is an example of how it serialises classes based on the [ChangeRecord](https://github.com/guardian/content-atom/blob/master/thrift/src/main/thrift/shared.thrift)
 thrift definition.

```scala
scala> import com.gu.scanamo._
scala> import com.gu.scanamo.scrooge.ScroogeDynamoFormat._
scala> import com.gu.contentatom.thrift._

scala> val changeRecord = ChangeRecord(1L, Some(User("email", Some("f"), None)))
scala> DynamoFormat[ChangeRecord].write(changeRecord)
res0: com.amazonaws.services.dynamodbv2.model.AttributeValue = {M: {date={N: 1,}, user={M: {email={S: email,}, firstName={S: f,}, lastName=null},}},}

scala> DynamoFormat[ChangeRecord].read(DynamoFormat[ChangeRecord].write(changeRecord))
res1: cats.data.Xor[error.DynamoReadError, ChangeRecord] = Right(ChangeRecord(1,Some(User(email,Some(f),None))))
```