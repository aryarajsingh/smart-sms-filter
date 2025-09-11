# Build and Install Smart SMS Filter on Android Device
# This script builds the app and installs it on connected devices

Write-Host "=== Install Smart SMS Filter on Android Device ===" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Split-Path $PSScriptRoot -Parent
$androidDir = Join-Path $projectRoot "android"

# Check if we're in the right directory
if (-not (Test-Path (Join-Path $projectRoot "README.md"))) {
    Write-Host "❌ Please run this script from the smart-sms-filter project root directory" -ForegroundColor Red
    exit 1
}

# Check if android directory exists
if (-not (Test-Path $androidDir)) {
    Write-Host "❌ Android project directory not found. Please run:" -ForegroundColor Red
    Write-Host "   .\scripts\init-project.ps1" -ForegroundColor Cyan
    exit 1
}

# Check if gradlew exists
$gradlewPath = Join-Path $androidDir "gradlew.bat"
if (-not (Test-Path $gradlewPath)) {
    Write-Host "❌ gradlew.bat not found in android directory" -ForegroundColor Red
    Write-Host "Please run the full setup first:" -ForegroundColor Red
    Write-Host "   .\scripts\setup-android-cli.ps1" -ForegroundColor Cyan
    exit 1
}

# Check for connected devices
Write-Host "Checking for connected Android devices..." -ForegroundColor Yellow

try {
    $devices = adb devices 2>&1
    $deviceLines = $devices | Where-Object { $_ -match "\t(device|unauthorized)" }
    
    if (-not $deviceLines) {
        Write-Host ""
        Write-Host "❌ No Android devices found!" -ForegroundColor Red
        Write-Host ""
        Write-Host "To connect your device:" -ForegroundColor Yellow
        Write-Host "1. Enable Developer Mode:" -ForegroundColor Gray
        Write-Host "   • Go to Settings → About phone" -ForegroundColor White
        Write-Host "   • Tap Build number 7 times" -ForegroundColor White
        Write-Host ""
        Write-Host "2. Enable USB Debugging:" -ForegroundColor Gray  
        Write-Host "   • Go to Settings → Developer options" -ForegroundColor White
        Write-Host "   • Turn on USB debugging" -ForegroundColor White
        Write-Host ""
        Write-Host "3. Connect via USB cable and run this script again" -ForegroundColor Gray
        Write-Host ""
        Write-Host "Debug info:" -ForegroundColor Gray
        Write-Host "$devices" -ForegroundColor DarkGray
        exit 1
    }
    
    $authorizedDevices = $deviceLines | Where-Object { $_ -match "\tdevice" }
    $unauthorizedDevices = $deviceLines | Where-Object { $_ -match "\tunauthorized" }
    
    if ($unauthorizedDevices) {
        Write-Host ""
        Write-Host "⚠️  Found unauthorized devices:" -ForegroundColor Yellow
        $unauthorizedDevices | ForEach-Object { Write-Host "   $_" -ForegroundColor Gray }
        Write-Host ""
        Write-Host "Please check your device and allow USB debugging when prompted." -ForegroundColor Yellow
        Write-Host "Then run this script again." -ForegroundColor Yellow
        exit 1
    }
    
    if ($authorizedDevices) {
        Write-Host "✅ Found connected devices:" -ForegroundColor Green
        $authorizedDevices | ForEach-Object { Write-Host "   $_" -ForegroundColor White }
        Write-Host ""
    }
    
} catch {
    Write-Host "❌ ADB not found. Please run setup-android-cli.ps1 first." -ForegroundColor Red
    exit 1
}

# Navigate to android directory
Set-Location $androidDir

try {
    Write-Host "Building and installing app..." -ForegroundColor Green
    Write-Host ""
    
    # Build and install the debug APK
    Write-Host "Installing debug build on device..." -ForegroundColor Yellow
    & .\gradlew.bat installDebug
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "🎉 App installed successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "📱 Smart SMS Filter is now installed on your device." -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor White
        Write-Host "• Check your device's app drawer for 'Smart SMS Filter'" -ForegroundColor Gray
        Write-Host "• Grant SMS permissions when the app requests them" -ForegroundColor Gray
        Write-Host "• View device logs: adb logcat" -ForegroundColor Gray
        Write-Host ""
        Write-Host "Useful commands:" -ForegroundColor White
        Write-Host "• View logs: adb logcat | Select-String SmartSms" -ForegroundColor Gray
        Write-Host "• Uninstall: adb uninstall com.smartsmsfilter" -ForegroundColor Gray
        Write-Host "• List installed: adb shell pm list packages | Select-String smartsms" -ForegroundColor Gray
        
    } else {
        Write-Host ""
        Write-Host "❌ Installation failed!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Try these solutions:" -ForegroundColor Yellow
        Write-Host "1. Make sure device is unlocked and USB debugging is authorized" -ForegroundColor Gray
        Write-Host "2. Try uninstalling any existing version first:" -ForegroundColor Gray
        Write-Host "   adb uninstall com.smartsmsfilter" -ForegroundColor Cyan
        Write-Host "3. Check device connection:" -ForegroundColor Gray
        Write-Host "   adb devices" -ForegroundColor Cyan
        Write-Host "4. Clean build and try again:" -ForegroundColor Gray
        Write-Host "   .\scripts\clean-android.ps1" -ForegroundColor Cyan
        Write-Host "   .\scripts\run-android.ps1" -ForegroundColor Cyan
        
        exit 1
    }
    
} catch {
    Write-Host ""
    Write-Host "❌ Installation error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
    
} finally {
    # Return to project root
    Set-Location $projectRoot
}

Write-Host ""
Write-Host "=== Installation Complete ===" -ForegroundColor Cyan
