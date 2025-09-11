# 🎯 START HERE - Complete Setup Guide

**Welcome to Android Development from the Command Line!** 

This guide will get you from zero to building Android apps in about 15-20 minutes, without needing Android Studio.

## 📋 What You'll Have After This Setup

✅ Complete Android development environment  
✅ Java Development Kit (JDK 17)  
✅ Android SDK with all necessary tools  
✅ Gradle build system  
✅ Git version control  
✅ ADB for device debugging  
✅ Ready-to-build Smart SMS Filter app  
✅ Helper scripts for all common tasks  

## 🚀 Quick Setup (3 Steps)

### Step 1: Initial Project Setup

First, let's create the basic project structure:

```powershell
# Run this from your project directory
.\scripts\init-project.ps1
```

This creates all the necessary Android project files and structure.

### Step 2: Install Development Tools

**Important: You MUST run this as Administrator**

1. Press `Win + X` and select "Windows PowerShell (Admin)"
2. Navigate to your project:
   ```powershell
   cd "C:\Users\Aryaraj Singh\smart-sms-filter"
   ```
3. Run the setup script:
   ```powershell
   .\scripts\setup-android-cli.ps1
   ```

This will automatically:
- Download and install Java 17 (~200MB)
- Install Git for Windows (~50MB)
- Download Android SDK Command Line Tools (~150MB)
- Install required Android SDK components (~1GB)
- Install Gradle build system (~100MB)
- Configure all environment variables
- Create development helper scripts

**⏱️ Expected time: 10-15 minutes** (depends on internet speed)

### Step 3: Restart PowerShell

After the setup completes:
1. **Close PowerShell completely**
2. Open a new PowerShell window (normal user, not admin)
3. Navigate back to your project:
   ```powershell
   cd "C:\Users\Aryaraj Singh\smart-sms-filter"
   ```

## ✅ Verify Your Setup

Run these commands to make sure everything works:

```powershell
# Check Java
java -version

# Check Git  
git --version

# Check Android tools
adb --version

# Check Gradle
gradle --version

# Check Android SDK
echo $env:ANDROID_HOME
```

You should see version information for each tool and the SDK path.

## 🏗️ Build Your First Android App

### Build the App
```powershell
.\scripts\build-android.ps1
```

This compiles your code and creates an APK file. First build may take a few minutes as it downloads dependencies.

### Test Your Code
```powershell
.\scripts\test-android.ps1
```

Runs unit tests and code quality checks.

## 📱 Install on Your Android Device

### Enable Developer Mode
1. Go to **Settings** → **About phone**
2. Tap **Build number** 7 times
3. You'll see "You are now a developer!"

### Enable USB Debugging
1. Go to **Settings** → **Developer options**
2. Turn on **USB debugging**
3. Turn on **Install via USB** (if available)

### Connect and Install
1. Connect your Android device via USB cable
2. When prompted on your phone, tap **OK** to allow USB debugging
3. Run:
   ```powershell
   .\scripts\run-android.ps1
   ```

Your app will be built and installed automatically! 🎉

## 📁 Project Structure You Now Have

```
smart-sms-filter/
├── android/                           # Main Android app
│   ├── app/src/main/java/com/smartsmsfilter/
│   │   ├── MainActivity.kt            # App entry point
│   │   ├── ui/theme/                  # Jetpack Compose themes
│   │   ├── presentation/              # ViewModels (empty, ready for code)
│   │   ├── domain/                    # Business logic (empty, ready for code)
│   │   ├── data/                      # Database & repositories (empty)
│   │   └── ml/                        # AI model integration (empty)
│   ├── app/src/main/AndroidManifest.xml  # App permissions & configuration
│   ├── app/src/main/res/              # App resources (strings, themes)
│   ├── build.gradle                   # Project configuration
│   └── gradlew.bat                    # Build tool
├── models/                            # AI model training (empty, ready for ML work)
├── scripts/                           # Helper scripts
│   ├── setup-android-cli.ps1          # Main setup script
│   ├── init-project.ps1               # Project initialization
│   ├── build-android.ps1              # Build the app
│   ├── run-android.ps1                # Install on device
│   ├── test-android.ps1               # Run tests
│   └── clean-android.ps1              # Clean build files
├── WARP.md                            # Development guidelines
├── SETUP-GUIDE.md                     # Detailed setup instructions
└── START-HERE.md                      # This file
```

## 🛠️ Daily Development Commands

Once set up, these are the main commands you'll use:

```powershell
# Build the app
.\scripts\build-android.ps1

# Install on connected device
.\scripts\run-android.ps1

# Run tests
.\scripts\test-android.ps1

# Clean build files
.\scripts\clean-android.ps1

# Check connected devices
adb devices

# View device logs
adb logcat
```

## 🚨 Troubleshooting

### "Command not found" errors
**Solution**: Restart PowerShell to load environment variables

### Build fails with "SDK not found"
**Solution**: Check that setup completed successfully:
```powershell
echo $env:ANDROID_HOME
# Should show: C:\AndroidDev\Android\Sdk
```

### Device not detected
**Solution**: 
1. Make sure USB debugging is enabled
2. Try a different USB cable/port
3. Check with: `adb devices`

### First build is slow
**Normal**: First build downloads all dependencies (~500MB). Subsequent builds are much faster.

## 🎯 What's Next?

1. **Learn Kotlin**: The language used for Android development
2. **Explore Jetpack Compose**: Modern UI toolkit (already set up!)
3. **Read WARP.md**: Contains detailed development guidelines
4. **Start coding**: Add features to your SMS filter app

## 📚 Learning Resources

- [Kotlin Basics](https://kotlinlang.org/docs/basic-syntax.html)
- [Jetpack Compose Tutorial](https://developer.android.com/jetpack/compose/tutorial)
- [Android Development Guide](https://developer.android.com/guide)

## 💡 Pro Tips

1. **Use the scripts**: They handle all the complex command-line arguments
2. **Keep PowerShell open**: Switching directories frequently is normal
3. **Device stays connected**: Keep your Android device connected while developing
4. **Build incrementally**: Small changes build much faster than complete rebuilds

---

## 🆘 Need Help?

If something goes wrong:

1. **Check the error message** - it usually tells you what's wrong
2. **Try cleaning and rebuilding**:
   ```powershell
   .\scripts\clean-android.ps1
   .\scripts\build-android.ps1
   ```
3. **Restart PowerShell** - fixes most environment variable issues
4. **Run setup again** - it's safe to run multiple times

---

**🎉 Congratulations!** You now have a complete Android development environment that works entirely from the command line. No Android Studio needed!

**Ready to start coding?** Your first step is to run the setup script and build your first app! 🚀
