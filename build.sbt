name := "monorail-producer"

version := "1.0"

scalaVersion := "2.11.8"

val vAkka = "2.4.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % vAkka,
  "com.typesafe.akka" %% "akka-http-experimental" % vAkka
)