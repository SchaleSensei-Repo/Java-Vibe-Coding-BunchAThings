pipeline {
    agent any

    triggers {
        pollSCM('H/10 * * * *')
    }

    environment {
        OUTPUT_DIR = 'out'
        RELEASE_PACKAGE_DIR = 'release_package'
        GITHUB_REPO = 'SchaleSensei-Repo/Java-Vibe-Coding-BunchAThings'
        GITHUB_CREDS = 'GITHUB_PAT'
        RELEASES_TO_KEEP = 3
        EMAIL_RECIPIENTS = 'ashlovedawn@gmail.com'
        LIBS_DIR_PATH = 'libs'
        TEST_CLASSES_DIR_BASE = 'out/test-classes'
        TEST_REPORTS_DIR_BASE = 'out/test-reports'
    }

    stages {
        stage('Clean and Checkout') {
            steps {
                cleanWs()
                checkout scm
            }
        }

        stage('Download Dependencies') {
            steps {
                script {
                    echo "--- Starting Dependency Download ---"
                    def libsFile = 'libs.txt'
                    if (!fileExists(libsFile)) {
                        error "Dependency list file '${libsFile}' not found in workspace."
                    }

                    def libUrls = readFile(libsFile).readLines().collect { it.trim() }.findAll { it }

                    if (libUrls.isEmpty()) {
                        echo "No URLs found in ${libsFile} or all lines were empty. Skipping download."
                    } else {
                        def libsDirPath = env.LIBS_DIR_PATH
                        bat "if not exist \"${libsDirPath}\" mkdir \"${libsDirPath}\""
                        
                        echo "Found ${libUrls.size()} URL(s) to process from ${libsFile}."
                        libUrls.each { url ->
                            try {
                                def fileName = url.substring(url.lastIndexOf('/') + 1)
                                if (!fileName || fileName.isEmpty()) {
                                    echo "WARNING: Could not extract a valid filename (empty after substring) from URL: ${url}. Skipping."
                                    return 
                                }
                                int queryIndex = fileName.indexOf('?')
                                if (queryIndex != -1) { fileName = fileName.substring(0, queryIndex) }
                                int fragmentIndex = fileName.indexOf('#')
                                if (fragmentIndex != -1) { fileName = fileName.substring(0, fragmentIndex) }
                                if (!fileName || fileName.isEmpty()) { 
                                    echo "WARNING: Could not extract a valid filename (empty after cleaning) from URL: ${url}. Skipping."
                                    return 
                                }
                                def destPath = "${libsDirPath}\\${fileName}".replace('/', '\\')
                                echo "Downloading ${url} to ${destPath}"
                                bat "powershell -NoProfile -NonInteractive -Command \"Invoke-WebRequest -Uri '${url}' -OutFile '${destPath}' -UseBasicParsing\""
                                echo "Successfully downloaded ${fileName}"
                            } catch (Exception e) { 
                                echo "ERROR: Failed to download or process filename for ${url}. Error: ${e.toString()}"
                            }
                        }
                        echo "--- Finished Dependency Download ---"
                        echo "Verifying contents of ${libsDirPath}:"
                        bat "dir \"${libsDirPath}\""
                    }
                }
            }
        }

        stage('Build Apps') {
            steps {
                script {
                    def start = System.currentTimeMillis()
                    bat "if exist \"${OUTPUT_DIR}\" rmdir /s /q \"${OUTPUT_DIR}\""
                    bat "mkdir \"${OUTPUT_DIR}\""

                    def psScriptContent = '''
                        $ErrorActionPreference = 'Stop';
                        $javaFilePaths = Get-ChildItem -Recurse -Filter *.java |
                            Where-Object { Select-String -Path $_.FullName -Pattern 'public static void main' -Quiet } |
                            ForEach-Object { $_.FullName };
                        if ($null -ne $javaFilePaths -and $javaFilePaths.Count -gt 0) {
                            [System.IO.File]::WriteAllLines('main_java_files.txt', $javaFilePaths, [System.Text.UTF8Encoding]::new($false))
                        } else {
                            Write-Host 'No Java files with a main method were found.';
                            Set-Content -Path 'main_java_files.txt' -Value ''
                        }
                    '''.stripIndent()

                    byte[] scriptBytes = psScriptContent.getBytes("UTF-16LE")
                    def encodedCommand = scriptBytes.encodeBase64().toString()
                    bat "powershell -NoProfile -NonInteractive -EncodedCommand ${encodedCommand}"

                    echo "Listing workspace root contents:"
                    bat "dir /b"

                    def javaFileContent = readFile(file: 'main_java_files.txt', encoding: 'UTF-8').trim()
                    if (javaFileContent.isEmpty()) {
                        error "No Java files with a main method were found (main_java_files.txt is empty or PowerShell script failed silently)."
                    }

                    def javaFiles = javaFileContent.split("\\r?\\n")
                        .collect { it.trim() }
                        .findAll { it }

                    if (javaFiles.isEmpty()) {
                        error "No Java files with a main method were found after processing main_java_files.txt."
                    }

                    def rootJarApps = ['AppMainRoot']
                    def dependencyJars = []
                    def workspacePath = env.WORKSPACE
                    def absoluteLibsDirPath = "${workspacePath}\\${env.LIBS_DIR_PATH}".replace('/', File.separator)

                    echo "DEBUG: Workspace path: '${workspacePath}'"
                    echo "DEBUG: LIBS_DIR_PATH from environment: '${env.LIBS_DIR_PATH}'"
                    echo "DEBUG: Calculated absolute path for libs directory: '${absoluteLibsDirPath}'"

                    if (env.LIBS_DIR_PATH) {
                       def libsDir = new File(absoluteLibsDirPath)
                       if (libsDir.isDirectory()) {
                           echo "SUCCESS: '${absoluteLibsDirPath}' is a directory. Listing its contents via 'bat dir':"
                           bat "dir \"${absoluteLibsDirPath}\""
                           File[] filesInLibsDir = libsDir.listFiles()
                           if (filesInLibsDir != null) {
                               for (File f : filesInLibsDir) {
                                   if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                                       dependencyJars.add(f.getAbsolutePath().replace('/', '\\'))
                                       echo "DEBUG: Found dependency JAR: ${f.getAbsolutePath()}"
                                   } else if (f.isFile()) {
                                       echo "DEBUG: Ignored non-JAR file in libs directory: ${f.getName()}"
                                   }
                               }
                           } else {
                               echo "WARNING: listFiles() returned null for '${absoluteLibsDirPath}'."
                           }
                       } else {
                           echo "WARNING: Calculated libs path '${absoluteLibsDirPath}' is NOT a directory (exists: ${libsDir.exists()}). Dependencies might be missing."
                       }
                    } else {
                        echo "WARNING: LIBS_DIR_PATH environment variable is not set."
                    }

                    if (!dependencyJars.isEmpty()) {
                        echo "Found dependency JARs to add to classpath: ${dependencyJars.join(File.pathSeparator)}"
                    } else {
                        echo "WARNING: No external dependency JARs found or loaded. Compilation may fail for projects needing these JARs."
                    }
                    def commonClassPath = dependencyJars.join(File.pathSeparator)
                    String classPathOpt = commonClassPath.isEmpty() ? "" : "-cp \"${commonClassPath}\""

                    def builds = javaFiles.collectEntries { fullFilePath ->
                        String groovyFilePath = fullFilePath.replace('\\', '/')
                        def fileName = groovyFilePath.tokenize('/')[-1]
                        def classNameOnly = fileName.replace('.java', '')
                        String packageSubPath = ""
                        String srcDirForSourcepathRelative = ""
                        String fqcn = classNameOnly
                        int srcMainJavaIdxInRelative = -1
                        int srcIdxInRelative = -1
                        int packageStartIndexInFilePath = -1
                        String workspacePrefix = env.WORKSPACE.replace('\\', '/') + "/"
                        String relativeFilePathToWorkspace = groovyFilePath.startsWith(workspacePrefix) ? groovyFilePath.substring(workspacePrefix.length()) : groovyFilePath
                        srcMainJavaIdxInRelative = relativeFilePathToWorkspace.lastIndexOf("src/main/java/")
                        srcIdxInRelative = relativeFilePathToWorkspace.lastIndexOf("src/")

                        if (srcMainJavaIdxInRelative != -1) {
                            packageStartIndexInFilePath = srcMainJavaIdxInRelative + "src/main/java/".length()
                            srcDirForSourcepathRelative = relativeFilePathToWorkspace.substring(0, packageStartIndexInFilePath -1)
                        } else if (srcIdxInRelative != -1) {
                            packageStartIndexInFilePath = srcIdxInRelative + "src/".length()
                            srcDirForSourcepathRelative = relativeFilePathToWorkspace.substring(0, packageStartIndexInFilePath -1)
                        } else {
                            String currentPath = relativeFilePathToWorkspace.contains('/') ? relativeFilePathToWorkspace.substring(0, relativeFilePathToWorkspace.lastIndexOf('/')) : '.'
                            List<String> pathParts = currentPath.tokenize('/')
                            int lastPotentialPackagePartIndex = pathParts.size() -1
                            while(lastPotentialPackagePartIndex >= 0 && !pathParts[lastPotentialPackagePartIndex].isEmpty()) {
                                if (pathParts[lastPotentialPackagePartIndex] ==~ /^[a-z_][a-z0-9_]*$/) {
                                     lastPotentialPackagePartIndex--
                                } else {
                                    break
                                }
                            }
                            srcDirForSourcepathRelative = pathParts.subList(0, lastPotentialPackagePartIndex + 1).join('/')
                            if ( (lastPotentialPackagePartIndex + 1) < pathParts.size() ) {
                                packageSubPath = pathParts.subList(lastPotentialPackagePartIndex + 1, pathParts.size()).join('/')
                            }
                            if (srcDirForSourcepathRelative.isEmpty() && currentPath != '.') {
                                srcDirForSourcepathRelative = currentPath
                            } else if (srcDirForSourcepathRelative.isEmpty() && currentPath == '.') {
                                srcDirForSourcepathRelative = "."
                            }
                        }

                        if (packageStartIndexInFilePath != -1 && relativeFilePathToWorkspace.lastIndexOf('/') > packageStartIndexInFilePath) {
                            packageSubPath = relativeFilePathToWorkspace.substring(packageStartIndexInFilePath, relativeFilePathToWorkspace.lastIndexOf('/'))
                        }
                        if (!packageSubPath.isEmpty()) {
                            fqcn = packageSubPath.replace('/', '.') + "." + classNameOnly
                        }
                        def appClassOutputDirRelative = "${OUTPUT_DIR}/${classNameOnly}_classes".replace('/', '\\')
                        bat "if exist \"${appClassOutputDirRelative}\" rmdir /s /q \"${appClassOutputDirRelative}\""
                        bat "mkdir \"${appClassOutputDirRelative}\""

                        [(classNameOnly): {
                            def jarName = "${classNameOnly}.jar"
                            def jarPathRelative = rootJarApps.contains(classNameOnly)
                                ? jarName.replace('/', '\\')
                                : "${OUTPUT_DIR}\\${jarName}".replace('/', '\\')
                            echo "--- Processing App: ${classNameOnly} ---"
                            echo "  Source File: ${fullFilePath}"
                            echo "  FQCN: ${fqcn}"
                            echo "  Sourcepath for javac (relative to workspace): ${srcDirForSourcepathRelative}"
                            echo "  .class output directory (relative to workspace): ${appClassOutputDirRelative}"
                            echo "  Output JAR (relative to workspace): ${jarPathRelative}"
                            if (!commonClassPath.isEmpty()) {
                                echo "  Compiler Classpath: ${commonClassPath}"
                            }
                            String batFullFilePath = fullFilePath.replace('/', '\\')
                            String batSrcDirForSourcepathCmd = srcDirForSourcepathRelative.replace('/', '\\')
                            def compileCommand = "javac -encoding UTF-8 ${classPathOpt} -d \"${appClassOutputDirRelative}\" -sourcepath \"${batSrcDirForSourcepathCmd}\" \"${batFullFilePath}\""
                            echo "  Compile CMD: ${compileCommand}"
                            bat compileCommand
                            def jarCommand = "jar cfe \"${jarPathRelative}\" ${fqcn} -C \"${appClassOutputDirRelative}\" ."
                            echo "  JAR CMD: ${jarCommand}"
                            bat jarCommand
                            echo "--- Finished App: ${classNameOnly} ---"
                        }]
                    }
                    parallel builds
                    def end = System.currentTimeMillis()
                    echo "✅ Build Apps stage completed in ${(end - start) / 1000}s"
                }
            }
        }
        
        stage('Create Release Package') {
            steps {
                bat "if exist \"${RELEASE_PACKAGE_DIR}\" rmdir /s /q \"${RELEASE_PACKAGE_DIR}\""
                bat "mkdir \"${RELEASE_PACKAGE_DIR}\""
                bat "xcopy \"${OUTPUT_DIR}\\*.jar\" \"${RELEASE_PACKAGE_DIR}\\\" /Y /I > nul 2>&1 || echo No JARs in ${OUTPUT_DIR} to copy."
                script {
                    def rootJarAppsList = ['AppMainRoot']
                    rootJarAppsList.each { appName ->
                        def jarFile = "${appName}.jar"
                        if (fileExists(jarFile)) {
                             echo "Copying root JAR ${jarFile} to ${RELEASE_PACKAGE_DIR}"
                            bat "copy \"${jarFile}\" \"${RELEASE_PACKAGE_DIR}\\\""
                        } 
                        else if (fileExists("${OUTPUT_DIR}/${jarFile}")) {
                            echo "Copying root JAR ${OUTPUT_DIR}/${jarFile} to ${RELEASE_PACKAGE_DIR}"
                            bat "copy \"${OUTPUT_DIR}\\${jarFile}\" \"${RELEASE_PACKAGE_DIR}\\\""
                        }
                        else {
                            echo "Warning: Root JAR ${jarFile} not found in workspace root or ${OUTPUT_DIR}."
                        }
                    }
                }
            }
        }

        stage('Publish Release') {
            steps {
                script {
                    def tag = "build-${env.BUILD_NUMBER}"
                    def message = "Automated build ${env.BUILD_NUMBER}"
                    def zipFileName = "${RELEASE_PACKAGE_DIR}.zip"
                    def zipFilePath = "${env.WORKSPACE}\\${zipFileName}".replace('/', '\\') 

                    echo "Creating archive: ${zipFilePath} from directory ${RELEASE_PACKAGE_DIR}"
                    def releasePackagePathForPS = "${env.WORKSPACE}\\${RELEASE_PACKAGE_DIR}".replace('/', '\\')
                    // Using -Command for Compress-Archive for simplicity here, assuming paths are simple
                    bat "powershell -NoProfile -NonInteractive Compress-Archive -Path \"${releasePackagePathForPS}\\*\" -DestinationPath \"${zipFilePath}\" -Force"

                    if (!fileExists(zipFilePath)) {
                        error "Failed to create release ZIP file: ${zipFilePath}"
                    }

                    String releaseData = "{ \\\"tag_name\\\": \\\"${tag}\\\", \\\"name\\\": \\\"${tag}\\\", \\\"body\\\": \\\"${message}\\\", \\\"draft\\\": false, \\\"prerelease\\\": false }"
                    
                    withCredentials([string(credentialsId: "${GITHUB_CREDS}", variable: 'GH_TOKEN')]) {
                        def psPublishScriptContent = """
                            \$ErrorActionPreference = 'Stop'
                            \$ProgressPreference = 'SilentlyContinue'

                            \$ghToken = "${GH_TOKEN}" 
                            \$repo = "${GITHUB_REPO}"
                            \$releaseDataJson = '${releaseData.replace("'", "''")}' 
                            \$zipFilePathForPS = @"
${zipFilePath.replace('\\', '\\\\')}
"@.Trim() # Using PS Here-String for path
                            \$zipFileNameForPS = "${zipFileName}"

                            Write-Host "DEBUG: Repo: \$repo"
                            Write-Host "DEBUG: Release Data JSON for PS: \$releaseDataJson"
                            Write-Host "DEBUG: Zip File Path for PS: \$zipFilePathForPS"
                            Write-Host "DEBUG: Zip File Name for PS: \$zipFileNameForPS"
                            
                            Write-Host "Creating GitHub release..."
                            \$headers = @{
                                "Accept"               = "application/vnd.github+json"
                                "Authorization"        = "Bearer \$ghToken"
                                "X-GitHub-Api-Version" = "2022-11-28"
                            }
                            
                            try {
                                \$releaseResponse = Invoke-RestMethod -Uri "https://api.github.com/repos/\$repo/releases" -Method Post -Headers \$headers -Body \$releaseDataJson -ContentType "application/json"
                                Write-Host "Successfully created GitHub release."
                            } catch {
                                Write-Error "Failed to create GitHub release: \$(\$_.Exception.Message)"
                                Write-Error "Response Status: \$(\$_.Exception.Response.StatusCode) \$(\$_.Exception.Response.StatusDescription)"
                                Write-Error "Response Content: \$(\$_.Exception.Response.Content | Out-String)"
                                exit 1
                            }

                            \$uploadUrlWithPlaceholder = \$releaseResponse.upload_url
                            if (!\$uploadUrlWithPlaceholder) {
                                Write-Error "Upload URL not found in release response."
                                exit 1
                            }
                            Write-Host ("Raw Upload URL with placeholder: " + \$uploadUrlWithPlaceholder)

                            \$uploadUrlBase = \$uploadUrlWithPlaceholder.Substring(0, \$uploadUrlWithPlaceholder.IndexOf('{'))
                            \$finalUploadUrl = \$uploadUrlBase + "?name=" + [System.Uri]::EscapeDataString(\$zipFileNameForPS)
                            Write-Host ("Formatted Upload URL for \$zipFileNameForPS: " + \$finalUploadUrl)

                            Write-Host "Uploading asset: \$zipFilePathForPS to \$finalUploadUrl"
                            
                            \$uploadHeaders = @{
                                "Authorization"        = "Bearer \$ghToken"
                                "X-GitHub-Api-Version" = "2022-11-28"
                                "Content-Type"         = "application/zip" 
                            }

                            try {
                                Invoke-RestMethod -Uri \$finalUploadUrl -Method Post -Headers \$uploadHeaders -InFile \$zipFilePathForPS
                                Write-Host "Successfully uploaded \$zipFileNameForPS to release ${tag}."
                            } catch {
                                Write-Error "Failed to upload asset: \$(\$_.Exception.Message)"
                                Write-Error "Response Status: \$(\$_.Exception.Response.StatusCode) \$(\$_.Exception.Response.StatusDescription)"
                                Write-Error "Response Content: \$(\$_.Exception.Response.Content | Out-String)"
                                exit 1
                            }
                        """.stripIndent()

                        byte[] scriptBytesUTF16LE = psPublishScriptContent.getBytes("UTF-16LE") 
                        def encodedCommand = scriptBytesUTF16LE.encodeBase64().toString()
                        
                        bat "powershell -NoProfile -NonInteractive -EncodedCommand ${encodedCommand}"
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
            mail to: "${env.EMAIL_RECIPIENTS}",
                 subject: "Build ${currentBuild.currentResult}: Job ${env.JOB_NAME} [#${env.BUILD_NUMBER}]",
                 body: "See details at: ${env.BUILD_URL}"
        }
    }
}