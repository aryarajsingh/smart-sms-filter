# Android Development Setup Script
# Run this as Administrator

param(
    [switch]$SkipJava,
    [switch]$SkipGit
)

Write-Host "=== Android Development Setup ===" -ForegroundColor Green
Write-Host ""

# Check if running as Administrator
$currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host "ERROR: This script must be run as Administrator" -ForegroundColor Red
    Write-Host "Right-click PowerShell and select 'Run as Administrator'" -ForegroundColor Yellow
    pause
    exit 1
}

# Create directories
$AndroidDevDir = "C:\AndroidDev"
$AndroidSdkDir = "$AndroidDevDir\Android\Sdk"
$ToolsDir = "$AndroidDevDir\tools"

Write-Host "Creating directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $AndroidDevDir -Force | Out-Null
New-Item -ItemType Directory -Path $AndroidSdkDir -Force | Out-Null
New-Item -ItemType Directory -Path $ToolsDir -Force | Out-Null
Write-Host "Directories created successfully" -ForegroundColor Green

# Set Android environment variables
Write-Host "Setting Android environment variables..." -ForegroundColor Yellow
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", $AndroidSdkDir, [System.EnvironmentVariableTarget]::Machine)
[System.Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $AndroidSdkDir, [System.EnvironmentVariableTarget]::Machine)

# Update current session
$env:ANDROID_HOME = $AndroidSdkDir
$env:ANDROID_SDK_ROOT = $AndroidSdkDir

Write-Host "Environment variables set:" -ForegroundColor Green
Write-Host "  ANDROID_HOME = $AndroidSdkDir" -ForegroundColor Cyan
Write-Host "  ANDROID_SDK_ROOT = $AndroidSdkDir" -ForegroundColor Cyan

# Function to add to PATH
function Add-PathEntry {
    param($PathToAdd)
    
    $currentPath = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::Machine)
    if ($currentPath -notlike "*$PathToAdd*") {
        Write-Host "Adding to PATH: $PathToAdd" -ForegroundColor Yellow
        $newPath = $currentPath + ";" + $PathToAdd
        [System.Environment]::SetEnvironmentVariable("Path", $newPath, [System.EnvironmentVariableTarget]::Machine)
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::Machine)
        Write-Host "Added to PATH successfully" -ForegroundColor Green
    } else {
        Write-Host "Already in PATH: $PathToAdd" -ForegroundColor Green
    }
}

# Add Android tools to PATH
Write-Host "Updating system PATH..." -ForegroundColor Yellow
Add-PathEntry "$AndroidSdkDir\cmdline-tools\latest\bin"
Add-PathEntry "$AndroidSdkDir\platform-tools"
Add-PathEntry "$AndroidSdkDir\tools\bin"

# Check Java installation
if (-not $SkipJava) {
    Write-Host "Checking Java installation..." -ForegroundColor Yellow
    try {
        $javaOutput = cmd /c "java -version 2>&1"
        if ($javaOutput -match "version") {
            Write-Host "Java is already installed" -ForegroundColor Green
            Write-Host $javaOutput[0] -ForegroundColor Cyan
        } else {
            throw "Java not found"
        }
    } catch {
        Write-Host "Java not found or not working properly" -ForegroundColor Red
        Write-Host "You will need to install Java manually after this script completes" -ForegroundColor Yellow
    }
}

# Check Git installation  
if (-not $SkipGit) {
    Write-Host "Checking Git installation..." -ForegroundColor Yellow
    try {
        $gitOutput = cmd /c "git --version 2>&1"
        if ($gitOutput -match "git version") {
            Write-Host "Git is already installed" -ForegroundColor Green
            Write-Host $gitOutput -ForegroundColor Cyan
        } else {
            throw "Git not found"
        }
    } catch {
        Write-Host "Git not found" -ForegroundColor Red
        Write-Host "You will need to install Git manually after this script completes" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== Script Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "NEXT STEPS - Complete these manually:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. INSTALL JAVA (if not already installed):" -ForegroundColor White
Write-Host "   - Download OpenJDK 17 from: https://adoptium.net/" -ForegroundColor Gray
Write-Host "   - Install it with default settings" -ForegroundColor Gray
Write-Host "   - Verify with: java -version" -ForegroundColor Gray
Write-Host ""
Write-Host "2. INSTALL GIT (if not already installed):" -ForegroundColor White  
Write-Host "   - Download from: https://git-scm.com/download/win" -ForegroundColor Gray
Write-Host "   - Install with default settings" -ForegroundColor Gray
Write-Host "   - Verify with: git --version" -ForegroundColor Gray
Write-Host ""
Write-Host "3. DOWNLOAD ANDROID COMMAND LINE TOOLS:" -ForegroundColor White
Write-Host "   - Go to: https://developer.android.com/studio#command-tools" -ForegroundColor Gray
Write-Host "   - Download: Command line tools only (Windows)" -ForegroundColor Gray
Write-Host "   - Extract the ZIP file" -ForegroundColor Gray
Write-Host "   - Copy the extracted 'cmdline-tools' folder contents to:" -ForegroundColor Gray
Write-Host "     $AndroidSdkDir\cmdline-tools\latest\" -ForegroundColor Cyan
Write-Host ""
Write-Host "4. INSTALL ANDROID SDK COMPONENTS:" -ForegroundColor White
Write-Host "   - Open a NEW PowerShell window as Administrator" -ForegroundColor Gray
Write-Host "   - Run these commands one by one:" -ForegroundColor Gray
Write-Host "     sdkmanager --licenses" -ForegroundColor Cyan
Write-Host "     sdkmanager platform-tools" -ForegroundColor Cyan
Write-Host "     sdkmanager 'platforms;android-34'" -ForegroundColor Cyan
Write-Host "     sdkmanager 'build-tools;34.0.0'" -ForegroundColor Cyan
Write-Host ""
Write-Host "5. RETURN TO YOUR PROJECT:" -ForegroundColor White
Write-Host "   - Close and reopen PowerShell" -ForegroundColor Gray
Write-Host "   - Navigate back to: $pwd" -ForegroundColor Gray
Write-Host "   - Run the project initialization script" -ForegroundColor Gray
Write-Host ""
Write-Host "Directory structure created at: $AndroidDevDir" -ForegroundColor Cyan

pause
