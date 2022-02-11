pipeline {
 agent none

 environment{
   SERVICE_NAME = "mfmarketdata"
   ORGANIZATION_NAME = "myfinance"
   DOCKERHUB_USER = "holgerfischer"

   //Snapshot Version
   VERSION = "0.21.0-alpha.${BUILD_ID}"
   //Release Version
   //VERSION = "0.19.0"

   K8N_IP = "192.168.100.73"
   REPOSITORY_TAG = "${DOCKERHUB_USER}/${ORGANIZATION_NAME}-${SERVICE_NAME}:${VERSION}"
   NEXUS_URL = "${K8N_IP}:31001"
   MVN_REPO = "http://${NEXUS_URL}/repository/maven-releases/"
   DOCKER_REPO = "${K8N_IP}:31003/repository/mydockerrepo/"
   TARGET_HELM_REPO = "http://${NEXUS_URL}/repository/myhelmrepo/"
   SONAR = "https://sonarcloud.io"
   DEV_NAMESPACE = "mfdev"
   TEST_NAMESPACE = "mftest"
   DEV_PORT = "30027"
   TEST_PORT = "30037"
 }
 
 stages{
   stage('preperation'){
    agent {
        docker {
            image 'maven:3.8.4-eclipse-temurin-17-alpine'
        }
    }      
     steps {
       cleanWs()
       git credentialsId: 'github', url: "https://github.com/myfinance/mfbackend.git"
     }
   }
   stage('build'){
    agent {
        docker {
            image 'maven:3.8.4-eclipse-temurin-17-alpine'
        }
    }      
     steps {
       sh '''mvn versions:set -DnewVersion=${VERSION} --settings ./settings.xml'''
       sh '''mvn clean deploy -DtargetRepository=${MVN_REPO} -Pjacoco -Dsonar.jacoco.reportPaths=./jacoco-ut.exec -Dsonar.jacoco.itReportPath=./jacoco-it.exec org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.projectKey=mfinstruments -Dsonar.organization=myfinance -Dsonar.host.url=${SONAR} -Dsonar.login=d48202ac4b86df8e851d5429983b0205916352ca --settings ./settings.xml'''
     }
   } 

   stage('build and push Images'){
    agent {
        docker {
            image 'maven:3.8.4-eclipse-temurin-17-alpine' 
        }
    }        
     steps {
        sh '''mvn compile jib:build -Djib.image-tag=${DOCKER_REPO}${REPOSITORY_TAG} --settings ./settings.xml'''
     }
   }

   stage('deploy to cluster'){
     agent any
     steps {
       //sh 'kubectl delete job.batch/mfupgrade'
       //sh 'envsubst < deploy.yaml | kubectl apply -f -'
       sh 'envsubst < ./helm/Chart_template.yaml > ./helm/Chart.yaml'
       sh 'helm install ${SERVICE_NAME} ./helm/  -n ${DEV_NAMESPACE} --set stage=dev --set ${SERVICE_NAME}.mf_http_port_ext=${DEV_PORT} --set repository=${DOCKER_REPO}${DOCKERHUB_USER}/${ORGANIZATION_NAME}-'
       //sh 'helm upgrade -i --cleanup-on-fail ${SERVICE_NAME} ./helm/  -n ${TEST_NAMESPACE} --set stage=test --set ${SERVICE_NAME}.mf_http_port_ext=${TEST_PORT} --set repository=${DOCKER_REPO}${DOCKERHUB_USER}/${ORGANIZATION_NAME}-'
       sh 'helm package helm -u -d helmcharts/'
       sh 'curl ${TARGET_HELM_REPO} --upload-file helmcharts/${SERVICE_NAME}-${VERSION}.tgz -v'
     }
   }

      stage('test'){
        agent any
        steps {
          sh 'HOST=${K8N_IP} PORT=${DEV_PORT} ./deploymenttest.sh'
        }
      }

 }
}
