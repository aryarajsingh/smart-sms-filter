#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Installs git hooks for the project
.DESCRIPTION
    This script installs git hooks that automatically sync documentation
    before commits to prevent discrepancies.
.EXAMPLE
    ./scripts/install-hooks.ps1
#>

$ErrorActionPreference = "Stop"

$rootDir = Join-Path -Path $PSScriptRoot -ChildPath ".."
$gitHooksDir = Join-Path -Path (Join-Path -Path $rootDir -ChildPath ".git") -ChildPath "hooks"
$preCommitHook = Join-Path -Path $gitHooksDir -ChildPath "pre-commit"

# Create pre-commit hook content
$hookContent = @'
#!/bin/sh
# Auto-sync documentation before commit

# Check if any android documentation files are staged
if git diff --cached --name-only | grep -q "^android/.*\(README\|CHANGELOG\)\.md$"; then
    echo "Android documentation changed, syncing to root..."
    
    # Run sync script
    if [ -f "scripts/sync-docs.ps1" ]; then
        pwsh scripts/sync-docs.ps1
        
        # Add synced files to the commit
        git add README.md CHANGELOG.md 2>/dev/null || true
        
        echo "Documentation synced successfully"
    else
        echo "Warning: sync-docs.ps1 not found, skipping sync"
    fi
fi

exit 0
'@

function Install-GitHooks {
    Write-Host "Installing Git hooks..." -ForegroundColor Cyan
    
    # Check if .git directory exists
    $gitPath = Join-Path -Path $rootDir -ChildPath ".git"
    if (-not (Test-Path $gitPath)) {
        Write-Host "Error: Not a git repository" -ForegroundColor Red
        exit 1
    }
    
    # Create hooks directory if it doesn't exist
    if (-not (Test-Path $gitHooksDir)) {
        New-Item -ItemType Directory -Path $gitHooksDir -Force | Out-Null
    }
    
    # Write pre-commit hook
    Set-Content -Path $preCommitHook -Value $hookContent -Encoding UTF8
    
    # Make hook executable on Unix-like systems
    if ($IsLinux -or $IsMacOS) {
        chmod +x $preCommitHook
    }
    
    Write-Host "Pre-commit hook installed successfully" -ForegroundColor Green
    Write-Host ""
    Write-Host "The following hooks have been installed:" -ForegroundColor Yellow
    Write-Host "  - pre-commit: Automatically syncs documentation from android/ to root" -ForegroundColor White
    Write-Host ""
    Write-Host "To test the hook, try:" -ForegroundColor Cyan
    Write-Host "  1. Edit android/README.md" -ForegroundColor White
    Write-Host "  2. git add android/README.md" -ForegroundColor White
    Write-Host "  3. git commit -m 'test'" -ForegroundColor White
    Write-Host ""
    Write-Host "The root README.md will be automatically updated!" -ForegroundColor Green
}

# Main execution
try {
    Install-GitHooks
} catch {
    $errorMsg = $_.Exception.Message
    Write-Host "Error installing hooks: $errorMsg" -ForegroundColor Red
    exit 1
}
