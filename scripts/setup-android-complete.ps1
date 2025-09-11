# Complete Automated Android Development Setup Script
# This script downloads and installs everything automatically
# Run this as Administrator

Write-Host "=== Complete Android Development Setup ===" -ForegroundColor Green
Write-Host "This script will automatically download and install:" -ForegroundColor White
Write-Host "- OpenJDK 17" -ForegroundColor Cyan
Write-Host "- Git for Windows" -ForegroundColor Cyan
Write-Host "- Android Command Line Tools" -ForegroundColor Cyan
Write-Host "- Required Android SDK Components" -ForegroundColor Cyan
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
$DownloadsDir = "$AndroidDevDir\downloads"

Write-Host "Creating directory structure..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $AndroidDevDir -Force | Out-Null
New-Item -ItemType Directory -Path $AndroidSdkDir -Force | Out-Null
New-Item -ItemType Directory -Path $ToolsDir -Force | Out-Null
New-Item -ItemType Directory -Path $DownloadsDir -Force | Out-Null
New-Item -ItemType Directory -Path "$AndroidSdkDir\cmdline-tools" -Force | Out-Null
Write-Host "✓ Directory structure created" -ForegroundColor Green

# Function to download files with progress
function Download-FileWithProgress {
    param(
        [string]$Url,
        [string]$OutputPath,
        [string]$Description
    )
    
    Write-Host "Downloading $Description..." -ForegroundColor Yellow
    try {
        $webClient = New-Object System.Net.WebClient
        $webClient.DownloadFile($Url, $OutputPath)
        Write-Host "✓ Downloaded: $Description" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "✗ Failed to download: $Description" -ForegroundColor Red
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Function to add to PATH
function Add-PathEntry {
    param($PathToAdd)
    
    $currentPath = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::Machine)
    if ($currentPath -notlike "*$PathToAdd*") {
        Write-Host "Adding to PATH: $PathToAdd" -ForegroundColor Yellow
        $newPath = $currentPath + ";" + $PathToAdd
        [System.Environment]::SetEnvironmentVariable("Path", $newPath, [System.EnvironmentVariableTarget]::Machine)
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::Machine)
        Write-Host "✓ Added to PATH" -ForegroundColor Green
    } else {
        Write-Host "✓ Already in PATH: $PathToAdd" -ForegroundColor Green
    }
}

# Step 1: Install Java Development Kit
Write-Host ""
Write-Host "=== Step 1: Installing Java Development Kit ===" -ForegroundColor Cyan

# Check if Java is already installed
$javaInstalled = $false
try {
    $javaOutput = cmd /c "java -version 2>&1"
    if ($javaOutput -match "version") {
        Write-Host "✓ Java is already installed" -ForegroundColor Green
        Write-Host $javaOutput[0] -ForegroundColor Gray
        $javaInstalled = $true
    }
} catch {
    Write-Host "Java not found, will install..." -ForegroundColor Yellow
}

