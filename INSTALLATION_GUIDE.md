# Smart SMS Filter - Installation Guide

## 📱 APK Installation Troubleshooting

If you're getting "App not installed" error, follow these steps:

### Option 1: Debug APK (Easier to Install)
**Location:** `android/app/build/outputs/apk/debug/app-debug.apk`
- This version is easier to install for testing
- No signature conflicts
- Suitable for all devices

### Option 2: Release APK (Production Ready)
**Location:** `android/app/build/outputs/apk/release/app-release.apk`
- Optimized and smaller size
- Signed for distribution
- Version: 1.2.1 (Build 6)

## 🔧 Installation Steps

### Step 1: Prepare Your Device
1. **Enable Unknown Sources:**
   - Go to Settings → Security
   - Enable "Install from Unknown Sources" or "Install unknown apps"
   - On newer Android: Settings → Apps → Special access → Install unknown apps → Select your file manager → Allow

2. **Uninstall Previous Versions:**
   - If you have any previous version installed, uninstall it first
   - Go to Settings → Apps → Smart SMS Filter → Uninstall
   - This prevents signature conflicts

### Step 2: Transfer the APK
- Send the APK file to your device via:
  - Email attachment
  - Google Drive / Dropbox
  - USB cable
  - WhatsApp / Telegram

### Step 3: Install the App
1. Open the APK file on your device
2. Tap "Install"
3. If prompted about Play Protect, tap "Install anyway"
4. Wait for installation to complete
5. Tap "Open" to launch the app

## ⚠️ Common Issues and Solutions

### "App not installed" Error
**Causes and Solutions:**

1. **Previous version installed:**
   - Uninstall the old version completely
   - Clear cache: Settings → Apps → Smart SMS Filter → Storage → Clear Cache

2. **Insufficient storage:**
   - Free up at least 100MB of space
   - The app needs ~50MB installed

3. **Android version too old:**
   - Minimum required: Android 7.0 (API 24)
   - Check your version: Settings → About Phone → Android Version

4. **Corrupted APK:**
   - Re-download or re-transfer the APK file
   - Try the debug version instead of release

5. **Package conflicts:**
   - Restart your device
   - Try installing in Safe Mode

### Installation Blocked by Play Protect
- Tap "More details"
- Select "Install anyway"
- This is normal for apps not from Play Store

### "Parse error" Message
- Your Android version is too old (below 7.0)
- The APK file is corrupted during transfer
- Try downloading again

## ✅ After Successful Installation

1. **Grant Permissions:**
   - SMS permissions (Read, Send, Receive)
   - Contacts permission
   - Notification permission (Android 13+)

2. **Set as Default SMS App:**
   - The app will prompt you to set it as default
   - This is required for full functionality

3. **Complete Onboarding:**
   - Follow the welcome screens
   - Configure your spam preferences
   - The app will start filtering messages

## 📊 APK Details

### Release APK (v1.2.1)
- **Size:** ~18.5 MB
- **Version Code:** 6
- **Min Android:** 7.0 (API 24)
- **Target Android:** 14 (API 34)
- **Architectures:** Universal (all devices)

### Debug APK
- **Size:** ~20 MB (larger, not optimized)
- **Same features as release**
- **Easier installation for testing**

## 🆘 Still Having Issues?

Try these commands to install via ADB (Android Debug Bridge):

```bash
# For release APK
adb install app-release.apk

# For debug APK
adb install app-debug.apk

# Force reinstall (replaces existing)
adb install -r app-release.apk

# If signature mismatch
adb uninstall com.smartsmsfilter
adb install app-release.apk
```

## 📝 Device Compatibility

**Tested on:**
- Samsung devices (Android 10+)
- Google Pixel (Android 11+)
- OnePlus (Android 10+)
- Xiaomi/Redmi (Android 9+)
- Realme/Oppo (Android 10+)

**Requirements:**
- Android 7.0 or higher
- 50MB free storage
- SMS capability (not tablets without SIM)

---

If you continue to have installation issues, please share:
1. Your device model
2. Android version
3. The exact error message
4. Whether you tried debug or release APK
