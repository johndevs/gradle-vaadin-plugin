pipeline {
  agent {
    label 'linux'
  }

  parameters {
     string(name: 'buildVersion',  description: 'Build version', defaultValue: '1.0-SNAPSHOT-${BUILD_NUMBER}')
     booleanParam(name: 'publish', description: 'Should the build be published to the Plugin Portal', defaultValue: false)
     booleanParam(name: 'documentation', description: 'Should the documentation be published', defaultValue: false)
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
      when {                
        expression { params.publish }
      }
      steps {     
        sh "./gradlew publishPlugins -PBUILD_VERSION=${params.buildVersion} -Pgradle.publish.key=${env.GRADLE_PUBLISH_KEY} -Pgradle.publish.secret=${env.GRADLE_PUBLISH_SECRET}"
        archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
      }
    }
    
    stage('Build Documentation') {
      when {                
        expression { params.documentation }
      }
      steps {
        sh "./gradlew groovydoc -PBUILD_VERSION=${params.buildVersion}"
      }      
    }
    
    stage('Publish Documentation') {
      when {                
        expression { params.documentation }
      }
      sh "git checkout gh-pages"
      sh "cp -r build/docs/groovydoc/* api"
      sh "git add api"
      sh "git -c user.name='Jenkins' -c user.email='jenkins@devsoap.com' commit -m 'Update Groovydoc for ${params.buildVersion}'"
      sh "git push"
      sh "git checkout master"      
    }

    stage('Cleanup') {
      steps {
        sh "./gradlew clean"
      }
    }
  }
}
