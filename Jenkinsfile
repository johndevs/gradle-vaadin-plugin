pipeline {
  agent {
    label 'linux'
  }

  parameters {
     string(name: 'buildVersion',  description: 'Build version', defaultValue: '1.0-SNAPSHOT-${BUILD_NUMBER}')
  }

  environment {
     GRADLE_PUBLISH_KEY = credentials('GRADLE_PUBLISH_KEY')
     GRADLE_PUBLISH_SECRET = credentials('GRADLE_PUBLISH_SECRET')
  }

  stages {
    stage('Build') {
      steps {
        sh "./gradlew assemble -PBUILD_VERSION=${params.buildVersion}"
      }
    }

    stage('Publish') {
      steps {
        sh "./gradlew publishPlugins -PBUILD_VERSION=${params.buildVersion} -Pgradle.publish.key=${env.GRADLE_PUBLISH_KEY} -Pgradle.publish.secret=${env.GRADLE_PUBLISH_SECRET}"
        archiveArtifacts artifacts: '**/files/build/libs/*.jar', fingerprint: true
      }
    }

    stage('Cleanup') {
      steps {
        sh "./gradlew clean"
      }
    }
  }
}
