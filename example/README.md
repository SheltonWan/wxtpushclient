# WXTPush Client SDK 示例应用

这是一个展示如何使用WXTPush多厂商推送服务客户端SDK的Flutter示例应用。

## 项目简介

WXTPush Client SDK 支持多个主流推送厂商，包括：
- 华为HMS Push
- 荣耀Honor Push  
- 小米Mi Push
- OPPO Push
- VIVO Push
- 苹果APNs (iOS)

## 快速开始

### 1. 环境要求
- Flutter 3.0.0 或更高版本
- Dart 3.0.0 或更高版本
- Android API 21+ (Android 5.0)
- iOS 10.0+ (如果需要iOS支持)

### 2. 安装依赖
```bash
flutter pub get
```

### 3. Android配置

#### 3.1 添加推送服务配置文件
将各厂商的配置文件放置到相应位置：
- 华为: `android/app/agconnect-services.json`
- 小米: 在 `android/app/build.gradle` 中配置AppId和AppKey
- OPPO/VIVO: 在代码中配置相应参数

#### 3.2 检查权限配置
确保 `android/app/src/main/AndroidManifest.xml` 包含必要权限：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.VIBRATE" />
<!-- 其他厂商特定权限 -->
```

### 4. 运行示例

#### Android
```bash
flutter run
```

#### iOS (需要额外配置)
```bash
flutter run -d ios
```

## 主要功能演示

### 1. 推送服务初始化
示例展示如何初始化和注册多个推送厂商：
```dart
await WxtpushClient.initialize();
await WxtpushClient.registerVendor(PushVendor.huawei, config);
```

### 2. Token获取
展示如何获取各厂商的推送Token：
```dart
final token = await WxtpushClient.getToken(PushVendor.huawei);
```

### 3. 消息接收
演示推送消息的接收和处理：
```dart
WxtpushClient.onMessageReceived.listen((message) {
  print('收到推送消息: ${message.title}');
});
```

### 4. 厂商检测
自动检测设备支持的推送厂商：
```dart
final availableVendors = await WxtpushClient.getAvailableVendors();
```

## 项目结构

```
lib/
├── main.dart              # 应用入口和主要演示代码
├── pages/                 # 页面文件
│   ├── home_page.dart     # 主页面
│   ├── token_page.dart    # Token管理页面
│   └── message_page.dart  # 消息历史页面
└── utils/                 # 工具类
    └── notification_helper.dart  # 通知辅助工具

android/
├── app/
│   ├── build.gradle       # Android构建配置
│   ├── agconnect-services.json  # 华为HMS配置
│   └── src/main/
│       └── AndroidManifest.xml  # Android权限配置
```

## 配置说明

### 华为HMS Push
1. 在华为开发者联盟注册应用
2. 下载 `agconnect-services.json` 放到 `android/app/` 目录
3. 在代码中使用AppId和AppSecret初始化

### 小米Mi Push
1. 在小米开放平台注册应用
2. 获取AppId、AppKey、AppSecret
3. 在 `build.gradle` 和代码中配置相应参数

### OPPO Push
1. 在OPPO开放平台注册应用
2. 获取AppId、AppKey、AppSecret
3. 在代码中配置推送参数

### VIVO Push
1. 在VIVO开放平台注册应用  
2. 获取AppId、AppKey、AppSecret
3. 在代码中配置推送参数

## 故障排除

### 常见问题

1. **Token获取失败**
   - 检查网络连接
   - 确认配置文件正确
   - 验证应用签名

2. **消息接收异常**
   - 检查权限配置
   - 确认后台服务运行
   - 验证消息格式

3. **厂商检测失败**
   - 确认设备支持相应厂商
   - 检查SDK版本兼容性
   - 验证依赖配置

### 调试技巧

启用调试日志：
```dart
WxtpushClient.setLogLevel(LogLevel.debug);
```

查看详细错误信息：
```dart
WxtpushClient.onError.listen((error) {
  print('推送错误: $error');
});
```

## 相关链接

- [WXTPush 主项目](../../README.md)
- [API 文档](../../doc/api.md)
- [厂商接入指南](../../doc/vendor_guide.md)
- [常见问题解答](../../doc/faq.md)

## 许可证

本项目遵循 MIT 许可证 - 详情请参见 [LICENSE](../../LICENSE) 文件。
