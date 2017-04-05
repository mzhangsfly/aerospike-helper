name := "aerospike-helper-java"
version := "1.2"
organization := "com.aerospike"

javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8", "-g:lines")

libraryDependencies ++= Seq("com.aerospike" % "aerospike-client" % "3.3.3",
	"commons-cli" % "commons-cli" % "1.3.1",
	"log4j" % "log4j" % "1.2.17",
	"joda-time" % "joda-time" % "2.9.4",
	"junit" % "junit" % "4.12"  % "test")
	
crossPaths := false
autoScalaLibrary := false

publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))