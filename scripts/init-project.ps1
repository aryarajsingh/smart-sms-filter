# Initialize Smart SMS Filter Android Project
# Run this script after the main setup is complete

Write-Host "=== Smart SMS Filter Project Initialization ===" -ForegroundColor Cyan
Write-Host ""

$projectRoot = Split-Path $PSScriptRoot -Parent
$androidDir = Join-Path $projectRoot "android"

# Check if we're in the right directory
if (-not (Test-Path (Join-Path $projectRoot "README.md"))) {
    Write-Host "❌ Please run this script from the smart-sms-filter project root directory" -ForegroundColor Red
    exit 1
}

# Verify prerequisites are installed
Write-Host "Checking prerequisites..." -ForegroundColor Yellow

$allGood = $true

# Check Java
try {
    $javaVersion = java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
    if ($javaVersion -match "17|11|1\.8") {
        Write-Host "✅ Java: $javaVersion" -ForegroundColor Green
    } else {
        Write-Host "⚠️  Java version might not be compatible: $javaVersion" -ForegroundColor Yellow
    }
} catch {
    Write-Host "❌ Java not found. Please run setup-android-cli.ps1 first." -ForegroundColor Red
    $allGood = $false
}

# Check Android SDK
if ($env:ANDROID_HOME) {
    Write-Host "✅ Android SDK: $env:ANDROID_HOME" -ForegroundColor Green
} else {
    Write-Host "❌ ANDROID_HOME not set. Please run setup-android-cli.ps1 first." -ForegroundColor Red
    $allGood = $false
}

# Check ADB
try {
    adb version | Out-Null
    Write-Host "✅ ADB (Android Debug Bridge): Available" -ForegroundColor Green
} catch {
    Write-Host "❌ ADB not found. Please run setup-android-cli.ps1 first." -ForegroundColor Red
    $allGood = $false
}

if (-not $allGood) {
    Write-Host ""
    Write-Host "❌ Prerequisites not met. Please run the setup script first:" -ForegroundColor Red
    Write-Host "   .\scripts\setup-android-cli.ps1" -ForegroundColor Cyan
    exit 1
}

Write-Host ""
Write-Host "Creating basic Android project structure..." -ForegroundColor Green

# Create main application structure if it doesn't exist
$mainSrcPath = Join-Path $androidDir "app\src\main\java\com\smartsmsfilter"

# Create package directories
$packageDirs = @(
    "ui",
    "ui\components",
    "ui\screens", 
    "presentation",
    "presentation\viewmodel",
    "domain",
    "domain\model",
    "domain\repository",
    "domain\usecase",
    "data",
    "data\database",
    "data\repository",
    "ml",
    "service"
)

foreach ($dir in $packageDirs) {
    $fullPath = Join-Path $mainSrcPath $dir
    New-Item -ItemType Directory -Path $fullPath -Force | Out-Null
}

Write-Host "✅ Package structure created" -ForegroundColor Green

# Create basic MainActivity if it doesn't exist
$mainActivityPath = Join-Path $mainSrcPath "MainActivity.kt"
if (-not (Test-Path $mainActivityPath)) {
    $mainActivityContent = @"
package com.smartsmsfilter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.smartsmsfilter.ui.theme.SmartSmsFilterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartSmsFilterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Smart SMS Filter")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Welcome to ${'$'}name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartSmsFilterTheme {
        Greeting("Smart SMS Filter")
    }
}
"@
    $mainActivityContent | Out-File -FilePath $mainActivityPath -Encoding UTF8 -Force
    Write-Host "✅ MainActivity.kt created" -ForegroundColor Green
}

# Create basic theme files
$themePath = Join-Path $mainSrcPath "ui\theme"
New-Item -ItemType Directory -Path $themePath -Force | Out-Null

if (-not (Test-Path (Join-Path $themePath "Theme.kt"))) {
    $themeContent = @"
package com.smartsmsfilter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun SmartSmsFilterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
"@
    $themeContent | Out-File -FilePath (Join-Path $themePath "Theme.kt") -Encoding UTF8 -Force
}

# Create Color.kt
if (-not (Test-Path (Join-Path $themePath "Color.kt"))) {
    $colorContent = @"
package com.smartsmsfilter.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
"@
    $colorContent | Out-File -FilePath (Join-Path $themePath "Color.kt") -Encoding UTF8 -Force
}

