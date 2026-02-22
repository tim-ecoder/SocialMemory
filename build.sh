#!/bin/bash
set -e

# ============================================================
# SocialMemory APK Build Script
# Uses Debian Android SDK tools (aapt, dx, zipalign, apksigner)
# Targets: Android 8.1 (API 27), minSdk 21
# ============================================================

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_SRC="$PROJECT_DIR/app/src/main"
BUILD_DIR="$PROJECT_DIR/build-output"
ANDROID_JAR="/usr/lib/android-sdk/platforms/android-23/android.jar"
AAPT="/usr/lib/android-sdk/build-tools/debian/aapt"
DX="/usr/lib/android-sdk/build-tools/debian/dx"
ZIPALIGN="/usr/lib/android-sdk/build-tools/debian/zipalign"
APKSIGNER="/usr/lib/android-sdk/build-tools/debian/apksigner"
KEYSTORE="$PROJECT_DIR/debug.keystore"

echo "========================================="
echo "  Building SocialMemory APK"
echo "========================================="

# Clean
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/classes"

# Step 1: Generate R.java from resources
echo "[1/6] Generating R.java..."
$AAPT package -f -m \
    -S "$APP_SRC/res" \
    -J "$BUILD_DIR/gen" \
    -M "$APP_SRC/AndroidManifest.xml" \
    -I "$ANDROID_JAR"

echo "      R.java generated at: $BUILD_DIR/gen/com/socialmemory/app/R.java"

# Step 2: Compile Java sources
echo "[2/6] Compiling Java sources..."
javac -source 1.8 -target 1.8 \
    -bootclasspath "$ANDROID_JAR" \
    -classpath "$ANDROID_JAR" \
    -d "$BUILD_DIR/classes" \
    "$BUILD_DIR/gen/com/socialmemory/app/R.java" \
    "$APP_SRC/java/com/socialmemory/app/MainActivity.java"

echo "      Compiled $(find "$BUILD_DIR/classes" -name '*.class' | wc -l) class files"

# Step 3: Convert to DEX
echo "[3/6] Converting to DEX format..."
$DX --dex --output="$BUILD_DIR/classes.dex" "$BUILD_DIR/classes"

echo "      DEX file size: $(du -h "$BUILD_DIR/classes.dex" | cut -f1)"

# Step 4: Package into APK
echo "[4/6] Packaging APK..."
$AAPT package -f \
    -S "$APP_SRC/res" \
    -M "$APP_SRC/AndroidManifest.xml" \
    -I "$ANDROID_JAR" \
    -F "$BUILD_DIR/socialmemory-unaligned.apk"

# Add DEX to APK
cd "$BUILD_DIR"
cp classes.dex classes.dex.bak
$AAPT add "$BUILD_DIR/socialmemory-unaligned.apk" classes.dex
cd "$PROJECT_DIR"

echo "      Unaligned APK created"

# Step 5: Zipalign
echo "[5/6] Zipaligning APK..."
$ZIPALIGN -f 4 \
    "$BUILD_DIR/socialmemory-unaligned.apk" \
    "$BUILD_DIR/socialmemory-aligned.apk"

echo "      Aligned APK size: $(du -h "$BUILD_DIR/socialmemory-aligned.apk" | cut -f1)"

# Step 6: Sign APK
echo "[6/6] Signing APK..."

# Generate debug keystore if it doesn't exist
if [ ! -f "$KEYSTORE" ]; then
    echo "      Generating debug keystore..."
    keytool -genkeypair \
        -keystore "$KEYSTORE" \
        -storepass android \
        -keypass android \
        -alias androiddebugkey \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi

$APKSIGNER sign \
    --ks "$KEYSTORE" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$BUILD_DIR/socialmemory-debug.apk" \
    "$BUILD_DIR/socialmemory-aligned.apk"

# Verify
$APKSIGNER verify "$BUILD_DIR/socialmemory-debug.apk"

# Copy to project root
cp "$BUILD_DIR/socialmemory-debug.apk" "$PROJECT_DIR/socialmemory-debug.apk"

echo ""
echo "========================================="
echo "  BUILD SUCCESSFUL"
echo "========================================="
echo "  APK: $PROJECT_DIR/socialmemory-debug.apk"
echo "  Size: $(du -h "$PROJECT_DIR/socialmemory-debug.apk" | cut -f1)"
echo ""
echo "  Install: adb install socialmemory-debug.apk"
echo "========================================="
