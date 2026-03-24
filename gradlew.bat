@echo off
setlocal enabledelayedexpansion
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%

rem Prefer JAVA_HOME if set and contains java.exe
set "JAVA_EXE="
if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

rem Fallback to java on PATH
if not defined JAVA_EXE (
  for %%J in (java.exe java) do (
	where %%J >nul 2>&1 && set "JAVA_EXE=%%~$PATH:J" && goto :foundJava
  )
)

:foundJava
if not defined JAVA_EXE (
  echo Java nicht gefunden. Bitte JDK im PATH oder JAVA_HOME setzen.
  exit /b 1
)

"%JAVA_EXE%" -jar "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" %*


