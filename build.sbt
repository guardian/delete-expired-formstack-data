name := "delete-expired-formstack-data"

version := "0.1"

scalaVersion := "2.13.4"

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
)

assemblyJarName in assembly := s"${name.value}.jar"

riffRaffUploadArtifactBucket := Some("riffraff-artifact")
riffRaffUploadManifestBucket := Some("riffraff-builds")
riffRaffManifestProjectName := s"Content Platforms::${name.value}"
