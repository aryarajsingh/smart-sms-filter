# Android Command Line Development Setup Script
# This script sets up everything needed for Android development without Android Studio

Write-Host "=== Android Command Line Development Setup ===" -ForegroundColor Cyan
Write-Host "This script will install and configure everything needed for Android development from the command line." -ForegroundColor White
Write-Host ""

# Check if running as Administrator
function Test-Administrator {
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

if (-not (Test-Administrator)) {
    Write-Host "⚠️  This script needs to be run as Administrator to install software." -ForegroundColor Yellow
    Write-Host "Please right-click on PowerShell and select 'Run as Administrator', then run this script again." -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Function to add to PATH if not already present
function Add-ToPath {
    param([string]$PathToAdd)
    
    $currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
    if ($currentPath -notlike "*$PathToAdd*") {
        Write-Host "Adding $PathToAdd to system PATH..." -ForegroundColor Yellow
        [Environment]::SetEnvironmentVariable("Path", $currentPath + ";$PathToAdd", "Machine")
        $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine")
    } else {
        Write-Host "✓ $PathToAdd already in PATH" -ForegroundColor Green
    }
}

# Function to download file with progress
function Download-File {
    param([string]$Url, [string]$OutputPath)
    
    Write-Host "Downloading $(Split-Path $OutputPath -Leaf)..." -ForegroundColor Yellow
    $webClient = New-Object System.Net.WebClient
    $webClient.DownloadFile($Url, $OutputPath)
}

# Create development directories
$DevDir = "C:\AndroidDev"
$SdkDir = "$DevDir\Android\Sdk"
$ToolsDir = "$DevDir\tools"

Write-Host "Creating development directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $DevDir -Force | Out-Null
New-Item -ItemType Directory -Path $SdkDir -Force | Out-Null
New-Item -ItemType Directory -Path $ToolsDir -Force | Out-Null

# Step 1: Install Java Development Kit (JDK)
Write-Host ""
Write-Host "=== Step 1: Installing Java Development Kit ===" -ForegroundColor Cyan

try {
    $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
    Write-Host "✓ Java is already installed: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "Installing OpenJDK 17..." -ForegroundColor Yellow
    
    # Download and install OpenJDK 17
    $jdkUrl = "https://download.java.net/java/GA/jdk17.0.2/dfd4a8d0985749f896bed50d7138ee7f/8/GPL/openjdk-17.0.2_windows-x64_bin.zip"
    $jdkZip = "$ToolsDir\openjdk-17.0.2.zip"
    $jdkPath = "$DevDir\jdk-17.0.2"
    
    if (-not (Test-Path $jdkPath)) {
        Download-File -Url $jdkUrl -OutputPath $jdkZip
        
        # Extract JDK
        Write-Host "Extracting JDK..." -ForegroundColor Yellow
        Expand-Archive -Path $jdkZip -DestinationPath $DevDir -Force
        Remove-Item $jdkZip -Force
    }
    
    # Set JAVA_HOME
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $jdkPath, "Machine")
    $env:JAVA_HOME = $jdkPath
    
    # Add to PATH
    Add-ToPath "$jdkPath\bin"
    
    Write-Host "✓ OpenJDK 17 installed and configured" -ForegroundColor Green
}

# Step 2: Install Git
Write-Host ""
Write-Host "=== Step 2: Installing Git ===" -ForegroundColor Cyan

try {
    $gitVersion = git --version
    Write-Host "✓ Git is already installed: $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "Installing Git for Windows..." -ForegroundColor Yellow
    
    # Download Git installer
    $gitUrl = "https://github.com/git-for-windows/git/releases/download/v2.42.0.windows.2/Git-2.42.0.2-64-bit.exe"
    $gitInstaller = "$ToolsDir\git-installer.exe"
    
    Download-File -Url $gitUrl -OutputPath $gitInstaller
    
    # Install Git silently
    Write-Host "Installing Git (this may take a few minutes)..." -ForegroundColor Yellow
    Start-Process -FilePath $gitInstaller -ArgumentList "/SILENT" -Wait
    Remove-Item $gitInstaller -Force
    
    # Refresh environment variables
    $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine")
    
    Write-Host "✓ Git installed successfully" -ForegroundColor Green
}

# Step 3: Install Android Command Line Tools
Write-Host ""
Write-Host "=== Step 3: Installing Android Command Line Tools ===" -ForegroundColor Cyan

$cmdLineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
$cmdLineToolsZip = "$ToolsDir\commandlinetools.zip"
$cmdLineToolsPath = "$SdkDir\cmdline-tools"

if (-not (Test-Path "$cmdLineToolsPath\latest\bin\sdkmanager.bat")) {
    Download-File -Url $cmdLineToolsUrl -OutputPath $cmdLineToolsZip
    
    # Extract command line tools
    Write-Host "Extracting Android Command Line Tools..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $cmdLineToolsPath -Force | Out-Null
    Expand-Archive -Path $cmdLineToolsZip -DestinationPath $cmdLineToolsPath -Force
    
    # Rename cmdline-tools folder to latest
    if (Test-Path "$cmdLineToolsPath\cmdline-tools") {
        Move-Item "$cmdLineToolsPath\cmdline-tools" "$cmdLineToolsPath\latest" -Force
    }
    
    Remove-Item $cmdLineToolsZip -Force
    Write-Host "✓ Android Command Line Tools extracted" -ForegroundColor Green
} else {
    Write-Host "✓ Android Command Line Tools already installed" -ForegroundColor Green
}

# Set Android environment variables
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $SdkDir, "Machine")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $SdkDir, "Machine")
$env:ANDROID_HOME = $SdkDir
$env:ANDROID_SDK_ROOT = $SdkDir

