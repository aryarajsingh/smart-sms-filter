# Run Tests for Smart SMS Filter Android App
# This script runs unit tests and code quality checks

Write-Host "=== Running Smart SMS Filter Tests ===" -ForegroundColor Cyan
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

# Verify development environment
Write-Host "Checking test environment..." -ForegroundColor Yellow

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | Select-Object -First 1
    Write-Host "✅ Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Java not found. Please run setup-android-cli.ps1 first." -ForegroundColor Red
    exit 1
}

# Check Android SDK
if ($env:ANDROID_HOME) {
    Write-Host "✅ Android SDK: $env:ANDROID_HOME" -ForegroundColor Green
} else {
    Write-Host "❌ ANDROID_HOME not set. Please run setup-android-cli.ps1 first." -ForegroundColor Red
    exit 1
}

Write-Host ""

# Navigate to android directory
Set-Location $androidDir

$testsPassed = $true

try {
    Write-Host "Running unit tests..." -ForegroundColor Green
    Write-Host ""
    
    # Run unit tests
    & .\gradlew.bat test
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Unit tests passed!" -ForegroundColor Green
    } else {
        Write-Host ""
        Write-Host "❌ Unit tests failed!" -ForegroundColor Red
        $testsPassed = $false
    }
    
    Write-Host ""
    Write-Host "Running lint checks..." -ForegroundColor Green
    Write-Host ""
    
    # Run lint checks
    & .\gradlew.bat lint
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✅ Lint checks passed!" -ForegroundColor Green
        
        # Check if lint report was generated
        $lintReportPath = "app\build\reports\lint-results.html"
        if (Test-Path $lintReportPath) {
            Write-Host ""
            Write-Host "📊 Lint report generated:" -ForegroundColor Cyan
            Write-Host "   $((Get-Item $lintReportPath).FullName)" -ForegroundColor White
        }
        
    } else {
        Write-Host ""
        Write-Host "⚠️  Lint checks found issues" -ForegroundColor Yellow
        
        # Check if lint report was generated
        $lintReportPath = "app\build\reports\lint-results.html"
        if (Test-Path $lintReportPath) {
            Write-Host ""
            Write-Host "📊 Lint report with issues:" -ForegroundColor Yellow
            Write-Host "   $((Get-Item $lintReportPath).FullName)" -ForegroundColor White
            Write-Host ""
            Write-Host "Open this file in a web browser to see detailed lint issues." -ForegroundColor Gray
        }
    }
    
    Write-Host ""
    Write-Host "Checking test coverage..." -ForegroundColor Green
    Write-Host ""
    
    # Run tests with coverage (if available)
    & .\gradlew.bat testDebugUnitTestCoverage 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Test coverage report generated!" -ForegroundColor Green
        
        # Look for coverage reports
        $coverageReports = Get-ChildItem -Path "app\build\reports" -Filter "*coverage*" -Recurse -Directory 2>$null
        if ($coverageReports) {
            Write-Host ""
            Write-Host "📊 Coverage reports:" -ForegroundColor Cyan
            $coverageReports | ForEach-Object {
                $htmlReport = Get-ChildItem -Path $_.FullName -Filter "index.html" 2>$null
                if ($htmlReport) {
                    Write-Host "   $($htmlReport.FullName)" -ForegroundColor White
                }
            }
        }
    } else {
        Write-Host "ℹ️  Test coverage not available (this is normal for new projects)" -ForegroundColor Gray
    }
    
    Write-Host ""
    
    if ($testsPassed) {
        Write-Host "🎉 All tests and checks completed successfully!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Test reports location:" -ForegroundColor White
        Write-Host "• Unit test results: app\build\reports\tests\testDebugUnitTest\index.html" -ForegroundColor Gray
        Write-Host "• Lint results: app\build\reports\lint-results.html" -ForegroundColor Gray
        Write-Host "• Coverage report: app\build\reports\coverage (if available)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "Next steps:" -ForegroundColor White
        Write-Host "• Build the app: .\scripts\build-android.ps1" -ForegroundColor Gray
        Write-Host "• Install on device: .\scripts\run-android.ps1" -ForegroundColor Gray
        
    } else {
        Write-Host "❌ Some tests failed!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Check the test results above and fix any issues." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Useful commands:" -ForegroundColor White
        Write-Host "• Run specific test: .\gradlew.bat test --tests YourTestClass" -ForegroundColor Gray
        Write-Host "• Clean and test: .\scripts\clean-android.ps1 && .\scripts\test-android.ps1" -ForegroundColor Gray
        
        exit 1
    }
    
} catch {
    Write-Host ""
    Write-Host "❌ Test error: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
    
} finally {
    # Return to project root
    Set-Location $projectRoot
}

Write-Host ""
Write-Host "=== Testing Complete ===" -ForegroundColor Cyan
