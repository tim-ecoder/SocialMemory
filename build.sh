#!/bin/bash
set -e

# Who's Who - Android MVP build script
# Uses system Android SDK tools + AndroidX/Material from dandar3 GitHub repos

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT="$SCRIPT_DIR"
APP="$PROJECT/app"

SDK="${SDK:-/usr/lib/android-sdk}"
ANDROID_JAR="$SDK/platforms/android-23/android.jar"
DX="$SDK/build-tools/debian/dx"
ZIPALIGN="$SDK/build-tools/debian/zipalign"
APKSIGNER="$SDK/build-tools/debian/apksigner"
JAVAC="${JAVAC:-$(which javac)}"
KEYTOOL="${KEYTOOL:-$(which keytool)}"
AAPT="${AAPT:-$(which aapt)}"

BUILD="$PROJECT/build_ci"
LIBS="$BUILD/libs"

PACKAGE="com.whoswho.app"
VERSION_CODE=100
VERSION_NAME="1.0.0"

echo "=== Who's Who MVP Build ==="
echo "Project: $PROJECT"
echo "Java: $($JAVAC -version 2>&1)"
echo "aapt: $($AAPT version 2>&1)"

# Validate tools
for tool in "$DX" "$JAVAC" "$AAPT" "$ZIPALIGN" "$APKSIGNER"; do
    if [ ! -f "$tool" ] && ! which "$tool" >/dev/null 2>&1; then
        echo "ERROR: Tool not found: $tool"
        exit 1
    fi
done

if [ ! -f "$ANDROID_JAR" ]; then
    echo "ERROR: android.jar not found: $ANDROID_JAR"
    exit 1
fi

# Clean build dir (preserve libs cache)
rm -rf "$BUILD/gen" "$BUILD/classes" "$BUILD/compiled"
mkdir -p "$BUILD"/{gen,classes} "$LIBS"

echo ""
echo "=== Step 1: Download dependencies ==="

download_lib() {
    local name="$1" url="$2" dest="$LIBS/$1"
    if [ -f "$dest" ] && [ "$(stat -c%s "$dest")" -gt 100 ]; then
        echo "  [cached] $name ($(stat -c%s "$dest") bytes)"
        return 0
    fi
    echo -n "  Downloading $name... "
    local retries=0
    while [ $retries -lt 4 ]; do
        if curl -sL --connect-timeout 15 -o "$dest" "$url" && [ -f "$dest" ] && [ "$(stat -c%s "$dest")" -gt 100 ]; then
            echo "OK ($(stat -c%s "$dest") bytes)"
            return 0
        fi
        retries=$((retries + 1))
        local wait=$((2 ** retries))
        echo -n "retry in ${wait}s... "
        sleep $wait
    done
    echo "FAILED"
    rm -f "$dest"
    return 1
}

DANDAR3="https://raw.githubusercontent.com/dandar3"

