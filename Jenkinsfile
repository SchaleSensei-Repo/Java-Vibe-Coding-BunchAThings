pipeline {
    agent any

    triggers {
        pollSCM('H/10 * * * *')
    }

    environment {
        OUTPUT_DIR = 'out'
        RELEASE_PACKAGE_DIR = 'release_package'
        TEMP_RELEASE_STAGING_DIR = 'temp_release_staging'
        MAIN_HUB_JAR_NAME = 'Jar_Main_Hub.jar'
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

                    // PowerShell script to find ALL Java files grouped by module, and also identify main files
                    def psScriptContent = '''
                        $ProgressPreference = 'SilentlyContinue';
                        $WarningPreference = 'SilentlyContinue';
                        $VerbosePreference = 'SilentlyContinue';
                        $InformationPreference = 'SilentlyContinue';
                        $ErrorActionPreference = 'Stop';

                        $WorkspacePath = $env:WORKSPACE

                        # Determine module roots. Assumes modules are subdirectories under a 'source' directory,
                        # or direct subdirectories of workspace if 'source' doesn't exist and they contain 'src'.
                        $potentialModuleRoots = @()
                        $sourceDirCandidate = Join-Path $WorkspacePath "source"
                        if (Test-Path $sourceDirCandidate -PathType Container) {
                            Get-ChildItem -Path $sourceDirCandidate -Directory | ForEach-Object { $potentialModuleRoots += $_.FullName }
                        } else {
                            Get-ChildItem -Path $WorkspacePath -Directory | Where-Object { Test-Path (Join-Path $_.FullName "src") -PathType Container } | ForEach-Object { $potentialModuleRoots += $_.FullName }
                        }

                        $moduleRoots = $potentialModuleRoots | ForEach-Object {
                            $moduleRootPath = $_
                            # A directory is a module root if it contains src/main/java, src/java, or src with .java files
                            if (Test-Path (Join-Path $moduleRootPath "src\\main\\java") -PathType Container -or `
                                Test-Path (Join-Path $moduleRootPath "src\\java") -PathType Container -or `
                                (Test-Path (Join-Path $moduleRootPath "src") -PathType Container -and (Get-ChildItem (Join-Path $moduleRootPath "src") -Recurse -Filter *.java -ErrorAction SilentlyContinue).Count -gt 0) ) {
                                $moduleRootPath
                            }
                        } | Get-Unique

                        $javaFilesByModule = @{}
                        Write-Host "DEBUG [BuildApps PS]: Found module roots: $($moduleRoots -join '; ')"

                        foreach ($rootPathAbs in $moduleRoots) {
                            $allJavaFilesInModule = @()
                            $srcMainJavaPath = Join-Path -Path $rootPathAbs -ChildPath 'src\\main\\java'
                            $srcJavaPath = Join-Path -Path $rootPathAbs -ChildPath 'src\\java'
                            $srcPath = Join-Path -Path $rootPathAbs -ChildPath 'src'

                            if (Test-Path $srcMainJavaPath -PathType Container) {
                                $allJavaFilesInModule += Get-ChildItem -Path $srcMainJavaPath -Recurse -Filter *.java -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
                            } elseif (Test-Path $srcJavaPath -PathType Container) {
                                $allJavaFilesInModule += Get-ChildItem -Path $srcJavaPath -Recurse -Filter *.java -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
                            } elseif (Test-Path $srcPath -PathType Container) {
                                $allJavaFilesInModule += Get-ChildItem -Path $srcPath -Recurse -Filter *.java -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
                            }

                            if ($allJavaFilesInModule.Count -gt 0) {
                                $relativeRoot = $rootPathAbs.Substring($WorkspacePath.Length).TrimStart('\\').TrimStart('/');
                                $javaFilesByModule[$relativeRoot] = @($allJavaFilesInModule | Get-Unique)
                                Write-Host "DEBUG [BuildApps PS]: Module '$relativeRoot' identified with $($allJavaFilesInModule.Count) java files."
                            } else {
                                Write-Host "DEBUG [BuildApps PS]: Module '$($rootPathAbs.Substring($WorkspacePath.Length).TrimStart('\\').TrimStart('/'))' has no java files found in expected src locations (src/main/java, src/java, or src/)."
                            }
                        }

                        # Find main files
                        $mainJavaFilePaths = Get-ChildItem -Path $WorkspacePath -Recurse -Filter *.java -ErrorAction SilentlyContinue |
                            Where-Object { Select-String -Path $_.FullName -Pattern 'public static void main' -Quiet -ErrorAction SilentlyContinue } |
                            ForEach-Object { $_.FullName };
                        if ($null -ne $mainJavaFilePaths -and $mainJavaFilePaths.Count -gt 0) {
                            [System.IO.File]::WriteAllLines('main_java_files.txt', $mainJavaFilePaths, [System.Text.UTF8Encoding]::new($false))
                            Write-Host "DEBUG [BuildApps PS]: Found $($mainJavaFilePaths.Count) main method files. List saved to main_java_files.txt"
                        } else {
                            Write-Host 'DEBUG [BuildApps PS]: No Java files with a main method were found.';
                            Set-Content -Path 'main_java_files.txt' -Value ''
                        }
                        return $javaFilesByModule | ConvertTo-Json -Depth 5
                    '''.stripIndent()

                    byte[] scriptBytes = psScriptContent.getBytes("UTF-16LE")
                    def encodedCommand = scriptBytes.encodeBase64().toString()

                    def psCommandToExecute = "powershell -NoProfile -NonInteractive -EncodedCommand ${encodedCommand} 2" + ">" + "\$null 3" + ">" + "\$null 4" + ">" + "\$null 5" + ">" + "\$null 6" + ">" + "\$null 7" + ">" + "\$null"
                    def psOutputJson = bat(script: psCommandToExecute, returnStdout: true).trim()
                    echo "DEBUG: Raw psOutputJson from PowerShell (Build Apps): '${psOutputJson}'"

                    def jsonStart = psOutputJson.indexOf('{')
                    def jsonEnd = psOutputJson.lastIndexOf('}')
                    def allJavaFilesByModule = [:]
                    if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                        def cleanJsonString = psOutputJson.substring(jsonStart, jsonEnd + 1)
                        echo "DEBUG: cleanJsonString to be parsed (Build Apps): '${cleanJsonString}'"
                        try {
                            allJavaFilesByModule = readJSON(text: cleanJsonString)
                        } catch (e) {
                            echo "ERROR: Failed to parse JSON from PowerShell (Build Apps): ${e.getMessage()}. JSON was: '${cleanJsonString}'"
                            allJavaFilesByModule = [:] // Ensure it's an empty map
                        }
                    } else {
                         echo "WARNING: Could not extract JSON from PowerShell output (Build Apps): '${psOutputJson}'. Module-aware compilation might fail."
                    }
                    if (allJavaFilesByModule.isEmpty()){
                        echo "WARNING: No modules with Java files were identified by PowerShell. Compilation will likely fail if main files exist."
                    }

                    def mainFileContent = readFile(file: 'main_java_files.txt', encoding: 'UTF-8').trim()
                    if (mainFileContent.isEmpty()) {
                        error "No Java files with a main method were found (main_java_files.txt is empty)."
                    }
                    def mainMethodFiles = mainFileContent.split("\\r?\\n").collect { it.trim() }.findAll { it }
                    if (mainMethodFiles.isEmpty()) {
                        error "No Java files with a main method were found after processing main_java_files.txt."
                    }

                    def rootJarApps = ['AppMainRoot'] // This seems to be for a specific app, adjust if Jar_Main_Hub should be root
                    def dependencyJars = []
                    def workspacePath = env.WORKSPACE.replace('\\','/') // Use forward slashes for Groovy logic
                    def absoluteLibsDirPath = "${workspacePath}/${env.LIBS_DIR_PATH}".replace('/', File.separator)

                    if (env.LIBS_DIR_PATH) {
                       def libsDir = new File(absoluteLibsDirPath)
                       if (libsDir.isDirectory()) {
                           File[] filesInLibsDir = libsDir.listFiles()
                           if (filesInLibsDir != null) {
                               for (File f : filesInLibsDir) {
                                   if (f.isFile() && f.getName().toLowerCase().endsWith(".jar")) {
                                       dependencyJars.add(f.getAbsolutePath().replace('/', '\\'))
                                   }
                               }
                           }
                       }
                    }
                    def commonExternalClasspath = dependencyJars.join(File.pathSeparator)
                    echo "DEBUG: Common external classpath: ${commonExternalClasspath}"

                    // Phase 1: Compile all modules
                    def moduleCompilationJobs = [:]
                    allJavaFilesByModule.each { moduleRelPath, moduleJavaFilesList ->
                        if (moduleJavaFilesList.isEmpty()) {
                            echo "Skipping empty module: ${moduleRelPath}"
                            return // continue to next module
                        }
                        def moduleName = moduleRelPath.tokenize('\\/')[-1]
                        def moduleClassOutputDir = "${OUTPUT_DIR}/${moduleName}_classes".replace('/', '\\')
                        
                        // Determine source path for this module (e.g., source/myModule/src/main/java or source/myModule/src)
                        def moduleWorkspaceAbsPath = "${workspacePath}/${moduleRelPath}".replace('/', File.separator)
                        def srcMainJava = new File("${moduleWorkspaceAbsPath}${File.separator}src${File.separator}main${File.separator}java")
                        def srcJava = new File("${moduleWorkspaceAbsPath}${File.separator}src${File.separator}java")
                        def srcDir = new File("${moduleWorkspaceAbsPath}${File.separator}src")
                        String actualModuleSourcePathForJavac = ""
                        if (srcMainJava.isDirectory()) {
                            actualModuleSourcePathForJavac = srcMainJava.getAbsolutePath()
                        } else if (srcJava.isDirectory()) {
                            actualModuleSourcePathForJavac = srcJava.getAbsolutePath()
                        } else if (srcDir.isDirectory()) {
                            actualModuleSourcePathForJavac = srcDir.getAbsolutePath()
                        } else {
                            echo "ERROR: Could not determine source directory for module ${moduleName} at ${moduleRelPath}. Using module root as fallback."
                            actualModuleSourcePathForJavac = moduleWorkspaceAbsPath // Fallback, might not be ideal
                        }
                        actualModuleSourcePathForJavac = actualModuleSourcePathForJavac.replace('/', '\\')


                        moduleCompilationJobs[moduleName] = {
                            echo "--- Compiling Module: ${moduleName} ---"
                            echo "  Source Path for javac: ${actualModuleSourcePathForJavac}"
                            echo "  Output Dir: ${moduleClassOutputDir}"
                            bat "if not exist \"${moduleClassOutputDir}\" mkdir \"${moduleClassOutputDir}\""
                            
                            def currentModuleCompileClasspathParts = new ArrayList(dependencyJars)
                            // Add other compiled module outputs to classpath - this assumes a flat dependency or that they get compiled in time
                            allJavaFilesByModule.keySet().each { otherModuleKey ->
                                if (otherModuleKey != moduleRelPath) {
                                    def otherModName = otherModuleKey.tokenize('\\/')[-1]
                                    def otherModOutputDir = "${OUTPUT_DIR}/${otherModName}_classes".replace('/', '\\')
                                    // Check if dir exists - it might not if parallel execution order is not guaranteed for dependencies
                                    // For simplicity, we add it. `javac` will ignore non-existent classpath entries.
                                    currentModuleCompileClasspathParts.add(otherModOutputDir)
                                }
                            }
                            def currentModuleCompileCp = currentModuleCompileClasspathParts.unique().join(File.pathSeparator)
                            String currentModuleCpOpt = currentModuleCompileCp.isEmpty() ? "" : "-cp \"${currentModuleCompileCp}\""

                            def filesToCompile = moduleJavaFilesList.collect { "\"${it.replace('/','\\')}\"" }.join(" ")
                            def compileCmd = "javac -encoding UTF-8 ${currentModuleCpOpt} -d \"${moduleClassOutputDir}\" -sourcepath \"${actualModuleSourcePathForJavac}\" ${filesToCompile}"
                            try {
                                echo "  Compile CMD for module ${moduleName}: ${compileCmd}"
                                bat compileCmd
                                echo "  Module ${moduleName} compiled successfully."
                            } catch (e) {
                                error("Compilation failed for module ${moduleName}: ${e.getMessage()}. Command was: ${compileCmd}")
                            }
                        }
                    }
                    if (!moduleCompilationJobs.isEmpty()) {
                        try {
                            parallel moduleCompilationJobs
                        } catch (org.jenkinsci.plugins.workflow.cps.CpsCompilationErrorsException e) {
                            error "Error setting up parallel compilation: ${e.getMessage()}"
                        }
                    } else {
                        echo "No modules found to compile."
                    }

                    // Phase 2: Create JARs for main classes
                    def jarCreationJobs = [:]
                    mainMethodFiles.each { fullMainFilePath ->
                        String groovyMainFilePath = fullMainFilePath.replace('\\', '/') // C:/Jenkins/.../source/module/src/main/java/com/pack/Main.java
                        def mainClassNameOnly = groovyMainFilePath.tokenize('/')[-1].replace('.java', '')

                        String mainFileModuleRelPath = allJavaFilesByModule.find { moduleRelP, fileList ->
                            fileList.any { it.replace('\\','/') == groovyMainFilePath }
                        }?.key

                        if (!mainFileModuleRelPath) {
                            echo "WARNING: Could not determine module for main file ${fullMainFilePath}. Skipping JAR creation."
                            return // continue to next mainMethodFile
                        }
                        def mainFileModuleName = mainFileModuleRelPath.tokenize('\\/')[-1]
                        def mainFileModuleClassOutputDir = "${OUTPUT_DIR}/${mainFileModuleName}_classes".replace('/', '\\')

                        // FQCN Derivation
                        String fqcn = mainClassNameOnly
                        String mainFileModuleWorkspaceAbsPath = "${workspacePath}/${mainFileModuleRelPath}".replace('/', File.separator)
                        String mainFileSrcMainJavaPath = new File("${mainFileModuleWorkspaceAbsPath}${File.separator}src${File.separator}main${File.separator}java").getAbsolutePath().replace('\\','/')
                        String mainFileSrcJavaPath = new File("${mainFileModuleWorkspaceAbsPath}${File.separator}src${File.separator}java").getAbsolutePath().replace('\\','/')
                        String mainFileSrcPath = new File("${mainFileModuleWorkspaceAbsPath}${File.separator}src").getAbsolutePath().replace('\\','/')
                        
                        String packagePathPart = ""
                        if (groovyMainFilePath.startsWith(mainFileSrcMainJavaPath + "/")) {
                            packagePathPart = groovyMainFilePath.substring((mainFileSrcMainJavaPath + "/").length())
                        } else if (groovyMainFilePath.startsWith(mainFileSrcJavaPath + "/")) {
                            packagePathPart = groovyMainFilePath.substring((mainFileSrcJavaPath + "/").length())
                        } else if (groovyMainFilePath.startsWith(mainFileSrcPath + "/")) {
                            packagePathPart = groovyMainFilePath.substring((mainFileSrcPath + "/").length())
                        } else {
                             // Fallback: try to find common path part if main file is not in a standard src layout
                            String commonPath = mainFileModuleRelPath.replace('\\','/')
                            if (groovyMainFilePath.startsWith(workspacePath + "/" + commonPath + "/")) {
                                packagePathPart = groovyMainFilePath.substring((workspacePath + "/" + commonPath + "/").length())
                                // Further strip common prefixes like 'java/' if they are not part of package
                                if (packagePathPart.startsWith("java/")) packagePathPart = packagePathPart.substring("java/".length())
                            }
                        }
                        
                        if (packagePathPart.contains('/')) {
                            fqcn = packagePathPart.substring(0, packagePathPart.lastIndexOf('/')).replace('/', '.') + "." + mainClassNameOnly
                        }
                        // End FQCN derivation

                        jarCreationJobs[mainClassNameOnly] = {
                            echo "--- Creating JAR for: ${mainClassNameOnly} (from module ${mainFileModuleName}) ---"
                            echo "  FQCN for JAR: ${fqcn}"
                            echo "  Using classes from: ${mainFileModuleClassOutputDir}"
                            def jarName = "${mainClassNameOnly}.jar"
                            // Special handling for MAIN_HUB_JAR_NAME
                            if (mainClassNameOnly == MAIN_HUB_JAR_NAME.replace(".jar","") || mainClassNameOnly == "Jar_Main_Hub") { // Be flexible
                                jarName = MAIN_HUB_JAR_NAME
                            }
                            def jarPathRelative = "${OUTPUT_DIR}\\${jarName}".replace('/', '\\')
                            if (rootJarApps.contains(mainClassNameOnly) && jarName != MAIN_HUB_JAR_NAME) { // If it's a root app AND not the main hub already handled
                                jarPathRelative = jarName.replace('/', '\\') // Place in workspace root
                            } else if (jarName == MAIN_HUB_JAR_NAME) {
                                // If it's the main hub, it might have specific placement rules or be handled by TEMP_RELEASE_STAGING_DIR logic later
                                // For now, ensure it's created. If MAIN_HUB_JAR_NAME is in rootJarApps, it's covered.
                                // If Jar_Main_Hub.jar is expected in root, ensure rootJarApps or MAIN_HUB_JAR_NAME logic covers it.
                                // The current MAIN_HUB_JAR_NAME is 'Jar_Main_Hub.jar'.
                                // If classNameOnly is 'Jar_Main_Hub', jarName becomes 'Jar_Main_Hub.jar'.
                                // If 'Jar_Main_Hub' is in rootJarApps, it will be placed in root.
                                // Let's assume MAIN_HUB_JAR_NAME should be placed in root if it's the main hub.
                                if (jarName == MAIN_HUB_JAR_NAME) {
                                     jarPathRelative = jarName.replace('/', '\\') // Place main hub in workspace root
                                     echo "INFO: ${jarName} is the designated Main Hub JAR, will be placed in workspace root."
                                }
                            }
                            
                            if (!fileExists(mainFileModuleClassOutputDir)) {
                                error "Output directory ${mainFileModuleClassOutputDir} does not exist for JARing ${mainClassNameOnly}. Module compilation likely failed."
                            }

                            def jarCommand = "jar cfe \"${jarPathRelative}\" ${fqcn} -C \"${mainFileModuleClassOutputDir}\" ."
                            try {
                                echo "  JAR CMD: ${jarCommand}"
                                bat jarCommand
                                echo "  JAR creation successful for ${mainClassNameOnly}."
                            } catch (e) {
                                error("JAR creation failed for ${mainClassNameOnly}: ${e.getMessage()}")
                            }
                        }
                    }
                    if (!jarCreationJobs.isEmpty()) {
                         try {
                            parallel jarCreationJobs
                        } catch (org.jenkinsci.plugins.workflow.cps.CpsCompilationErrorsException e) {
                            error "Error setting up parallel JAR creation: ${e.getMessage()}"
                        }
                    } else {
                        echo "No main method files found to JAR."
                    }

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

                    def workspacePath = env.WORKSPACE.replace('\\', '/') 
                    def libsDirPath = env.LIBS_DIR_PATH
                    def absoluteLibsDirPath = "${workspacePath}/${libsDirPath}".replace('/', File.separator)

                    def dependencyJars = []
                    def libsDir = new File(absoluteLibsDirPath)
                    if (libsDir.isDirectory()) {
                        File[] filesInLibsDir = libsDir.listFiles()
                        if (filesInLibsDir != null) {
                            for (File f : filesInLibsDir) {
                                if (f.isFile() && f.name.toLowerCase().endsWith(".jar")) {
                                    dependencyJars.add(f.getAbsolutePath().replace('/', '\\'))
                                }
                            }
                        }
                    }
                    def commonTestClasspathParts = new ArrayList(dependencyJars)

                    def junitConsoleJar = dependencyJars.find { it.contains("junit-platform-console-standalone") }
                    if (!junitConsoleJar) {
                        error "JUnit Platform Console Standalone JAR not found in ${libsDirPath}. Cannot run tests."
                    }
                    echo "Using JUnit Runner: ${junitConsoleJar.replace('/', '\\')}"

                    def mainJavaFilesTxt = 'main_java_files.txt' // Still needed to find main app classes for classpath
                    if (!fileExists(mainJavaFilesTxt)) {
                        error "File '${mainJavaFilesTxt}' not found. Was 'Build Apps' stage successful?"
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
                                    $relativeRoot = $rootPathAbs.Substring($WorkspacePath.Length).TrimStart('\\').TrimStart('/');
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
                    echo "DEBUG: Raw psOutputJson from PowerShell (Unit Test): '${psOutputJson}'"

                    if (psOutputJson.isEmpty() || psOutputJson == "{}") {
                        echo "No test modules with src/test/java/*.java files found. Skipping test execution."
                        echo "Check 'powershell_test_discovery_debug.log' in workspace for details."
                        return
                    }
                    if (psOutputJson == null || psOutputJson.trim().isEmpty()) {
                        error("PowerShell script for test discovery returned empty or null. Check 'powershell_test_discovery_debug.log'.")
                    }

                    def jsonTestStart = psOutputJson.indexOf('{')
                    def jsonTestEnd = psOutputJson.lastIndexOf('}')
                    def cleanTestJsonString = ""
                    if (jsonTestStart != -1 && jsonTestEnd != -1 && jsonTestEnd > jsonTestStart) {
                        cleanTestJsonString = psOutputJson.substring(jsonTestStart, jsonTestEnd + 1)
                        echo "DEBUG: cleanTestJsonString to be parsed (Unit Test): '${cleanTestJsonString}'"
                    } else {
                         error("Could not extract JSON from PowerShell output (Unit Test): '${psOutputJson}'. Check 'powershell_test_discovery_debug.log'.")
                    }
                    def testModulesData = readJSON(text: cleanTestJsonString)

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
                        def associatedAppClassOutputDir = null // This is the key: out/ModuleName_classes

                        // Find the main application classes directory for this test module
                        // The moduleName derived from test discovery (e.g., "main_hub") should match the moduleName used in "Build Apps"
                        associatedAppClassOutputDir = "${env.OUTPUT_DIR}\\${moduleName}_classes".replace('/', '\\')
                        if (fileExists(associatedAppClassOutputDir)) {
                            echo "  INFO: Linked test module '${moduleName}' to app output '${associatedAppClassOutputDir}'."
                        } else {
                            echo "  WARNING: Main app class output directory '${associatedAppClassOutputDir}' not found for test module '${moduleName}'. Test compilation might fail."
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
                        def compileTestCommand = "javac -encoding UTF-8 -d \"${moduleTestClassesDir}\" -sourcepath \"${testSrcJavaDir.replace('/','\\')}\" -cp \"${classpathStringForCompile}\" ${testFilesToCompile}"
                        try {
                            echo "    Compile CMD: ${compileTestCommand}"
                            bat compileTestCommand
                            echo "  Test compilation successful for ${moduleName}."
                        } catch (e) {
                            error "Test compilation failed for module ${moduleName}. Error: ${e.getMessage()}. Command: ${compileTestCommand}"
                        }

                        def classpathForJUnit = new ArrayList(currentModuleClasspath)
                        classpathForJUnit.add(moduleTestClassesDir) // Add compiled test classes
                        def junitClasspathString = classpathForJUnit.unique().join(File.pathSeparator)

                        echo "  Running tests for module ${moduleName} (Reports: ${moduleTestReportsDir})..."
                        def junitCommand = "java -jar \"${junitConsoleJar.replace('/', '\\')}\" --scan-classpath --classpath \"${junitClasspathString}\" --reports-dir \"${moduleTestReportsDir}\""
                        try {
                            echo "    JUnit CMD: ${junitCommand}"
                            bat junitCommand
                            echo "  JUnit tests execution completed for ${moduleName}."
                        } catch (e) {
                            echo "  ERROR: JUnit execution for module ${moduleName} failed. Error: ${e.getMessage()}"
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
                    // Check if main hub JAR is in workspace root (if it was a rootJarApp) or in OUTPUT_DIR
                    def mainHubJarSourcePath = MAIN_HUB_JAR_NAME // Assume in root first
                    if (!fileExists(mainHubJarSourcePath)) {
                        mainHubJarSourcePath = "${OUTPUT_DIR}\\${MAIN_HUB_JAR_NAME}" // Check in OUTPUT_DIR
                    }


                    if (fileExists(mainHubJarSourcePath)) {
                        echo "Copying main hub JAR: ${mainHubJarSourcePath} to ${TEMP_RELEASE_STAGING_DIR}\\${MAIN_HUB_JAR_NAME}"
                        bat "copy \"${mainHubJarSourcePath.replace('/','\\')}\" \"${TEMP_RELEASE_STAGING_DIR}\\${quotedMainHubJarNameForBat}\""
                    } else {
                       echo "WARNING: Main hub JAR ${MAIN_HUB_JAR_NAME} not found in workspace root or ${OUTPUT_DIR}. It will be missing from the release ZIP root."
                    }

                    echo "Copying other JARs from ${OUTPUT_DIR} to ${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\\"
                    bat """
                        for %%f in ("${OUTPUT_DIR}\\*.jar") do (
                            if /I not "%%~nxf"==${quotedMainHubJarNameForBat} (
                                copy "%%f" "${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\\"
                            ) else if not "%%f"=="${mainHubJarSourcePath.replace('/','\\')}" (
                                copy "%%f" "${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\\"
                            ) else (
                                echo "Skipping %%~nxf as it's the main hub JAR ('${MAIN_HUB_JAR_NAME}') and already copied to root or handled."
                            )
                        )
                    """
                    
                    // Handle AppMainRoot.jar if it's different from MAIN_HUB_JAR_NAME and was a rootJarApp
                    def appMainRootJarName = "AppMainRoot.jar"
                    def quotedAppMainRootJarNameForBat = "\"${appMainRootJarName}\""
                    if (appMainRootJarName != MAIN_HUB_JAR_NAME) {
                        def appMainRootSourcePath = appMainRootJarName // Assume in root
                        if (!fileExists(appMainRootSourcePath)) {
                            appMainRootSourcePath = "${OUTPUT_DIR}\\${appMainRootJarName}" // Check in OUTPUT_DIR
                        }

                        if (fileExists(appMainRootSourcePath)) {
                             echo "Copying specific root JAR ${appMainRootSourcePath} to ${TEMP_RELEASE_STAGING_DIR}\\${appMainRootJarName}"
                            bat "copy \"${appMainRootSourcePath.replace('/','\\')}\" \"${TEMP_RELEASE_STAGING_DIR}\\${quotedAppMainRootJarNameForBat}\""
                            // If it was copied from OUTPUT_DIR, remove it from the subdirectory in staging to avoid duplication
                            if (appMainRootSourcePath.startsWith(OUTPUT_DIR)) {
                                bat "if exist \"${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\\${appMainRootJarName}\" del \"${TEMP_RELEASE_STAGING_DIR}\\${OUTPUT_DIR}\\${quotedAppMainRootJarNameForBat}\""
                            }
                        } else {
                            echo "Warning: Specific root JAR ${appMainRootJarName} not found in workspace root or ${OUTPUT_DIR}."
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
