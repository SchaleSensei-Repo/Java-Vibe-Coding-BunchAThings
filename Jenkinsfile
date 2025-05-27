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
        RELEASES_TO_KEEP = 3                // Defined but not used in original script for cleanup
        EMAIL_RECIPIENTS = 'ashlovedawn@gmail.com'
        // USER ACTION: If you create a 'libs' folder at the root of your repo for dependency JARs:
         LIBS_DIR_PATH = 'libs'
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
                    bat "if exist \"${OUTPUT_DIR}\" rmdir /s /q \"${OUTPUT_DIR}\""
                    bat "mkdir \"${OUTPUT_DIR}\""

                    // PowerShell: Find .java files with main() and save to main_java_files.txt (UTF-8 without BOM)
                    bat '''\
                        powershell -Command "\
                            $javaFilePaths = Get-ChildItem -Recurse -Filter *.java | `
                                Where-Object { Select-String -Path $_.FullName -Pattern 'public static void main' -Quiet } | `
                                ForEach-Object { $_.FullName }; `
                            if ($null -ne $javaFilePaths -and $javaFilePaths.Count -gt 0) { `
                                [System.IO.File]::WriteAllLines('main_java_files.txt', $javaFilePaths, [System.Text.UTF8Encoding]::new($false)) `
                            } else { `
                                Write-Host 'No Java files with a main method were found.'; `
                                Set-Content -Path 'main_java_files.txt' -Value '' `
                            }" \
                    '''
                    
                    def javaFileContent = readFile(file: 'main_java_files.txt', encoding: 'UTF-8').trim()
                    if (javaFileContent.isEmpty()) {
                        error "No Java files with a main method were found (main_java_files.txt is empty)."
                    }

                    def javaFiles = javaFileContent.split("\\r?\\n")
                        .collect { it.trim() }
                        .findAll { it }

                    if (javaFiles.isEmpty()) {
                        error "No Java files with a main method were found after processing main_java_files.txt."
                    }

                    def rootJarApps = ['AppMainRoot'] // Short class names of apps to be placed in workspace root

                    // --- BEGIN: Dependency Management (USER ACTION REQUIRED) ---
                    // You MUST manage external dependencies (org.json, gson, etc.).
                    // Option: Place all dependency JARs into a folder (e.g., 'libs') in your repository root.
                    // Then, uncomment and adjust the LIBS_DIR_PATH environment variable and the following scan.
                    def dependencyJars = []
                     if (env.LIBS_DIR_PATH) {
                        def libsDir = new File(env.LIBS_DIR_PATH)
                        if (libsDir.isDirectory()) {
                            libsDir.eachFileRecurse { f -> // Use eachFileRecurse if JARs can be in subdirs of libs
                                if (f.name.endsWith(".jar")) {
                                    dependencyJars.add(f.absolutePath.replace('/', '\\'))
                                }
                            }
                        }
                     }
                    // Example: Manually list paths to dependency JARs if they are elsewhere or few
                    // dependencyJars.add("C:\\path\\to\\your-libs\\org.json.jar".replace('/', '\\'))
                    // dependencyJars.add("C:\\path\\to\\your-libs\\gson.jar".replace('/', '\\'))
                    
                    if (!dependencyJars.isEmpty()) {
                        echo "Found dependency JARs: ${dependencyJars.join(', ')}"
                    } else {
                        echo "No external dependency JARs configured. Compilation might fail for projects needing them."
                    }
                    def commonClassPath = dependencyJars.join(File.pathSeparator)
                    // --- END: Dependency Management ---

                    def builds = javaFiles.collectEntries { fullFilePath ->
                        String groovyFilePath = fullFilePath.replace('\\', '/') // Use forward slashes for Groovy logic

                        def fileName = groovyFilePath.tokenize('/')[-1]
                        def classNameOnly = fileName.replace('.java', '') // Short class name, e.g., "MilleBornesGUI"

                        String packageSubPath = ""
                        String srcDirForSourcepath = "" // This will be the root for `-sourcepath` (e.g., "project/src/main/java")
                        String fqcn = classNameOnly // Fully Qualified Class Name, defaults to short name

                        // Heuristic to determine package structure and sourcepath root from file path
                        int srcMainJavaIdx = groovyFilePath.lastIndexOf("src/main/java/")
                        int srcIdx = groovyFilePath.lastIndexOf("src/")
                        int packageStartIndexInFilePath = -1

                        if (srcMainJavaIdx != -1) {
                            packageStartIndexInFilePath = srcMainJavaIdx + "src/main/java/".length()
                            srcDirForSourcepath = groovyFilePath.substring(0, packageStartIndexInFilePath -1) 
                        } else if (srcIdx != -1) {
                            packageStartIndexInFilePath = srcIdx + "src/".length()
                            srcDirForSourcepath = groovyFilePath.substring(0, packageStartIndexInFilePath -1)
                        } else {
                            // Fallback: if no "src/main/java" or "src" marker, assume the source path is the directory
                            // containing the package structure (if any), or just the file's directory.
                            // This part is the most heuristic. For a file "com/example/MyClass.java", sourcepath should be parent of "com".
                            // For this example, we'll try to infer it.
                            String currentPath = groovyFilePath.substring(0, groovyFilePath.lastIndexOf('/'))
                            List<String> pathParts = currentPath.tokenize('/')
                            int lastPotentialPackagePartIndex = pathParts.size() -1
                            while(lastPotentialPackagePartIndex >= 0) {
                                if (pathParts[lastPotentialPackagePartIndex] ==~ /^[a-z_][a-z0-9_]*$/) { // Basic check for package name segment
                                     lastPotentialPackagePartIndex--
                                } else {
                                    break
                                }
                            }
                            srcDirForSourcepath = pathParts.subList(0, lastPotentialPackagePartIndex + 1).join('/')
                            if ( (lastPotentialPackagePartIndex + 1) < pathParts.size() ) {
                                packageSubPath = pathParts.subList(lastPotentialPackagePartIndex + 1, pathParts.size()).join('/')
                            }
                            if (srcDirForSourcepath.isEmpty() && groovyFilePath.contains('/')) { // If file was like "MyClass.java" in root, this would be empty.
                                srcDirForSourcepath = "." // current directory
                            } else if (srcDirForSourcepath.isEmpty()) {
                                srcDirForSourcepath = groovyFilePath.substring(0, groovyFilePath.lastIndexOf('/'))
                            }
                        }
                        
                        // Determine FQCN
                        if (packageStartIndexInFilePath != -1 && groovyFilePath.lastIndexOf('/') > packageStartIndexInFilePath) { // Ensure there's a path component after src/main/java/
                            packageSubPath = groovyFilePath.substring(packageStartIndexInFilePath, groovyFilePath.lastIndexOf('/'))
                        }
                        
                        if (!packageSubPath.isEmpty()) {
                            fqcn = packageSubPath.replace('/', '.') + "." + classNameOnly
                        }

                        def appClassOutputDir = "${OUTPUT_DIR}/${classNameOnly}_classes" // Per-app directory for .class files
                        bat "if exist \"${appClassOutputDir.replace('/', '\\')}\" rmdir /s /q \"${appClassOutputDir.replace('/', '\\')}\""
                        bat "mkdir \"${appClassOutputDir.replace('/', '\\')}\""
                        
                        [(classNameOnly): { // Key for parallel map; ensure classNameOnly is sufficiently unique
                            def jarName = "${classNameOnly}.jar"
                            def jarPath = rootJarApps.contains(classNameOnly)
                                ? jarName.replace('/', '\\')
                                : "${OUTPUT_DIR}\\${jarName}".replace('/', '\\')

                            echo "--- Processing: ${classNameOnly} ---"
                            echo "  Source File: ${fullFilePath}"
                            echo "  FQCN: ${fqcn}"
                            echo "  Sourcepath for javac: ${srcDirForSourcepath}"
                            echo "  .class output directory: ${appClassOutputDir}"
                            echo "  Output JAR: ${jarPath}"
                            if (!commonClassPath.isEmpty()) {
                                echo "  Compiler Classpath: ${commonClassPath}"
                            }

                            String batFullFilePath = fullFilePath.replace('/', '\\')
                            String batSrcDirForSourcepath = srcDirForSourcepath.replace('/', '\\')
                            String batAppClassOutputDir = appClassOutputDir.replace('/', '\\')
                            String classPathOpt = commonClassPath.isEmpty() ? "" : "-cp \"${commonClassPath}\""
                            
                            def compileCommand = "javac -encoding UTF-8 ${classPathOpt} -d \"${batAppClassOutputDir}\" -sourcepath \"${batSrcDirForSourcepath}\" \"${batFullFilePath}\""
                            echo "  Compile CMD: ${compileCommand}"
                            bat compileCommand

                            // Create JAR using the FQCN for Main-Class and packaging all classes from appClassOutputDir
                            def jarCommand = "jar cfe \"${jarPath}\" ${fqcn} -C \"${batAppClassOutputDir}\" ."
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
                bat "if exist \"${RELEASE_PACKAGE_DIR}\" rmdir /s /q \"${RELEASE_PACKAGE_DIR}\""
                bat "mkdir \"${RELEASE_PACKAGE_DIR}\""
                
                // Copy JARs from OUTPUT_DIR
                bat "xcopy \"${OUTPUT_DIR}\\*.jar\" \"${RELEASE_PACKAGE_DIR}\\\" /Y /I > nul 2>&1 || echo No JARs in ${OUTPUT_DIR} to copy."
                
                // Copy JARs from workspace root (for rootJarApps)
                script {
                    def rootJarAppsList = ['AppMainRoot'] // Must match definition in build stage
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
                    def zipFile = "${RELEASE_PACKAGE_DIR}.zip" // Will be created in workspace root

                    // Compress-Archive might create an empty zip if RELEASE_PACKAGE_DIR is empty.
                    // Add -ErrorAction SilentlyContinue if an error on empty dir is not desired, or check dir contents first.
                    bat "powershell Compress-Archive -Path \"${RELEASE_PACKAGE_DIR}\\*\" -DestinationPath \"${zipFile}\" -Force"
                    
                    // Construct JSON payload carefully to avoid issues with quotes and spaces
                    String releaseData = "{ \\\"tag_name\\\": \\\"${tag}\\\", \\\"name\\\": \\\"${tag}\\\", \\\"body\\\": \\\"${message}\\\", \\\"draft\\\": false, \\\"prerelease\\\": false }"

                    withCredentials([string(credentialsId: "${GITHUB_CREDS}", variable: 'GH_TOKEN')]) {
                        // This curl command creates the GitHub release metadata.
                        // It does NOT upload the zipFile. A separate step is needed to upload assets.
                        bat """
                            curl -L -X POST ^
                                 -H "Accept: application/vnd.github+json" ^
                                 -H "Authorization: Bearer %GH_TOKEN%" ^
                                 -H "X-GitHub-Api-Version: 2022-11-28" ^
                                 https://api.github.com/repos/${GITHUB_REPO}/releases ^
                                 -d "${releaseData}"
                        """
                        // To upload the zipFile asset, you would parse 'upload_url' from the above response and make another POST.
                    }
                    // TODO: Add logic to clean up old releases using RELEASES_TO_KEEP if desired.
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