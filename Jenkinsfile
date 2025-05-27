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

                    def javaFiles = findFiles(glob: '**/Main*.java')

                    if (javaFiles.length == 0) {
                        error "No Main*.java files found!"
                    }

                    def rootJarApps = ['AppMainRoot'] // Put class names here that go to root

                    def builds = javaFiles.collect { file ->
                        return {
                            def className = file.name.replace('.java', '')
                            def jarPath = rootJarApps.contains(className)
                                ? "${className}.jar"
                                : "${OUTPUT_DIR}\\${className}.jar"

                            echo "🛠️ Compiling ${file.path}"
                            bat """
                            javac ${file.path}
                            if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
                            jar cfe ${jarPath} ${className} ${file.path.replace('.java', '.class')}
                            """
                        }
                    }

                    parallel builds
                    def end = System.currentTimeMillis()
                    echo "⏱ Build duration: ${(end - start) / 1000}s"
                }
            }
        }

        stage('Prepare Release Zip') {
            steps {
                script {
                    def timestamp = new Date().format("yyyyMMdd-HHmmss")
                    env.ZIP_FILE = "java_release_build-${timestamp}.zip"

                    bat """
                    if exist ${RELEASE_PACKAGE_DIR} rmdir /s /q ${RELEASE_PACKAGE_DIR}
                    mkdir ${RELEASE_PACKAGE_DIR}
                    copy /Y *.jar ${RELEASE_PACKAGE_DIR}\\ >NUL 2>&1
                    xcopy /E /I ${OUTPUT_DIR} ${RELEASE_PACKAGE_DIR}\\out >NUL
                    powershell -Command "Compress-Archive -Path ${RELEASE_PACKAGE_DIR}\\* -DestinationPath ${env.ZIP_FILE}"
                    """
                }
            }
        }

        stage('Publish GitHub Release') {
            steps {
                withCredentials([string(credentialsId: env.GITHUB_CREDS, variable: 'TOKEN')]) {
                    script {
                        def tag = "release-${new Date().format('yyyyMMdd-HHmmss')}"
                        def apiUrl = "https://api.github.com/repos/${env.GITHUB_REPO}/releases"

                        def createPayload = """
                        {
                            "tag_name": "${tag}",
                            "name": "${tag}",
                            "body": "Automated release from Jenkins",
                            "draft": false,
                            "prerelease": false
                        }
                        """

                        writeFile file: 'release.json', text: createPayload

                        bat """
                        curl -s -X POST -H "Authorization: token %TOKEN%" ^
                            -H "Content-Type: application/json" ^
                            --data @release.json ^
                            ${apiUrl} > release_response.json
                        """

                        def json = readJSON file: 'release_response.json'
                        def uploadUrl = json.upload_url.replace("{?name,label}", "") + "?name=${env.ZIP_FILE}"

                        echo "📤 Uploading ${env.ZIP_FILE} to GitHub release..."
                        bat """
                        curl -X POST -H "Authorization: token %TOKEN%" ^
                            -H "Content-Type: application/zip" ^
                            --data-binary @${env.ZIP_FILE} ^
                            "${uploadUrl}"
                        """
                    }
                }
            }
        }

        stage('Delete Old Releases') {
            steps {
                withCredentials([string(credentialsId: env.GITHUB_CREDS, variable: 'TOKEN')]) {
                    bat """
                    curl -s -H "Authorization: token %TOKEN%" ^
                        https://api.github.com/repos/${env.GITHUB_REPO}/releases > all.json
                    """
                    script {
                        def releases = readJSON file: 'all.json'
                        releases.sort { a, b -> b.created_at <=> a.created_at }
                        def toDelete = releases.drop(env.RELEASES_TO_KEEP.toInteger())

                        toDelete.each { rel ->
                            echo "🗑️ Deleting release: ${rel.tag_name}"
                            bat """
                            curl -s -X DELETE -H "Authorization: token %TOKEN%" ^
                                https://api.github.com/repos/${env.GITHUB_REPO}/releases/${rel.id}
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build and release succeeded!"
            // Uncomment if email is configured
            
            emailext(
                subject: "✅ SUCCESS: Build #${env.BUILD_NUMBER}",
                body: "Build succeeded and release was published: ${env.BUILD_URL}",
                to: "${EMAIL_RECIPIENTS}"
            )
            
        }
        failure {
            echo "❌ Build failed!"
            
            emailext(
                subject: "❌ FAILURE: Build #${env.BUILD_NUMBER}",
                body: "Build failed. See logs: ${env.BUILD_URL}",
                to: "${EMAIL_RECIPIENTS}"
            )
            
        }
    }
}
