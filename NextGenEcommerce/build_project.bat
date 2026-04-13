@echo off
cd /d "%~dp0"
call gradlew.bat assembleDebug
echo.
echo Build completed with exit code: %ERRORLEVEL%
