// Folders
def workspaceFolderName = "${WORKSPACE_NAME}"
def projectFolderName = "${PROJECT_NAME}"

// Variables
def siteRepoName = "doa17-static-page"
def siteRepoUrl = "ssh://jenkins@gerrit:29418/${PROJECT_NAME}/" + siteRepoName
def doa17BucketName = "doa17-name-bucket"

// Views
def doa17SitePipeline = buildPipelineView(projectFolderName + "/DOA17_S3_Site_Pipeline")

// Jobs
def doa17PullCode = freeStyleJob(projectFolderName + "/DOA17_Pull_Code")
def doa17CreateBucket = freeStyleJob(projectFolderName + "/DOA17_Create_Bucket")
def doa17DeploySite = freeStyleJob(projectFolderName + "/DOA17_Deploy_Site")


// DOA17_S3_Site_Pipeline
doa17SitePipeline.with{
    title('DOA17 S3 Site Pipeline')
    displayedBuilds(5)
    selectedJob(projectFolderName + "/DOA17_Pull_Code")
    showPipelineDefinitionHeader()
    alwaysAllowManualTrigger()
    refreshFrequency(5)
}

// DOA17_Pull_Code
doa17PullCode.with{
  description("Clones Static Page Repo")
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
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/DOA17_Create_Bucket"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          currentBuild()
        }
      }
    }
  }
}

// DOA17_Create_Bucket
doa17CreateBucket.with{
  description("Create AWS S3 Bucket")
  environmentVariables {
    env('WORKSPACE_NAME', workspaceFolderName)
    env('PROJECT_NAME', projectFolderName)
  }
  wrappers {
    preBuildCleanup()
    maskPasswords()
  }
  label("docker")
  publishers{
    downstreamParameterized{
      trigger(projectFolderName + "/DOA17_Deploy_Site"){
        condition("UNSTABLE_OR_BETTER")
        parameters{
          currentBuild()
        }
      }
    }
  }
}

// DOA17_Deploy_Site
doa17DeploySite.with{
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
  }
}
