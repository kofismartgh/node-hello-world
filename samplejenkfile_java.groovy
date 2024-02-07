pipeline{

    agent any

    options {
        timestamps()
        timeout(time:20, unit:'MINUTES')

        //Setup for Webhook alert 
        office365ConnectorWebhooks([[
            name: 'eTranzact MSTeams', //Office 365 Connector name; manage_jenkins>system
            startNotification: true,
            notifySuccess: true,
            notifyAborted: true,
            notifyFailure: true,
            url: "${Build_Alerts_WebHook_URL}" //webhook url
        ]])
    }

    environment {
        RELEASE_TAG = "release" // Release Tag
        DEV_BUILD_TAG = "dev" //dev Tag
        REPOSITORY_NAME = "gh-cloudify-waec-service" //gh-*Project name*
    }


    stages{
     // Run Static Code Analysis
        stage('Code Quality Checks') {
            steps {
                withSonarQubeEnv(credentialsId: "${SonarQube_Credentials_ID}", installationName: "eTranzact GH Sonarqube Server") {
                    sh 'mvn clean verify -DskipTests=true sonar:sonar' 
// Executes Maven command to perform code quality checks and analysis using SonarQube.
// SonarQube_Credentials_ID env varible set in manage jenkins>system
// installationName: "eTranzact GH Sonarqube Server this is the name of sonar installation in manage jenkins>configure system
                }
        }
    }


        // Build Code Into Deployable Artifact
        stage("Build Artifact & Run Unit Test Cases"){
            steps{
                script{
                    // Executes Maven command to clean and package the Java project into an executable UBER JAR file(quarkus)
                    sh"""
                        mvn clean package -DskipTests=true -Dquarkus.package.type=uber-jar
                    """
                }
            }
        }


        // Package Deployable Artifact & Publish To ECR Storage Repository
        stage ('Package & Publish') {
            when{
                anyOf {
                    branch 'develop'
                    expression {
                        return env.BRANCH_NAME.startsWith('release/') 
                    //stage will run only when branch is develop or release
                    }
                }
            }
            steps{

                script{
                    //Logs into AWS ECR.
                    sh 'aws ecr get-login-password --region eu-west-1 | docker login --username AWS --password-stdin ${AWS_Account_URL}'
                    //to get the app version number
                    def pom = readMavenPom file: 'pom.xml'
                    def version = pom.version 

                    //Determines the appropriate tag for the Docker image based on the branch.
                    if (env.BRANCH_NAME == 'develop') { 

                        TAG = "${DEV_BUILD_TAG}-${version}-${env.BUILD_NUMBER}"
                    //Builds the Docker image.
                        echo "Building Docker Image For Release ${TAG}"
                        app = docker.build("$IMAGE:$TAG")
                    }

                    else {

                        TAG = "${RELEASE_TAG}-${version}-${env.BUILD_NUMBER}"
                    //Builds the Docker image.
                        echo "Building Docker Image For Release ${TAG}"
                        app = docker.build("$REPOSITORY_NAME:$TAG")
                    }
                    
                    //Pushes the Docker image to the AWS ECR repository.
                    docker.withRegistry("${ECR_Repository_URL}/${REPOSITORY_NAME}") {
                    app.push("$TAG")
                    }
                }
            }
        }
    }

}