@echo off
echo ================================================
echo NextGen E-Commerce - Prepare for Transfer
echo ================================================
echo.
echo This script will clean the project for transfer.
echo It will remove:
echo  - Build folders
echo  - Gradle cache
echo  - IDE-specific files (can be regenerated)
echo.
echo This will reduce the project size significantly.
echo.
pause

echo.
echo Cleaning project...
echo.

REM Remove build folders
if exist "build\" (
    echo Removing root build folder...
    rmdir /S /Q "build"
)

if exist "app\build\" (
    echo Removing app build folder...
    rmdir /S /Q "app\build"
)

if exist "backend\node_modules\" (
    echo Removing backend node_modules...
    rmdir /S /Q "backend\node_modules"
)

REM Remove .gradle folder
if exist ".gradle\" (
    echo Removing .gradle folder...
    rmdir /S /Q ".gradle"
)

REM Remove .idea folder (optional - IDE settings)
if exist ".idea\" (
    echo Removing .idea folder...
    rmdir /S /Q ".idea"
)

REM Remove local.properties (machine-specific)
if exist "local.properties" (
    echo Removing local.properties...
    del "local.properties"
)

REM Remove Gradle daemon
if exist "%USERPROFILE%\.gradle\daemon\" (
    echo Cleaning Gradle daemon...
    rmdir /S /Q "%USERPROFILE%\.gradle\daemon"
)

echo.
echo ================================================
echo Cleanup Complete!
echo ================================================
echo.
echo Project is now ready for transfer.
echo Current folder size is significantly reduced.
echo.
echo NEXT STEPS:
echo 1. Copy the entire "NextGenEcommerce" folder to USB
echo 2. On new PC, copy from USB to desired location
echo 3. Open with Android Studio
echo 4. Let Gradle sync (will download dependencies)
echo.
echo Press any key to exit...
pause >nul
