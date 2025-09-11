# Build Smart SMS Filter Android App
# This script compiles the Android app and creates APK files

Write-Host "=== Building Smart SMS Filter Android App ===" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Split-Path $PSScriptRoot -Parent
$androidDir = Join-Path $projectRoot "android"

# Check if we're in the right directory
if (-not (Test-Path (Join-Path $projectRoot "README.md"))) {
    Write-Host "Failed - Please run this script from the smart-sms-filter project root directory" -ForegroundColor Red
    exit 1
}

# Check if android directory exists
if (-not (Test-Path $androidDir)) {
    Write-Host "Failed - Android project directory not found. Please run:" -ForegroundColor Red
    Write-Host "   .\scripts\init-project.ps1" -ForegroundColor Cyan
    exit 1
}

# Check if gradlew exists
$gradlewPath = Join-Path $androidDir "gradlew.bat"
if (-not (Test-Path $gradlewPath)) {
    Write-Host "Failed - gradlew.bat not found in android directory" -ForegroundColor Red
    Write-Host "Your Android project may not be fully set up." -ForegroundColor Red
    Write-Host "Please run the full setup first:" -ForegroundColor Red
    Write-Host "   .\scripts\setup-android-cli.ps1" -ForegroundColor Cyan
    exit 1
}

# Verify development environment
Write-Host "Checking build environment..." -ForegroundColor Yellow

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "Success - Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "Failed - Java not found. Please run setup-android-cli.ps1 first." -ForegroundColor Red
    exit 1
}

# Check Android SDK
if ($env:ANDROID_HOME) {
    Write-Host "Success - Android SDK: $env:ANDROID_HOME" -ForegroundColor Green
} else {
    Write-Host "Failed - ANDROID_HOME not set. Please run setup-android-cli.ps1 first." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Starting build process..." -ForegroundColor Green

# Navigate to android directory
Set-Location $androidDir

try {
    Write-Host "Building debug APK..." -ForegroundColor Yellow
    Write-Host ""
    
    # Build the debug APK
    & .\gradlew.bat assembleDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "Build successful!" -ForegroundColor Green
        Write-Host ""
        
        # Find the generated APK
        $apkPath = Get-ChildItem -Path "app\build\outputs\apk\debug" -Filter "*.apk" -Recurse | Select-Object -First 1
        if ($apkPath) {
            Write-Host "APK generated at:" -ForegroundColor Cyan
            Write-Host "   $($apkPath.FullName)" -ForegroundColor White
            Write-Host ""
            Write-Host "APK size: $([math]::Round($apkPath.Length / 1MB, 2)) MB" -ForegroundColor Gray
        }
        
        Write-Host "Next steps:" -ForegroundColor White
        Write-Host "- Install on device: .\scripts\run-android.ps1" -ForegroundColor Gray
        Write-Host "- Run tests: .\scripts\test-android.ps1" -ForegroundColor Gray
        Write-Host "- Clean build: .\scripts\clean-android.ps1" -ForegroundColor Gray
        
    } else {
        Write-Host ""
        Write-Host "Build failed!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Try these solutions:" -ForegroundColor Yellow
        Write-Host "1. Clean and rebuild:" -ForegroundColor Gray
        Write-Host "   .\scripts\clean-android.ps1" -ForegroundColor Cyan
        Write-Host "   .\scripts\build-android.ps1" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "2. Check that setup completed successfully:" -ForegroundColor Gray
        Write-Host "   java -version" -ForegroundColor Cyan
        Write-Host "   echo `$env:ANDROID_HOME" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "3. Re-run setup if needed:" -ForegroundColor Gray
        Write-Host "   .\scripts\setup-android-cli.ps1" -ForegroundColor Cyan
        
        exit 1
    }
    
} catch {
    Write-Host ""
    Write-Host "Build error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
    
} finally {
    # Return to project root
    Set-Location $projectRoot
}

Write-Host ""
Write-Host "=== Build Complete ===" -ForegroundColor Cyan
