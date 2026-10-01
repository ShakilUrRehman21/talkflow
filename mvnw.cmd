@REM Maven Wrapper script for Windows
@REM This downloads Maven if needed and runs it

@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

@REM If Maven is installed locally, use it directly
where mvn >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

@REM Otherwise try to find Maven distribution
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6"
if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    "%MAVEN_HOME%\bin\mvn.cmd" %*
    exit /b %ERRORLEVEL%
)

@REM Download Maven if not available
echo Downloading Apache Maven 3.9.6...
set "DOWNLOAD_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip"
set "MAVEN_ZIP=%TEMP%\apache-maven-3.9.6-bin.zip"

powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%MAVEN_ZIP%'"
if %ERRORLEVEL% NEQ 0 (
    echo Error: Failed to download Maven. Please install Maven manually.
    exit /b 1
)

echo Extracting Maven...
mkdir "%USERPROFILE%\.m2\wrapper\dists" 2>nul
powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
move "%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6" "%MAVEN_HOME%" 2>nul

"%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%
