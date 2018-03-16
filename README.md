# ADOP S3 Site Cartridge Pipeline

This cartridge creates an S3 Bucket and deploy a static page on it.

### Step 1
Open your github account and fork these repositories:
- https://github.com/chuymarin/doa17-s3-site-cartridge
- https://github.com/chuymarin/doa17-static-page

Note: A fork is a copy of a repository to another account

### Step 2
Open the file: doa17-s3-site-cartridge/jenkins/jobs/dsl/doa17_s3_site_cartridge.groovy
- Change the value of the variable doa17BucketName = "doa17-{your-github-user}-bucket"
- Example: doa17-chuymarin-bucket
- Add, Commit and Push your changes

### Step 3
Open the file: doa17-static-page/index.html
- Search where it says: {name} and change it with your github user name
- Add, Commit and Push your changes

### Step 4
Open ADOP url (will be provided by your instructors)
- Open Jenkins
- Go to DevOps_Academy Workspace
- Create a Project (Name: your github user)

### Step 5
Loading Cartridge
- In your created project, open load_cartrdige job
- Fill the values and execute the job
- It should create a new directory, some jobs and views

### Step 6
Execute the pipeline
- Open your created view and execute the pipeline
- If it works, check the log of the last job and copy the url
- Open a web browser and paste that url to see your new page

### Good Job!


