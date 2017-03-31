// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def siteRepoName = "fime-static-page"
def siteRepoUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + siteRepoName
def fimeBucketName = "fime-chuymarin-bucket"

// Views
def fimeSitePipeline = buildPipelineView(projectFolderName + "/FIME_S3_Site_Pipeline")

// Jobs
def fimePullCode = freeStyleJob(projectFolderName + "/FIME_Pull_Code")
def fimeCreateBucket = freeStyleJob(projectFolderName + "/FIME_Create_Bucket")
def fimeDeploySite = freeStyleJob(projectFolderName + "/FIME_Deploy_Site")


// FIME_S3_Site_Pipeline
fimeSitePipeline.with{
    title('FIME S3 Site Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + "/FIME_Pull_Code")
    showPipelineDefinitionHeader()
    alwaysAllowManualTrigger()
    refreshFrequency(5)
}

// FIME_Pull_Code
fimePullCode.with{
  description("Clones Static Page Repo")
  parameters{
    stringParam("S3_BUCKET",fimeBucketName,"AWS S3 Bucket Name")
    stringParam("AWS_REGION",'',"AWS Region")
    stringParam("AWS_ACCES_KEY",'',"AWS Access Key")
    stringParam("AWS_SECRET_KEY",'',"AWS Secret Key")
  }
  environmentVariables {
    env('WORKSPACE_NAME', workspaceFolderName)
    env('PROJECT_NAME', projectFolderName)
  }
  wrappers {
    preBuildCleanup()
    maskPasswords()
    sshAgent("adop-jenkins-master")
  }
  label("docker")
  triggers {
   gerrit {
     events {
       refUpdated()
     }
     project("${PROJECT_NAME}/" + siteRepoName, 'master')
    }
  }
  scm{
     git{
       remote{
         url(siteRepoUrl)
         credentials("adop-jenkins-master")
       }
       branch("*/master")
     }
  }
  publishers{
    archiveArtifacts("**/*")
    downstreamParameterized{
      trigger(projectFolderName + "/FIME_Create_Bucket"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          currentBuild()
        }
      }
    }
  }
}

// FIME_Create_Bucket
fimeCreateBucket.with{
  description("Create AWS S3 Bucket")
  parameters{
    stringParam("S3_BUCKET",'',"AWS S3 Bucket Name")
    stringParam("AWS_REGION",'',"AWS Region")
    stringParam("AWS_ACCES_KEY",'',"AWS Access Key")
    stringParam("AWS_SECRET_KEY",'',"AWS Secret Key")
  }
  environmentVariables {
    env('WORKSPACE_NAME', workspaceFolderName)
    env('PROJECT_NAME', projectFolderName)
  }
  wrappers {
    preBuildCleanup()
    maskPasswords()
  }
  label("docker")
  steps {
    shell('''
set +x

echo "[LOG] Export AWS Configuration"
export AWS_ACCESS_KEY_ID=$AWS_ACCES_KEY
export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_KEY
export AWS_DEFAULT_REGION=$AWS_REGION


echo "[LOG] Create S3 Bucket $S3_BUCKET if not exists"
S3_BUCKETS=$(aws s3 ls)
if grep -q $S3_BUCKET <<<$S3_BUCKETS; then
  echo "[LOG] S3 Bucket $S3_BUCKET already exists"
else
  echo "[LOG] Creating S3 Bucket $S3_BUCKET"
  aws s3 mb s3://$S3_BUCKET
fi

set -x'''.stripMargin()
    )
  }
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/FIME_Deploy_Site"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          currentBuild()
        }
      }
    }
  }
}

// FIME_Deploy_Site
fimeDeploySite.with{
  description("Deploy Site to AWS S3")
  parameters{
    stringParam("S3_BUCKET",'',"AWS S3 Bucket Name")
    stringParam("AWS_REGION",'',"AWS Region")
    stringParam("AWS_ACCES_KEY",'',"AWS Access Key")
    stringParam("AWS_SECRET_KEY",'',"AWS Secret Key")
  }
  environmentVariables {
    env('WORKSPACE_NAME', workspaceFolderName)
    env('PROJECT_NAME', projectFolderName)
  }
  wrappers {
    preBuildCleanup()
    maskPasswords()
  }
  label("docker")
  steps {
    copyArtifacts(projectFolderName + "/FIME_Pull_Code") {
      buildSelector {
        latestSuccessful(true)
      }
    }
    shell('''
set +x

echo "[LOG] Export AWS Configuration"
export AWS_ACCESS_KEY_ID=$AWS_ACCES_KEY
export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_KEY
export AWS_DEFAULT_REGION=$AWS_REGION

echo "[LOG] Deploy Site to AWS S3 Bucket"
aws s3 cp index.html s3://$S3_BUCKET/index.html --acl public-read

echo "[LOG] Create Website from S3 Bucket"
aws s3 website s3://$S3_BUCKET/ --index-document index.html

echo "[LOG] Check your new site at:"
echo "http://$S3_BUCKET.s3-website-$AWS_REGION.amazonaws.com"

set -x'''.stripMargin()
    )
  }
}