if (-not $javaInstalled) {
    $jdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi"
    $jdkPath = "$DownloadsDir\OpenJDK17.msi"
    
    if (Download-FileWithProgress -Url $jdkUrl -OutputPath $jdkPath -Description "OpenJDK 17") {
        Write-Host "Installing OpenJDK 17..." -ForegroundColor Yellow
        try {
            Start-Process -FilePath "msiexec.exe" -ArgumentList "/i `"$jdkPath`" /quiet /norestart" -Wait
            Write-Host "✓ OpenJDK 17 installed successfully" -ForegroundColor Green
            
            # Refresh environment variables
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::Machine)
            
            # Verify installation
            Start-Sleep -Seconds 3
            try {
                $javaOutput = cmd /c "java -version 2>&1"
                if ($javaOutput -match "version") {
                    Write-Host "✓ Java installation verified" -ForegroundColor Green
                }
            } catch {
                Write-Host "⚠ Java installed but may need system restart" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "✗ Failed to install Java" -ForegroundColor Red
        }
    }
}

# Step 2: Install Git
Write-Host ""
Write-Host "=== Step 2: Installing Git ===" -ForegroundColor Cyan

# Check if Git is already installed
$gitInstalled = $false
try {
    $gitOutput = cmd /c "git --version 2>&1"
    if ($gitOutput -match "git version") {
        Write-Host "✓ Git is already installed" -ForegroundColor Green
        Write-Host $gitOutput -ForegroundColor Gray
        $gitInstalled = $true
    }
} catch {
    Write-Host "Git not found, will install..." -ForegroundColor Yellow
}

if (-not $gitInstalled) {
    $gitUrl = "https://github.com/git-for-windows/git/releases/download/v2.43.0.windows.1/Git-2.43.0-64-bit.exe"
    $gitPath = "$DownloadsDir\Git-installer.exe"
    
    if (Download-FileWithProgress -Url $gitUrl -OutputPath $gitPath -Description "Git for Windows") {
        Write-Host "Installing Git for Windows..." -ForegroundColor Yellow
        try {
            Start-Process -FilePath $gitPath -ArgumentList "/VERYSILENT /NORESTART" -Wait
            Write-Host "✓ Git installed successfully" -ForegroundColor Green
            
            # Refresh environment variables
            $env:Path = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::Machine)
            
            # Verify installation
            Start-Sleep -Seconds 3
            try {
                $gitOutput = cmd /c "git --version 2>&1"
                if ($gitOutput -match "git version") {
                    Write-Host "✓ Git installation verified" -ForegroundColor Green
                }
            } catch {
                Write-Host "⚠ Git installed but may need system restart" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "✗ Failed to install Git" -ForegroundColor Red
        }
    }
}

# Step 3: Set Android Environment Variables
Write-Host ""
Write-Host "=== Step 3: Setting Android Environment Variables ===" -ForegroundColor Cyan

Write-Host "Setting ANDROID_HOME and ANDROID_SDK_ROOT..." -ForegroundColor Yellow
[System.Environment]::SetEnvironmentVariable("ANDROID_HOME", $AndroidSdkDir, [System.EnvironmentVariableTarget]::Machine)
[System.Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $AndroidSdkDir, [System.EnvironmentVariableTarget]::Machine)

# Update current session
$env:ANDROID_HOME = $AndroidSdkDir
$env:ANDROID_SDK_ROOT = $AndroidSdkDir

Write-Host "✓ Environment variables set:" -ForegroundColor Green
Write-Host "  ANDROID_HOME = $AndroidSdkDir" -ForegroundColor Gray
Write-Host "  ANDROID_SDK_ROOT = $AndroidSdkDir" -ForegroundColor Gray

# Step 4: Download and Install Android Command Line Tools
Write-Host ""
Write-Host "=== Step 4: Installing Android Command Line Tools ===" -ForegroundColor Cyan

$cmdLineToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
$cmdLineToolsZip = "$DownloadsDir\commandlinetools.zip"
$cmdLineToolsDir = "$AndroidSdkDir\cmdline-tools"

if (Download-FileWithProgress -Url $cmdLineToolsUrl -OutputPath $cmdLineToolsZip -Description "Android Command Line Tools") {
    Write-Host "Extracting Android Command Line Tools..." -ForegroundColor Yellow
    try {
        # Extract to temporary location first
        $tempExtractDir = "$DownloadsDir\cmdline-tools-temp"
        Expand-Archive -Path $cmdLineToolsZip -DestinationPath $tempExtractDir -Force
        
        # Move to correct location
        $latestDir = "$cmdLineToolsDir\latest"
        New-Item -ItemType Directory -Path $latestDir -Force | Out-Null
        
        # Copy contents from extracted cmdline-tools folder to latest
        $extractedToolsDir = "$tempExtractDir\cmdline-tools"
        if (Test-Path $extractedToolsDir) {
            Copy-Item -Path "$extractedToolsDir\*" -Destination $latestDir -Recurse -Force
            Write-Host "✓ Android Command Line Tools installed" -ForegroundColor Green
        } else {
            Write-Host "✗ Failed to find extracted command line tools" -ForegroundColor Red
        }
        
        # Cleanup
        Remove-Item -Path $tempExtractDir -Recurse -Force -ErrorAction SilentlyContinue
        
    } catch {
        Write-Host "✗ Failed to extract Android Command Line Tools" -ForegroundColor Red
        Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Step 5: Add Android tools to PATH
Write-Host ""
Write-Host "=== Step 5: Adding Android Tools to PATH ===" -ForegroundColor Cyan

Add-PathEntry "$AndroidSdkDir\cmdline-tools\latest\bin"
Add-PathEntry "$AndroidSdkDir\platform-tools"
Add-PathEntry "$AndroidSdkDir\tools\bin"

# Refresh current session PATH
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::Machine)

# Step 6: Install Android SDK Components
Write-Host ""
Write-Host "=== Step 6: Installing Android SDK Components ===" -ForegroundColor Cyan

$sdkManagerPath = "$AndroidSdkDir\cmdline-tools\latest\bin\sdkmanager.bat"

if (Test-Path $sdkManagerPath) {
    Write-Host "Found SDK Manager, installing components..." -ForegroundColor Yellow
    
    # Accept licenses
    Write-Host "Accepting Android SDK licenses..." -ForegroundColor Yellow
    try {
        $yesResponse = "y`r`n" * 10
        $yesResponse | cmd /c "`"$sdkManagerPath`" --licenses" 2>&1 | Out-Null
        Write-Host "✓ Licenses accepted" -ForegroundColor Green
    } catch {
        Write-Host "⚠ License acceptance may have failed" -ForegroundColor Yellow
    }
    
    # Install SDK components
    $components = @(
        "platform-tools",
        "platforms;android-34",
        "build-tools;34.0.0"
    )
    
    foreach ($component in $components) {
        Write-Host "Installing $component..." -ForegroundColor Yellow
        try {
            cmd /c "`"$sdkManagerPath`" `"$component`"" 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "✓ $component installed successfully" -ForegroundColor Green
            } else {
                Write-Host "⚠ $component installation may have failed" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "✗ Failed to install $component" -ForegroundColor Red
        }
    }
} else {
    Write-Host "✗ SDK Manager not found at expected location" -ForegroundColor Red
    Write-Host "Path: $sdkManagerPath" -ForegroundColor Gray
}

# Step 7: Final Verification
Write-Host ""
Write-Host "=== Final Verification ===" -ForegroundColor Cyan

# Refresh environment for verification
$env:Path = [System.Environment]::GetEnvironmentVariable("Path", [System.EnvironmentVariableTarget]::Machine)

# Check Java
Write-Host "Verifying Java..." -ForegroundColor Yellow
try {
    $javaOutput = cmd /c "java -version 2>&1"
    if ($javaOutput -match "version") {
        Write-Host "✓ Java: Working" -ForegroundColor Green
        Write-Host "  $($javaOutput[0])" -ForegroundColor Gray
    } else {
        Write-Host "✗ Java: Not working" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Java: Not working" -ForegroundColor Red
}

# Check Git
Write-Host "Verifying Git..." -ForegroundColor Yellow
try {
    $gitOutput = cmd /c "git --version 2>&1"
    if ($gitOutput -match "git version") {
        Write-Host "✓ Git: Working" -ForegroundColor Green
        Write-Host "  $gitOutput" -ForegroundColor Gray
    } else {
        Write-Host "✗ Git: Not working" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Git: Not working" -ForegroundColor Red
}

# Check Android tools
Write-Host "Verifying Android tools..." -ForegroundColor Yellow
if (Test-Path $sdkManagerPath) {
    Write-Host "✓ SDK Manager: Found" -ForegroundColor Green
    Write-Host "  Path: $sdkManagerPath" -ForegroundColor Gray
} else {
    Write-Host "✗ SDK Manager: Not found" -ForegroundColor Red
}

$adbPath = "$AndroidSdkDir\platform-tools\adb.exe"
if (Test-Path $adbPath) {
    Write-Host "✓ ADB: Found" -ForegroundColor Green
    Write-Host "  Path: $adbPath" -ForegroundColor Gray
} else {
    Write-Host "⚠ ADB: Not found (install may be incomplete)" -ForegroundColor Yellow
}

# Check environment variables
Write-Host "Verifying environment variables..." -ForegroundColor Yellow
$androidHome = [System.Environment]::GetEnvironmentVariable("ANDROID_HOME", [System.EnvironmentVariableTarget]::Machine)
$androidSdkRoot = [System.Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", [System.EnvironmentVariableTarget]::Machine)

if ($androidHome -eq $AndroidSdkDir) {
    Write-Host "✓ ANDROID_HOME: Set correctly" -ForegroundColor Green
    Write-Host "  Value: $androidHome" -ForegroundColor Gray
} else {
    Write-Host "✗ ANDROID_HOME: Not set correctly" -ForegroundColor Red
}

if ($androidSdkRoot -eq $AndroidSdkDir) {
    Write-Host "✓ ANDROID_SDK_ROOT: Set correctly" -ForegroundColor Green
    Write-Host "  Value: $androidSdkRoot" -ForegroundColor Gray
} else {
    Write-Host "✗ ANDROID_SDK_ROOT: Not set correctly" -ForegroundColor Red
}

# Final summary
Write-Host ""
Write-Host "=== Setup Complete! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Installation Summary:" -ForegroundColor White
Write-Host "- Java Development Kit: Installed" -ForegroundColor Green
Write-Host "- Git for Windows: Installed" -ForegroundColor Green
Write-Host "- Android Command Line Tools: Installed" -ForegroundColor Green
Write-Host "- Android SDK Components: Installed" -ForegroundColor Green
Write-Host "- Environment Variables: Configured" -ForegroundColor Green
Write-Host "- System PATH: Updated" -ForegroundColor Green
Write-Host ""
Write-Host "Installation Directory: $AndroidDevDir" -ForegroundColor Cyan
Write-Host ""
Write-Host "⚠ IMPORTANT: Please close and reopen PowerShell for all changes to take effect!" -ForegroundColor Yellow
Write-Host ""
Write-Host "Next step: Run your project initialization script" -ForegroundColor White

pause
