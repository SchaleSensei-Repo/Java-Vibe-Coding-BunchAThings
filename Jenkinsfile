pipeline {
    agent any

    triggers {
        pollSCM('H/10 * * * *') // Poll Git every 10 mins
    }

    environment {
        OUTPUT_DIR = 'out'
        RELEASE_PACKAGE_DIR = 'release_package'
        GITHUB_REPO = 'SchaleSensei-Repo/Java-Vibe-Coding-BunchAThings'
        GITHUB_CREDS = 'GITHUB_PAT' // Create this in Jenkins credentials
        RELEASES_TO_KEEP = 3
        EMAIL_RECIPIENTS = 'ashlovedawn@gmail.com'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Apps') {
            steps {
                script {
                    def start = System.currentTimeMillis()
                    bat "if exist ${OUTPUT_DIR} rmdir /s /q ${OUTPUT_DIR}"
                    bat "mkdir ${OUTPUT_DIR}"

                    // Use PowerShell to find Java files that contain a main method
                    def psCommand = '''
                        Get-ChildItem -Recurse -Filter *.java | 
                        Where-Object { Select-String -Path $_.FullName -Pattern 'public static void main' } |
                        ForEach-Object { $_.FullName } > main_java_files.txt
                    '''
                    bat "powershell -Command \"${psCommand.trim()}\""

                    def javaFiles = readFile('main_java_files.txt').split("\\r?\\n").findAll { it.trim() }

                    if (javaFiles.size() == 0) {
                        error "No Java files with main method found!"
                    }

                    def rootJarApps = ['AppMainRoot'] // Class names to go in root directory

                    def builds = javaFiles.collect { filePath ->
                        return {
                            def fileName = filePath.tokenize('\\\\')[-1]
                            def className = fileName.replace('.java', '')

                            def jarPath = rootJarApps.contains(className)
                                ? "${className}.jar"
                                : "${OUTPUT_DIR}\\${className}.jar"

                            echo "🛠️ Compiling ${filePath} to ${jarPath}"

                            bat "javac \"${filePath}\""
                            bat "jar cfe \"${jarPath}\" ${className} ${className}.class"
                        }
                    }

                    parallel builds

                    def end = System.currentTimeMillis()
                    echo "✅ Compilation completed in ${(end - start) / 1000}s"
                }
            }
        }

        stage('Create Release Package') {
            steps {
                bat "if exist ${RELEASE_PACKAGE_DIR} rmdir /s /q ${RELEASE_PACKAGE_DIR}"
                bat "mkdir ${RELEASE_PACKAGE_DIR}"
                bat "copy ${OUTPUT_DIR}\\*.jar ${RELEASE_PACKAGE_DIR}"
            }
        }

        stage('Publish Release') {
            steps {
                script {
                    def tag = "build-${env.BUILD_NUMBER}"
                    def message = "Automated build ${env.BUILD_NUMBER}"
                    def zipFile = "${RELEASE_PACKAGE_DIR}.zip"

                    bat "powershell Compress-Archive -Path ${RELEASE_PACKAGE_DIR}\\* -DestinationPath ${zipFile}"

                    withCredentials([string(credentialsId: "${GITHUB_CREDS}", variable: 'TOKEN')]) {
                        bat """
                            curl -X POST -H "Authorization: token %TOKEN%" ^
                                 -H "Accept: application/vnd.github+json" ^
                                 https://api.github.com/repos/${GITHUB_REPO}/releases ^
                                 -d "{\\"tag_name\\": \\"${tag}\\", \\"name\\": \\"${tag}\\", \\"body\\": \\"${message}\\", \\"draft\\": false, \\"prerelease\\": false}"
                        """
                        // Note: You can also upload assets using GitHub API v3 if desired
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build succeeded."
        }
        failure {
            echo "❌ Build failed."
        }
        always {
            mail to: "${EMAIL_RECIPIENTS}",
                 subject: "Build ${currentBuild.currentResult}: Job ${env.JOB_NAME} [#${env.BUILD_NUMBER}]",
                 body: "See details at: ${env.BUILD_URL}"
        }
    }
}
