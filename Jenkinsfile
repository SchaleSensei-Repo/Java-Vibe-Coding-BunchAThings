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
        LIBS_DIR_PATH = 'libs' // Assuming your libs folder is at the root of the repo
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

                    // CORRECTED: PowerShell script execution using -EncodedCommand
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
                    '''.stripIndent() // Use triple-single-quotes to avoid Groovy interpolation issues with $

                    // Encode the PowerShell script to Base64 (UTF-16LE encoding)
                    byte[] scriptBytes = psScriptContent.getBytes("UTF-16LE")
                    def encodedCommand = scriptBytes.encodeBase64().toString()

                    bat "powershell -NoProfile -NonInteractive -EncodedCommand ${encodedCommand}"
                    
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
                    def actualLibsDirPath = env.LIBS_DIR_PATH // Get the value once
                    if (actualLibsDirPath) {
                       def libsDir = new File(actualLibsDirPath)
                       if (libsDir.isDirectory()) {
                           libsDir.eachFile { f -> 
                               if (f.name.endsWith(".jar")) {
                                   dependencyJars.add(f.absolutePath.replace('/', '\\'))
                               }
                           }
                       } else {
                           echo "Warning: LIBS_DIR_PATH '${actualLibsDirPath}' is not a directory."
                       }
                    }
                    
                    if (!dependencyJars.isEmpty()) {
                        echo "Found dependency JARs: ${dependencyJars.join(', ')}"
                    } else {
                        echo "No external dependency JARs configured or found in '${actualLibsDirPath}'. Compilation might fail for projects needing them."
                    }
                    def commonClassPath = dependencyJars.join(File.pathSeparator)

                    def builds = javaFiles.collectEntries { fullFilePath ->
                        String groovyFilePath = fullFilePath.replace('\\', '/') 
                        def fileName = groovyFilePath.tokenize('/')[-1]
                        def classNameOnly = fileName.replace('.java', '') 
                        String packageSubPath = ""
                        String srcDirForSourcepath = "" 
                        String fqcn = classNameOnly 
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
                            String currentPath = groovyFilePath.contains('/') ? groovyFilePath.substring(0, groovyFilePath.lastIndexOf('/')) : '.'
                            List<String> pathParts = currentPath.tokenize('/')
                            int lastPotentialPackagePartIndex = pathParts.size() -1
                            while(lastPotentialPackagePartIndex >= 0 && !pathParts[lastPotentialPackagePartIndex].isEmpty()) {
                                if (pathParts[lastPotentialPackagePartIndex] ==~ /^[a-z_][a-z0-9_]*$/) {
                                     lastPotentialPackagePartIndex--
                                } else {
                                    break
                                }
                            }
                            srcDirForSourcepath = pathParts.subList(0, lastPotentialPackagePartIndex + 1).join('/')
                            if ( (lastPotentialPackagePartIndex + 1) < pathParts.size() ) {
                                packageSubPath = pathParts.subList(lastPotentialPackagePartIndex + 1, pathParts.size()).join('/')
                            }
                            if (srcDirForSourcepath.isEmpty() && currentPath != '.') { 
                                srcDirForSourcepath = currentPath
                            } else if (srcDirForSourcepath.isEmpty() && currentPath == '.') {
                                srcDirForSourcepath = "." 
                            }
                        }
                        
                        if (packageStartIndexInFilePath != -1 && groovyFilePath.lastIndexOf('/') > packageStartIndexInFilePath) {
                            packageSubPath = groovyFilePath.substring(packageStartIndexInFilePath, groovyFilePath.lastIndexOf('/'))
                        }
                        
                        if (!packageSubPath.isEmpty()) {
                            fqcn = packageSubPath.replace('/', '.') + "." + classNameOnly
                        }

                        def appClassOutputDir = "${OUTPUT_DIR}/${classNameOnly}_classes"
                        bat "if exist \"${appClassOutputDir.replace('/', '\\')}\" rmdir /s /q \"${appClassOutputDir.replace('/', '\\')}\""
                        bat "mkdir \"${appClassOutputDir.replace('/', '\\')}\""
                        
                        [(classNameOnly): { 
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
            mail to: "${EMAIL_RECIPIENTS}",
                 subject: "Build ${currentBuild.currentResult}: Job ${env.JOB_NAME} [#${env.BUILD_NUMBER}]",
                 body: "See details at: ${env.BUILD_URL}"
        }
    }
}