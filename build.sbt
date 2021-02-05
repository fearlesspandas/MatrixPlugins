
scalaVersion := "2.12.10"
val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.4.2"