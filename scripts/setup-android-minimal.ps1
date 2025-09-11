# Minimal Android Setup Script
Write-Host "=== Minimal Android Setup ===" -ForegroundColor Cyan

# Check Administrator
$currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
$isAdmin = $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "This script needs Administrator privileges." -ForegroundColor Yellow
    Write-Host "Please restart PowerShell as Administrator and run again." -ForegroundColor Yellow
    exit 1
}

# Create directories
$DevDir = "C:\AndroidDev"
$SdkDir = "$DevDir\Android\Sdk"
$ToolsDir = "$DevDir\tools"

Write-Host "Creating directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $DevDir -Force | Out-Null
New-Item -ItemType Directory -Path $SdkDir -Force | Out-Null  
New-Item -ItemType Directory -Path $ToolsDir -Force | Out-Null

# Check Java
try {
    $javaVersion = java -version 2>&1
    Write-Host "Java is installed: $($javaVersion[0])" -ForegroundColor Green
} catch {
    Write-Host "Java not found. Installing OpenJDK 17..." -ForegroundColor Yellow
    
    # Use Chocolatey if available, otherwise manual install
    try {
        choco install openjdk17 -y
        Write-Host "Java installed via Chocolatey" -ForegroundColor Green
    } catch {
        Write-Host "Please install Java manually from https://adoptium.net/" -ForegroundColor Yellow
    }
}

# Set Android environment variables
Write-Host "Setting Android environment variables..." -ForegroundColor Yellow
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $SdkDir, "Machine")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $SdkDir, "Machine")
$env:ANDROID_HOME = $SdkDir
$env:ANDROID_SDK_ROOT = $SdkDir

Write-Host "✓ Basic setup completed" -ForegroundColor Green
Write-Host "ANDROID_HOME set to: $SdkDir" -ForegroundColor Cyan

Write-Host ""
Write-Host "Next: You will need to download Android command line tools manually:" -ForegroundColor Yellow
Write-Host "1. Go to https://developer.android.com/studio#command-tools" -ForegroundColor Gray
Write-Host "2. Download Command line tools only for Windows" -ForegroundColor Gray
Write-Host "3. Extract to $SdkDir\cmdline-tools\latest\" -ForegroundColor Gray
Write-Host "4. Install platform tools with: sdkmanager platform-tools" -ForegroundColor Gray

Read-Host "Press Enter to exit"
