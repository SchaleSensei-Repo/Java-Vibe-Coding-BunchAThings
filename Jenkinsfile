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
                                   }
                               }
                           } else {
                               echo "WARNING: listFiles() returned null for '${absoluteLibsDirPath}'."
                           }
                       } else {
                           echo "WARNING: Calculated libs path '${absoluteLibsDirPath}' is NOT a directory (exists: ${libsDir.exists()})."
                       }
                    } else {
                        echo "WARNING: LIBS_DIR_PATH environment variable is not set."
                    }

                    if (!dependencyJars.isEmpty()) {
                        echo "Found dependency JARs to add to classpath: ${dependencyJars.join(File.pathSeparator)}"
                    } else {
                        echo "WARNING: No external dependency JARs found."
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

        stage('Unit Tests') {
            steps {
                script {
                    echo "--- Starting Unit Tests ---"
                    def workspacePath = env.WORKSPACE.replace('\\', '/')
                    def workspacePathForPS = env.WORKSPACE.replace('/', '\\') 
                    def testClassesBaseDir = "${workspacePath}/${env.TEST_CLASSES_DIR_BASE}".replace('/', File.separator)
                    def testReportsBaseDir = "${workspacePath}/${env.TEST_REPORTS_DIR_BASE}".replace('/', File.separator)
                    def psOutputFilePathGroovy = "${workspacePathForPS}\\test_root_dirs.txt" // Groovy variable with Windows path

                    bat "if exist \"${testClassesBaseDir}\" rmdir /s /q \"${testClassesBaseDir}\""
                    bat "mkdir \"${testClassesBaseDir}\""
                    bat "if exist \"${testReportsBaseDir}\" rmdir /s /q \"${testReportsBaseDir}\""
                    bat "mkdir \"${testReportsBaseDir}\""

                    String groovyPathForFileCheck = psOutputFilePathGroovy.replace('\\', '/')
                    if (fileExists(groovyPathForFileCheck)) {
                        echo "INFO Jenkinsfile GROOVY: Pre-deleting existing '${groovyPathForFileCheck}'"
                        try { bat "del /Q /F \"${psOutputFilePathGroovy}\"" } catch (e) { 
                            echo "WARNING Jenkinsfile GROOVY: Could not pre-delete '${psOutputFilePathGroovy}'. ${e.getMessage()}"
                        }
                    }

                    // Using the SIMPLIFIED PowerShell script for debugging file creation
                    def psSimplifiedScript = """
                        \$ErrorActionPreference = 'Stop'; 
                        \$outputFilePathInPS = '${psOutputFilePathGroovy.replace('\\', '\\\\')}'

                        Write-Host "DEBUG PS: This is PowerShell speaking."
                        Write-Host "DEBUG PS: Will attempt to write to: '\$outputFilePathInPS'"
                        
                        \$markerContent = "PowerShell_was_here_and_created_this_file_successfully_$(Get-Date)"
                        
                        try {
                            Set-Content -Path "\$outputFilePathInPS" -Value \$markerContent -Encoding utf8NoBOM -Force 
                            Write-Host "DEBUG PS: Set-Content command executed for '\$outputFilePathInPS'."
                            if (Test-Path -Path "\$outputFilePathInPS" -PathType Leaf) {
                                Write-Host "DEBUG PS: SUCCESS - Test-Path confirms '\$outputFilePathInPS' EXISTS after Set-Content."
                                \$fileContent = Get-Content -Path "\$outputFilePathInPS" -Raw
                                Write-Host "DEBUG PS CONTENT: \$fileContent"
                            } else {
                                Write-Host "DEBUG PS ERROR: FAILURE - Test-Path confirms '\$outputFilePathInPS' DOES NOT EXIST or is not a file after Set-Content."
                                exit 1 
                            }
                        } catch {
                            Write-Host "DEBUG PS ERROR: Exception during Set-Content or Test-Path: \$(\$_.Exception.ToString())"
                            exit 1 
                        }
                        exit 0 
                    """.stripIndent()
                    
                    // This is the original complex script to find actual test roots.
                    // Keep it commented out until the simplified script above works for file creation.
                    /*
                    def psComplexFindTestRootsScript = ""\"
                        \$ErrorActionPreference = 'SilentlyContinue'; // Can be 'Stop' for harder failure
                        \$baseSourcePath = "${workspacePath}/source"
                        // Use the Groovy variable directly in PS, ensuring paths are PS-friendly
                        \$outputFilePathInPS = '${psOutputFilePathGroovy.replace('\\', '\\\\')}'

                        Write-Host "DEBUG Jenkinsfile PS: Searching for test roots under: \$baseSourcePath"
                        Write-Host "DEBUG Jenkinsfile PS: PowerShell will write output to: \$outputFilePathInPS"

                        \$allJavaDirs = Get-ChildItem -Path \$baseSourcePath -Recurse -Directory -Filter "java" | ForEach-Object { \$_.FullName }
                        if (\$null -ne \$allJavaDirs -and \$allJavaDirs.Count -gt 0) {
                            Write-Host "DEBUG Jenkinsfile PS: Found directories named 'java' under '\$baseSourcePath':"
                            \$allJavaDirs | ForEach-Object { Write-Host ("  JENKINS_JAVA_DIR_FOUND: " + \$_) }
                        } else {
                            Write-Host "DEBUG Jenkinsfile PS: No directories named 'java' found under '\$baseSourcePath'."
                        }

                        \$testRootDirs = \$allJavaDirs | Where-Object { \$_ -match '[\\\\\\/]src[\\\\\\/]test[\\\\\\/]java\$' }

                        if (\$null -ne \$testRootDirs -and \$testRootDirs.Count -gt 0) {
                            Write-Host "DEBUG Jenkinsfile PS: Filtered test root directories (matching 'src/test/java' pattern):"
                            \$testRootDirs | ForEach-Object { Write-Host ("  JENKINS_TEST_ROOT_MATCHED: " + \$_) }
                            \$testRootDirs | Out-File -FilePath \$outputFilePathInPS -Encoding utf8NoBOM -Force
                        } else {
                            Write-Host "DEBUG Jenkinsfile PS: No directories matched the pattern '[\\\\\\/]src[\\\\\\/]test[\\\\\\/]java\$' after filtering."
                            Set-Content -Path \$outputFilePathInPS -Value '' -Force
                        }
                        exit 0 
                    ""\".stripIndent()
                    */
                    
                    // Determine which PowerShell script to run (simplified for now)
                    byte[] psScriptBytesToRun = psSimplifiedScript.getBytes("UTF-16LE")
                    def encodedPsCommandToRun = psScriptBytesToRun.encodeBase64().toString()

                    echo "DEBUG Jenkinsfile GROOVY: Executing PowerShell script."
                    try {
                        bat "powershell -NoProfile -NonInteractive -EncodedCommand ${encodedPsCommandToRun}"
                    } catch (e) {
                        echo "ERROR Jenkinsfile GROOVY: The 'bat' step for running the PowerShell script FAILED!"
                        echo "Exception: ${e.toString()}"
                        error "PowerShell script execution via bat failed: ${e.getMessage()}"
                    }
                    echo "DEBUG Jenkinsfile GROOVY: PowerShell script execution via bat (supposedly) completed."

                    echo "DEBUG Jenkinsfile GROOVY: Checking if file exists from Groovy: '${groovyPathForFileCheck}'"
                    if (fileExists(groovyPathForFileCheck)) {
                        echo "SUCCESS Jenkinsfile GROOVY: File '${groovyPathForFileCheck}' exists after bat call."
                        def fileContentFromGroovy = readFile(file: groovyPathForFileCheck, encoding: 'UTF-8').trim()
                        echo "CONTENT Jenkinsfile GROOVY: Read from file: '${fileContentFromGroovy}'"
                        if (fileContentFromGroovy.contains("PowerShell_was_here")) {
                            echo "SUCCESS Jenkinsfile GROOVY: Marker content found in file!"
                            echo "INFO: File creation debug successful. To run actual tests, use the complex PowerShell script and uncomment the main test loop."
                            // For this debug, we stop further processing.
                            // In a real run with the complex script, testRootDirsContentActual would be used.
                        } else {
                            error "Marker content not found in '${groovyPathForFileCheck}'. Content: '${fileContentFromGroovy}'"
                        }
                    } else {
                        echo "FAILURE Jenkinsfile GROOVY: File '${groovyPathForFileCheck}' DOES NOT exist after bat call. Trying a small delay."
                        sleep 2 
                        if (fileExists(groovyPathForFileCheck)) {
                            echo "SUCCESS Jenkinsfile GROOVY: File '${groovyPathForFileCheck}' exists after 2s delay."
                            def fileContentFromGroovy = readFile(file: groovyPathForFileCheck, encoding: 'UTF-8').trim()
                            echo "CONTENT Jenkinsfile GROOVY (after delay): Read from file: '${fileContentFromGroovy}'"
                        } else {
                            echo "FAILURE Jenkinsfile GROOVY: File '${groovyPathForFileCheck}' STILL DOES NOT exist after 2s delay."
                            echo "Listing workspace contents after PS script:"
                            bat "dir"
                            error "'${groovyPathForFileCheck}' was not created or visible."
                        }
                    }

                    // Placeholder for original test processing logic (currently bypassed by simplified PS script)
                    def testRootDirsContentActual = readFile(file: groovyPathForFileCheck, encoding: 'UTF-8').trim() // Re-read in case of delay
                    if (testRootDirsContentActual.isEmpty() || !testRootDirsContentActual.contains("C:\\")) { 
                        echo "INFO: Content of '${groovyPathForFileCheck}' is marker or empty. Skipping actual test processing loop for this debug iteration."
                    } else {
                        // This block would contain the full 'for' loop to process actual test roots
                        // if the psComplexFindTestRootsScript was used and successful.
                        echo "PLACEHOLDER: Actual test processing loop would run here if PowerShell script wrote paths."
                        // def testRootDirsPaths = testRootDirsContentActual.split("\\r?\\n").collect { it.trim().replace('\\', '/') }.findAll { it }
                        // for (String testRootDirPath : testRootDirsPaths) { ... }
                    }

                    junit allowEmptyResults: true, testResults: "${env.TEST_REPORTS_DIR_BASE.replace('/', File.separator)}/**/*.xml"
                    echo "--- Finished Unit Tests ---"
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
                            bat "copy \"${jarFile}\" \"${RELEASE_PACKAGE_DIR}\\\""
                        } else {
                            echo "Warning: Root JAR ${jarFile} not found in workspace root."
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
                    def zipFile = "${RELEASE_PACKAGE_DIR}.zip"
                    bat "powershell Compress-Archive -Path \"${RELEASE_PACKAGE_DIR}\\*\" -DestinationPath \"${zipFile}\" -Force"
                    String releaseData = "{ \\\"tag_name\\\": \\\"${tag}\\\", \\\"name\\\": \\\"${tag}\\\", \\\"body\\\": \\\"${message}\\\", \\\"draft\\\": false, \\\"prerelease\\\": false }"
                    withCredentials([string(credentialsId: "${GITHUB_CREDS}", variable: 'GH_TOKEN')]) {
                        bat """
                            curl -L -X POST ^
                                 -H "Accept: application/vnd.github+json" ^
                                 -H "Authorization: Bearer %GH_TOKEN%" ^
                                 -H "X-GitHub-Api-Version: 2022-11-28" ^
                                 https://api.github.com/repos/${GITHUB_REPO}/releases ^
                                 -d "${releaseData}"
                        """
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