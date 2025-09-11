# Fully Automated Android Development Setup Script (Hardened)
# Run as Administrator (PowerShell 5.1+)
$ErrorActionPreference = "Stop"
$ProgressPreference = 'SilentlyContinue'

Write-Host "=== Android Development Setup (Automated) ===" -ForegroundColor Green

# Ensure TLS 1.2 for downloads
try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 } catch {}

# Admin check
$principal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
  Write-Host "ERROR: Please run this script in an elevated PowerShell (Run as Administrator)." -ForegroundColor Red
  exit 1
}

# Constants/paths
$BaseDir = "C:\AndroidDev"
$SdkDir = Join-Path $BaseDir "Android\Sdk"
$Downloads = Join-Path $BaseDir "downloads"
$CmdlineToolsZip = Join-Path $Downloads "commandlinetools.zip"
$CmdlineToolsTemp = Join-Path $Downloads "cmdline-tools-temp"
$CmdlineToolsLatest = Join-Path $SdkDir "cmdline-tools\latest"

# Create directories
Write-Host "Creating directories..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $BaseDir -Force | Out-Null
New-Item -ItemType Directory -Path $SdkDir -Force | Out-Null
New-Item -ItemType Directory -Path $Downloads -Force | Out-Null
New-Item -ItemType Directory -Path (Split-Path $CmdlineToolsLatest -Parent) -Force | Out-Null

# Util: Retry helper
function Invoke-WithRetry {
  param([ScriptBlock]$Script, [int]$Retries = 3, [int]$DelaySec = 3)
  for ($i=1; $i -le $Retries; $i++) {
    try { return & $Script } catch {
      if ($i -eq $Retries) { throw }
      Start-Sleep -Seconds $DelaySec
    }
  }
}

# Util: Robust download (IWR -> BITS -> curl), with retries
function Download-Resilient {
  param([Parameter(Mandatory=$true)][string]$Url, [Parameter(Mandatory=$true)][string]$OutFile)
  Invoke-WithRetry -Retries 3 -DelaySec 4 -Script {
    try {
      Write-Host "Downloading (Invoke-WebRequest): $Url" -ForegroundColor Yellow
      Invoke-WebRequest -UseBasicParsing -Uri $Url -OutFile $OutFile
      if (-not (Test-Path $OutFile) -or ((Get-Item $OutFile).Length -lt 1024)) { throw "IWR produced empty/small file" }
      return
    } catch {
      Write-Host "IWR failed: $($_.Exception.Message). Trying BITS..." -ForegroundColor DarkYellow
      try {
        Start-BitsTransfer -Source $Url -Destination $OutFile -ErrorAction Stop
        if (-not (Test-Path $OutFile) -or ((Get-Item $OutFile).Length -lt 1024)) { throw "BITS produced empty/small file" }
        return
      } catch {
        Write-Host "BITS failed: $($_.Exception.Message). Trying curl..." -ForegroundColor DarkYellow
        & curl.exe -L --retry 3 --retry-delay 3 -o $OutFile $Url
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $OutFile) -or ((Get-Item $OutFile).Length -lt 1024)) {
          throw "curl failed or file invalid"
        }
      }
    }
  }
}

# Util: Add PATH entry (Machine level)
function Add-Path-Machine {
  param([Parameter(Mandatory=$true)][string]$PathToAdd)
  $currentPath = [Environment]::GetEnvironmentVariable("Path", "Machine")
  if ($currentPath -notlike "*${PathToAdd}*") {
    Write-Host "Adding to PATH: $PathToAdd" -ForegroundColor Yellow
    $newPath = ($currentPath.TrimEnd(';')) + ";" + $PathToAdd
    [Environment]::SetEnvironmentVariable("Path", $newPath, "Machine")
  } else {
    Write-Host "Already on PATH: $PathToAdd" -ForegroundColor Green
  }
  # Refresh current session PATH
  $env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine")
}

# Detect package managers
$HasWinget = $false
$HasChoco = $false
try { $null = winget --version 2>$null; if ($LASTEXITCODE -eq 0) { $HasWinget = $true } } catch {}
try { $null = choco --version 2>$null; if ($LASTEXITCODE -eq 0) { $HasChoco = $true } } catch {}

# Step 1: Install Java (Temurin 17) via winget/choco/fallback
$JavaOk = $false
try {
  $jv = & cmd /c "java -version 2>&1"
  if ($jv -match "version") { $JavaOk = $true }
} catch {}

