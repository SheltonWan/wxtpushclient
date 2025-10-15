# Android 构建问题快速修复指南

## 🚨 问题症状
```
A project with the name android already exists.
The supplied phased action failed with an exception.
Duplicate root element android
```

## ⚡ 快速修复（推荐）

### 方法一：使用自动修复脚本
```bash
./fix_android_build.sh
```

### 方法二：手动执行命令
```bash
# 在项目根目录执行
flutter clean && flutter pub get

# 清理 example 项目
cd example
flutter clean && flutter pub get

# 清理 Android 缓存
cd android
rm -rf .gradle .idea build
./gradlew clean

# 测试构建
cd ..
flutter build apk --debug
```

## 📋 问题原因

1. **Flutter 插件未正确同步** - `.flutter-plugins` 文件过期
2. **Gradle 缓存污染** - `.gradle` 和 `.idea` 目录包含旧配置
3. **依赖未解析** - `pub get` 未执行或执行不完整

## ✅ 修复验证

执行以下命令检查是否修复成功：
```bash
cd example
flutter build apk --debug
```

成功标志：
```
✓ Built build/app/outputs/flutter-apk/app-debug.apk
```

## 🔍 预防措施

### 每次修改依赖后执行
```bash
flutter clean && flutter pub get
cd example && flutter clean && flutter pub get
```

### 每次拉取代码后执行
```bash
./fix_android_build.sh
```

## 📝 常见问题

### Q: 为什么需要清理缓存？
A: Flutter 和 Gradle 的缓存可能包含过期的配置信息，导致构建冲突。

### Q: 脚本执行失败怎么办？
A: 检查以下内容：
1. 确保 Flutter SDK 已正确安装：`flutter doctor`
2. 确保有网络连接（需要下载依赖）
3. 检查 Android SDK 路径配置

### Q: 还是报同样的错误？
A: 尝试完全删除并重新生成：
```bash
cd example
rm -rf android
flutter create --platforms=android .
```

## 🛠️ 高级故障排查

### 查看详细错误日志
```bash
cd example/android
./gradlew assembleDebug --stacktrace --info
```

### 检查 Gradle 配置
```bash
cd example/android
./gradlew properties
```

### 验证插件注册
```bash
cat example/.flutter-plugins
```

## 📞 获取帮助

如果问题仍未解决，请提供以下信息：
- Flutter 版本：`flutter --version`
- Gradle 版本：`cd example/android && ./gradlew --version`
- 完整错误日志：`flutter build apk --debug --verbose`

---
**最后更新**: 2025年10月16日  
**状态**: ✅ 已修复并验证