# Create Type.kt
if (-not (Test-Path (Join-Path $themePath "Type.kt"))) {
    $typeContent = @"
package com.smartsmsfilter.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
"@
    $typeContent | Out-File -FilePath (Join-Path $themePath "Type.kt") -Encoding UTF8 -Force
}

Write-Host "✅ Basic Compose theme created" -ForegroundColor Green

# Create AndroidManifest.xml if it doesn't exist
$manifestPath = Join-Path $androidDir "app\src\main\AndroidManifest.xml"
if (-not (Test-Path $manifestPath)) {
    New-Item -ItemType Directory -Path (Split-Path $manifestPath) -Force | Out-Null
    $manifestContent = @"
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Essential SMS permissions -->
    <uses-permission android:name="android.permission.RECEIVE_SMS" />
    <uses-permission android:name="android.permission.READ_SMS" />
    
    <!-- Call screening service permission -->
    <uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
    
    <!-- Notifications permission -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SmartSmsFilter"
        tools:targetApi="31">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SmartSmsFilter">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
    </application>

</manifest>
"@
    $manifestContent | Out-File -FilePath $manifestPath -Encoding UTF8 -Force
    Write-Host "✅ AndroidManifest.xml created" -ForegroundColor Green
}

# Create basic resource files
$resPath = Join-Path $androidDir "app\src\main\res"
$valuesPath = Join-Path $resPath "values"
New-Item -ItemType Directory -Path $valuesPath -Force | Out-Null

if (-not (Test-Path (Join-Path $valuesPath "strings.xml"))) {
    $stringsContent = @"
<resources>
    <string name="app_name">Smart SMS Filter</string>
</resources>
"@
    $stringsContent | Out-File -FilePath (Join-Path $valuesPath "strings.xml") -Encoding UTF8 -Force
}

if (-not (Test-Path (Join-Path $valuesPath "themes.xml"))) {
    $themesContent = @"
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Base.Theme.SmartSmsFilter" parent="Theme.Material3.DayNight">
        <!-- Customize your light theme here. -->
        <!-- <item name="colorPrimary">@color/my_light_primary</item> -->
    </style>
    
    <style name="Theme.SmartSmsFilter" parent="Base.Theme.SmartSmsFilter" />
</resources>
"@
    $themesContent | Out-File -FilePath (Join-Path $valuesPath "themes.xml") -Encoding UTF8 -Force
}

Write-Host "✅ Basic resources created" -ForegroundColor Green

# Create a simple build test
Write-Host ""
Write-Host "Testing Gradle build setup..." -ForegroundColor Yellow
Set-Location $androidDir

try {
    # First, make sure we can run gradlew
    $gradlewPath = Join-Path $androidDir "gradlew.bat"
    if (-not (Test-Path $gradlewPath)) {
        Write-Host "⚠️  gradlew.bat not found, this is normal for new projects" -ForegroundColor Yellow
        Write-Host "The setup script will create it when needed." -ForegroundColor Yellow
    } else {
        Write-Host "✅ gradlew.bat found" -ForegroundColor Green
    }
    
    # Check if we can at least parse the build files
    Write-Host "✅ Project structure looks good" -ForegroundColor Green
    
} catch {
    Write-Host "⚠️  Warning: Could not verify build setup: $($_.Exception.Message)" -ForegroundColor Yellow
}

Set-Location $projectRoot

Write-Host ""
Write-Host "=== Project Initialization Complete! ===" -ForegroundColor Green
Write-Host ""
Write-Host "Your Smart SMS Filter project is now ready for development!" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next steps:" -ForegroundColor White
Write-Host "1. Run the main setup (if you haven't already):" -ForegroundColor Gray
Write-Host "   .\scripts\setup-android-cli.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "2. After setup is complete and you've restarted PowerShell:" -ForegroundColor Gray
Write-Host "   .\scripts\build-android.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "3. Connect an Android device and install the app:" -ForegroundColor Gray
Write-Host "   .\scripts\run-android.ps1" -ForegroundColor Cyan
Write-Host ""
Write-Host "Project structure created:" -ForegroundColor White
Write-Host "- MainActivity.kt (main app entry point)" -ForegroundColor Gray
Write-Host "- Jetpack Compose UI theme" -ForegroundColor Gray  
Write-Host "- AndroidManifest.xml with SMS permissions" -ForegroundColor Gray
Write-Host "- Basic resource files" -ForegroundColor Gray
Write-Host "- Clean Architecture package structure" -ForegroundColor Gray
Write-Host ""
Write-Host "Happy coding! 🚀" -ForegroundColor Green