if (-not $JavaOk) {
  Write-Host "Java not detected. Installing Temurin 17..." -ForegroundColor Yellow
  if ($HasWinget) {
    Write-Host "Using winget to install Temurin 17..." -ForegroundColor Yellow
    winget install -e --id EclipseAdoptium.Temurin.17.JDK --accept-source-agreements --accept-package-agreements --silent
  } elseif ($HasChoco) {
    Write-Host "Using Chocolatey to install Temurin 17..." -ForegroundColor Yellow
    choco install temurin17 -y --no-progress
  } else {
    $JdkMsi = Join-Path $Downloads "temurin17.msi"
    $JdkUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jdk_x64_windows_hotspot_17.0.9_9.msi"
    Download-Resilient -Url $JdkUrl -OutFile $JdkMsi
    Start-Process msiexec.exe -ArgumentList "/i `"$JdkMsi`" /qn /norestart" -Wait
  }
  Start-Sleep -Seconds 3
  # Try detect JAVA_HOME from standard install path
  $jdkRoot = "C:\Program Files\Eclipse Adoptium"
  $javaHome = $null
  if (Test-Path $jdkRoot) {
    $candidate = Get-ChildItem -Path $jdkRoot -Filter "jdk-17*" -Directory | Sort-Object Name -Descending | Select-Object -First 1
    if ($candidate) { $javaHome = $candidate.FullName }
  }
  if ($javaHome) {
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $javaHome, "Machine")
    Add-Path-Machine (Join-Path $javaHome "bin")
  }
  try { $jv = & cmd /c "java -version 2>&1"; if ($jv -match "version") { $JavaOk = $true } } catch {}
}
if ($JavaOk) {
  Write-Host "Java status: OK" -ForegroundColor Green
} else {
  Write-Host "Java status: NOT OK" -ForegroundColor Red
}

# Step 2: Install Git for Windows via winget/choco/fallback
$GitOk = $false
try { $gv = & cmd /c "git --version 2>&1"; if ($gv -match "git version") { $GitOk = $true } } catch {}

if (-not $GitOk) {
  Write-Host "Git not detected. Installing Git for Windows..." -ForegroundColor Yellow
  if ($HasWinget) {
    Write-Host "Using winget to install Git..." -ForegroundColor Yellow
    winget install -e --id Git.Git --accept-source-agreements --accept-package-agreements --silent
  } elseif ($HasChoco) {
    Write-Host "Using Chocolatey to install Git..." -ForegroundColor Yellow
    choco install git -y --no-progress
  } else {
    $GitExe = Join-Path $Downloads "git-installer.exe"
    $GitUrl = "https://github.com/git-for-windows/git/releases/download/v2.43.0.windows.1/Git-2.43.0-64-bit.exe"
    Download-Resilient -Url $GitUrl -OutFile $GitExe
    Start-Process -FilePath $GitExe -ArgumentList "/VERYSILENT /NORESTART" -Wait
  }
  Start-Sleep -Seconds 3
  try { $gv = & cmd /c "git --version 2>&1"; if ($gv -match "git version") { $GitOk = $true } } catch {}
}
if ($GitOk) {
  Write-Host "Git status: OK" -ForegroundColor Green
} else {
  Write-Host "Git status: NOT OK" -ForegroundColor Red
}

# Step 3: Configure ANDROID_HOME and ANDROID_SDK_ROOT
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $SdkDir, "Machine")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $SdkDir, "Machine")
$env:ANDROID_HOME = $SdkDir
$env:ANDROID_SDK_ROOT = $SdkDir
Write-Host "Configured ANDROID_HOME and ANDROID_SDK_ROOT" -ForegroundColor Green

# Step 4: Download and install Android Command Line Tools
$SdkToolsOk = $false
try {
  Write-Host "Installing Android command line tools..." -ForegroundColor Yellow
  if (Test-Path $CmdlineToolsTemp) { Remove-Item -Recurse -Force $CmdlineToolsTemp }
  if (Test-Path $CmdlineToolsZip) { Remove-Item -Force $CmdlineToolsZip }
  $ToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
  Download-Resilient -Url $ToolsUrl -OutFile $CmdlineToolsZip
  Expand-Archive -Path $CmdlineToolsZip -DestinationPath $CmdlineToolsTemp -Force
  New-Item -ItemType Directory -Path $CmdlineToolsLatest -Force | Out-Null
  $extracted = Join-Path $CmdlineToolsTemp "cmdline-tools"
  if (Test-Path $extracted) {
    Copy-Item -Path (Join-Path $extracted "*") -Destination $CmdlineToolsLatest -Recurse -Force
    $SdkToolsOk = $true
  }
  if (Test-Path $CmdlineToolsTemp) { Remove-Item -Recurse -Force $CmdlineToolsTemp }
  if (Test-Path $CmdlineToolsZip) { Remove-Item -Force $CmdlineToolsZip }
} catch {
  Write-Host "Failed to install Android command line tools: $($_.Exception.Message)" -ForegroundColor Red
}
if ($SdkToolsOk) {
  Write-Host "Android cmdline tools: OK" -ForegroundColor Green
} else {
  Write-Host "Android cmdline tools: NOT OK" -ForegroundColor Red
}

# Step 5: Add Android tools to PATH
Add-Path-Machine (Join-Path $CmdlineToolsLatest "bin")
Add-Path-Machine (Join-Path $SdkDir "platform-tools")
Add-Path-Machine (Join-Path $SdkDir "tools\bin")

# Step 6: Install SDK components
$SdkManager = Join-Path $CmdlineToolsLatest "bin\sdkmanager.bat"
$SdkComponentsOk = $false
if (Test-Path $SdkManager) {
    Write-Host "Accepting Android SDK licenses..." -ForegroundColor Yellow
    try {
        $yes = "y`r`n" * 20
        $yes | & cmd /c "`"$SdkManager`" --licenses" | Out-Null
    } catch {}
    Write-Host "Installing SDK components..." -ForegroundColor Yellow
    $components = @("platform-tools","platforms;android-34","build-tools;34.0.0")
    foreach ($c in $components) {
        try {
            & cmd /c "`"$SdkManager`" `"$c`"" | Out-Null
        } catch {}
    }
    # Basic verification: adb exists after platform-tools
  if (Test-Path (Join-Path $SdkDir "platform-tools\adb.exe")) { $SdkComponentsOk = $true }
}
if ($SdkComponentsOk) {
  Write-Host "SDK components: OK" -ForegroundColor Green
} else {
  Write-Host "SDK components: NOT OK" -ForegroundColor Red
}

