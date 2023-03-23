name := "example-project"

version := "0.1"

scalaVersion := "2.12.8"

val akkaVersion = "2.5.20"
val akkaHttpVersion = "10.1.7"
val scalaTestVersion = "3.0.5"

val dependencies = Seq(
    // akka streams
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    // akka http
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
    // testing
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
    "org.scalatest" %% "scalatest" % scalaTestVersion,

    // JWT
    "com.pauldijou" %% "jwt-spray-json" % "2.1.0",

    //database
    "com.typesafe.slick" %% "slick" % "3.3.3",
    "org.postgresql" % "postgresql" % "42.5.1",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
    "com.github.tminglei" %% "slick-pg" % "0.20.3",
    "com.github.tminglei" %% "slick-pg_play-json" % "0.20.3"

)

lazy val root = project
  .in(file("."))
  .settings(
      libraryDependencies ++= dependencies
  )
