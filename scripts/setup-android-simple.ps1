# Simple Android Development Setup Script
# Run this as Administrator

Write-Host "=== Android Development Setup ===" -ForegroundColor Green
Write-Host ""

# Check Administrator
$currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
$isAdmin = $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "ERROR: Run as Administrator" -ForegroundColor Red
    pause
    exit 1
}

# Create directories
$AndroidDevDir = "C:\AndroidDev"
$AndroidSdkDir = "$AndroidDevDir\Android\Sdk"
$DownloadsDir = "$AndroidDevDir\downloads"

Write-Host "Creating directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $AndroidDevDir -Force | Out-Null
New-Item -ItemType Directory -Path $AndroidSdkDir -Force | Out-Null
New-Item -ItemType Directory -Path $DownloadsDir -Force | Out-Null
New-Item -ItemType Directory -Path "$AndroidSdkDir\cmdline-tools" -Force | Out-Null
Write-Host "Directories created" -ForegroundColor Green

# Set environment variables
Write-Host "Setting environment variables..." -ForegroundColor Yellow
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $AndroidSdkDir, "Machine")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $AndroidSdkDir, "Machine")
$env:ANDROID_HOME = $AndroidSdkDir
$env:ANDROID_SDK_ROOT = $AndroidSdkDir
Write-Host "Environment variables set" -ForegroundColor Green

# Add to PATH function
function AddToPath($path) {
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($currentPath -notlike "*$path*") {
        Write-Host "Adding to PATH: $path" -ForegroundColor Yellow
        $newPath = $currentPath + ";" + $path
        [Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
        $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine")
        Write-Host "Added to PATH" -ForegroundColor Green
    } else {
        Write-Host "Already in PATH: $path" -ForegroundColor Green
    }
}

# Update PATH
Write-Host "Updating PATH..." -ForegroundColor Yellow
AddToPath "$AndroidSdkDir\cmdline-tools\latest\bin"
AddToPath "$AndroidSdkDir\platform-tools"
AddToPath "$AndroidSdkDir\tools\bin"

# Check Java
Write-Host "Checking Java..." -ForegroundColor Yellow
$javaCheck = $null
try {
    $javaCheck = cmd /c "java -version 2>&1"
}
catch {
    # Java check failed
}

if ($javaCheck -and ($javaCheck -match "version")) {
    Write-Host "Java is installed" -ForegroundColor Green
} else {
    Write-Host "Java not found - you need to install it manually" -ForegroundColor Red
    Write-Host "Download from: https://adoptium.net/" -ForegroundColor Yellow
}

# Check Git
Write-Host "Checking Git..." -ForegroundColor Yellow
$gitCheck = $null
try {
    $gitCheck = cmd /c "git --version 2>&1"
}
catch {
    # Git check failed
}

if ($gitCheck -and ($gitCheck -match "git version")) {
    Write-Host "Git is installed" -ForegroundColor Green
} else {
    Write-Host "Git not found - you need to install it manually" -ForegroundColor Red
    Write-Host "Download from: https://git-scm.com/download/win" -ForegroundColor Yellow
}

# Download Android Command Line Tools
Write-Host "Downloading Android Command Line Tools..." -ForegroundColor Yellow
$cmdLineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
$cmdLineToolsZip = "$DownloadsDir\commandlinetools.zip"

try {
    $webClient = New-Object System.Net.WebClient
    $webClient.DownloadFile($cmdLineToolsUrl, $cmdLineToolsZip)
    Write-Host "Downloaded Android Command Line Tools" -ForegroundColor Green
    
    # Extract
    Write-Host "Extracting..." -ForegroundColor Yellow
    $tempDir = "$DownloadsDir\temp"
    Expand-Archive -Path $cmdLineToolsZip -DestinationPath $tempDir -Force
    
    # Move to correct location
    $latestDir = "$AndroidSdkDir\cmdline-tools\latest"
    New-Item -ItemType Directory -Path $latestDir -Force | Out-Null
    Copy-Item -Path "$tempDir\cmdline-tools\*" -Destination $latestDir -Recurse -Force
    
    # Cleanup
    Remove-Item -Path $tempDir -Recurse -Force
    Remove-Item -Path $cmdLineToolsZip -Force
    
    Write-Host "Android Command Line Tools installed" -ForegroundColor Green
}
catch {
    Write-Host "Failed to download/install Android tools" -ForegroundColor Red
    Write-Host "You may need to do this manually" -ForegroundColor Yellow
}

# Install SDK components if possible
$sdkManagerPath = "$AndroidSdkDir\cmdline-tools\latest\bin\sdkmanager.bat"
if (Test-Path $sdkManagerPath) {
    Write-Host "Installing Android SDK components..." -ForegroundColor Yellow
    
    # Accept licenses
    $yesInput = "y`ny`ny`ny`ny`ny`ny`ny`ny`ny`ny`n"
    $yesInput | cmd /c "`"$sdkManagerPath`" --licenses" | Out-Null
    
    # Install components
    cmd /c "`"$sdkManagerPath`" platform-tools" | Out-Null
    cmd /c "`"$sdkManagerPath`" `"platforms;android-34`"" | Out-Null  
    cmd /c "`"$sdkManagerPath`" `"build-tools;34.0.0`"" | Out-Null
    
    Write-Host "SDK components installed" -ForegroundColor Green
} else {
    Write-Host "SDK Manager not found" -ForegroundColor Red
}

# Final verification
Write-Host ""
Write-Host "=== Verification ===" -ForegroundColor Cyan

# Refresh PATH
$env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine")

# Check environment variables
$androidHome = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "Machine")
if ($androidHome -eq $AndroidSdkDir) {
    Write-Host "ANDROID_HOME: OK" -ForegroundColor Green
} else {
    Write-Host "ANDROID_HOME: NOT SET" -ForegroundColor Red
}

# Check SDK Manager
if (Test-Path $sdkManagerPath) {
    Write-Host "SDK Manager: Found" -ForegroundColor Green
} else {
    Write-Host "SDK Manager: Missing" -ForegroundColor Red
}

# Check ADB
$adbPath = "$AndroidSdkDir\platform-tools\adb.exe"
if (Test-Path $adbPath) {
    Write-Host "ADB: Found" -ForegroundColor Green
} else {
    Write-Host "ADB: Missing" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Setup Summary ===" -ForegroundColor Green
Write-Host "Android SDK Directory: $AndroidSdkDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "If you see any RED items above, you may need to:" -ForegroundColor Yellow
Write-Host "1. Install Java from https://adoptium.net/" -ForegroundColor Gray
Write-Host "2. Install Git from https://git-scm.com/download/win" -ForegroundColor Gray
Write-Host "3. Restart PowerShell and try running SDK manager manually" -ForegroundColor Gray
Write-Host ""
Write-Host "Close and reopen PowerShell, then run your project init script" -ForegroundColor White

pause
