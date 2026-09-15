#!/bin/bash
set -euo pipefail

# ============================================================
#  PumpkinToolkit iOS 本地一键打包脚本（不签名）
#
#  用法: ./build-ios.sh
#  输出: build/ipa/PumpkinToolkit.ipa
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

RED='\033[0;31m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${CYAN}ℹ️  $*${NC}"; }
ok()    { echo -e "${GREEN}✅ $*${NC}"; }

echo ""
echo "========================================"
echo "  PumpkinToolkit iOS Build (Unsigned)"
echo "========================================"

# ── 版本号 ──
VER_CODE=$(grep '^appVersionCode\s*=' gradle/libs.versions.toml | sed 's/.*= *"\(.*\)"/\1/')
VER_NAME=$(grep '^appVersionName\s*=' gradle/libs.versions.toml | sed 's/.*= *"\(.*\)"/\1/')
info "Version = $VER_NAME ($VER_CODE)"
echo ""

# ── Step 1: 同步版本号 ──
info "Step 1/4: 同步版本号 ..."
XCCONFIG="iosApp/Configuration/Config.xcconfig"
sed -i '' "s/^CURRENT_PROJECT_VERSION=.*/CURRENT_PROJECT_VERSION=$VER_CODE/" "$XCCONFIG"
sed -i '' "s/^MARKETING_VERSION=.*/MARKETING_VERSION=$VER_NAME/" "$XCCONFIG"
ok "版本号 = $VER_NAME ($VER_CODE)"

# ── Step 2: 编译 Kotlin 框架 ──
info "Step 2/4: 编译 Kotlin 共享框架 (iosArm64) ..."
echo ""
./gradlew :shared:linkReleaseFrameworkIosArm64 --no-daemon \
    -Dorg.gradle.jvmargs=-Xmx4g 2>&1 | tail -5
ok "Kotlin 框架编译完成"
echo ""

# ── Step 3: Xcode Archive + IPA ──
info "Step 3/4: Xcode Archive → IPA ..."
echo ""
rm -rf build/PumpkinToolkit.xcarchive build/ipa

xcodebuild \
    -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Release \
    -sdk iphoneos \
    -archivePath build/PumpkinToolkit.xcarchive \
    -destination 'generic/platform=iOS' \
    CURRENT_PROJECT_VERSION="$VER_CODE" \
    MARKETING_VERSION="$VER_NAME" \
    CODE_SIGNING_ALLOWED=NO \
    CODE_SIGNING_REQUIRED=NO \
    CODE_SIGN_IDENTITY="" \
    archive 2>&1 | tail -3

APP_PATH="build/PumpkinToolkit.xcarchive/Products/Applications/PumpkinToolkit.app"
PAYLOAD_DIR="build/ipa/Payload"
mkdir -p "$PAYLOAD_DIR"
cp -R "$APP_PATH" "$PAYLOAD_DIR/"
cd build/ipa && zip -qr PumpkinToolkit.ipa Payload && cd ../..

IPA_PATH="build/ipa/PumpkinToolkit.ipa"
IPA_SIZE=$(du -h "$IPA_PATH" | cut -f1)
ok "IPA 打包完成: $IPA_PATH ($IPA_SIZE)"

# ── Step 4: 验证 ──
info "Step 4/4: 验证 ..."
echo "App Version : $(plutil -p "$APP_PATH/Info.plist" | grep CFBundleShortVersionString | awk '{print $3}')"
echo "Build       : $(plutil -p "$APP_PATH/Info.plist" | grep CFBundleVersion | awk '{print $3}')"

WIDGET_PATH="$APP_PATH/PlugIns/TodayScheduleWidget.appex"
if [ -d "$WIDGET_PATH" ]; then
    ok "Widget 扩展已包含"
else
    echo "⚠️  Widget 扩展未找到"
fi

echo ""
echo "========================================"
ok "打包完成！IPA 位于: $IPA_PATH"
echo "========================================"