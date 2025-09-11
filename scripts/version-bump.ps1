# Version Bump Script for Smart SMS Filter
# Usage: .\scripts\version-bump.ps1 -Type [patch|minor|major] -Message "commit message"

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("patch", "minor", "major")]
    [string]$Type,
    
    [Parameter(Mandatory=$true)]
    [string]$Message
)

# Get current directory
$projectRoot = Split-Path -Parent $PSScriptRoot
$buildGradlePath = Join-Path $projectRoot "android\app\build.gradle"

# Read current version from build.gradle
$buildGradleContent = Get-Content $buildGradlePath
$versionCodeLine = $buildGradleContent | Where-Object { $_ -match "versionCode\s+(\d+)" }
$versionNameLine = $buildGradleContent | Where-Object { $_ -match 'versionName\s+"([^"]+)"' }

$currentVersionCode = [int]($versionCodeLine -replace ".*versionCode\s+(\d+).*", '$1')
$currentVersionName = ($versionNameLine -replace '.*versionName\s+"([^"]+)".*', '$1')

Write-Host "Current version: $currentVersionName (code: $currentVersionCode)"

# Parse current version
$versionParts = $currentVersionName.Split('.')
$major = [int]$versionParts[0]
$minor = [int]$versionParts[1] 
$patch = [int]$versionParts[2]

# Increment version based on type
switch ($Type) {
    "patch" { 
        $patch += 1 
    }
    "minor" { 
        $minor += 1
        $patch = 0 
    }
    "major" { 
        $major += 1
        $minor = 0
        $patch = 0 
    }
}

$newVersionName = "$major.$minor.$patch"
$newVersionCode = $currentVersionCode + 1

Write-Host "New version: $newVersionName (code: $newVersionCode)"

# Update build.gradle
$buildGradleContent = $buildGradleContent -replace "versionCode\s+\d+", "versionCode $newVersionCode"
$buildGradleContent = $buildGradleContent -replace 'versionName\s+"[^"]+"', "versionName `"$newVersionName`""
$buildGradleContent | Set-Content $buildGradlePath

# Update CHANGELOG.md
$changelogPath = Join-Path $projectRoot "CHANGELOG.md"
$currentDate = Get-Date -Format "yyyy-MM-dd"
$newEntry = @"
## [$newVersionName] - $currentDate
### Changed
- $Message

"@

$changelogContent = Get-Content $changelogPath -Raw
$insertPoint = $changelogContent.IndexOf("## [")
$newChangelogContent = $changelogContent.Insert($insertPoint, $newEntry)
$newChangelogContent | Set-Content $changelogPath

# Git operations
Write-Host "Committing changes..."
git add .
git commit -m "$Type`: $Message

- Version bumped to $newVersionName
- Updated versionCode to $newVersionCode"

git tag "v$newVersionName"

Write-Host "Version bumped to $newVersionName successfully!"
Write-Host "To push to GitHub: git push origin master --tags"
