@echo off
echo ==========================================
echo ScrapBH Marketplace Setup Verification
echo ==========================================
echo.

REM Check Java version
echo 1. Checking Java version...
java -version 2>&1 | findstr /C:"version"
echo.

REM Check Maven version
echo 2. Checking Maven version...
mvn -version 2>&1 | findstr /C:"Apache Maven"
echo.

REM Verify project structure
echo 3. Verifying project structure...
if exist "pom.xml" (
    echo [OK] pom.xml found
) else (
    echo [FAIL] pom.xml not found
)

if exist "src\main\resources\application.properties" (
    echo [OK] application.properties found
) else (
    echo [FAIL] application.properties not found
)

if exist "src\main\resources\db\schema.sql" (
    echo [OK] schema.sql found
) else (
    echo [FAIL] schema.sql not found
)

if exist ".env.example" (
    echo [OK] .env.example found
) else (
    echo [FAIL] .env.example not found
)
echo.

REM Count entity files
echo 4. Checking entity files...
dir /b /s src\main\java\com\scrapbh\marketplace\entity\*.java 2>nul | find /c ".java"
echo    entity files found (expected: 7)
echo.

REM Count enum files
echo 5. Checking enum files...
dir /b /s src\main\java\com\scrapbh\marketplace\enums\*.java 2>nul | find /c ".java"
echo    enum files found (expected: 5)
echo.

REM Compile check
echo 6. Attempting to compile project...
call mvn clean compile -q
if %ERRORLEVEL% EQU 0 (
    echo [OK] Project compiled successfully
) else (
    echo [FAIL] Compilation failed - check Maven output above
)
echo.

echo ==========================================
echo Setup verification complete!
echo ==========================================
echo.
echo Next steps:
echo 1. Copy .env.example to .env and fill in your credentials
echo 2. Set up your Supabase database using DATABASE_SETUP.md
echo 3. Run 'mvn spring-boot:run' to start the application

pause
