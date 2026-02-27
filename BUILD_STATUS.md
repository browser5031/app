# IMSI Catcher Detector - Build Instructions

## ✅ Project Structure Complete!

Your Android project has been successfully restructured with the proper Android build configuration.

## 📁 Project Structure

```
/app/
├── .github/
│   └── workflows/
│       └── build-apk.yml          # GitHub Actions workflow
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/imsidetector/   # All Kotlin source files
│   │       ├── AndroidManifest.xml       # App manifest
│   │       └── res/                      # Resources (colors, strings, icons)
│   ├── build.gradle.kts             # App module build config
│   └── proguard-rules.pro          # ProGuard configuration
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar      # Gradle wrapper binary
│       └── gradle-wrapper.properties
├── build.gradle.kts                # Project build config
├── settings.gradle.kts             # Project settings
├── gradlew                         # Gradle wrapper script (Linux/Mac)
├── gradlew.bat                     # Gradle wrapper script (Windows)
└── Dockerfile                      # Docker build environment
```

## 🚀 Build Options

### Option 1: GitHub Actions (Recommended - Easiest)

**This is the easiest way to build your APK without installing anything locally!**

#### Steps:

1. **Push to GitHub:**
   ```bash
   git add .
   git commit -m "Android project restructured for build"
   git push origin main
   ```

2. **GitHub Actions will automatically:**
   - Set up Java 17
   - Download Gradle dependencies
   - Build debug APK
   - Build release APK (unsigned)
   - Upload both as downloadable artifacts

3. **Download Your APK:**
   - Go to your GitHub repository
   - Click "Actions" tab
   - Click on the latest workflow run
   - Scroll to "Artifacts" section
   - Download:
     - `imsi-detector-debug` - For testing
     - `imsi-detector-release` - For production

4. **Install on Android Device:**
   - Transfer APK to your phone
   - Enable "Install from Unknown Sources"
   - Open the APK file and install

### Option 2: Docker Build (Local)

If you have Docker installed:

```bash
# Build the Docker image with Android SDK
docker build -t android-builder .

# Build debug APK
docker run --rm -v $(pwd):/workspace android-builder ./gradlew assembleDebug

# Build release APK  
docker run --rm -v $(pwd):/workspace android-builder ./gradlew assembleRelease

# APKs will be in: app/build/outputs/apk/
```

### Option 3: Local Build (Requires Android SDK)

If you have Android Studio or Android SDK installed:

```bash
# Make gradlew executable (Linux/Mac)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Find APKs in:
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release-unsigned.apk
```

## 📦 What Gets Built

### Debug APK (`app-debug.apk`)
- **Purpose:** Testing and development
- **Size:** Larger (includes debug symbols)
- **Optimization:** None
- **Install:** Works on any Android 12+ device
- **Use:** Best for development and testing

### Release APK (`app-release-unsigned.apk`)
- **Purpose:** Production use
- **Size:** Smaller (optimized with ProGuard)
- **Optimization:** Full code minification
- **Install:** Works on any Android 12+ device
- **Use:** Best for distribution (can be signed for Play Store)

## 🔧 System Requirements

### For GitHub Actions Build:
- ✅ GitHub account (free)
- ✅ Git installed
- ✅ Nothing else needed!

### For Docker Build:
- Docker Desktop or Docker Engine
- 8GB RAM minimum
- 10GB free disk space

### For Local Build:
- Android Studio Flamingo or later
- JDK 17
- Android SDK 35
- Build Tools 35.0.0
- 16GB RAM recommended

## 📱 Installation Requirements

Your Android device needs:
- **Android 12 or higher** (API level 30+)
- **Permissions:** Phone, Location, SMS, Notifications
- **Storage:** ~100 MB free space

## 🐛 Troubleshooting

### Build Fails on GitHub Actions

**Error: "Gradle task assembleDebug failed"**
- Check the Actions log for specific errors
- Verify all Kotlin files have correct package declarations
- Ensure AndroidManifest.xml is valid

**Error: "Unable to find dependency"**
- This usually resolves itself on retry
- Click "Re-run jobs" in GitHub Actions

### APK Won't Install

**Error: "App not installed"**
- Ensure Android version is 12 or higher
- Enable "Install from Unknown Sources"
- Uninstall any previous version first

**Error: "Parsing error"**
- APK file may be corrupted
- Re-download the APK
- Try debug version instead

### App Crashes on Launch

**Common fixes:**
1. Grant all requested permissions
2. Ensure device has active cellular connection
3. Check if airplane mode is OFF
4. Restart device and try again

## 📊 Build Time Estimates

- **GitHub Actions:** 5-10 minutes
- **Docker:** 10-15 minutes (first build), 2-3 minutes (subsequent)
- **Local:** 2-5 minutes

## ✅ Verification Checklist

After building, verify:
- [ ] APK file size is reasonable (40-80 MB)
- [ ] APK installs without errors
- [ ] App launches successfully
- [ ] Permissions can be granted
- [ ] Main screen displays
- [ ] No immediate crashes

## 🎯 Next Steps

1. **Build the APK** using one of the methods above
2. **Install on device** and grant permissions
3. **Test basic functionality:**
   - Check if cell tower info displays
   - Verify threat level indicator works
   - Test navigation (Main → History → Settings)
4. **Monitor for issues** and check logcat if needed

## 🔗 Quick Links

- **README:** Comprehensive app documentation
- **BUILD_GUIDE:** Detailed build instructions
- **DESIGN:** Architecture and design docs
- **GitHub Actions:** `.github/workflows/build-apk.yml`

## 💡 Pro Tips

1. **Use GitHub Actions** for the simplest build experience
2. **Debug builds** are faster to build and easier to debug
3. **Release builds** are optimized but harder to debug
4. **Sign your APK** before Play Store distribution
5. **Keep build logs** for troubleshooting

## 🆘 Need Help?

If you encounter issues:
1. Check the troubleshooting section above
2. Review GitHub Actions logs for errors
3. Verify Android SDK versions match requirements
4. Check that all source files are in correct locations
5. Ensure gradlew has execute permissions (Linux/Mac)

---

**Status: ✅ Ready to Build!**

Your project is now properly structured and ready to build. Choose your preferred build method and follow the instructions above.
