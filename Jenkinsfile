pipeline {
    agent any

    triggers {
        pollSCM('H/10 * * * *') // Check for code changes every 10 mins
    }

    environment {
        BUILD_DIR = 'out'
        ZIP_NAME = 'java_release_build.zip'
        RELEASE_TAG = "release-${env.BUILD_NUMBER}"
        RELEASE_NAME = "Build #${env.BUILD_NUMBER}"
        GITHUB_REPO = 'SchaleSensei-Repo/Java-Vibe-Coding-BunchAThings'
        GITHUB_CREDS = 'GITHUB_PAT' // Set this in Jenkins credentials
        EMAIL_RECIPIENTS = 'ashlovedawn@gmail.com'
    }

    stages {
        stage('Clean') {
            steps {
                script {
                    def start = System.currentTimeMillis()
                    deleteDir()
                    echo "Cleaned workspace"
                    def end = System.currentTimeMillis()
                    echo "🧹 Clean took ${(end - start) / 1000}s"
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    def start = System.currentTimeMillis()
                    checkout scm
                    def end = System.currentTimeMillis()
                    echo "📥 Checkout took ${(end - start) / 1000}s"
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    def start = System.currentTimeMillis()

                    // Create build directory
                    sh "mkdir -p ${env.BUILD_DIR}"

                    // Loop through subfolders and compile each Main*.java
                    def apps = findFiles(glob: '**/Main*.java')
                    for (file in apps) {
                        def appName = file.name.replace('.java', '')
                        def appDir = file.path.replaceAll('/[^/]+$', '')
                        def outputJar = appName == 'MainMainApp' ? "${appName}.jar" : "${env.BUILD_DIR}/${appName}.jar"

                        echo "📦 Building ${appName}..."
                        sh """
                            javac ${file.path}
                            jar cfe ${outputJar} ${appName} ${file.path.replace('.java', '.class')}
                        """
                    }

                    def end = System.currentTimeMillis()
                    echo "⚙️ Build step took ${(end - start) / 1000}s"
                }
            }
        }

        stage('Zip Output') {
            steps {
                script {
                    def start = System.currentTimeMillis()

                    sh """
                        mkdir -p zip_out/out
                        mv ${env.BUILD_DIR}/*.jar zip_out/out/
                        mv *.jar zip_out/ || true
                        cd zip_out && zip -r ../${env.ZIP_NAME} .
                    """

                    def end = System.currentTimeMillis()
                    echo "🗜️ Zipping took ${(end - start) / 1000}s"
                }
            }
        }

        stage('Publish GitHub Release') {
            steps {
                script {
                    def start = System.currentTimeMillis()

                    withCredentials([string(credentialsId: env.GITHUB_CREDS, variable: 'TOKEN')]) {
                        sh """
                            curl -s -X POST -H "Authorization: token \$TOKEN" \
                                -d '{ "tag_name": "${env.RELEASE_TAG}", "name": "${env.RELEASE_NAME}", "body": "Automated release", "draft": false, "prerelease": false }' \
                                https://api.github.com/repos/${env.GITHUB_REPO}/releases > response.json

                            upload_url=\$(jq -r '.upload_url' response.json | sed "s/{?name,label}//")
                            curl -s -X POST -H "Authorization: token \$TOKEN" -H "Content-Type: application/zip" \
                                --data-binary @${env.ZIP_NAME} "\$upload_url?name=${env.ZIP_NAME}"
                        """
                    }

                    def end = System.currentTimeMillis()
                    echo "🚀 GitHub release took ${(end - start) / 1000}s"
                }
            }
        }

        stage('Cleanup Old Releases') {
            steps {
                script {
                    def start = System.currentTimeMillis()

                    withCredentials([string(credentialsId: env.GITHUB_CREDS, variable: 'TOKEN')]) {
                        sh """
                            curl -s -H "Authorization: token \$TOKEN" https://api.github.com/repos/${env.GITHUB_REPO}/releases > all.json
                            echo "🔍 Checking for old releases to delete..."

                            ids=\$(jq -r '.[10:][] | .id' all.json)
                            for id in \$ids; do
                                echo "🗑️ Deleting release ID: \$id"
                                curl -s -X DELETE -H "Authorization: token \$TOKEN" https://api.github.com/repos/${env.GITHUB_REPO}/releases/\$id
                            done
                        """
                    }

                    def end = System.currentTimeMillis()
                    echo "🧹 Cleanup took ${(end - start) / 1000}s"
                }
            }
        }
    }

    post {
        success {
            mail to: "${env.EMAIL_RECIPIENTS}",
                subject: "✅ Jenkins Build #${env.BUILD_NUMBER} Succeeded",
                body: "The build completed successfully. View it at ${env.BUILD_URL}"
        }
        failure {
            mail to: "${env.EMAIL_RECIPIENTS}",
                subject: "❌ Jenkins Build #${env.BUILD_NUMBER} Failed",
                body: "The build failed. Check details at ${env.BUILD_URL}"
        }
    }
}