# Step 7: Final verification
Write-Host "=== Verification ===" -ForegroundColor Cyan
$overallOk = $true

# Refresh PATH for current session
$env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine")

# Java
try {
    $jv = & cmd /c "java -version 2>&1"
    if ($jv -match "version") { Write-Host "Java: OK" -ForegroundColor Green } else { Write-Host "Java: NOT OK" -ForegroundColor Red; $overallOk = $false }
} catch { Write-Host "Java: NOT OK" -ForegroundColor Red; $overallOk = $false }

# Git
try {
    $gv = & cmd /c "git --version 2>&1"
    if ($gv -match "git version") { Write-Host "Git: OK" -ForegroundColor Green } else { Write-Host "Git: NOT OK" -ForegroundColor Red; $overallOk = $false }
} catch { Write-Host "Git: NOT OK" -ForegroundColor Red; $overallOk = $false }

# SDK Manager
if (Test-Path $SdkManager) { Write-Host "SDK Manager: OK" -ForegroundColor Green } else { Write-Host "SDK Manager: NOT OK" -ForegroundColor Red; $overallOk = $false }
# ADB
if (Test-Path (Join-Path $SdkDir "platform-tools\adb.exe")) { Write-Host "ADB: OK" -ForegroundColor Green } else { Write-Host "ADB: NOT OK" -ForegroundColor Red; $overallOk = $false }

# Env vars
$ah = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "Machine")
$asr = [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", "Machine")
if ($ah -eq $SdkDir) { Write-Host "ANDROID_HOME: OK" -ForegroundColor Green } else { Write-Host "ANDROID_HOME: NOT OK" -ForegroundColor Red; $overallOk = $false }
if ($asr -eq $SdkDir) { Write-Host "ANDROID_SDK_ROOT: OK" -ForegroundColor Green } else { Write-Host "ANDROID_SDK_ROOT: NOT OK" -ForegroundColor Red; $overallOk = $false }

Write-Host "=== Summary ===" -ForegroundColor Green
Write-Host "Base directory: $BaseDir" -ForegroundColor Gray
if ($overallOk) {
  Write-Host "Overall status: SUCCESS" -ForegroundColor Green
  Write-Host "All set. Please close and reopen PowerShell to ensure PATH is refreshed." -ForegroundColor Green
  exit 0
} else {
  Write-Host "Overall status: NEEDS ATTENTION" -ForegroundColor Yellow
  Write-Host "Some items failed. You may need to restart PowerShell and re-run the script, or install missing items manually." -ForegroundColor Yellow
  exit 2
}

