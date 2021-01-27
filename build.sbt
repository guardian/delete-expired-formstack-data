name := "delete-expired-formstack-data-lambdas"

version := "0.1"

scalaVersion := "2.13.4"

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
)

enablePlugins(AssemblyPlugin, RiffRaffArtifact)

assemblyJarName in assembly := s"app.jar"

riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Some("riffraff-artifact")
riffRaffUploadManifestBucket := Some("riffraff-builds")
riffRaffManifestProjectName := s"Content Platforms::${name.value}"
riffRaffArtifactResources ++= Seq(
  // TODO: comment on why it is necessary to duplicate the template.yaml file
  (baseDirectory.value / "template.yaml" -> "cloudformation-account-1/template.yaml"),
  (baseDirectory.value / "template.yaml" -> "cloudformation-account-2/template.yaml")
)