# --- AndroidX core libraries ---
download_lib "androidx-core.jar" "$DANDAR3/android-androidx-core/main/libs/androidx-core.jar"
download_lib "androidx-appcompat.jar" "$DANDAR3/android-androidx-appcompat/main/libs/androidx-appcompat.jar"
download_lib "androidx-appcompat-resources.jar" "$DANDAR3/android-androidx-appcompat-resources/master/libs/androidx-appcompat-resources.jar"
download_lib "androidx-fragment.jar" "$DANDAR3/android-androidx-fragment/master/libs/androidx-fragment.jar"
download_lib "androidx-activity.jar" "$DANDAR3/android-androidx-activity/master/libs/androidx-activity.jar"
download_lib "androidx-annotation.jar" "$DANDAR3/android-androidx-annotation/master/libs/androidx-annotation.jar"
download_lib "androidx-annotation-experimental.jar" "$DANDAR3/android-androidx-annotation-experimental/master/libs/androidx-annotation-experimental.jar"
download_lib "androidx-collection.jar" "$DANDAR3/android-androidx-collection/master/libs/androidx-collection.jar"
download_lib "androidx-concurrent-futures.jar" "$DANDAR3/android-androidx-concurrent-futures/master/libs/androidx-concurrent-futures.jar"
download_lib "androidx-cursoradapter.jar" "$DANDAR3/android-androidx-cursoradapter/master/libs/androidx-cursoradapter.jar"
download_lib "androidx-customview.jar" "$DANDAR3/android-androidx-customview/master/libs/androidx-customview.jar"
download_lib "androidx-drawerlayout.jar" "$DANDAR3/android-androidx-drawerlayout/master/libs/androidx-drawerlayout.jar"
download_lib "androidx-interpolator.jar" "$DANDAR3/android-androidx-interpolator/master/libs/androidx-interpolator.jar"
download_lib "androidx-loader.jar" "$DANDAR3/android-androidx-loader/master/libs/androidx-loader.jar"
download_lib "androidx-savedstate.jar" "$DANDAR3/android-androidx-savedstate/master/libs/androidx-savedstate.jar"
download_lib "androidx-tracing.jar" "$DANDAR3/android-androidx-tracing/master/libs/androidx-tracing.jar"
download_lib "androidx-versionedparcelable.jar" "$DANDAR3/android-androidx-versionedparcelable/master/libs/androidx-versionedparcelable.jar"
download_lib "androidx-vectordrawable.jar" "$DANDAR3/android-androidx-vectordrawable/master/libs/androidx-vectordrawable.jar"
download_lib "androidx-vectordrawable-animated.jar" "$DANDAR3/android-androidx-vectordrawable-animated/master/libs/androidx-vectordrawable-animated.jar"
download_lib "androidx-viewpager.jar" "$DANDAR3/android-androidx-viewpager/master/libs/androidx-viewpager.jar"
download_lib "androidx-viewpager2.jar" "$DANDAR3/android-androidx-viewpager2/master/libs/androidx-viewpager2.jar"

# --- Lifecycle ---
download_lib "androidx-lifecycle-common.jar" "$DANDAR3/android-androidx-lifecycle-common/master/libs/androidx-lifecycle-common.jar"
download_lib "androidx-lifecycle-runtime.jar" "$DANDAR3/android-androidx-lifecycle-runtime/master/libs/androidx-lifecycle-runtime.jar"
download_lib "androidx-lifecycle-viewmodel.jar" "$DANDAR3/android-androidx-lifecycle-viewmodel/master/libs/androidx-lifecycle-viewmodel.jar"
download_lib "androidx-lifecycle-viewmodel-savedstate.jar" "$DANDAR3/android-androidx-lifecycle-viewmodel-savedstate/master/libs/androidx-lifecycle-viewmodel-savedstate.jar"
download_lib "androidx-lifecycle-livedata.jar" "$DANDAR3/android-androidx-lifecycle-livedata/master/libs/androidx-lifecycle-livedata.jar"
download_lib "androidx-lifecycle-livedata-core.jar" "$DANDAR3/android-androidx-lifecycle-livedata-core/master/libs/androidx-lifecycle-livedata-core.jar"

# --- Arch ---
download_lib "androidx-arch-core-common.jar" "$DANDAR3/android-androidx-arch-core-common/master/libs/androidx-arch-core-common.jar"
download_lib "androidx-arch-core-runtime.jar" "$DANDAR3/android-androidx-arch-core-runtime/master/libs/androidx-arch-core-runtime.jar"

# --- UI components ---
download_lib "androidx-recyclerview.jar" "$DANDAR3/android-androidx-recyclerview/master/libs/androidx-recyclerview.jar"
download_lib "androidx-cardview.jar" "$DANDAR3/android-androidx-cardview/master/libs/androidx-cardview.jar"
download_lib "androidx-coordinatorlayout.jar" "$DANDAR3/android-androidx-coordinatorlayout/master/libs/androidx-coordinatorlayout.jar"
download_lib "androidx-transition.jar" "$DANDAR3/android-androidx-transition/master/libs/androidx-transition.jar"
download_lib "google-material.jar" "$DANDAR3/android-google-material/master/libs/google-material.jar"

# --- Guava listenablefuture ---
download_lib "google-guava-listenablefuture.jar" "$DANDAR3/android-google-guava-listenablefuture/master/libs/google-guava-listenablefuture.jar"

# --- Gson from Maven Central ---
download_lib "gson-2.10.1.jar" "https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"

echo ""
echo "=== Step 2: Package resources with aapt ==="
# Patch manifest: replace ${applicationId} placeholder
MANIFEST_PATCHED="$BUILD/AndroidManifest.xml"
sed "s/\${applicationId}/$PACKAGE/g" "$APP/src/main/AndroidManifest.xml" > "$MANIFEST_PATCHED"

