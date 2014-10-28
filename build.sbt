import AssemblyKeys._


name := "ec2 consistency check"

version := "0.0.1"

organization := "com.github.ezhulenev"

scalaVersion := "2.10.4"

scalacOptions += "-deprecation"

scalacOptions += "-feature"

net.virtualvoid.sbt.graph.Plugin.graphSettings

// Merge strategy shared between app & test

val sharedMergeStrategy: (String => MergeStrategy) => String => MergeStrategy =
  old => {
    case x if x.startsWith("META-INF/ECLIPSEF.RSA") => MergeStrategy.last
    case x if x.startsWith("META-INF/mailcap") => MergeStrategy.last
    case x if x.endsWith("plugin.properties") => MergeStrategy.last
    case x => old(x)
  }

// Load Assembly Settings

assemblySettings

// Assembly App

mainClass in assembly := Some("com.github.ezhulenev.RunEC2ConsistencyCheck")

jarName in assembly := "ec2-consistency-check-app.jar"

mergeStrategy in assembly <<= (mergeStrategy in assembly)(sharedMergeStrategy)

// Resolvers

resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

resolvers += "Scalafi Bintray Repo" at "http://dl.bintray.com/ezhulenev/releases"

// Library Dependencies

libraryDependencies ++= Seq(
  "com.github.scopt"  %% "scopt"           % "3.2.0",
  "com.google.guava"    % "guava"          % "18.0"
)

// Test Dependencies

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest"   % "2.2.0" % "test"
)