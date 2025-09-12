#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Synchronizes documentation files from android directory to root
.DESCRIPTION
    This script ensures that the root directory always has the latest
    documentation from the android project, maintaining a single source of truth.
.EXAMPLE
    ./scripts/sync-docs.ps1
#>

param(
    [switch]$Force = $false,
    [switch]$Verbose = $false
)

$ErrorActionPreference = "Stop"

# Define source and destination paths
$androidDir = Join-Path -Path (Join-Path -Path $PSScriptRoot -ChildPath "..") -ChildPath "android"
$rootDir = Join-Path -Path $PSScriptRoot -ChildPath ".."

# Define files to sync
$filesToSync = @(
    @{
        Source = "README.md"
        Destination = "README.md"
        Transform = $true  # Apply root-specific transformations
    },
    @{
        Source = "CHANGELOG.md"
        Destination = "CHANGELOG.md"
        Transform = $false
    }
)

function Write-Status {
    param([string]$Message, [string]$Type = "Info")
    
    $color = switch ($Type) {
        "Success" { "Green" }
        "Warning" { "Yellow" }
        "Error" { "Red" }
        default { "White" }
    }
    
    Write-Host "[$Type] $Message" -ForegroundColor $color
}

function Transform-RootReadme {
    param([string]$Content)
    
    # Add root-specific header if needed
    $header = @"
<!-- This file is auto-generated from android/README.md. Do not edit directly. -->
<!-- Last synchronized: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss") -->

"@
    
    # Fix relative paths for root context
    $Content = $Content -replace '\./gradlew', './android/gradlew'
    $Content = $Content -replace 'cd smart-sms-filter/android', 'cd smart-sms-filter'
    $Content = $Content -replace '\(CHANGELOG\.md\)', '(android/CHANGELOG.md)'
    $Content = $Content -replace '\(CONTRIBUTING\.md\)', '(android/CONTRIBUTING.md)'
    
    # Add navigation to android directory for development
    $navigation = @"

## 📁 Project Structure

The main Android application is located in the `android/` directory. All development should be done there.

\`\`\`
smart-sms-filter/
├── android/          # Main Android application
│   ├── app/         # Application module
│   ├── gradle/      # Gradle wrapper
│   └── README.md    # Source documentation (edit this)
├── docs/            # Additional documentation
├── models/          # ML models (future)
├── scripts/         # Utility scripts
└── README.md        # Auto-synced from android/README.md
\`\`\`

> **Note:** Always edit documentation in `android/` directory. Root files are auto-synchronized.

"@
    
    # Insert navigation after the Quick Start section
    $lines = $Content -split "`n"
    $result = @()
    $inserted = $false
    
    foreach ($line in $lines) {
        $result += $line
        if ($line -match "^## Technical Stack" -and -not $inserted) {
            $result += $navigation
            $inserted = $true
        }
    }
    
    if (-not $inserted) {
        # If section not found, add at the beginning after header
        return $header + $Content + $navigation
    }
    
    return $header + ($result -join "`n")
}

function Sync-Documentation {
    Write-Status "Starting documentation synchronization..." "Info"
    
    foreach ($file in $filesToSync) {
        $sourcePath = Join-Path $androidDir $file.Source
        $destPath = Join-Path $rootDir $file.Destination
        
        if (-not (Test-Path $sourcePath)) {
            Write-Status "Source file not found: $sourcePath" "Warning"
            continue
        }
        
        Write-Status "Syncing $($file.Source)..." "Info"
        
        # Read source content
        $content = Get-Content $sourcePath -Raw
        
        # Apply transformations if needed
        if ($file.Transform) {
            $content = Transform-RootReadme -Content $content
        }
        
        # Check if destination exists and compare
        $needsUpdate = $true
        if (Test-Path $destPath) {
            $existingContent = Get-Content $destPath -Raw
            if ($existingContent -eq $content) {
                $needsUpdate = $false
                Write-Status "$($file.Destination) is already up to date" "Success"
            }
        }
        
        if ($needsUpdate -or $Force) {
            # Write to destination
            Set-Content -Path $destPath -Value $content -Encoding UTF8
            Write-Status "Updated $($file.Destination)" "Success"
        }
    }
    
    Write-Status "Documentation synchronization complete!" "Success"
}

# Main execution
try {
    Sync-Documentation
    
    # Check if we're in a git repository
    $gitDir = Join-Path $rootDir ".git"
    if (Test-Path $gitDir) {
        Write-Status "Checking git status..." "Info"
        
        Set-Location $rootDir
        $gitStatus = git status --porcelain
        
        if ($gitStatus) {
            Write-Status "Changes detected in documentation files" "Warning"
            Write-Host ""
            Write-Host "To commit these changes, run:" -ForegroundColor Cyan
            Write-Host "  git add README.md CHANGELOG.md" -ForegroundColor Yellow
            Write-Host "  git commit -m 'docs: Sync documentation from android directory'" -ForegroundColor Yellow
            Write-Host "  git push origin master" -ForegroundColor Yellow
        } else {
            Write-Status "No changes detected" "Info"
        }
    }
} catch {
    Write-Status "Error: $_" "Error"
    exit 1
}
