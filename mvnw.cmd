@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script for Invora (Java 21 LTS)
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set "DIR=%~dp0"
if "%DIR:~-1%"=="\" set "DIR=%DIR:~0,-1%"
set "MAVEN_PROJECTBASEDIR=%DIR%"

set "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

@REM Prioritize Java 21 for Invora
if exist "D:\CSE B\jdk-21\jdk-21.0.12.1+1\bin\java.exe" (
    set "JAVA_HOME=D:\CSE B\jdk-21\jdk-21.0.12.1+1"
) else if defined JAVA21_HOME (
    set "JAVA_HOME=%JAVA21_HOME%"
)

set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=java"
)

"%JAVA_EXE%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %*
if %ERRORLEVEL% NEQ 0 exit /b %ERRORLEVEL%
