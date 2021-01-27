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
  // The riff-raff.yaml file specifies a "cloud-formation" deployment action for each Formstack account
  // (see FAQs in README as to why there is a Cloud Formation stack per Formstack account).
  // The URI of the stack template used by each deployment action is in part determined by its deployment name.
  // Since these have to be unique per deployment action (being keys in a YAML object)
  // this necessitates duplicating the template across URIs.
  (baseDirectory.value / "template.yaml" -> "cloudformation-account-1/template.yaml"),
  (baseDirectory.value / "template.yaml" -> "cloudformation-account-2/template.yaml")
)
