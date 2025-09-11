# 🚀 Beginner's Guide to Android Development Setup

Welcome to Android development! This guide will help you set up everything you need to build the Smart SMS Filter app from the command line, without needing Android Studio.

## 📋 What We're Going to Install

1. **Java Development Kit (JDK)** - Required to run Android build tools
2. **Git** - Version control system for managing code
3. **Android SDK** - Android development tools and libraries
4. **Gradle** - Build system for compiling Android apps

## 🔧 Step-by-Step Setup

### Step 1: Run the Setup Script

1. **Open PowerShell as Administrator**
   - Press `Win + X` and select "Windows PowerShell (Admin)"
   - Or search for "PowerShell" in Start menu, right-click and "Run as administrator"

2. **Navigate to the project directory**
   ```powershell
   cd "C:\Users\Aryaraj Singh\smart-sms-filter"
   ```

3. **Run the setup script**
   ```powershell
   .\scripts\setup-android-cli.ps1
   ```

   This script will:
   - Download and install Java 17
   - Install Git for Windows
   - Download Android SDK command line tools
   - Install required Android SDK components
   - Install Gradle build system
   - Create helpful development scripts
   - Configure all environment variables

### Step 2: Restart PowerShell

After the script completes, **close PowerShell completely** and open a new PowerShell window (doesn't need to be as admin). This ensures all environment variables are loaded.

### Step 3: Verify Installation

Run these commands to make sure everything is working:

```powershell
# Check Java
java -version

# Check Git
git --version

# Check Android tools
adb --version

# Check Gradle
gradle --version
```

You should see version information for each tool.

## 🏗️ Building Your First Android App

Once setup is complete, you can use these simple commands:

### Navigate to Project
```powershell
cd "C:\Users\Aryaraj Singh\smart-sms-filter"
```

### Build the App
```powershell
.\scripts\build-android.ps1
```

This will:
- Compile all Kotlin code
- Process resources
- Create an APK file ready for installation

### Test the Code
```powershell
.\scripts\test-android.ps1
```

This will:
- Run unit tests
- Perform code quality checks (lint)
- Generate test reports

### Clean Build Files
```powershell
.\scripts\clean-android.ps1
```

This removes all compiled files, useful when you want a fresh build.

## 📱 Installing on Your Android Device

### Step 1: Enable Developer Options
1. Go to **Settings** > **About phone**
2. Tap **Build number** 7 times
3. You'll see "You are now a developer!"

### Step 2: Enable USB Debugging
1. Go to **Settings** > **Developer options**
2. Turn on **USB debugging**
3. Turn on **Install via USB** (if available)

### Step 3: Connect Device and Install
1. Connect your Android device via USB
2. When prompted on your phone, tap **OK** to allow USB debugging
3. Run the install command:
   ```powershell
   .\scripts\run-android.ps1
   ```

This will build the app and install it directly to your connected device!

## 📁 Understanding the Project Structure

```
smart-sms-filter/
├── android/                    # Main Android app
│   ├── app/                    # App source code and resources
│   ├── build.gradle            # Project-level build configuration
│   └── gradlew.bat            # Gradle wrapper (build tool)
├── models/                     # AI model files and training
├── scripts/                    # Helper scripts we created
│   ├── setup-android-cli.ps1  # Main setup script
│   ├── build-android.ps1      # Build the app
│   ├── run-android.ps1        # Install on device
│   ├── test-android.ps1       # Run tests
│   └── clean-android.ps1      # Clean build files
└── WARP.md                     # Development guidelines
```

## 🔍 What Happens When You Build

1. **Gradle reads configuration** from `build.gradle` files
2. **Kotlin code is compiled** to bytecode
3. **Resources are processed** (images, layouts, strings)
4. **Dependencies are downloaded** (libraries your app needs)
5. **APK file is created** (Android Package - installable app file)

## 🚨 Troubleshooting Common Issues

### "Command not found" errors
- **Solution**: Restart PowerShell to reload environment variables
- Make sure you ran the setup script as Administrator

### Build fails with "SDK not found"
- **Solution**: Check that `$env:ANDROID_HOME` points to the SDK:
  ```powershell
  echo $env:ANDROID_HOME
  ```
  Should show: `C:\AndroidDev\Android\Sdk`

### Device not detected
- **Solution**: 
  1. Make sure USB debugging is enabled
  2. Try different USB cable/port
  3. Check with: `adb devices`

### Gradle build fails
- **Solution**:
  1. Clean the project: `.\scripts\clean-android.ps1`
  2. Try building again: `.\scripts\build-android.ps1`

## 🎯 Next Steps for Development

1. **Learn Kotlin** - The programming language used for Android
2. **Understand Jetpack Compose** - Modern UI toolkit for Android
3. **Explore the codebase** - Look at files in `android/app/src/main/java/`
4. **Read the WARP.md** - Contains detailed development guidelines

## 📚 Useful Resources

- [Kotlin Official Documentation](https://kotlinlang.org/docs/home.html)
- [Android Developer Guide](https://developer.android.com/guide)
- [Jetpack Compose Tutorial](https://developer.android.com/jetpack/compose/tutorial)

## 🆘 Getting Help

If you encounter issues:
1. Check the error message carefully
2. Look in the "Troubleshooting" section above
3. Try cleaning and rebuilding the project
4. Make sure all tools are properly installed by running the verification commands

Remember: Every developer encounters build issues - it's part of the learning process! Don't get discouraged, and take it one step at a time.

---

**You're all set!** 🎉 You now have a complete Android development environment ready for building the Smart SMS Filter app.
