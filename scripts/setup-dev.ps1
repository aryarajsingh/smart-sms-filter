# Smart SMS Filter - Development Environment Setup Script
# PowerShell script for Windows development setup

Write-Host "Setting up Smart SMS Filter development environment..." -ForegroundColor Green

# Check prerequisites
function Test-Prerequisites {
    Write-Host "Checking prerequisites..." -ForegroundColor Yellow
    
    # Check Java
    try {
        $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString().Split('"')[1] }
        Write-Host "✓ Java found: $javaVersion" -ForegroundColor Green
    }
    catch {
        Write-Host "✗ Java not found. Please install JDK 11 or newer." -ForegroundColor Red
        Write-Host "  Download from: https://adoptium.net/" -ForegroundColor Cyan
        return $false
    }
    
    # Check Android Studio
    $androidStudioPaths = @(
        "$env:LOCALAPPDATA\Google\AndroidStudio*",
        "$env:ProgramFiles\Android\Android Studio*",
        "${env:ProgramFiles(x86)}\Android\Android Studio*"
    )
    
    $androidStudioFound = $false
    foreach ($path in $androidStudioPaths) {
        if (Test-Path $path) {
            Write-Host "✓ Android Studio installation detected" -ForegroundColor Green
            $androidStudioFound = $true
            break
        }
    }
    
    if (-not $androidStudioFound) {
        Write-Host "✗ Android Studio not found" -ForegroundColor Red
        Write-Host "  Download from: https://developer.android.com/studio" -ForegroundColor Cyan
        return $false
    }
    
    # Check Git
    try {
        $gitVersion = git --version
        Write-Host "✓ Git found: $gitVersion" -ForegroundColor Green
    }
    catch {
        Write-Host "✗ Git not found. Please install Git for Windows." -ForegroundColor Red
        Write-Host "  Download from: https://git-scm.com/download/win" -ForegroundColor Cyan
        return $false
    }
    
    return $true
}

# Setup Android SDK environment
function Set-AndroidEnvironment {
    Write-Host "Setting up Android SDK environment..." -ForegroundColor Yellow
    
    # Try to detect Android SDK
    $sdkPaths = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:ANDROID_HOME",
        "$env:ANDROID_SDK_ROOT"
    )
    
    $sdkPath = $null
    foreach ($path in $sdkPaths) {
        if ($path -and (Test-Path $path)) {
            $sdkPath = $path
            break
        }
    }
    
    if ($sdkPath) {
        Write-Host "✓ Android SDK found at: $sdkPath" -ForegroundColor Green
        
        # Set environment variables
        [Environment]::SetEnvironmentVariable("ANDROID_HOME", $sdkPath, "User")
        [Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $sdkPath, "User")
        
        # Add to PATH
        $currentPath = [Environment]::GetEnvironmentVariable("Path", "User")
        $toolsPaths = @(
            "$sdkPath\tools",
            "$sdkPath\tools\bin",
            "$sdkPath\platform-tools"
        )
        
        foreach ($toolPath in $toolsPaths) {
            if ($currentPath -notlike "*$toolPath*") {
                $currentPath += ";$toolPath"
            }
        }
        
        [Environment]::SetEnvironmentVariable("Path", $currentPath, "User")
        Write-Host "✓ Android SDK environment variables set" -ForegroundColor Green
    }
    else {
        Write-Host "⚠ Android SDK not found. Please set ANDROID_HOME manually." -ForegroundColor Yellow
        Write-Host "  Typical location: $env:LOCALAPPDATA\Android\Sdk" -ForegroundColor Cyan
    }
}

