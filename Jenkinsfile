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
                    def testClassesBaseDir = "${workspacePath}/${env.TEST_CLASSES_DIR_BASE}".replace('/', File.separator)
                    def testReportsBaseDir = "${workspacePath}/${env.TEST_REPORTS_DIR_BASE}".replace('/', File.separator)

                    bat "if exist \"${testClassesBaseDir}\" rmdir /s /q \"${testClassesBaseDir}\""
                    bat "mkdir \"${testClassesBaseDir}\""
                    bat "if exist \"${testReportsBaseDir}\" rmdir /s /q \"${testReportsBaseDir}\""
                    bat "mkdir \"${testReportsBaseDir}\""

                    def psDebugFindTestRootsScript = """
                        \$ErrorActionPreference = 'SilentlyContinue';
                        \$baseSourcePath = "${workspacePath}/source"
                        Write-Host "DEBUG Jenkinsfile: Searching for test roots under: \$baseSourcePath"

                        \$allJavaDirs = Get-ChildItem -Path \$baseSourcePath -Recurse -Directory -Filter "java" | ForEach-Object { \$_.FullName }
                        if (\$null -ne \$allJavaDirs -and \$allJavaDirs.Count -gt 0) {
                            Write-Host "DEBUG Jenkinsfile: Found directories named 'java' under '\$baseSourcePath':"
                            \$allJavaDirs | ForEach-Object { Write-Host ("  JENKINS_JAVA_DIR_FOUND: " + \$_) }
                        } else {
                            Write-Host "DEBUG Jenkinsfile: No directories named 'java' found under '\$baseSourcePath'."
                        }

                        \$testRootDirs = \$allJavaDirs | Where-Object { \$_ -match '[\\\\\\/]src[\\\\\\/]test[\\\\\\/]java\$' }

                        if (\$null -ne \$testRootDirs -and \$testRootDirs.Count -gt 0) {
                            Write-Host "DEBUG Jenkinsfile: Filtered test root directories (matching 'src/test/java' pattern):"
                            \$testRootDirs | ForEach-Object { Write-Host ("  JENKINS_TEST_ROOT_MATCHED: " + \$_) }
                            \$testRootDirs | Out-File -FilePath 'test_root_dirs.txt' -Encoding utf8NoBOM
                        } else {
                            Write-Host "DEBUG Jenkinsfile: No directories matched the pattern '[\\\\\\/]src[\\\\\\/]test[\\\\\\/]java\$' after filtering."
                            Set-Content -Path 'test_root_dirs.txt' -Value ''
                        }
                        exit 0 # Explicitly exit with 0
                    """.stripIndent()
                    byte[] debugTestRootsScriptBytes = psDebugFindTestRootsScript.getBytes("UTF-16LE")
                    def encodedDebugTestRootsCommand = debugTestRootsScriptBytes.encodeBase64().toString()

                    echo "DEBUG Jenkinsfile: Executing PowerShell script to find test roots."
                    try {
                        bat "powershell -NoProfile -NonInteractive -EncodedCommand ${encodedDebugTestRootsCommand}"
                    } catch (e) {
                        echo "ERROR: The 'bat' step for running the PowerShell script to find test roots failed!"
                        echo "Exception: ${e.toString()}"
                        error "PowerShell script execution via bat failed: ${e.getMessage()}"
                    }
                    echo "DEBUG Jenkinsfile: PowerShell script execution via bat (supposedly) completed."

                    def testRootDirsContent = readFile(file: 'test_root_dirs.txt', encoding: 'UTF-8').trim()
                    if (testRootDirsContent.isEmpty()) {
                        echo "No 'src/test/java' directories found matching the pattern. Skipping unit tests."
                    } else {
                        def testRootDirsPaths = testRootDirsContent.split("\\r?\\n").collect { it.trim().replace('\\', '/') }.findAll { it }
                        echo "Processing test root directories: ${testRootDirsPaths}"

                        def absoluteLibsDirPath = "${workspacePath}/${env.LIBS_DIR_PATH}".replace('/', File.separator)
                        def dependencyJarsForTests = []
                        if (env.LIBS_DIR_PATH) {
                            def libsDirFile = new File(absoluteLibsDirPath)
                            if (libsDirFile.isDirectory()) {
                                File[] filesInLibsDir = libsDirFile.listFiles()
                                if (filesInLibsDir != null) {
                                    for (File f : filesInLibsDir) {
                                        if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                                            dependencyJarsForTests.add(f.getAbsolutePath().replace('/', '\\'))
                                        }
                                    }
                                }
                            }
                        }
                        if (dependencyJarsForTests.isEmpty()) {
                            echo "WARNING: No dependency JARs found in '${absoluteLibsDirPath}'. Tests might fail."
                        }
                        def junitConsoleLauncherJar = dependencyJarsForTests.find { it.toLowerCase().contains("junit-platform-console-standalone") }
                        if (!junitConsoleLauncherJar) {
                            error "JUnit Platform Console Standalone JAR not found in libs directory ('${absoluteLibsDirPath}'). Cannot run tests."
                        }

                        for (String testRootDirPath : testRootDirsPaths) {
                            echo "\n--- Processing tests in module containing: ${testRootDirPath} ---"

                            File testRootFileObj = new File(testRootDirPath)
                            File srcDirFileObj = testRootFileObj.getParentFile().getParentFile()
                            File appModuleDirFileObj = srcDirFileObj.getParentFile()

                            String appNameForOutputs = appModuleDirFileObj.getName()
                            String derivedAppClassName = ""

                            File mainJavaDir = new File(srcDirFileObj, "main/java")
                            if (mainJavaDir.exists() && mainJavaDir.isDirectory()) {
                                mainJavaDir.eachFileRecurse(groovy.io.FileType.FILES) { file ->
                                    if (derivedAppClassName.isEmpty() && file.name.endsWith('.java')) {
                                        if (file.text.contains("public static void main")) {
                                            derivedAppClassName = file.name.replace('.java', '')
                                        }
                                    }
                                }
                            }

                            if (derivedAppClassName.isEmpty()) {
                                derivedAppClassName = appModuleDirFileObj.getName()
                                echo "INFO: Could not find a main class in ${mainJavaDir}. Using module name '${derivedAppClassName}' for _classes dir lookup."
                            } else {
                                echo "INFO: Found main class '${derivedAppClassName}' in ${mainJavaDir} for module ${appModuleDirFileObj.getName()}."
                            }

                            appNameForOutputs = derivedAppClassName
                            echo "DEBUG: Module dir name: ${appModuleDirFileObj.getName()}, App Class Name for _classes dir: ${derivedAppClassName}"
                            def appClassOutputDir = "${workspacePath}/${env.OUTPUT_DIR}/${derivedAppClassName}_classes".replace('/', File.separator)

                            echo "Deduced AppName for test outputs: ${appNameForOutputs}"
                            echo "Expected App class output dir: ${appClassOutputDir}"

                            if (!new File(appClassOutputDir).exists()) {
                                echo "ERROR: Application class output directory '${appClassOutputDir}' not found for module/app '${derivedAppClassName}'. Trying to find related _classes dir..."
                                String searchPatternForClasses = appModuleDirFileObj.getName().toLowerCase()
                                def foundMatchingClassDir = ""
                                new File("${workspacePath}/${env.OUTPUT_DIR}".replace('/', File.separator)).eachDirMatch(~/.*_classes/) { dir ->
                                    String classNameFromDir = dir.name.replace('_classes', '').toLowerCase()
                                    if (testRootDirPath.toLowerCase().contains(classNameFromDir) || classNameFromDir.contains(searchPatternForClasses) || appModuleDirFileObj.getName().toLowerCase().contains(classNameFromDir)) {
                                        if (foundMatchingClassDir.isEmpty()) {
                                            foundMatchingClassDir = dir.getAbsolutePath().replace('/', File.separator)
                                            echo "INFO: Found plausible related app class dir: ${foundMatchingClassDir}"
                                        } else {
                                            echo "INFO: Found another plausible related app class dir: ${dir.getAbsolutePath().replace('/', File.separator)}, sticking with first: ${foundMatchingClassDir}"
                                        }
                                    }
                                }
                                if (!foundMatchingClassDir.isEmpty()) {
                                    appClassOutputDir = foundMatchingClassDir
                                } else {
                                    echo "ERROR: Still could not find a suitable application class output directory. Skipping tests for this module: ${appNameForOutputs}"
                                    continue
                                }
                            }

                            def currentTestClassesDir = "${testClassesBaseDir}/${appNameForOutputs}".replace('/', File.separator)
                            def currentTestReportsDir = "${testReportsBaseDir}/${appNameForOutputs}".replace('/', File.separator)
                            bat "if not exist \"${currentTestClassesDir}\" mkdir \"${currentTestClassesDir}\""
                            bat "if not exist \"${currentTestReportsDir}\" mkdir \"${currentTestReportsDir}\""

                            def psFindSpecificTestsScript = """
                                \$ErrorActionPreference = 'Stop';
                                \$testJavaFiles = Get-ChildItem -Path "${testRootDirPath.replace('/', '\\')}" -Recurse -Filter *.java | ForEach-Object { \$_.FullName };
                                if (\$null -ne \$testJavaFiles -and \$testJavaFiles.Count -gt 0) {
                                    [System.IO.File]::WriteAllLines("test_java_files_${appNameForOutputs}.txt", \$testJavaFiles, [System.Text.UTF8Encoding]::new(\$false))
                                } else {
                                    Set-Content -Path "test_java_files_${appNameForOutputs}.txt" -Value ''
                                }
                            """.stripIndent()
                            byte[] specificTestScriptBytes = psFindSpecificTestsScript.getBytes("UTF-16LE")
                            def encodedSpecificTestCommand = specificTestScriptBytes.encodeBase64().toString()
                            bat "powershell -NoProfile -NonInteractive -EncodedCommand ${encodedSpecificTestCommand}"

                            def specificTestFileContent = readFile(file: "test_java_files_${appNameForOutputs}.txt", encoding: 'UTF-8').trim()
                            bat "del \"test_java_files_${appNameForOutputs}.txt\""

                            if (specificTestFileContent.isEmpty()) {
                                echo "No Java test files found in ${testRootDirPath}. Skipping tests for this module."
                                continue
                            }
                            def testJavaFiles = specificTestFileContent.split("\\r?\\n").collect { it.trim().replace('\\', '/') }.findAll { it }
                            echo "Test files for ${appNameForOutputs}: ${testJavaFiles}"

                            String testSourcePathForCompiler = srcDirFileObj.getAbsolutePath().replace('/', File.separator)

                            def testCompileClasspathList = dependencyJarsForTests.clone()
                            testCompileClasspathList.add(appClassOutputDir)
                            def testCompileClasspath = testCompileClasspathList.join(File.pathSeparator)
                            String testCompileCpOpt = testCompileClasspath.isEmpty() ? "" : "-cp \"${testCompileClasspath}\""

                            def testFilesToCompileCmd = testJavaFiles.collect { "\"${it.replace('/', '\\')}\"" }.join(" ")
                            def testCompileCommand = "javac -encoding UTF-8 ${testCompileCpOpt} -d \"${currentTestClassesDir}\" -sourcepath \"${testSourcePathForCompiler}\" ${testFilesToCompileCmd}"
                            echo "Test Compile CMD for ${appNameForOutputs}: ${testCompileCommand}"
                            try {
                                bat testCompileCommand
                            } catch (e) {
                                echo "ERROR during test compilation for ${appNameForOutputs}."
                                echo "Command was: ${testCompileCommand}"
                                echo "Exception: ${e.toString()}"
                                echo "Stderr/Stdout from bat might be in the build log above this message."
                                echo "WARNING: Test compilation failed for ${appNameForOutputs}, continuing with next module if any."
                                continue
                            }

                            def testRuntimeClasspathList = [currentTestClassesDir] + testCompileClasspathList
                            def testRuntimeClasspath = testRuntimeClasspathList.unique().join(File.pathSeparator)

                            def runTestsCommand = "java -jar \"${junitConsoleLauncherJar}\" --classpath \"${testRuntimeClasspath}\" --scan-classpath \"${currentTestClassesDir}\" --reports-dir \"${currentTestReportsDir}\""
                            echo "Run Tests CMD for ${appNameForOutputs}: ${runTestsCommand}"
                            try {
                                bat runTestsCommand
                            } catch (e) {
                                echo "ERROR during test execution for ${appNameForOutputs} (bat command itself failed)."
                                echo "Command was: ${runTestsCommand}"
                                echo "Exception: ${e.toString()}"
                                echo "Stderr/Stdout from bat might be in the build log above this message."
                                echo "This might indicate a problem with the JUnit runner setup or classpath, not necessarily just test failures."
                            }
                        }
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