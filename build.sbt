organization := "org.lucky7"

name := "feelinglucky"

version := "1.0-SNAPSHOT"

libraryDependencies += "org.scalaj" %% "scalaj-http" % "0.3.16"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.2"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

scalaVersion := "2.11.2"