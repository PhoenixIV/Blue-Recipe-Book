@echo off
setlocal
cd /d "%~dp0"
py -3 "%~dp0tools\readme\render_readme.py"
if errorlevel 1 exit /b %ERRORLEVEL%
call "%~dp0gradlew.bat" --gradle-user-home "%USERPROFILE%\.gradle" :neoforge:remapJar %*
if errorlevel 1 exit /b %ERRORLEVEL%
if exist "%~dp0local\build.local.cmd" call "%~dp0local\build.local.cmd"
exit /b 0
