@echo off
echo Stopping Gradle Daemon...

REM Kill all Java processes that might be Gradle daemons
taskkill /F /FI "IMAGENAME eq java.exe" /FI "WINDOWTITLE eq *gradle*" 2>nul

REM Try to stop Gradle daemon properly
gradlew --stop 2>nul

REM Delete Gradle daemon directory to force clean restart
rmdir /S /Q "%USERPROFILE%\.gradle\daemon" 2>nul

echo.
echo Gradle daemon stopped and cache cleared.
echo Please restart Android Studio and rebuild the project.
pause
