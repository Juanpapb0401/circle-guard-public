// Pipeline DEV — disparado por webhook en push a circle-guard-public
// Jenkins hace checkout automático del repo; el workspace ES la raíz del proyecto.
//
// Credenciales requeridas en Jenkins (Secret Text):
//   AZURE_CLIENT_ID, AZURE_CLIENT_SECRET, AZURE_TENANT_ID, AZURE_SUBSCRIPTION_ID
//   GITHUB_TOKEN — PAT con write access al ops repo

pipeline {
    agent any

    environment {
        ACR_NAME         = 'circleguardacr'
        ACR_LOGIN_SERVER = "${ACR_NAME}.azurecr.io"
        RESOURCE_GROUP   = 'circleguard-rg'
        AKS_CLUSTER      = 'circleguard-aks'
        NAMESPACE        = 'circleguard-dev'
        OPS_REPO_URL     = 'https://github.com/Juanpapb0401/circle-guard-ops.git'
        SERVICES         = 'auth identity promotion notification form gateway'
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew build -x test --parallel'
                sh '''
                    for SERVICE in ${SERVICES}; do
                        LIBS="services/circleguard-${SERVICE}-service/build/libs"
                        JAR=$(ls ${LIBS}/*.jar | grep -v plain | head -1)
                        cp "$JAR" "${LIBS}/app.jar"
                    done
                '''
            }
        }

        stage('Unit Tests') {
            steps {
                sh '''
                    ./gradlew \
                        :services:circleguard-auth-service:test \
                        :services:circleguard-identity-service:test \
                        :services:circleguard-form-service:test \
                        :services:circleguard-gateway-service:test \
                        :services:circleguard-notification-service:test \
                        --parallel
                '''
            }
            post {
                always {
                    junit 'services/*/build/test-results/**/*.xml'
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([
                    string(credentialsId: 'AZURE_CLIENT_ID',      variable: 'ARM_CLIENT_ID'),
                    string(credentialsId: 'AZURE_CLIENT_SECRET',   variable: 'ARM_CLIENT_SECRET'),
                    string(credentialsId: 'AZURE_TENANT_ID',       variable: 'ARM_TENANT_ID'),
                    string(credentialsId: 'AZURE_SUBSCRIPTION_ID', variable: 'ARM_SUBSCRIPTION_ID')
                ]) {
                    sh '''
                        az login --service-principal \
                            --username $ARM_CLIENT_ID \
                            --password $ARM_CLIENT_SECRET \
                            --tenant   $ARM_TENANT_ID
                        az account set --subscription $ARM_SUBSCRIPTION_ID
                        az acr login --name ${ACR_NAME}

                        for SERVICE in ${SERVICES}; do
                            docker build \
                                --platform linux/amd64 \
                                -t ${ACR_LOGIN_SERVER}/circleguard-${SERVICE}:${BUILD_NUMBER} \
                                -t ${ACR_LOGIN_SERVER}/circleguard-${SERVICE}:latest \
                                -f services/circleguard-${SERVICE}-service/Dockerfile \
                                .
                            docker push ${ACR_LOGIN_SERVER}/circleguard-${SERVICE}:${BUILD_NUMBER}
                            docker push ${ACR_LOGIN_SERVER}/circleguard-${SERVICE}:latest
                        done
                    '''
                }
            }
        }

        stage('Update Ops Repo') {
            steps {
                withCredentials([string(credentialsId: 'GITHUB_TOKEN', variable: 'GH_TOKEN')]) {
                    sh '''
                        git clone https://${GH_TOKEN}@github.com/Juanpapb0401/circle-guard-ops.git ops-clone

                        for SERVICE in ${SERVICES}; do
                            sed -i "s|${ACR_LOGIN_SERVER}/circleguard-${SERVICE}:.*|${ACR_LOGIN_SERVER}/circleguard-${SERVICE}:${BUILD_NUMBER}|g" \
                                ops-clone/k8s/services/${SERVICE}-service.yaml
                        done

                        git -C ops-clone config user.email "jenkins@circleguard.local"
                        git -C ops-clone config user.name  "Jenkins CI"
                        git -C ops-clone add k8s/services/
                        git -C ops-clone diff --cached --quiet || \
                            git -C ops-clone commit -m "ci: update dev image tags to build ${BUILD_NUMBER} [skip ci]"
                        git -C ops-clone push origin main
                    '''
                }
            }
        }

        stage('Deploy to Dev') {
            steps {
                withCredentials([
                    string(credentialsId: 'AZURE_CLIENT_ID',      variable: 'ARM_CLIENT_ID'),
                    string(credentialsId: 'AZURE_CLIENT_SECRET',   variable: 'ARM_CLIENT_SECRET'),
                    string(credentialsId: 'AZURE_TENANT_ID',       variable: 'ARM_TENANT_ID'),
                    string(credentialsId: 'AZURE_SUBSCRIPTION_ID', variable: 'ARM_SUBSCRIPTION_ID')
                ]) {
                    sh '''
                        az login --service-principal \
                            --username $ARM_CLIENT_ID \
                            --password $ARM_CLIENT_SECRET \
                            --tenant   $ARM_TENANT_ID
                        az account set --subscription $ARM_SUBSCRIPTION_ID
                        az aks get-credentials \
                            --resource-group ${RESOURCE_GROUP} \
                            --name           ${AKS_CLUSTER} \
                            --overwrite-existing

                        kubectl apply -f ops-clone/k8s/infrastructure/ -n ${NAMESPACE}
                        kubectl apply -f ops-clone/k8s/services/       -n ${NAMESPACE}
                        kubectl rollout status deployment               -n ${NAMESPACE} --timeout=300s
                    '''
                }
            }
        }
    }

    post {
        success { echo "Build ${BUILD_NUMBER} desplegado exitosamente en ${NAMESPACE}" }
        failure { echo "Pipeline DEV falló en build ${BUILD_NUMBER}" }
        always  { cleanWs() }
    }
}