# Add Android tools to PATH
Add-ToPath "$SdkDir\cmdline-tools\latest\bin"
Add-ToPath "$SdkDir\platform-tools"
Add-ToPath "$SdkDir\tools\bin"

Write-Host "✓ Android environment variables configured" -ForegroundColor Green

# Step 4: Install required Android SDK components
Write-Host ""
Write-Host "=== Step 4: Installing Android SDK Components ===" -ForegroundColor Cyan

$sdkManager = "$SdkDir\cmdline-tools\latest\bin\sdkmanager.bat"

# Accept licenses first
Write-Host "Accepting Android SDK licenses..." -ForegroundColor Yellow
"y" * 20 | & $sdkManager --licenses | Out-Null

# Install essential SDK components
$sdkComponents = @(
    "platform-tools",
    "platforms;android-34",
    "platforms;android-24",
    "build-tools;34.0.0",
    "build-tools;30.0.3",
    "emulator",
    "system-images;android-34;google_apis;x86_64"
)

foreach ($component in $sdkComponents) {
    Write-Host "Installing $component..." -ForegroundColor Yellow
    & $sdkManager $component | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ $component installed successfully" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Warning: Failed to install $component" -ForegroundColor Yellow
    }
}

# Step 5: Install Gradle
Write-Host ""
Write-Host "=== Step 5: Installing Gradle ===" -ForegroundColor Cyan

$gradleVersion = "8.4"
$gradleUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
$gradleZip = "$ToolsDir\gradle-$gradleVersion.zip"
$gradlePath = "$DevDir\gradle-$gradleVersion"

if (-not (Test-Path "$gradlePath\bin\gradle.bat")) {
    Download-File -Url $gradleUrl -OutputPath $gradleZip
    
    Write-Host "Extracting Gradle..." -ForegroundColor Yellow
    Expand-Archive -Path $gradleZip -DestinationPath $DevDir -Force
    Remove-Item $gradleZip -Force
    
    # Add Gradle to PATH
    Add-ToPath "$gradlePath\bin"
    
    Write-Host "✓ Gradle installed and configured" -ForegroundColor Green
} else {
    Write-Host "✓ Gradle already installed" -ForegroundColor Green
    Add-ToPath "$gradlePath\bin"
}

# Step 6: Create development scripts
Write-Host ""
Write-Host "=== Step 6: Creating Development Helper Scripts ===" -ForegroundColor Cyan

$projectRoot = Split-Path $PSScriptRoot -Parent

# Create build script
$buildScript = @"
# Android Build Script
# Run this script from the project root directory

Set-Location "$projectRoot\android"

Write-Host "Building Android project..." -ForegroundColor Green
.\gradlew.bat build

if (`$LASTEXITCODE -eq 0) {
    Write-Host "✓ Build completed successfully!" -ForegroundColor Green
    Write-Host "APK location: android\app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Cyan
} else {
    Write-Host "✗ Build failed!" -ForegroundColor Red
    exit 1
}
"@

$buildScript | Out-File -FilePath "$projectRoot\scripts\build-android.ps1" -Encoding UTF8 -Force

# Create run script
$runScript = @"
# Android Run Script
# This script builds and installs the app on a connected device

Set-Location "$projectRoot\android"

# Check if device is connected
Write-Host "Checking for connected devices..." -ForegroundColor Yellow
adb devices

Write-Host ""
Write-Host "Building and installing app..." -ForegroundColor Green
.\gradlew.bat installDebug

if (`$LASTEXITCODE -eq 0) {
    Write-Host "✓ App installed successfully!" -ForegroundColor Green
    Write-Host "You can now find 'Smart SMS Filter' app on your device." -ForegroundColor Cyan
} else {
    Write-Host "✗ Installation failed!" -ForegroundColor Red
    Write-Host "Make sure your device is connected and USB debugging is enabled." -ForegroundColor Yellow
}
"@