$AAPT package -f -m \
    -J "$BUILD/gen" \
    -M "$MANIFEST_PATCHED" \
    -S "$APP/src/main/res" \
    -I "$ANDROID_JAR" \
    --auto-add-overlay \
    --min-sdk-version 27 \
    --target-sdk-version 27 \
    --version-code "$VERSION_CODE" \
    --version-name "$VERSION_NAME" 2>&1
echo "  R.java generated: $(find "$BUILD/gen" -name "R.java" | wc -l) files"

echo ""
echo "=== Step 3: BuildConfig ==="
mkdir -p "$BUILD/gen/com/whoswho/app"
cat > "$BUILD/gen/com/whoswho/app/BuildConfig.java" << BCJAVA
package com.whoswho.app;
public final class BuildConfig {
    public static final boolean DEBUG = true;
    public static final String APPLICATION_ID = "$PACKAGE";
    public static final String BUILD_TYPE = "debug";
    public static final int VERSION_CODE = $VERSION_CODE;
    public static final String VERSION_NAME = "$VERSION_NAME";
}
BCJAVA

echo ""
echo "=== Step 4: Compile Java ==="
CP="$ANDROID_JAR"
for jar in "$LIBS"/*.jar; do CP="$CP:$jar"; done

find "$APP/src/main/java" -name "*.java" > "$BUILD/sources.txt"
find "$BUILD/gen" -name "*.java" >> "$BUILD/sources.txt"

NSOURCES=$(wc -l < "$BUILD/sources.txt")
echo "  $NSOURCES Java source files"

mkdir -p "$BUILD/classes"
$JAVAC -source 1.8 -target 1.8 -encoding UTF-8 \
    -classpath "$CP" \
    -d "$BUILD/classes" \
    -Xlint:-options \
    @"$BUILD/sources.txt" 2>&1

echo "  Compiled classes: $(find "$BUILD/classes" -name "*.class" | wc -l)"

echo ""
echo "=== Step 5: DEX ==="
# Strip META-INF/versions that dx can't handle
for jar in "$LIBS"/*.jar; do
    zip -q -d "$jar" 'META-INF/versions/*' 2>/dev/null || true
    zip -q -d "$jar" 'module-info.class' 2>/dev/null || true
done

$DX --dex --min-sdk-version=27 --output="$BUILD/classes.dex" "$BUILD/classes" "$LIBS"/*.jar 2>&1

echo ""
echo "=== Step 6: Package APK ==="
$AAPT package -f \
    -M "$MANIFEST_PATCHED" \
    -S "$APP/src/main/res" \
    -I "$ANDROID_JAR" \
    --auto-add-overlay \
    --min-sdk-version 27 \
    --target-sdk-version 27 \
    --version-code "$VERSION_CODE" \
    --version-name "$VERSION_NAME" \
    -F "$BUILD/app-unsigned.apk" 2>&1

# Add DEX file
(cd "$BUILD" && zip -q -u app-unsigned.apk classes.dex)

echo ""
echo "=== Step 7: Align + Sign ==="
$ZIPALIGN -f 4 "$BUILD/app-unsigned.apk" "$BUILD/app-aligned.apk"

if [ ! -f "$PROJECT/debug.keystore" ]; then
    echo "  Generating debug keystore..."
    $KEYTOOL -genkey -v -keystore "$PROJECT/debug.keystore" \
        -storepass android -alias androiddebugkey -keypass android \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" 2>&1
else
    echo "  Reusing existing keystore"
fi

$APKSIGNER sign \
    --ks "$PROJECT/debug.keystore" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$BUILD/app-signed.apk" \
    "$BUILD/app-aligned.apk"

$APKSIGNER verify "$BUILD/app-signed.apk"

OUTPUT_APK="$PROJECT/whoswho-debug.apk"
cp "$BUILD/app-signed.apk" "$OUTPUT_APK"
SIZE=$(stat -c%s "$OUTPUT_APK")

echo ""
echo "=== BUILD COMPLETE ==="
echo "Output: whoswho-debug.apk ($SIZE bytes / $(echo "scale=1; $SIZE/1048576" | bc) MB)"
echo "Path: $OUTPUT_APK"
