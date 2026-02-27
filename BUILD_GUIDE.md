# Building the IMSI Catcher Detector APK

## Option 1: Automated Build with GitHub Actions (Easiest)

### Step 1: Create a GitHub Account
If you don't have one, create a free account at https://github.com

### Step 2: Create a New Repository
1. Go to https://github.com/new
2. Name it `imsi-catcher-detector`
3. Choose "Public" or "Private" (your preference)
4. Click "Create repository"

### Step 3: Upload the Project Files
You have two options:

**Option A: Using GitHub Web Interface (Easiest)**
1. Go to your new repository
2. Click "Add file" → "Upload files"
3. Drag and drop all project files
4. Commit the changes

**Option B: Using Git Command Line**
```bash
cd /home/ubuntu/imsi-catcher-detector
git init
git add .
git commit -m "Initial commit: IMSI Catcher Detector"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/imsi-catcher-detector.git
git push -u origin main
```

### Step 4: GitHub Actions Will Automatically Build
1. The workflow will trigger automatically when you push code
2. Go to your repository's "Actions" tab
3. Wait for the build to complete (usually 5-10 minutes)
4. Click on the completed workflow
5. Scroll down to "Artifacts"
6. Download either:
   - `imsi-detector-debug` - For testing
   - `imsi-detector-release` - For production use

### Step 5: Install the APK on Your Android Device
1. Download the APK file to your phone
2. Go to Settings → Security → Unknown Sources (enable if needed)
3. Open the APK file with your file manager
4. Tap "Install"
5. Grant permissions when prompted

## Option 2: Manual Build with Docker

If you have Docker installed on a computer, you can build locally:

```bash
# Build Docker image with Android SDK
docker build -t android-builder .

# Build APK inside container
docker run --rm -v $(pwd):/workspace android-builder \
  bash -c "cd /workspace && ./gradlew assembleRelease"

# APK will be in app/build/outputs/apk/release/
```

## Option 3: Using Online Build Services

### Codemagic
1. Go to https://codemagic.io
2. Sign up with GitHub
3. Connect your repository
4. Select Android as the platform
5. Build will start automatically
6. Download APK from build artifacts

### AppCenter
1. Go to https://appcenter.ms
2. Sign up with GitHub
3. Create new app
4. Connect repository
5. Configure build settings
6. Download built APK

## APK Installation Methods

### Method 1: Direct Installation (Easiest)
1. Download APK on your Android phone
2. Open file manager
3. Tap the APK file
4. Tap "Install"

### Method 2: Using adb (if you have Android SDK tools)
```bash
adb install app-debug.apk
```

### Method 3: Via Email or Cloud Storage
1. Download APK on computer
2. Email it to yourself or upload to Google Drive
3. Download on phone
4. Install as above

## APK Types Explained

**Debug APK** (`app-debug.apk`)
- Larger file size
- Includes debugging information
- Good for testing and development
- Can be installed on any device

**Release APK** (`app-release-unsigned.apk`)
- Smaller file size
- Optimized for production
- Unsigned (can still be installed on test devices)
- Better performance

## Permissions Required

When you first launch the app, you'll be asked to grant these permissions:
- Phone State (to read cell tower info)
- Location (for cell tower triangulation)
- SMS (to detect silent SMS)
- Notifications (for threat alerts)
- Camera (optional, for future features)

**Grant all permissions for full functionality.**

## Troubleshooting

### Build Failed on GitHub Actions
- Check the error message in the Actions tab
- Common issues:
  - Java version mismatch (should be 17)
  - Gradle cache issues (clear and retry)
  - Missing dependencies (check internet connection)

### APK Won't Install
- Check Android version (requires Android 12 or higher)
- Enable "Unknown Sources" in Security settings
- Try clearing app cache: Settings → Apps → Clear Cache
- Uninstall any previous version first

### App Crashes on Launch
- Check if all permissions are granted
- Ensure device has active cellular connection
- Try restarting the device
- Check logcat for error messages

### No Cell Information Displayed
- Verify phone has active cellular connection
- Check if airplane mode is off
- Ensure all permissions are granted
- Try restarting the app

## GitHub Actions Workflow Details

The workflow file (`.github/workflows/build-apk.yml`) does the following:

1. **Checkout Code** - Downloads your project
2. **Setup Java 17** - Installs required Java version
3. **Build Debug APK** - Creates debug version
4. **Build Release APK** - Creates release version
5. **Upload Artifacts** - Makes APKs available for download
6. **Create Release** - (Optional) Creates GitHub release

### Viewing Build Logs
1. Go to Actions tab
2. Click on the workflow run
3. Click "build" job
4. Expand any step to see detailed logs

## Security Notes

- The debug APK is safe for testing
- The release APK is unsigned (you can sign it if needed)
- All data stays on your device
- No information is sent to external servers
- Open source code is available for review

## Next Steps

After successfully building and installing:

1. **Launch the App** - Grant all permissions
2. **Monitor Your Network** - Watch the threat level indicator
3. **Check History** - Review detected anomalies
4. **Configure Settings** - Adjust monitoring preferences
5. **Report Issues** - Open GitHub issues for bugs

## Support

For issues or questions:
1. Check the README.md for usage guide
2. Review DESIGN.md for architecture details
3. Open an issue on GitHub
4. Check logcat for error messages

## Building Signed Release APK

For production distribution, you'll need to sign the APK:

```bash
# Create keystore (one time)
keytool -genkey -v -keystore imsi-detector.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias imsi-detector

# Sign the APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 \
  -keystore imsi-detector.keystore \
  app/build/outputs/apk/release/app-release-unsigned.apk imsi-detector

# Verify signature
jarsigner -verify -verbose -certs \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

## Distribution Options

Once you have a signed APK:

1. **Google Play Store** - Official Android app store
2. **F-Droid** - Open source app repository
3. **GitHub Releases** - Direct download from your repository
4. **APKPure** - Alternative app store
5. **Direct Distribution** - Share APK file directly

---

**Happy building! If you encounter any issues, refer to the troubleshooting section or open a GitHub issue.**
