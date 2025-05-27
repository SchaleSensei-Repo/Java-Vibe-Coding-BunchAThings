pipeline {
    agent any

    triggers {
        pollSCM('H/10 * * * *') // Poll Git every 10 mins
    }

    environment {
        OUTPUT_DIR = 'out'
        RELEASE_PACKAGE_DIR = 'release_package'
        GITHUB_REPO = 'SchaleSensei-Repo/Java-Vibe-Coding-BunchAThings' // Format: "owner/repo"
        GITHUB_CREDS = 'GITHUB_PAT'         // Credential ID for GitHub PAT
        RELEASES_TO_KEEP = 3
        EMAIL_RECIPIENTS = 'ashlovedawn@gmail.com'
        LIBS_DIR_PATH = 'libs' // Relative path within the workspace
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
                    // Paths for rmdir/mkdir are relative to workspace, which is fine for bat steps
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
                    
                    echo "Listing workspace root contents (use 'ls -la' for Linux agents if 'dir' fails):"
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
                    // ** FIX: Construct absolute path to libs directory using env.WORKSPACE **
                    def workspacePath = env.WORKSPACE 
                    def absoluteLibsDirPath = "${workspacePath}/${env.LIBS_DIR_PATH}".replace('/', File.separator) // Ensure OS-specific separator

                    echo "DEBUG: Workspace path: '${workspacePath}'"
                    echo "DEBUG: LIBS_DIR_PATH from environment: '${env.LIBS_DIR_PATH}'"
                    echo "DEBUG: Calculated absolute path for libs directory: '${absoluteLibsDirPath}'"

                    if (env.LIBS_DIR_PATH) {
                       def libsDir = new File(absoluteLibsDirPath) 
                       
                       echo "DEBUG: libsDir object refers to path: '${libsDir.getPath()}'"
                       echo "DEBUG: Absolute path for libsDir (from getAbsolutePath()): '${libsDir.getAbsolutePath()}'"
                       echo "DEBUG: Does libsDir exist? ${libsDir.exists()}"
                       echo "DEBUG: Is libsDir a directory? ${libsDir.isDirectory()}"
                       echo "DEBUG: Is libsDir a file? ${libsDir.isFile()}"

                       if (libsDir.isDirectory()) { 
                           echo "SUCCESS: '${absoluteLibsDirPath}' is a directory. Scanning for JARs..."
                           libsDir.eachFile { f -> 
                               if (f.name.endsWith(".jar")) {
                                   dependencyJars.add(f.getAbsolutePath().replace('/', '\\')) 
                                   echo "DEBUG: Found dependency JAR: ${f.getAbsolutePath()}"
                               }
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
                        echo "WARNING: No external dependency JARs found. Compilation will likely fail. Check the 'libs' directory ('${absoluteLibsDirPath}') and its contents."
                    }
                    def commonClassPath = dependencyJars.join(File.pathSeparator)
                    String classPathOpt = commonClassPath.isEmpty() ? "" : "-cp \"${commonClassPath}\""

                    def builds = javaFiles.collectEntries { fullFilePath ->
                        String groovyFilePath = fullFilePath.replace('\\', '/') 
                        def fileName = groovyFilePath.tokenize('/')[-1]
                        def classNameOnly = fileName.replace('.java', '') 
                        String packageSubPath = ""
                        String srcDirForSourcepathRelative = "" // This will be relative to workspace
                        String fqcn = classNameOnly 
                        int srcMainJavaIdx = groovyFilePath.lastIndexOf("src/main/java/")
                        int srcIdx = groovyFilePath.lastIndexOf("src/")
                        int packageStartIndexInFilePath = -1

                        // Determine srcDirForSourcepathRelative (path from workspace root to the java root dir like "project/src/main/java")
                        // fullFilePath is absolute. We need to make srcDirForSourcepath relative or ensure javac gets absolute.
                        String workspacePrefix = env.WORKSPACE.replace('\\', '/') + "/"
                        String relativeFilePathToWorkspace = groovyFilePath.startsWith(workspacePrefix) ? groovyFilePath.substring(workspacePrefix.length()) : groovyFilePath
                        
                        if (relativeFilePathToWorkspace.lastIndexOf("src/main/java/") != -1) {
                            srcMainJavaIdx = relativeFilePathToWorkspace.lastIndexOf("src/main/java/")
                            packageStartIndexInFilePath = srcMainJavaIdx + "src/main/java/".length()
                            srcDirForSourcepathRelative = relativeFilePathToWorkspace.substring(0, packageStartIndexInFilePath -1)
                        } else if (relativeFilePathToWorkspace.lastIndexOf("src/") != -1) {
                            srcIdx = relativeFilePathToWorkspace.lastIndexOf("src/")
                            packageStartIndexInFilePath = srcIdx + "src/".length()
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

                        // appClassOutputDir is relative to workspace for bat commands
                        def appClassOutputDirRelative = "${OUTPUT_DIR}/${classNameOnly}_classes".replace('/', '\\')
                        def appClassOutputDirAbsolute = "${env.WORKSPACE}/${appClassOutputDirRelative}".replace('/', File.separator)

                        bat "if exist \"${appClassOutputDirRelative}\" rmdir /s /q \"${appClassOutputDirRelative}\""
                        bat "mkdir \"${appClassOutputDirRelative}\""
                        
                        [(classNameOnly): { 
                            // jarPath is relative to workspace for bat commands
                            def jarName = "${classNameOnly}.jar"
                            def jarPathRelative = rootJarApps.contains(classNameOnly)
                                ? jarName.replace('/', '\\') 
                                : "${OUTPUT_DIR}\\${jarName}".replace('/', '\\')
                            def jarPathAbsolute = "${env.WORKSPACE}/${jarPathRelative}".replace('/', File.separator)


                            echo "--- Processing: ${classNameOnly} ---"
                            echo "  Source File: ${fullFilePath}" // Already absolute
                            echo "  FQCN: ${fqcn}"
                            // For javac -sourcepath, it's often easier to use paths relative to where javac is invoked (workspace)
                            // or absolute paths. Since bat commands run in workspace, relative srcDirForSourcepath is fine.
                            echo "  Sourcepath for javac (relative to workspace): ${srcDirForSourcepathRelative}" 
                            echo "  .class output directory (relative to workspace): ${appClassOutputDirRelative}"
                            echo "  Output JAR (relative to workspace): ${jarPathRelative}"
                            if (!commonClassPath.isEmpty()) {
                                echo "  Compiler Classpath: ${commonClassPath}" // This IS absolute paths to JARs
                            }

                            String batFullFilePath = fullFilePath.replace('/', '\\') // This is absolute
                            String batSrcDirForSourcepathCmd = srcDirForSourcepathRelative.replace('/', '\\') // Relative to workspace
                            
                            def compileCommand = "javac -encoding UTF-8 ${classPathOpt} -d \"${appClassOutputDirRelative}\" -sourcepath \"${batSrcDirForSourcepathCmd}\" \"${batFullFilePath}\""
                            echo "  Compile CMD: ${compileCommand}"
                            bat compileCommand

                            // jar command also runs in workspace CWD
                            def jarCommand = "jar cfe \"${jarPathRelative}\" ${fqcn} -C \"${appClassOutputDirRelative}\" ."
                            echo "  JAR CMD: ${jarCommand}"
                            bat jarCommand
                            echo "--- Finished: ${classNameOnly} ---"
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
                // Paths relative to workspace are fine here for bat commands
                bat "if exist \"${RELEASE_PACKAGE_DIR}\" rmdir /s /q \"${RELEASE_PACKAGE_DIR}\""
                bat "mkdir \"${RELEASE_PACKAGE_DIR}\""
                
                bat "xcopy \"${OUTPUT_DIR}\\*.jar\" \"${RELEASE_PACKAGE_DIR}\\\" /Y /I > nul 2>&1 || echo No JARs in ${OUTPUT_DIR} to copy."
                
                script {
                    def rootJarAppsList = ['AppMainRoot'] 
                    rootJarAppsList.each { appName ->
                        def jarFile = "${appName}.jar" // Relative to workspace
                        if (fileExists(jarFile)) { // fileExists checks relative to workspace
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
                    def zipFile = "${RELEASE_PACKAGE_DIR}.zip" // Relative to workspace

                    // Compress-Archive paths should be relative to powershell's CWD (which is workspace)
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
            mail to: "${EMAIL_RECIPIENTS}",
                 subject: "Build ${currentBuild.currentResult}: Job ${env.JOB_NAME} [#${env.BUILD_NUMBER}]",
                 body: "See details at: ${env.BUILD_URL}"
        }
    }
}