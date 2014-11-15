name := "echo123"

version := "1.0"

libraryDependencies += "org.scalaj" %% "scalaj-http" % "0.3.16"

resolvers += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.2"

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

resolvers += "ppurang bintray" at " http://dl.bintray.com/ppurang/maven"

libraryDependencies += "org.purang.net" %% "asynch" %"0.4.5" withSources()

scalaVersion := "2.11.2"