organization := "org.lucky7"

name := "feelinglucky"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.4"

libraryDependencies += "org.scalaj" %% "scalaj-http" % "0.3.16"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "org.purang.net" %% "asynch" %"0.4.5" withSources()

libraryDependencies += "io.argonaut" %% "argonaut" % "6.1-M4"

resolvers += "ppurang bintray" at " http://dl.bintray.com/ppurang/maven"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

fork in run := true

cancelable in GlobalScope := true