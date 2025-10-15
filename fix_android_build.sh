#!/bin/bash
# Android 构建问题自动修复脚本
# 用于解决 "Duplicate root element android" 和 "Eclipse resource errors" 等构建错误

set -e

echo "🔧 开始修复 Android 构建问题..."
echo ""

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

echo "🛑 步骤 1/8: 停止 Gradle 守护进程..."
cd example/android && ./gradlew --stop 2>/dev/null || true
cd ../..
echo "✅ Gradle 进程已停止"
echo ""

echo "🗑️  步骤 2/8: 删除锁文件和缓存..."
find . -path "*/.gradle/*.lock" -delete 2>/dev/null || true
rm -rf example/android/.gradle android/.gradle 2>/dev/null || true
find . -type d -name ".idea" -exec rm -rf {} + 2>/dev/null || true
find . -name "*.iml" -delete 2>/dev/null || true
echo "✅ 锁文件和缓存已删除"
echo ""

echo "📦 步骤 3/8: 清理主项目..."
flutter clean
echo "✅ 主项目清理完成"
echo ""

echo "📥 步骤 4/8: 获取主项目依赖..."
flutter pub get
echo "✅ 主项目依赖获取完成"
echo ""

echo "📦 步骤 5/8: 清理 example 项目..."
cd example
flutter clean
echo "✅ Example 项目清理完成"
echo ""

echo "📥 步骤 6/8: 获取 example 项目依赖..."
flutter pub get
echo "✅ Example 项目依赖获取完成"
echo ""

echo "🧹 步骤 7/8: 重建 Gradle 缓存..."
cd android
./gradlew clean --refresh-dependencies --quiet
echo "✅ Gradle 缓存重建完成"
echo ""

echo "🔨 步骤 8/8: 测试构建..."
cd ..
flutter build apk --debug
echo ""

echo "✅ 所有步骤完成！Android 构建问题已修复。"
echo ""
echo "📱 APK 位置: example/build/app/outputs/flutter-apk/app-debug.apk"
echo ""
echo "📝 下一步："
echo "   在 VS Code 中按 Cmd+Shift+P，执行："
echo "   'Java: Clean Java Language Server Workspace'"
echo "   或者直接重启 VS Code"
