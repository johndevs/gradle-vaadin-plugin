pipeline {
  agent {
    label 'linux'
  }

  parameters {
     string(defaultValue: '1.0-SNAPSHOT-${BUILD_NUMBER}', description: 'Build version', name: 'buildVersion')
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
