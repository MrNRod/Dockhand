#!/usr/bin/env bash
set -euo pipefail

# Packages DockhandMac into a real, ad-hoc-signed .app bundle and optional DMG.
# Must run on macOS (arm64) with Xcode installed (needs full Xcode, not just Command
# Line Tools, for the same cinterop reason core/README.md documents).
#
# Ad-hoc signed only (codesign --sign -): no Apple Developer ID, no notarization.
# Gatekeeper will warn "unidentified developer" on first launch — users get past it
# with right-click -> Open, or `xattr -cr Dockhand.app`.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CORE_FRAMEWORK_DIR="$ROOT_DIR/core/build/bin/macosArm64/releaseFramework"
BUILD_DIR="$SCRIPT_DIR/build"
DIST_DIR="$BUILD_DIR/dist"
SCRATCH_DIR="$BUILD_DIR/scratch"
APP_NAME="Dockhand"
LIBUSB_SRC="/opt/homebrew/opt/libusb/lib/libusb-1.0.0.dylib"

BUILD_DMG=true
for arg in "$@"; do
    case "$arg" in
        --app-only)
            BUILD_DMG=false
            ;;
        --dmg)
            BUILD_DMG=true
            ;;
    esac
done

rm -rf "$SCRATCH_DIR"
mkdir -p "$DIST_DIR" "$SCRATCH_DIR"

echo "==> Reading app.version"
APP_VERSION="$(cd "$ROOT_DIR" && ./gradlew -q printVersion | tail -n1 | tr -d '[:space:]')"
echo "    $APP_VERSION"

echo "==> Building release Kotlin/Native framework"
(cd "$ROOT_DIR" && ./gradlew :core:linkReleaseFrameworkMacosArm64)

echo "==> Building Swift executable (release)"
export DOCKHAND_FRAMEWORK_DIR=releaseFramework
BIN_PATH="$(swift build -c release --package-path "$SCRIPT_DIR" --show-bin-path)"
swift build -c release --package-path "$SCRIPT_DIR"

APP="$SCRATCH_DIR/$APP_NAME.app"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources" "$APP/Contents/Frameworks"

echo "==> Copying executable"
cp "$BIN_PATH/DockhandMac" "$APP/Contents/MacOS/DockhandMac"
chmod u+w "$APP/Contents/MacOS/DockhandMac"

echo "==> Generating AppIcon.icns from AppIcon.png"
ICONSET="$SCRATCH_DIR/AppIcon.iconset"
mkdir -p "$ICONSET"
SRC_PNG="$SCRIPT_DIR/Sources/DockhandMac/Resources/AppIcon.png"
for size in 16 32 128 256 512; do
    sips -z "$size" "$size" "$SRC_PNG" --out "$ICONSET/icon_${size}x${size}.png" >/dev/null
    double=$((size * 2))
    sips -z "$double" "$double" "$SRC_PNG" --out "$ICONSET/icon_${size}x${size}@2x.png" >/dev/null
done
iconutil -c icns "$ICONSET" -o "$APP/Contents/Resources/AppIcon.icns"

echo "==> Writing Info.plist"
sed -e "s/__APP_VERSION__/$APP_VERSION/g" \
    "$SCRIPT_DIR/packaging/Info.plist.template" > "$APP/Contents/Info.plist"

echo "==> Embedding core.framework"
cp -R "$CORE_FRAMEWORK_DIR/core.framework" "$APP/Contents/Frameworks/core.framework"
CORE_BIN="$APP/Contents/Frameworks/core.framework/Versions/A/core"
chmod u+w "$CORE_BIN"
# core.framework already carries a proper @rpath-relative install name and the
# standard app-bundle rpaths (@executable_path/../Frameworks, @loader_path/Frameworks)
# from Kotlin/Native's own linker — no relinking needed for the framework itself.

echo "==> Fixing DockhandMac's rpath (was an absolute build-machine path)"
# `swift build` bakes in an absolute -rpath pointing at $CORE_FRAMEWORK_DIR (see
# Package.swift) so local dev/testing works without any bundle at all. That path
# won't exist on another machine, so swap it for the portable, bundle-relative one.
install_name_tool -delete_rpath "$CORE_FRAMEWORK_DIR" "$APP/Contents/MacOS/DockhandMac"
install_name_tool -add_rpath "@executable_path/../Frameworks" "$APP/Contents/MacOS/DockhandMac"

echo "==> Embedding + relinking libusb-1.0.0.dylib"
if [ ! -f "$LIBUSB_SRC" ]; then
    echo "error: $LIBUSB_SRC not found (expects Homebrew's libusb to be installed on the build machine)" >&2
    exit 1
fi
cp "$LIBUSB_SRC" "$APP/Contents/Frameworks/libusb-1.0.0.dylib"
chmod u+w "$APP/Contents/Frameworks/libusb-1.0.0.dylib"
install_name_tool -id "@rpath/libusb-1.0.0.dylib" "$APP/Contents/Frameworks/libusb-1.0.0.dylib"
install_name_tool -change "$LIBUSB_SRC" "@rpath/libusb-1.0.0.dylib" "$CORE_BIN"

echo "==> Ad-hoc code signing"
codesign --force --sign - "$APP/Contents/Frameworks/libusb-1.0.0.dylib"
codesign --force --sign - "$APP/Contents/Frameworks/core.framework"
codesign --force --sign - "$APP/Contents/MacOS/DockhandMac"
codesign --force --sign - "$APP"
codesign --verify --deep --strict "$APP"

echo "==> Exporting .app bundle to dist"
rm -rf "$DIST_DIR/$APP_NAME.app"
cp -R "$APP" "$DIST_DIR/"
echo "    App bundle created: $DIST_DIR/$APP_NAME.app"

if [ "$BUILD_DMG" = true ]; then
    echo "==> Building DMG"
    DMG_STAGING="$SCRATCH_DIR/dmg"
    mkdir -p "$DMG_STAGING"
    cp -R "$DIST_DIR/$APP_NAME.app" "$DMG_STAGING/"
    ln -s /Applications "$DMG_STAGING/Applications"
    DMG_PATH="$DIST_DIR/Dockhand-$APP_VERSION-macos-arm64.dmg"
    rm -f "$DMG_PATH"
    hdiutil create -volname "$APP_NAME" -srcfolder "$DMG_STAGING" -ov -format UDZO "$DMG_PATH"
    echo "    DMG created: $DMG_PATH"
fi

echo "==> Done!"
