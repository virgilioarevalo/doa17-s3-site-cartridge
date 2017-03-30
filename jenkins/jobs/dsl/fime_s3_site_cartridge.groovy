// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def siteRepoName = "fime-static-page"
def siteRepoUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + siteRepoName
def fimeBucketName = "fime-site-bucket"

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
    steps {
    shell('''
set +x
echo "[LOG] Pull Site Repository"
set -x'''.stripMargin()
    )
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
    stringParam("S3_BUCKET","","AWS S3 Bucket Name")
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
echo "[LOG] Create S3 Bucket if not exists"
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
    stringParam("S3_BUCKET","","AWS S3 Bucket Name")
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
echo "[LOG] Deploy Site to AWS S3 Bucket"
set -x'''.stripMargin()
    )
  }
}
