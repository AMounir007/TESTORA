pipeline {
  agent { docker { image 'maven:3-eclipse-temurin-21' } }
  parameters {
    string(name: 'ENV', defaultValue: 'qa')
    string(name: 'GROUPS', defaultValue: 'smoke')
  }
  stages {
    stage('Test') { steps { sh "mvn -B test -Denv=${params.ENV} -Dgroups=${params.GROUPS}" } }
  }
  post { always { archiveArtifacts artifacts: 'target/testora/**', allowEmptyArchive: true } }
}
