pipeline {
    agent any

    environment {
        GIT_REPO = 'https://github.com/SchaleSensei-Repo/Java-Vibe-Coding-BunchAThings.git'
        OUTPUT_DIR = 'out'
        RELEASE_PACKAGE_DIR = 'release_package'
        RELEASES_TO_KEEP = 3
        GITHUB_OWNER = 'SchaleSensei-Repo'
        GITHUB_REPO = 'Java-Vibe-Coding-BunchAThings'
        // Set notification emails (comma-separated)
        EMAIL_RECIPIENTS = 'ashlovedawn@gmail.com'
    }

    triggers {
        startup()
    }

    stages {
        stage('Check for New Commits') {
            steps {
                script {
                    echo "⏳ [Check] Started at: ${new Date()}"
                    def startTime = System.currentTimeMillis()

                    def lastBuildSHA = ''
                    if (currentBuild.rawBuild.getPreviousBuild()) {
                        lastBuildSHA = currentBuild.rawBuild.getPreviousBuild().getEnvironment(listener).get('GIT_COMMIT')
                    }

                    def latestSHA = bat(script: 'git ls-remote origin HEAD', returnStdout: true).trim().split()[0]
                    if (lastBuildSHA == latestSHA) {
                        echo "✅ No new commits. Skipping build."
                        currentBuild.result = 'SUCCESS'
                        return
                    }

                    echo "🟢 New commits found."
                    def duration = (System.currentTimeMillis() - startTime) / 1000
                    echo "⏱ [Check] Duration: ${duration}s"
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    def startTime = System.currentTimeMillis()
                    checkout scm
                    def duration = (System.currentTimeMillis() - startTime) / 1000
                    echo "⏱ [Checkout] Duration: ${duration}s"
                }
            }
        }

        stage('Find and Build All Apps') {
            steps {
                script {
                    def startTime = System.currentTimeMillis()
                    def javaFiles = findFiles(glob: '**/Main*.java')

                    if (javaFiles.length == 0) {
                        error "❌ No Main*.java files found."
                    }

                    bat "if exist ${OUTPUT_DIR} rmdir /s /q ${OUTPUT_DIR}"
                    bat "mkdir ${OUTPUT_DIR}"

                    // Define apps that output jar to root folder (put your exact class names here)
                    def rootJarApps = ['JarHubApp']

                    def builds = javaFiles.collect { file ->
                        return {
                            def className = file.name.replace('.java', '')
                            def jarPath

                            if (rootJarApps.contains(className)) {
                                jarPath = "${className}.jar" // root folder
                            } else {
                                jarPath = "${OUTPUT_DIR}\\${className}.jar" // inside /out/
                            }

                            echo "🛠️ Building ${file.path}, output jar: ${jarPath}"

                            bat """
                            javac ${file.path}
                            if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
                            jar cfe ${jarPath} ${className} ${file.path.replace('.java', '.class')}
                            """
                        }
                    }

                    parallel builds

                    def duration = (System.currentTimeMillis() - startTime) / 1000
                    echo "⏱ [Build] Duration: ${duration}s"
                }
            }
        }

        stage('Archive JARs') {
            steps {
                archiveArtifacts artifacts: "${OUTPUT_DIR}/*.jar", fingerprint: true
                archiveArtifacts artifacts: "*.jar", excludes: "${OUTPUT_DIR}/*.jar", fingerprint: true
            }
        }

        stage('Prepare and Zip Release Package') {
            steps {
                script {
                    def timestamp = new Date().format("yyyyMMdd-HHmmss")
                    env.ZIP_FILE = "java_release_build-${timestamp}.zip"

                    // Clean previous release_package folder
                    bat "if exist ${RELEASE_PACKAGE_DIR} rmdir /s /q ${RELEASE_PACKAGE_DIR}"
                    bat "mkdir ${RELEASE_PACKAGE_DIR}"

                    // Copy root jars into release_package
                    bat "copy /Y *.jar ${RELEASE_PACKAGE_DIR}\\"

                    // Copy entire /out folder preserving structure
                    bat "xcopy /E /I ${OUTPUT_DIR} ${RELEASE_PACKAGE_DIR}\\out"

                    // Zip release_package folder contents into the zip file
                    echo "🗜️ Creating zip ${ZIP_FILE} with root jars + /out/ folder"
                    bat "powershell -Command \"Compress-Archive -Path ${RELEASE_PACKAGE_DIR}\\* -DestinationPath ${ZIP_FILE}\""
                }
            }
        }

        stage('Push ZIP to GitHub Release') {
            when {
                expression { return credentials('GITHUB_PAT') != null }
            }
            steps {
                withCredentials([string(credentialsId: 'GITHUB_PAT', variable: 'TOKEN')]) {
                    script {
                        def versionTag = "auto-release-${new Date().format('yyyyMMdd-HHmmss')}"
                        def apiUrl = "https://api.github.com/repos/${GITHUB_OWNER}/${GITHUB_REPO}/releases"

                        def createPayload = """
                        {
                          "tag_name": "${versionTag}",
                          "name": "${versionTag}",
                          "body": "Automated build with zipped JARs",
                          "draft": false,
                          "prerelease": false
                        }
                        """

                        def response = httpRequest(
                            httpMode: 'POST',
                            contentType: 'APPLICATION_JSON',
                            requestBody: createPayload,
                            customHeaders: [[name: 'Authorization', value: "token ${TOKEN}"]],
                            url: apiUrl
                        )

                        def releaseInfo = readJSON text: response.content
                        def uploadUrl = releaseInfo.upload_url.replace("{?name,label}", "") + "?name=${ZIP_FILE}"

                        echo "📤 Uploading ${ZIP_FILE} to GitHub Release"
                        httpRequest(
                            httpMode: 'POST',
                            requestBody: readFile("${ZIP_FILE}"),
                            contentType: 'application/zip',
                            customHeaders: [[name: 'Authorization', value: "token ${TOKEN}"]],
                            url: uploadUrl
                        )

                        // Cleanup old releases
                        echo "🗑️ Cleaning up old releases, keeping only last ${RELEASES_TO_KEEP}"

                        def listResponse = httpRequest(
                            httpMode: 'GET',
                            customHeaders: [[name: 'Authorization', value: "token ${TOKEN}"]],
                            url: apiUrl
                        )
                        def releases = readJSON text: listResponse.content
                        releases.sort { a, b -> b.created_at <=> a.created_at }

                        if (releases.size() > RELEASES_TO_KEEP) {
                            def toDelete = releases.drop(RELEASES_TO_KEEP)
                            toDelete.each { rel ->
                                echo "Deleting old release: ${rel.tag_name} (ID: ${rel.id})"
                                httpRequest(
                                    httpMode: 'DELETE',
                                    customHeaders: [[name: 'Authorization', value: "token ${TOKEN}"]],
                                    url: "${apiUrl}/${rel.id}"
                                )
                            }
                        } else {
                            echo "No old releases to delete."
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            echo "✅ Build and ZIP release uploaded successfully!"
            // Uncomment and configure your email to enable notifications:
            
            emailext(
                subject: "Jenkins Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Good news! The build succeeded.\nCheck the release: https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}/releases",
                to: "${EMAIL_RECIPIENTS}"
            )
            
        }
        failure {
            echo "❌ Build or release failed. See logs."
            
            emailext(
                subject: "Jenkins Build Failure: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                body: "Build failed. Please check Jenkins logs for details.",
                to: "${EMAIL_RECIPIENTS}"
            )
            
        }
    }
}
