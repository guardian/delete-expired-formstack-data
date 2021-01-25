# For each lambda in the Cloud Formation template,
# overwrite the FunctionCode object used to specify the CodeUri parameter, with the path to the local artifact instead.
# This is to enable using the sam cli to invoke the lambda locally.

# An initial attempt was made to support local invocation by having CodeUri (Type: string) as a template parameter,
# and then setting this to either the local artifact path (sam local invoke --parameter-overrides)
# or the URI of the S3 artifact (in the Cloud Formation UI) depending on the context.
# However, this didn't work since the serverless Cloud Formation transform doesn't support intrinsic functions
# (crucially for our purposes !Ref) when the CodeUri is specified as a string.

# For more information re this issue, see:
# - https://github.com/aws/serverless-application-model/issues/271; and
# - https://github.com/aws/serverless-application-model/issues/526

from cfn_tools import load_yaml, dump_yaml

lambda_names = ['FormstackFormDeletionLambda', 'FormstackSubmissionDeletionLambda']
local_artifact_path = "./target/scala-2.13/app.jar"

with open("/usr/local/bin/template.yaml", "r") as stream:
    cfn = load_yaml(stream)
    for lambda_name in lambda_names:
        cfn['Resources'][lambda_name]['Properties']['CodeUri'] = local_artifact_path
    print(dump_yaml(cfn))
