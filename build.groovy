properties([
        parameters([
                string(defaultValue: params.GIT_USER_CREDENTIALS_ID ?: "git-interset-readonly", description: 'Git user credentials identifier', name: 'GIT_USER_CREDENTIALS_ID', trim: false),
                string(defaultValue: params.REPO_URL ?: "git@github.com:MicroFocus/opensearch-build.git", description: 'OpenSearch Build Git Repo URL', name: 'REPO_URL', trim: false),
                string(defaultValue: params.BUILD_NODE ?: 'docker', description: 'Node to perform build on', name: 'BUILD_NODE', trim: false),
                string(defaultValue: params.OPENSEARCH_PRODUCT ?: 'opensearch', description: 'Specify the product, e.g. opensearch or opensearch-dashboards', name: 'OPENSEARCH_PRODUCT', trim: false),
                string(defaultValue: params.OPENSEARCH_VERSION ?: '1.0.0', description: 'Specify the version of opensearch eg: 1.0.0 or 1.0.0-beta1', name: 'OPENSEARCH_VERSION', trim: false),
                string(defaultValue: params.OPENSEARCH_DOCKERFILE ?: 'dockerfiles/opensearch.microfocus.dockerfile', description: 'Specify the name of dockerfile for opensearch', name: 'OPENSEARCH_DOCKERFILE', trim: false),
                string(defaultValue: params.OPENSEARCH_ARCH ?: 'x64', description: 'Specify one and only one architecture, e.g. x64 or arm64', name: 'OPENSEARCH_ARCH', trim: false)
        ]),
        buildDiscarder(logRotator(numToKeepStr: '10')),
        disableConcurrentBuilds()
])

node("${params.BUILD_NODE}") {

    env.docker_repo = 'dev'

    stage('Pull Source') {
        timeout(time: 5, unit: 'MINUTES') {
            git branch: env.BRANCH_NAME, credentialsId: params.GIT_USER_CREDENTIALS_ID, url: params.REPO_URL
        }
        sh 'git clean -d -x -f'
        sh 'git log --format="%ae" | head -1 > commit-author.txt'
        script {
            currentBuild.description = env.BRANCH_NAME + ", " + readFile('commit-author.txt').trim()
        }
    }

    stage('Build Docker Image') {
        timeout(time: 30, unit: 'MINUTES') {
            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                sh '''
                    pushd docker/release
                    sh ./build-image-single-arch.sh -v ${OPENSEARCH_VERSION} -p ${OPENSEARCH_PRODUCT} -f ${OPENSEARCH_DOCKERFILE} -a ${OPENSEARCH_ARCH}
                    popd
                '''
            }
            if(currentBuild.currentResult == "FAILURE") {
                sh 'exit 1'
            }
        }
    }

    stage('Publish Docker Image') {
        timeout(time: 20, unit: 'MINUTES') {
            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                 withCredentials([usernamePassword(credentialsId: 'Microfocus-Artifactory', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh '''
                        export DOCKER_REGISTRY=arcsight-docker.svsartifactory.swinfra.net/${docker_repo}
                        docker login -u ${USERNAME} -p ${PASSWORD} $DOCKER_REGISTRY
                        sh ./docker/release/build-docker.sh -t push -v ${OPENSEARCH_VERSION} -p ${OPENSEARCH_PRODUCT}
                    '''
                }
            }
        }
    }

    stage('Cleanup Docker Image') {
        timeout(time: 10, unit: 'MINUTES') {
            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {
                sh '''
                    export DOCKER_REGISTRY=arcsight-docker.svsartifactory.swinfra.net/"${docker_repo}"
                    sh ./docker/release/build-docker.sh -t cleanup -v ${OPENSEARCH_VERSION} -p ${OPENSEARCH_PRODUCT}
                '''
            }
        }
    }

    stage('Cleanup Workspace') {
        cleanWs()
    }
}
