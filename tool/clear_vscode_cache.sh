#!/bin/bash
# 清除 VS Code Java 扩展缓存，解决持久性错误提示问题

echo "🧹 清除 VS Code 缓存..."

# 清除 VS Code 的工作区存储
echo "清理工作区缓存..."
rm -rf .vscode/.browse.* 2>/dev/null
rm -rf .vscode/settings.json.bak 2>/dev/null

# 清除 Java 语言服务器工作区数据
WORKSPACE_CACHE="$HOME/Library/Application Support/Code/User/workspaceStorage"
if [ -d "$WORKSPACE_CACHE" ]; then
    echo "找到 VS Code 工作区缓存目录"
    # 注意：这不会删除所有缓存，只是提示位置
    echo "📁 缓存位置: $WORKSPACE_CACHE"
fi

# 清除项目的 .idea 目录（如果存在）
find . -type d -name ".idea" -exec rm -rf {} + 2>/dev/null
echo "✅ 已清理 .idea 目录"

# 清除所有 .iml 文件
find . -name "*.iml" -delete 2>/dev/null
echo "✅ 已清理 .iml 文件"

# 清除 Gradle 守护进程
cd example/android 2>/dev/null && ./gradlew --stop 2>/dev/null
echo "✅ 已停止 Gradle 守护进程"

# 重新同步项目
cd ../..
echo ""
echo "🔄 重新同步 Flutter 项目..."
flutter clean > /dev/null 2>&1
flutter pub get > /dev/null 2>&1
cd example
flutter pub get > /dev/null 2>&1

echo ""
echo "✅ 缓存清理完成！"
echo ""
echo "📝 下一步操作："
echo "1. 在 VS Code 中按 Cmd+Shift+P（Mac）或 Ctrl+Shift+P（Windows/Linux）"
echo "2. 输入 'Java: Clean Java Language Server Workspace'"
echo "3. 选择该命令并确认重启"
echo ""
echo "或者直接重启 VS Code（推荐）"
