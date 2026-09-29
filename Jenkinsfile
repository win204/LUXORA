pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        CI = 'true'
        NEXT_TELEMETRY_DISABLED = '1'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Backend - Test & Package') {
            steps {
                dir('backend') {
                    echo 'Running Backend Tests with Maven...'
                    sh 'mvn clean test'
                    echo 'Packaging Backend JAR...'
                    sh 'mvn package -DskipTests'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Frontend - Lint & Build') {
            steps {
                dir('frontend') {
                    echo 'Installing Frontend Dependencies...'
                    sh 'npm ci'
                    echo 'Linting Frontend...'
                    sh 'npm run lint'
                    echo 'Building Frontend Next.js...'
                    sh 'npm run build'
                }
            }
        }

        stage('Docker - Build Images') {
            steps {
                echo 'Building Docker images via Docker Compose...'
                sh 'test -f .env || cp .env.example .env'
                sh 'docker compose build'
            }
        }
    }

    post {
        success {
            echo '======================================'
            echo ' LUXORA CI/CD Pipeline Succeeded!     '
            echo '======================================'
        }
        failure {
            echo '======================================'
            echo ' LUXORA CI/CD Pipeline Failed!        '
            echo '======================================'
        }
        always {
            cleanWs(deleteDirs: true, notFailBuild: true)
        }
    }
}
