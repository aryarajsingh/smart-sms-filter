# Clean Smart SMS Filter Android Build
# This script cleans build files and resets the build environment

Write-Host "=== Cleaning Smart SMS Filter Build ===" -ForegroundColor Cyan
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

Write-Host "Cleaning build files..." -ForegroundColor Yellow

# Navigate to android directory
Set-Location $androidDir

try {
    # Get initial build directory size for comparison
    $buildDirs = @()
    $buildDirs += Get-ChildItem -Path "." -Filter "build" -Recurse -Directory 2>$null
    $buildDirs += Get-ChildItem -Path "." -Filter ".gradle" -Recurse -Directory 2>$null
    
    $initialSizeMB = 0
    if ($buildDirs) {
        foreach ($dir in $buildDirs) {
            try {
                $size = Get-ChildItem -Path $dir.FullName -Recurse -File 2>$null | Measure-Object -Property Length -Sum
                if ($size.Sum) {
                    $initialSizeMB += $size.Sum / 1MB
                }
            } catch {
                # Ignore errors for individual directories
            }
        }
    }
    
    if ($initialSizeMB -gt 0) {
        Write-Host "Build directory size before cleaning: $([math]::Round($initialSizeMB, 2)) MB" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "Running Gradle clean..." -ForegroundColor Green
    
    # Run gradle clean
    & .\gradlew.bat clean
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Gradle clean completed!" -ForegroundColor Green
        
        # Additional cleanup
        Write-Host ""
        Write-Host "Performing additional cleanup..." -ForegroundColor Yellow
        
        $cleanedItems = @()
        
        # Clean additional build artifacts
        $artifactPaths = @(
            "app\build",
            ".gradle",
            "build",
            "*.iml",
            ".idea\modules"
        )
        
        foreach ($pattern in $artifactPaths) {
            try {
                $items = Get-ChildItem -Path $pattern -Force 2>$null
                if ($items) {
                    foreach ($item in $items) {
                        try {
                            if ($item.PSIsContainer) {
                                Remove-Item -Path $item.FullName -Recurse -Force 2>$null
                            } else {
                                Remove-Item -Path $item.FullName -Force 2>$null
                            }
                            $cleanedItems += $item.Name
                        } catch {
                            # Ignore individual file cleanup errors
                        }
                    }
                }
            } catch {
                # Ignore pattern errors
            }
        }
        
        # Clean Gradle daemon cache if requested
        Write-Host ""
        Write-Host "Stopping Gradle daemon..." -ForegroundColor Yellow
        & .\gradlew.bat --stop 2>$null
        
        if ($cleanedItems.Count -gt 0) {
            Write-Host ""
            Write-Host "✅ Additional cleanup completed:" -ForegroundColor Green
            $cleanedItems | Select-Object -Unique | ForEach-Object { Write-Host "   • $_" -ForegroundColor Gray }
        }
        
        # Calculate cleaned space
        $remainingSizeMB = 0
        $remainingBuildDirs = Get-ChildItem -Path "." -Filter "build" -Recurse -Directory 2>$null
        if ($remainingBuildDirs) {
            foreach ($dir in $remainingBuildDirs) {
                try {
                    $size = Get-ChildItem -Path $dir.FullName -Recurse -File 2>$null | Measure-Object -Property Length -Sum
                    if ($size.Sum) {
                        $remainingSizeMB += $size.Sum / 1MB
                    }
                } catch {
                    # Ignore errors
                }
            }
        }
        
        $cleanedMB = $initialSizeMB - $remainingSizeMB
        if ($cleanedMB -gt 0) {
            Write-Host ""
            Write-Host "💾 Disk space freed: $([math]::Round($cleanedMB, 2)) MB" -ForegroundColor Cyan
        }
        
        Write-Host ""
        Write-Host "🎉 Clean completed successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "The project is now ready for a fresh build:" -ForegroundColor White
        Write-Host "• Build: .\scripts\build-android.ps1" -ForegroundColor Gray
        Write-Host "• Test: .\scripts\test-android.ps1" -ForegroundColor Gray
        Write-Host "• Install: .\scripts\run-android.ps1" -ForegroundColor Gray
        
    } else {
        Write-Host ""
        Write-Host "❌ Gradle clean failed!" -ForegroundColor Red
        Write-Host ""
        Write-Host "This might happen if:" -ForegroundColor Yellow
        Write-Host "• Build files are locked by running processes" -ForegroundColor Gray
        Write-Host "• Android Studio or other tools are running" -ForegroundColor Gray
        Write-Host "• Permission issues with build directories" -ForegroundColor Gray
        Write-Host ""
        Write-Host "Solutions to try:" -ForegroundColor White
        Write-Host "1. Close Android Studio and any running emulators" -ForegroundColor Gray
        Write-Host "2. Stop all Java/Gradle processes" -ForegroundColor Gray
        Write-Host "3. Run PowerShell as Administrator" -ForegroundColor Gray
        Write-Host "4. Manually delete build directories if needed" -ForegroundColor Gray
        
        exit 1
    }
    
} catch {
    Write-Host ""
    Write-Host "❌ Clean error: $($_.Exception.Message)" -ForegroundColor Red
    
    Write-Host ""
    Write-Host "Manual cleanup options:" -ForegroundColor Yellow
    Write-Host "• Delete build directories manually" -ForegroundColor Gray
    Write-Host "• Restart your computer to free locked files" -ForegroundColor Gray
    Write-Host "• Check for running Android Studio instances" -ForegroundColor Gray
    
    exit 1
    
} finally {
    # Return to project root
    Set-Location $projectRoot
}

Write-Host ""
Write-Host "=== Clean Complete ===" -ForegroundColor Cyan
