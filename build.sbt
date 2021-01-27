name := "delete-expired-formstack-data-lambdas"

version := "0.1"

scalaVersion := "2.13.4"

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-generic-extras" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.jlib" % "jlib-awslambda-logback" % "1.0.0",
  "org.typelevel" %% "cats-core" % "2.1.0"
)

// Mostly cherry picked from https://tpolecat.github.io/2017/04/25/scalac-flags.html
scalacOptions ++= Seq(
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Ymacro-annotations", // required by circe macro annotations
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
)

// Merge strategy to avoid deduplicate strategy being applied to module-info.class,
// which would cause an error since the content of these files are different for different dependencies.
assemblyMergeStrategy in assembly := {
  case PathList("module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

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
