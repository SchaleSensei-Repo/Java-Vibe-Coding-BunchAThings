pipeline {
    agent any

    triggers {
        pollSCM('H/10 * * * *')
    }

    environment {
        OUTPUT_DIR = 'out'
        RELEASE_PACKAGE_DIR = 'release_package' 
        TEMP_RELEASE_STAGING_DIR = 'temp_release_staging' 
        MAIN_HUB_JAR_NAME = 'Jar_Main_Hub.jar' // Adjusted for space
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
                        $ProgressPreference = 'SilentlyContinue';
                        $WarningPreference = 'SilentlyContinue';
                        $VerbosePreference = 'SilentlyContinue';
                        $InformationPreference = 'SilentlyContinue';
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
                    // Suppress extraneous output from this PowerShell script as well
                    bat "powershell -NoProfile -NonInteractive -EncodedCommand ${encodedCommand} 2" + ">" + "\$null 3" + ">" + "\$null 4" + ">" + "\$null 5" + ">" + "\$null 6" + ">" + "\$null 7" + ">" + "\$null"


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
                            def jarPathRelative = "${OUTPUT_DIR}\\${jarName}".replace('/', '\\')
                            if (rootJarApps.contains(classNameOnly)) {
                                jarPathRelative = jarName.replace('/', '\\') 
                                echo "INFO: ${classNameOnly} is a root JAR, will be placed in workspace root: ${jarPathRelative}"
                            }
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

        stage('Unit Test') {
            steps {
                script {
                    echo "--- Starting Unit Test Stage ---"
                    def testClassesDirBase = env.TEST_CLASSES_DIR_BASE
                    def testReportsDirBase = env.TEST_REPORTS_DIR_BASE
                    bat "if not exist \"${testClassesDirBase}\" mkdir \"${testClassesDirBase}\""
                    bat "if not exist \"${testReportsDirBase}\" mkdir \"${testReportsDirBase}\""

                    def workspacePath = env.WORKSPACE.replace('\\', '/') // Use forward slashes for internal Groovy logic
                    def libsDirPath = env.LIBS_DIR_PATH
                    def absoluteLibsDirPath = "${workspacePath}/${libsDirPath}".replace('/', File.separator)
                    
                    def dependencyJars = []
                    def libsDir = new File(absoluteLibsDirPath)
                    if (libsDir.isDirectory()) {
                        echo "INFO [Unit Test]: Scanning for JARs in ${absoluteLibsDirPath}"
                        File[] filesInLibsDir = libsDir.listFiles()
                        if (filesInLibsDir != null) {
                            for (File f : filesInLibsDir) { 
                                if (f.isFile() && f.name.toLowerCase().endsWith(".jar")) {
                                    dependencyJars.add(f.getAbsolutePath().replace('/', '\\'))
                                    echo "DEBUG [Unit Test]: Found dependency JAR: ${f.getAbsolutePath()}"
                                }
                            }
                        } else {
                            echo "WARNING [Unit Test]: listFiles() returned null for ${absoluteLibsDirPath}."
                        }
                    } else { 
                         echo "WARNING [Unit Test]: Calculated libs path '${absoluteLibsDirPath}' is NOT a directory (exists: ${libsDir.exists()})."
                    }

                    if (dependencyJars.isEmpty()) {
                        echo "WARNING: No dependency JARs found in ${libsDirPath} after scanning. Tests might fail."
                    }
                    def commonTestClasspathParts = new ArrayList(dependencyJars)

                    def junitConsoleJar = dependencyJars.find { it.contains("junit-platform-console-standalone") }
                    if (!junitConsoleJar) {
                        error "JUnit Platform Console Standalone JAR not found in ${libsDirPath}. Cannot run tests."
                    }
                    echo "Using JUnit Runner: ${junitConsoleJar.replace('/', '\\')}"

                    def mainJavaFilesTxt = 'main_java_files.txt'
                    if (!fileExists(mainJavaFilesTxt)) {
                        error "File '${mainJavaFilesTxt}' not found. Was 'Build Apps' stage successful? Cannot determine application class outputs."
                    }
                    def mainAppFullPaths = readFile(file: mainJavaFilesTxt, encoding: 'UTF-8').trim().split("\\r?\\n")
                        .collect { it.trim().replace('\\','/') } 
                        .findAll { it }

                    echo "Scanning for test modules (directories containing src/test/java)..."
                    def psFindTestModulesScript = '''
                        $ProgressPreference = 'SilentlyContinue';
                        $WarningPreference = 'SilentlyContinue';
                        $VerbosePreference = 'SilentlyContinue';
                        $InformationPreference = 'SilentlyContinue';

                        $DebugFilePath = Join-Path $env:WORKSPACE "powershell_test_discovery_debug.log";
                        Out-File -Path $DebugFilePath -InputObject "--- PowerShell Test Discovery Debug Log ---`n" -Encoding UTF8 -Force;

                        $ErrorActionPreference = 'Stop'
                        $WorkspacePath = $env:WORKSPACE
                        $moduleRoots = Get-ChildItem -Path $WorkspacePath -Recurse -Directory -Filter 'java' |
                            Where-Object { $_.Name -eq 'java' -and $_.Parent.Name -eq 'test' -and $_.Parent.Parent.Name -eq 'src' } |
                            ForEach-Object { $_.Parent.Parent.Parent.FullName } | Get-Unique
                        
                        $testFilesByModule = @{}

                        Add-Content -Path $DebugFilePath -Value "Workspace Path: $WorkspacePath`n";
                        Add-Content -Path $DebugFilePath -Value "Found Module Roots (`$moduleRoots.Count): $($moduleRoots.Count)`n";
                        $moduleRoots | ForEach-Object { Add-Content -Path $DebugFilePath -Value "  - $_" }
                        Add-Content -Path $DebugFilePath -Value "`n"; 

                        foreach ($rootPathAbs in $moduleRoots) {
                            $testJavaDir = Join-Path -Path $rootPathAbs -ChildPath 'src\\test\\java';
                            Add-Content -Path $DebugFilePath -Value "Processing root: $rootPathAbs, Test Java Dir: $testJavaDir`n";
                            if (Test-Path $testJavaDir -PathType Container) {
                                Add-Content -Path $DebugFilePath -Value "  '$testJavaDir' exists and is a directory.`n";
                                $javaFiles = Get-ChildItem -Path $testJavaDir -Recurse -Filter *.java | ForEach-Object { $_.FullName }
                                Add-Content -Path $DebugFilePath -Value "  Found Java files in $testJavaDir (`$javaFiles.Count): $($javaFiles.Count)`n";
                                if ($javaFiles.Count -gt 0) {
                                    $javaFiles | ForEach-Object { Add-Content -Path $DebugFilePath -Value "    - $($_)" }
                                    $relativeRoot = $rootPathAbs.Substring($WorkspacePath.Length).TrimStart('\\').TrimStart('/')
                                    $testFilesByModule[$relativeRoot] = @($javaFiles)
                                    Add-Content -Path $DebugFilePath -Value "  Mapped relative root '$relativeRoot' to $($javaFiles.Count) files.`n";
                                } else {
                                     Add-Content -Path $DebugFilePath -Value "  No Java files found in $testJavaDir.`n";
                                }
                            } else {
                                Add-Content -Path $DebugFilePath -Value "  '$testJavaDir' does NOT exist or is not a directory.`n";
                            }
                        }
                        Add-Content -Path $DebugFilePath -Value "`nFinal `$testFilesByModule before JSON conversion (`$testFilesByModule.Count entries): $($testFilesByModule.Count)`n";
                        $testFilesByModule.GetEnumerator() | ForEach-Object { Add-Content -Path $DebugFilePath -Value "  Key: $($_.Name), Files: $($_.Value.Count)" }
                        Add-Content -Path $DebugFilePath -Value "`n--- End of PowerShell Debug Log ---`n";

                        return $testFilesByModule | ConvertTo-Json -Depth 5
                    '''.stripIndent()
                    byte[] psScriptBytes = psFindTestModulesScript.getBytes("UTF-16LE")
                    def encodedPsCommand = psScriptBytes.encodeBase64().toString()
                    
                    def commandToExecute = "powershell -NoProfile -NonInteractive -EncodedCommand ${encodedPsCommand} 2" + ">" + "\$null 3" + ">" + "\$null 4" + ">" + "\$null 5" + ">" + "\$null 6" + ">" + "\$null 7" + ">" + "\$null"
                    echo "DEBUG: Executing PowerShell command for test discovery." 
                    def psOutputJson = bat(script: commandToExecute, returnStdout: true).trim()
                    echo "DEBUG: Raw psOutputJson from PowerShell: '${psOutputJson}'"

                    if (psOutputJson.isEmpty() || psOutputJson == "{}") {
                        echo "No test modules with src/test/java/*.java files found according to PowerShell script. Skipping test execution."
                        echo "Check 'powershell_test_discovery_debug.log' in workspace for details."
                        return
                    }
                    if (psOutputJson == null || psOutputJson.trim().isEmpty()) {
                        error("PowerShell script for test discovery returned empty or null. Cannot parse JSON. Check 'powershell_test_discovery_debug.log'.")
                    }
                    def testModulesData = readJSON(text: psOutputJson)

                    if (testModulesData.isEmpty()) {
                        echo "No test modules found after parsing JSON. Skipping test execution."
                        return
                    }

                    boolean hasExecutionErrors = false

                    testModulesData.each { moduleRelativePath, testFileFullPathsList ->
                        def testFilePaths = []
                        if (testFileFullPathsList instanceof List) {
                            testFilePaths.addAll(testFileFullPathsList)
                        } else if (testFileFullPathsList != null) {
                            testFilePaths.add(testFileFullPathsList.toString())
                        }

                        if (testFilePaths.isEmpty() || testFilePaths.every { it == null || it.toString().trim().isEmpty() }) {
                            echo "Skipping module '${moduleRelativePath}' as it has no test files listed."
                            return 
                        }

                        def moduleName = moduleRelativePath.tokenize('\\/')[-1]
                        echo "--- Processing tests for module: ${moduleName} (Path: ${moduleRelativePath}) ---"

                        def moduleWorkspacePath = "${workspacePath}/${moduleRelativePath}".replace('/', File.separator) 
                        def testSrcJavaDir = "${moduleWorkspacePath}${File.separator}src${File.separator}test${File.separator}java"

                        def associatedMainAppClassNameOnly = null
                        def associatedAppClassOutputDir = null

                        def expectedMainSrcPrefix = "${workspacePath}/${moduleRelativePath}/src/main/java".replace('//','/')
                        def foundMainAppFile = mainAppFullPaths.find { mainAppFullPath ->
                            mainAppFullPath.startsWith(expectedMainSrcPrefix)
                        }

                        if (foundMainAppFile) {
                            def mainAppFileName = foundMainAppFile.tokenize('/')[-1]
                            associatedMainAppClassNameOnly = mainAppFileName.replace('.java', '')
                            associatedAppClassOutputDir = "${env.OUTPUT_DIR}\\${associatedMainAppClassNameOnly}_classes".replace('/', '\\')
                            echo "  INFO: Linked module '${moduleName}' to app output '${associatedAppClassOutputDir}' via main file '${mainAppFileName}'."
                        } else {
                            echo "  WARNING: Could not find a main app file in '${expectedMainSrcPrefix}' for module '${moduleName}'. App classes might be missing from test classpath."
                        }

                        def moduleTestClassesDir = "${testClassesDirBase.replace('/', '\\')}\\${moduleName}_tests"
                        def moduleTestReportsDir = "${testReportsDirBase.replace('/', '\\')}\\${moduleName}"
                        bat "if not exist \"${moduleTestClassesDir}\" mkdir \"${moduleTestClassesDir}\""
                        bat "if not exist \"${moduleTestReportsDir}\" mkdir \"${moduleTestReportsDir}\""

                        def currentModuleClasspath = new ArrayList(commonTestClasspathParts)
                        if (associatedAppClassOutputDir && fileExists(associatedAppClassOutputDir)) {
                            currentModuleClasspath.add(associatedAppClassOutputDir)
                        }
                        def classpathStringForCompile = currentModuleClasspath.join(File.pathSeparator)
                        def testFilesToCompile = testFilePaths.collect { "\"${it.replace('/','\\')}\"" }.join(" ")

                        echo "  Compiling test files for module ${moduleName}..."
                        def compileTestCommand = "javac -encoding UTF-8 -d \"${moduleTestClassesDir}\" -sourcepath \"${testSrcJavaDir}\" -cp \"${classpathStringForCompile}\" ${testFilesToCompile}"
                        try {
                            bat compileTestCommand
                            echo "  Test compilation successful for ${moduleName}."
                        } catch (e) {
                            error "Test compilation failed for module ${moduleName}. Error: ${e.getMessage()}"
                        }

                        def classpathForJUnit = new ArrayList(currentModuleClasspath)
                        classpathForJUnit.add(moduleTestClassesDir)
                        def junitClasspathString = classpathForJUnit.unique().join(File.pathSeparator)

                        echo "  Running tests for module ${moduleName} (Reports: ${moduleTestReportsDir})..."
                        def junitCommand = "java -jar \"${junitConsoleJar.replace('/', '\\')}\" --scan-classpath --classpath \"${junitClasspathString}\" --reports-dir \"${moduleTestReportsDir}\""
                        try {
                            bat junitCommand
                            echo "  JUnit tests execution completed for ${moduleName}."
                        } catch (e) { 
                            echo "  ERROR: JUnit execution for module ${moduleName} failed with non-zero exit code. Error: ${e.getMessage()}"
                            hasExecutionErrors = true 
                        }
                    }

                    if (hasExecutionErrors) {
                        currentBuild.result = 'UNSTABLE' 
                        echo "One or more test modules encountered execution errors."
                    }
                    echo "--- Finished Unit Test Stage ---"
                }
            }
            post {
                always {
                    script {
                        echo "Archiving JUnit test reports..."
                        junit '**/test-reports/**/*.xml'
                    }
                }
            }
        }
        
        stage('Create Release Package') {
            steps {
                script {
                    bat "if exist \"${TEMP_RELEASE_STAGING_DIR}\" rmdir /s /q \"${TEMP_RELEASE_STAGING_DIR}\""
                    bat "mkdir \"${TEMP_RELEASE_STAGING_DIR}\""
                    bat "mkdir \"${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\"" 

                    def quotedMainHubJarNameForBat = "\"${MAIN_HUB_JAR_NAME}\"" 
                    def mainHubJarSourcePathInOutput = "${OUTPUT_DIR}\\${MAIN_HUB_JAR_NAME}" 

                    if (fileExists(mainHubJarSourcePathInOutput)) {
                        echo "Copying main hub JAR: ${mainHubJarSourcePathInOutput} to ${TEMP_RELEASE_STAGING_DIR}\\${MAIN_HUB_JAR_NAME}"
                        bat "copy \"${mainHubJarSourcePathInOutput}\" \"${TEMP_RELEASE_STAGING_DIR}\\${quotedMainHubJarNameForBat}\""
                    } else if (MAIN_HUB_JAR_NAME == "AppMainRoot.jar" && fileExists(MAIN_HUB_JAR_NAME)) { 
                         echo "Copying root AppMainRoot.jar (as Main Hub): ${MAIN_HUB_JAR_NAME} to ${TEMP_RELEASE_STAGING_DIR}\\${MAIN_HUB_JAR_NAME}"
                         bat "copy \"${MAIN_HUB_JAR_NAME}\" \"${TEMP_RELEASE_STAGING_DIR}\\${quotedMainHubJarNameForBat}\""
                    } else {
                       echo "WARNING: Main hub JAR ${MAIN_HUB_JAR_NAME} not found in ${OUTPUT_DIR} or workspace root. It will be missing from the release ZIP root."
                    }
                    
                    echo "Copying other JARs from ${OUTPUT_DIR} to ${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\\"
                    bat """
                        for %%f in ("${OUTPUT_DIR}\\*.jar") do (
                            if /I not "%%~nxf"==${quotedMainHubJarNameForBat} (
                                copy "%%f" "${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\\"
                            ) else (
                                echo "Skipping %%~nxf as it's the main hub JAR ('${MAIN_HUB_JAR_NAME}') and already copied to root."
                            )
                        )
                    """
                    
                    def appMainRootJarName = "AppMainRoot.jar"
                    def quotedAppMainRootJarNameForBat = "\"${appMainRootJarName}\""
                    if (appMainRootJarName != MAIN_HUB_JAR_NAME) { 
                        if (fileExists(appMainRootJarName)) { 
                             echo "Copying specific root JAR ${appMainRootJarName} to ${TEMP_RELEASE_STAGING_DIR}\\${appMainRootJarName}"
                            bat "copy \"${appMainRootJarName}\" \"${TEMP_RELEASE_STAGING_DIR}\\${quotedAppMainRootJarNameForBat}\""
                        } else if (fileExists("${OUTPUT_DIR}\\${appMainRootJarName}")) { 
                            echo "Copying specific root JAR ${OUTPUT_DIR}\\${appMainRootJarName} to ${TEMP_RELEASE_STAGING_DIR}\\${appMainRootJarName}"
                            bat "copy \"${OUTPUT_DIR}\\${appMainRootJarName}\" \"${TEMP_RELEASE_STAGING_DIR}\\${quotedAppMainRootJarNameForBat}\""
                            bat "if exist \"${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\\${appMainRootJarName}\" del \"${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\\${quotedAppMainRootJarNameForBat}\""
                        } else {
                            echo "Warning: Specific root JAR ${appMainRootJarName} not found."
                        }
                    }
                    echo "Contents of staging directory ${TEMP_RELEASE_STAGING_DIR}:"
                    bat "dir /s \"${TEMP_RELEASE_STAGING_DIR}\""
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

                    def tempStagingPathForPS = "${env.WORKSPACE}\\${TEMP_RELEASE_STAGING_DIR}".replace('/', '\\')
                    echo "Creating archive: ${zipFilePath} from directory ${tempStagingPathForPS}"
                    bat "powershell -NoProfile -NonInteractive Compress-Archive -Path \"${tempStagingPathForPS}\\*\" -DestinationPath \"${zipFilePath}\" -Force"

                    if (!fileExists(zipFilePath)) {
                        error "Failed to create release ZIP file: ${zipFilePath}"
                    }
                    
                    withCredentials([string(credentialsId: "${GITHUB_CREDS}", variable: 'GH_TOKEN')]) {
                        def psPublishScriptContent = """
                            \$ErrorActionPreference = 'Stop'
                            \$ProgressPreference = 'SilentlyContinue'

                            \$ghToken = "${GH_TOKEN}" 
                            \$repo = "${GITHUB_REPO}"
                            \$tag_name = "${tag}"
                            \$release_name = "${tag}"
                            \$release_body = @"
${message.replace("`", "``").replace('"', '""').replace("\$", "`\$")}
"@.Trim() 
                            \$draft_status = \$false
                            \$prerelease_status = \$false

                            \$zipFilePathForPS = @"
${zipFilePath.replace('\\', '\\\\')}
"@.Trim()
                            \$zipFileNameForPS = "${zipFileName}" 
                            
                            \$releaseBodyHashtable = @{
                                tag_name   = \$tag_name
                                name       = \$release_name
                                body       = \$release_body
                                draft      = \$draft_status
                                prerelease = \$prerelease_status
                            }
                            \$releaseDataJsonForApi = \$releaseBodyHashtable | ConvertTo-Json -Depth 5 

                            Write-Host "DEBUG: Repo: \$repo"
                            Write-Host "DEBUG: Release Data JSON for API: \$releaseDataJsonForApi"
                            Write-Host "DEBUG: Zip File Path for PS (local path to upload): \$zipFilePathForPS"
                            Write-Host "DEBUG: Zip File Name for PS (asset name on GitHub): \$zipFileNameForPS"
                            
                            Write-Host "Creating GitHub release..."
                            \$headers = @{
                                "Accept"               = "application/vnd.github+json"
                                "Authorization"        = "Bearer \$ghToken"
                                "X-GitHub-Api-Version" = "2022-11-28"
                            }
                            
                            try {
                                \$releaseResponse = Invoke-RestMethod -Uri "https://api.github.com/repos/\$repo/releases" -Method Post -Headers \$headers -Body \$releaseDataJsonForApi -ContentType "application/json"
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
                            
                            Write-Host ("Formatted Upload URL for \${zipFileNameForPS}: " + \$finalUploadUrl) 

                            Write-Host "Uploading asset: \$zipFilePathForPS to \$finalUploadUrl"
                            
                            \$uploadHeaders = @{
                                "Authorization"        = "Bearer \$ghToken"
                                "X-GitHub-Api-Version" = "2022-11-28"
                                "Content-Type"         = "application/zip" 
                            }

                            try {
                                Invoke-RestMethod -Uri \$finalUploadUrl -Method Post -Headers \$uploadHeaders -InFile \$zipFilePathForPS
                                Write-Host "Successfully uploaded \$zipFileNameForPS to release \$tag_name."
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
