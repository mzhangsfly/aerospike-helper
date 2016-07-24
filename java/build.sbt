name := "aerospike-helper-java"
version := "1.0.5"
organization := "com.aerospike"

javacOptions in (Compile, compile) ++= Seq("-source", "1.7", "-target", "1.7", "-g:lines")

libraryDependencies ++= Seq("com.aerospike" % "aerospike-client" % "3.2.4",
	"commons-cli" % "commons-cli" % "1.3.1",
	"log4j" % "log4j" % "1.2.17",
	"joda-time" % "joda-time" % "2.9.4",
	"junit" % "junit" % "4.12"  % "test")
	
crossPaths := false
autoScalaLibrary := false