# Setup Python environment for ML model training
function Set-PythonEnvironment {
    Write-Host "Setting up Python environment for model training..." -ForegroundColor Yellow
    
    # Check Python
    try {
        $pythonVersion = python --version
        Write-Host "✓ Python found: $pythonVersion" -ForegroundColor Green
        
        # Create virtual environment in models directory
        $modelsPath = Join-Path $PSScriptRoot "..\models"
        $venvPath = Join-Path $modelsPath "venv"
        
        if (-not (Test-Path $venvPath)) {
            Write-Host "Creating Python virtual environment..." -ForegroundColor Yellow
            python -m venv $venvPath
            
            # Activate and install requirements
            $activateScript = Join-Path $venvPath "Scripts\Activate.ps1"
            if (Test-Path $activateScript) {
                & $activateScript
                pip install tensorflow==2.13.0 numpy pandas scikit-learn transformers
                Write-Host "✓ Python dependencies installed" -ForegroundColor Green
            }
        }
        else {
            Write-Host "✓ Python virtual environment exists" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "⚠ Python not found. Install for ML model training (optional)." -ForegroundColor Yellow
        Write-Host "  Download from: https://www.python.org/downloads/" -ForegroundColor Cyan
    }
}

# Initialize Git repository
function Initialize-Git {
    Write-Host "Initializing Git repository..." -ForegroundColor Yellow
    
    $projectRoot = Split-Path $PSScriptRoot -Parent
    
    if (-not (Test-Path (Join-Path $projectRoot ".git"))) {
        Set-Location $projectRoot
        git init
        git add .
        git commit -m "Initial commit: Smart SMS Filter Android project setup"
        Write-Host "✓ Git repository initialized" -ForegroundColor Green
    }
    else {
        Write-Host "✓ Git repository already exists" -ForegroundColor Green
    }
}

# Create Android Gradle wrapper
function Set-GradleWrapper {
    Write-Host "Setting up Gradle wrapper..." -ForegroundColor Yellow
    
    $androidPath = Join-Path (Split-Path $PSScriptRoot -Parent) "android"
    
    if (-not (Test-Path (Join-Path $androidPath "gradlew"))) {
        Set-Location $androidPath
        
        # Create gradle wrapper files
        New-Item -ItemType Directory -Path "gradle\wrapper" -Force | Out-Null
        
        # Download gradle wrapper (simplified version)
        Write-Host "Creating Gradle wrapper files..." -ForegroundColor Yellow
        
        # Create gradle wrapper properties
        $wrapperProps = @"
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
"@
        $wrapperProps | Out-File -FilePath "gradle\wrapper\gradle-wrapper.properties" -Encoding UTF8
        
        Write-Host "✓ Gradle wrapper configured" -ForegroundColor Green
    }
    else {
        Write-Host "✓ Gradle wrapper already exists" -ForegroundColor Green
    }
}

# Create development aliases and shortcuts
function Set-DevAliases {
    Write-Host "Setting up development aliases..." -ForegroundColor Yellow
    
    # Create PowerShell profile if it doesn't exist
    if (-not (Test-Path $PROFILE)) {
        New-Item -ItemType File -Path $PROFILE -Force | Out-Null
    }
    
    # Add project-specific aliases
    $aliases = @"

# Smart SMS Filter Development Aliases
function sms-build { Set-Location '$((Split-Path $PSScriptRoot -Parent))\android'; .\gradlew build }
function sms-test { Set-Location '$((Split-Path $PSScriptRoot -Parent))\android'; .\gradlew test }
function sms-clean { Set-Location '$((Split-Path $PSScriptRoot -Parent))\android'; .\gradlew clean }
function sms-run { Set-Location '$((Split-Path $PSScriptRoot -Parent))\android'; .\gradlew installDebug }
function sms-studio { & "studio" '$((Split-Path $PSScriptRoot -Parent))\android' }

"@
    
    Add-Content -Path $PROFILE -Value $aliases
    Write-Host "✓ Development aliases added to PowerShell profile" -ForegroundColor Green
    Write-Host "  Restart PowerShell or run '. `$PROFILE' to load aliases" -ForegroundColor Cyan
}

# Main execution
function Main {
    Write-Host "=== Smart SMS Filter Development Setup ===" -ForegroundColor Cyan
    Write-Host ""
    
    if (-not (Test-Prerequisites)) {
        Write-Host "Please install missing prerequisites and run this script again." -ForegroundColor Red
        exit 1
    }
    
    Write-Host ""
    Set-AndroidEnvironment
    Write-Host ""
    Set-PythonEnvironment
    Write-Host ""
    Initialize-Git
    Write-Host ""
    Set-GradleWrapper
    Write-Host ""
    Set-DevAliases
    
    Write-Host ""
    Write-Host "=== Setup Complete! ===" -ForegroundColor Green
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Open Android Studio: File -> Open -> select the 'android' directory" -ForegroundColor White
    Write-Host "2. Wait for Gradle sync to complete" -ForegroundColor White
    Write-Host "3. Connect an Android device and run the app" -ForegroundColor White
    Write-Host "4. For ML model training, activate the Python environment:" -ForegroundColor White
    Write-Host "   cd models && venv\Scripts\Activate.ps1" -ForegroundColor Cyan
    Write-Host ""
}

# Run the setup
Main
