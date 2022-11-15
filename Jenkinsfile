
pipeline {
  agent any
  tools {
    maven 'MAVEN'
    jdk 'Java11'
  }

  triggers {
    pullRequestReview(reviewStates: ['pending', 'changes_requested'])
  }

  environment {
    TMP_DIR="$JENKINS_HOME/tmp/"
    MAVEN_ARGS="-T 8 -Djava.io.tmpdir=$TMP_DIR -Dfile.encoding=UTF-8 -Djava.awt.headless=true -Dquandl.api.key=JhaRv9Z_oddLQj3PRMVf"
  }

  stages {
    stage ('Build') {
      steps {
        script {
          if (env.CHANGE_ID) {
            pullRequest.comment("**Build STARTED**\n${env.BUILD_URL}")
          }
        }

        sh 'mvn -s $JENKINS_HOME/settings.xml -versions'
        sh 'mvn -s $JENKINS_HOME/settings.xml $MAVEN_ARGS clean dependency-check:aggregate deploy'
      }
    }
  }

  post {
      always {
        testNG()
        script {
          if (env.CHANGE_ID) {
              pullRequest.comment("**Build ${currentBuild.currentResult}**\n${env.BUILD_URL}")
          }
        }
      }
      changed {
        emailext to: "michele@d3xsystems.com zav@d3xsystems.com scott@d3xsystems.com",
            subject: "D3X-CORE Build ${currentBuild.currentResult}",
               body: "Build <b>${currentBuild.currentResult}</b>:<br><br>More Info can be found here: <a href='${env.BUILD_URL}'>${env.JOB_NAME}</a><br>"
      }
  }
}