$runScript | Out-File -FilePath "$projectRoot\scripts\run-android.ps1" -Encoding UTF8 -Force

# Create test script
$testScript = @"
# Android Test Script
# Run unit and integration tests

Set-Location "$projectRoot\android"

Write-Host "Running unit tests..." -ForegroundColor Green
.\gradlew.bat test

Write-Host ""
Write-Host "Running lint checks..." -ForegroundColor Green
.\gradlew.bat lint

if (`$LASTEXITCODE -eq 0) {
    Write-Host "✓ All tests passed!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Some tests failed. Check the output above." -ForegroundColor Yellow
}
"@

$testScript | Out-File -FilePath "$projectRoot\scripts\test-android.ps1" -Encoding UTF8 -Force

# Create clean script
$cleanScript = @"
# Android Clean Script
# Clean build artifacts

Set-Location "$projectRoot\android"

Write-Host "Cleaning build artifacts..." -ForegroundColor Yellow
.\gradlew.bat clean

Write-Host "✓ Clean completed!" -ForegroundColor Green
"@

$cleanScript | Out-File -FilePath "$projectRoot\scripts\clean-android.ps1" -Encoding UTF8 -Force

Write-Host "✓ Development scripts created in scripts/ directory" -ForegroundColor Green

# Step 7: Setup Gradle Wrapper for the project
Write-Host ""
Write-Host "=== Step 7: Setting up Gradle Wrapper ===" -ForegroundColor Cyan

Set-Location "$projectRoot\android"

if (-not (Test-Path "gradlew.bat")) {
    Write-Host "Initializing Gradle wrapper..." -ForegroundColor Yellow
    
    # Create gradle wrapper files
    & gradle wrapper --gradle-version 8.4
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Gradle wrapper created successfully" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Warning: Failed to create Gradle wrapper" -ForegroundColor Yellow
    }
} else {
    Write-Host "✓ Gradle wrapper already exists" -ForegroundColor Green
}

# Final setup verification
Write-Host ""
Write-Host "=== Setup Verification ===" -ForegroundColor Cyan

# Refresh environment variables for current session
$env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine")

try {
    $javaCheck = java -version 2>&1
    Write-Host "✓ Java: Working" -ForegroundColor Green
} catch {
    Write-Host "✗ Java: Not working" -ForegroundColor Red
}

try {
    $gitCheck = git --version
    Write-Host "✓ Git: Working" -ForegroundColor Green
} catch {
    Write-Host "✗ Git: Not working" -ForegroundColor Red
}

try {
    $adbCheck = adb version 2>&1
    Write-Host "✓ ADB: Working" -ForegroundColor Green
} catch {
    Write-Host "✗ ADB: Not working (will be available after restart)" -ForegroundColor Yellow
}

try {
    $gradleCheck = gradle --version 2>&1
    Write-Host "✓ Gradle: Working" -ForegroundColor Green
} catch {
    Write-Host "✗ Gradle: Not working (will be available after restart)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Setup Complete! ===" -ForegroundColor Green
Write-Host ""
Write-Host "🎉 Android command line development environment is now ready!" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠️  IMPORTANT: Please restart your PowerShell session or computer for all PATH changes to take effect." -ForegroundColor Yellow
Write-Host ""
Write-Host "Next steps:" -ForegroundColor White
Write-Host "1. Restart PowerShell (close and reopen)" -ForegroundColor Gray
Write-Host "2. Navigate to your project: cd '$projectRoot'" -ForegroundColor Gray
Write-Host "3. Build the project: .\scripts\build-android.ps1" -ForegroundColor Gray
Write-Host "4. Connect an Android device and run: .\scripts\run-android.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "Available scripts in scripts/ directory:" -ForegroundColor White
Write-Host "- build-android.ps1  : Build the Android app" -ForegroundColor Gray
Write-Host "- run-android.ps1    : Build and install app on device" -ForegroundColor Gray
Write-Host "- test-android.ps1   : Run tests and lint checks" -ForegroundColor Gray
Write-Host "- clean-android.ps1  : Clean build artifacts" -ForegroundColor Gray
Write-Host ""
Write-Host "Installation locations:" -ForegroundColor White
Write-Host "- Android SDK: $SdkDir" -ForegroundColor Gray
Write-Host "- Java JDK: $env:JAVA_HOME" -ForegroundColor Gray
Write-Host "- Tools: $DevDir" -ForegroundColor Gray
Write-Host ""

Read-Host "Press Enter to exit"
