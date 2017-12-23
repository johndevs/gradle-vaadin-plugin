pipeline {
  agent {
    label 'linux'
  }

  parameters {
     string(name: 'buildVersion',  description: 'Build version', defaultValue: '1.0-SNAPSHOT-${BUILD_NUMBER}')
  }

  environment {
     gradle.publish.key=credentials('GRADLE_PUBLISH_KEY')
     gradle.publish.secret=credentials('GRADLE_PUBLISH_SECRET')
  }

  stages {
    stage('Build') {
      steps {
        sh "./gradlew assemble -PBUILD_VERSION=${params.buildVersion}"
      }
    }

    stage('Publish') {
      steps {
        sh "./gradlew publishPlugins -PBUILD_VERSION=${params.buildVersion}"
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
