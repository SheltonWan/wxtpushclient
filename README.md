# WXTPush 客户端SDK

多厂商原生推送服务客户端SDK，支持华为HMS、小米Push、OPPO Push、VIVO Push、荣耀Push、苹果APNs。

## 🚀 特性

- ✅ **多厂商支持**: 华为、荣耀、小米、OPPO、VIVO、苹果
- ✅ **原生集成**: 直接集成各厂商官方SDK，非第三方封装
- ✅ **统一接口**: 提供统一的API接口，简化多厂商推送集成
- ✅ **自动识别**: 根据设备品牌自动选择合适的推送服务
- ✅ **Token管理**: 自动获取和管理各厂商推送Token
- ✅ **消息处理**: 统一的推送消息接收和点击处理
- ✅ **权限管理**: 自动处理推送权限申请
- 🎉 **v1.1.0新增**: **极简配置** - Android配置量减少90%+，5分钟完成集成！

## ⚠️ **重要说明**

**当前真实支持状态 (v1.1.0):**
- **完全可用**: 华为HMS、苹果APNs (2个厂商)
- **测试阶段**: 荣耀、小米、OPPO (3个厂商) - 基础功能完成，需要实际设备验证
- **开发中**: VIVO (1个厂商) - 正在等待官方SDK，当前版本不可用

我们致力于提供诚实透明的功能描述，不夸大宣传。

## 📱 支持的推送服务

| 厂商 | 推送服务 | Android支持 | iOS支持 | 真实状态 |
|------|----------|-------------|---------|----------|
| 华为 | HMS Push Kit | ✅ | ❌ | ✅ 生产可用 |
| 荣耀 | Honor Push | ✅ | ❌ | ⚠️ 基础完成，需验证 |
| 小米 | Mi Push | ✅ | ❌ | ⚠️ AAR集成，需验证 |
| OPPO | OPPO Push | ✅ | ❌ | ⚠️ 功能完成，待优化 |
| VIVO | VIVO Push | 🚧 | ❌ | ❌ 开发中，暂不可用 |
| 苹果 | APNs | ❌ | ✅ | ✅ 生产可用 |

## 🛠️ 安装

### 1. 添加依赖

在 `pubspec.yaml` 中添加：

```yaml
dependencies:
  wxtpush_client: 
      git:
      url: https://github.com/SheltonWan/wxtpush_client.git
      ref: main
```

### 2. 安装依赖

```bash
flutter pub get
```

## 🔧 集成配置

### 🎉 简化版配置（推荐）

WXTPush v1.0.0+ 提供了极大简化的集成方式，**90%的配置都自动处理**！

#### Android配置（只需3步）

**第1步：项目级build.gradle配置**
```gradle
// 项目根目录/build.gradle
buildscript {
    repositories {
        google()
        mavenCentral()
        // WXTPush会自动添加厂商仓库，但手动添加可加快速度
        maven { url 'https://developer.huawei.com/repo/' }
        maven { url 'https://developer.hihonor.com/repo/' }
    }
    
    dependencies {
        // 如果需要华为推送，添加AGConnect插件
        classpath 'com.huawei.agconnect:agcp:1.9.1.300'
        // 如果需要荣耀推送，添加荣耀插件  
        classpath 'com.hihonor.mcs:honor-mcs-plugin:1.0.0'
    }
}
```

**第2步：应用级build.gradle配置**
```gradle
// android/app/build.gradle
plugins {
    id 'com.android.application'
    // 如果使用华为推送
    id 'com.huawei.agconnect'
    // 如果使用荣耀推送
    id 'com.hihonor.mcs'
}

android {
    defaultConfig {
        // ✨ 只需配置您要使用的厂商参数，其他全自动！
        manifestPlaceholders = [
            // 华为推送（可选）
            HUAWEI_APP_ID: 'your_huawei_app_id',
            
            // 荣耀推送（可选）
            HONOR_APP_ID: 'your_honor_app_id',
            
            // 小米推送（可选）
            XIAOMI_APP_ID: 'your_xiaomi_app_id',
            XIAOMI_APP_KEY: 'your_xiaomi_app_key',
            
            // OPPO推送（可选）
            OPPO_APP_KEY: 'your_oppo_app_key',
            OPPO_APP_SECRET: 'your_oppo_app_secret',
            
            // VIVO推送（可选）
            VIVO_APP_ID: 'your_vivo_app_id',
            VIVO_APP_KEY: 'your_vivo_app_key'
        ]
    }
    
    // 确保有签名配置（推送需要）
    signingConfigs {
        release {
            // 您的签名配置
        }
    }
}

// ✨ 无需手动添加SDK依赖！WXTPush插件自动处理
```

**第3步：添加厂商配置文件（如需要）**
- 华为：将 `agconnect-services.json` 放到 `android/app/` 
- 荣耀：将 `mcs-services.json` 放到 `android/app/`
- 其他厂商：无需配置文件

### 传统配置方式（兼容保留）

如果您需要完全手动控制，仍可使用传统配置：

#### 2. 小米Push集成

在 `android/app/build.gradle` 中添加：

```gradle
dependencies {
    implementation 'com.xiaomi.mipush:mipush-android-sdk:5.0.8'
}
```

#### 3. OPPO Push集成

在 `android/app/build.gradle` 中添加：

```gradle
dependencies {
    implementation 'com.oppo.mcs:mcssdk:3.1.0'
}
```

#### 4. VIVO Push集成

在 `android/app/build.gradle` 中添加：

```gradle
dependencies {
    implementation 'com.vivo.mcs:mcssdk:3.1.1'
}
```

#### 5. 荣耀Push集成

在 `android/app/build.gradle` 中添加：

```gradle
dependencies {
    implementation 'com.honor.mcs:mcssdk:6.5.0.300'
}
```

### iOS配置

#### 1. 苹果APNs集成

在 `ios/Runner/Info.plist` 中添加：

```xml
<key>UIBackgroundModes</key>
<array>
    <string>remote-notification</string>
</array>
```

在Xcode中启用Push Notifications能力。

## 💻 使用方法

### 1. 简化初始化（推荐）

```dart
import 'package:wxtpush_client/wxtpush_client.dart';

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
    _initializePush();
  }
  
  Future<void> _initializePush() async {
    // ✨ 自动从manifest读取配置 - 超简单！
    final config = await PushConfig.fromManifest(
      // iOS端需要手动配置
      apple: const AppleConfig(
        bundleId: 'com.yourcompany.yourapp',
        useSandbox: false,
      ),
      debugMode: true,
    );

    // 自定义消息处理器
    final messageHandler = MyPushMessageHandler();

    // 初始化推送服务
    await WxtpushClient.instance.initialize(config, messageHandler: messageHandler);
  }
}
```

### 传统初始化方式（完全手动控制）

```dart
Future<void> _initializePushTraditional() async {
  // 手动配置所有厂商参数
  final config = PushConfig(
    // 华为HMS配置
    huawei: const HuaweiConfig(
      appId: 'your_huawei_app_id',
      appSecret: 'your_huawei_app_secret',
    ),
    // 小米推送配置
    xiaomi: const XiaomiConfig(
      appId: 'your_xiaomi_app_id',
      appKey: 'your_xiaomi_app_key',
      appSecret: 'your_xiaomi_app_secret',
    ),
    // OPPO推送配置
    oppo: const OppoConfig(
      appId: 'your_oppo_app_id',
      appKey: 'your_oppo_app_key',
      appSecret: 'your_oppo_app_secret',
    ),
    // VIVO推送配置
    vivo: const VivoConfig(
      appId: 'your_vivo_app_id',
      appKey: 'your_vivo_app_key',
      appSecret: 'your_vivo_app_secret',
    ),
    // 荣耀推送配置
    honor: const HonorConfig(
      appId: 'your_honor_app_id',
      appSecret: 'your_honor_app_secret',
    ),
    // 苹果APNs配置
    apple: const AppleConfig(
      bundleId: 'com.yourcompany.yourapp',
      useSandbox: false,
    ),
    debugMode: true,
  );

  await WxtpushClient.instance.initialize(config, messageHandler: messageHandler);
}
```

### 2. 自定义消息处理器

```dart
class MyPushMessageHandler implements PushMessageHandler {
  @override
  Future<void> onMessageReceived(PushMessage message) async {
    print('收到推送消息: ${message.title} - ${message.body}');
    // 处理收到的推送消息
  }

  @override
  Future<void> onMessageClicked(PushMessage message) async {
    print('点击推送消息: ${message.title} - ${message.body}');
    // 处理用户点击推送消息
  }

  @override
  Future<void> onTokenUpdated(String token, String vendor) async {
    print('Token更新: $vendor - $token');
    // 将新Token发送到服务器
    await uploadTokenToServer(token, vendor);
  }

  @override
  Future<void> onError(String error, String? vendor) async {
    print('推送服务错误: ${vendor ?? 'Unknown'} - $error');
    // 处理推送服务错误
  }
}
```

### 3. 获取推送Token

```dart
// 获取所有可用的推送Token
final tokens = await WxtpushClient.instance.getTokens();
for (final token in tokens) {
  print('${token.vendor.displayName}: ${token.token}');
}

// 获取指定厂商的推送Token
final huaweiToken = await WxtpushClient.instance.getToken(PushVendor.huawei);
if (huaweiToken != null) {
  print('华为Token: ${huaweiToken.token}');
}
```

### 4. 主题订阅

```dart
// 订阅主题
await WxtpushClient.instance.subscribeToTopic('news');

// 取消订阅主题
await WxtpushClient.instance.unsubscribeFromTopic('news');
```

### 5. 设置别名和标签

```dart
// 设置用户别名
await WxtpushClient.instance.setAlias('user_123');

// 设置用户标签
await WxtpushClient.instance.setTags(['vip', 'male', 'beijing']);
```

## 🔄 与服务端集成

获取到推送Token后，需要将Token上传到您的推送服务器：

```dart
Future<void> uploadTokenToServer(String token, String vendor) async {
  final response = await http.post(
    Uri.parse('https://your-server.com/api/push/tokens'),
    headers: {'Content-Type': 'application/json'},
    body: json.encode({
      'token': token,
      'vendor': vendor,
      'deviceId': await DeviceUtils.getDeviceId(),
      'platform': Platform.isAndroid ? 'android' : 'ios',
    }),
  );
  
  if (response.statusCode == 200) {
    print('Token上传成功');
  } else {
    print('Token上传失败: ${response.body}');
  }
}
```

## 🚨 注意事项

### 1. 权限申请
- Android: 自动处理推送权限和通知权限
- iOS: 自动申请推送通知权限

### 2. 厂商限制
- 华为设备只能使用HMS Push
- 小米设备建议使用Mi Push
- OPPO设备建议使用OPPO Push
- VIVO设备建议使用VIVO Push
- iOS设备只能使用APNs

### 3. Token有效性
- 推送Token可能会定期更新
- 建议在Token更新时及时上传到服务器
- 服务器应该处理Token失效的情况

### 4. 测试建议
- 开发阶段建议开启debugMode
- 使用真机测试推送功能
- 测试各种消息格式和点击跳转

## 📚 API文档

详细的API文档请参考：[API Reference](./docs/api.md)

## � 使用指南（完整）

本节提供从“快速开始 → 平台准备 → 代码接入 → 常用 API → 事件与回调 → 厂商要点 → 常见问题”的完整上手路径。

### 快速开始

- 目标：在 Flutter App 内统一初始化多厂商推送（华为/荣耀/小米/OPPO/VIVO/苹果），获取 Token 并接收消息/点击事件。
- 入口：`WxtpushClient.instance.initialize(config, messageHandler: ...)`
- 事件：`messageReceived`、`messageClicked`、`tokenUpdated`、`tokenError`、`permissionGranted/Denied`

### 平台准备

- Flutter/Dart：Dart 3.0+，Flutter 稳定版 3.x
- iOS：
  - Xcode Capabilities → Push Notifications，Background Modes → Remote notifications
  - APNs 配置（证书/密钥/Team/BundleID），真机测试
- Android：
  - Android 13+：需要 POST_NOTIFICATIONS 运行时权限
  - 各厂商控制台：必须配置“包名 + 签名(SHA1) + 密钥（appId/appKey/appSecret）”并与安装包一致
  - OPPO：清单需包含 `com.heytap.mcs.appkey`/`appsecret`（兼容写入 `com.coloros.mcs.*`），权限包含 `RECEIVE_MCS_MESSAGE`（heytap 与 coloros 两套）

  - 注意：OPPO 开放平台存在“测试权限”与“通知栏推送（正式权限）”的差异。仅有测试权限时，可能出现注册无回调、无法下发/展示通知等限制。线上发布前必须申请“通知栏推送”权限（需应用资源上架后申请）。详见《[OPPO 推送权限与上线指引](../doc/OPPO_PUSH_PERMISSION_GUIDE.md)》。

> 注：示例中给出的 Gradle 依赖坐标仅作参考，厂商 SDK 的获取方式以官方文档为准（部分版本需本地 AAR）。

### 代码接入

在应用启动后尽早初始化（如首页 initState）：

```dart
import 'package:wxtpush_client/wxtpush_client.dart';
import 'package:wxtpush_client/src/models/push_config.dart';
import 'package:wxtpush_client/src/handlers/push_message_handler.dart';

class MyPushHandler extends PushMessageHandler {
  @override
  void onTokenUpdated(String token, String vendor) {
    // 保存并上报到你的后端
    print('Token[$vendor] = $token');
  }

  @override
  void onMessageReceived(PushMessage message) {
    print('收到消息: ${message.title} - ${message.body}');
  }

  @override
  void onMessageClicked(PushMessage message) {
    print('点击消息: ${message.title}');
  }

  @override
  void onError(String error, String? vendor) {
    print('Push错误[$vendor]: $error');
  }

  @override
  void onPermissionChanged(bool granted, String? vendor) {
    print('通知权限: $granted vendor=$vendor');
  }
}

Future<void> initPush() async {
  final config = PushConfig(
    huawei: HuaweiConfig(appId: '华为appId', appSecret: '华为appSecret'),
    honor: HonorConfig(appId: '荣耀appId', appSecret: '荣耀appSecret'),
    xiaomi: XiaomiConfig(appId: '小米appId', appKey: '小米appKey', appSecret: '小米appSecret'),
    oppo: OppoConfig(appId: null, appKey: 'oppoAppKey', appSecret: 'oppoAppSecret'),
    vivo:  VivoConfig(appId: 'vivoAppId', appKey: 'vivoAppKey', appSecret: 'vivoAppSecret'),
    apple: AppleConfig(bundleId: '你的iOS Bundle ID', useSandbox: true),
  );

  await WxtpushClient.instance.initialize(
    config,
    messageHandler: MyPushHandler(),
  );
}
```

说明：
- `initialize` 会先建立事件监听，随后请求通知权限，再延迟初始化原生推送；
- Android 仅初始化当前设备支持的厂商；
- OPPO 侧会优先读取清单中的 `appkey/appsecret`（与 Dart 传入不一致时以清单为准），并在回调不触发时做轮询与重试。

### 常用 API

- 权限：`requestPermission()`、`isPermissionGranted()`
- Token：`getTokens()`、`getToken(vendor)`、`refreshToken(vendor)`、`deleteToken(vendor)`
- 推送开关与状态：`enablePush({vendor})`、`disablePush({vendor})`、`isPushEnabled({vendor})`
- Topic：`subscribeToTopic(topic,{vendor})`、`unsubscribeFromTopic(topic,{vendor})`
- 用户标识：`setAlias(alias,{vendor})`、`setTags(tags,{vendor})`
- 资源释放：`dispose()`

### 事件与回调

SDK 通过 `EventChannel('wxtpush_client/events')` 下发事件，`WxtpushClient` 会分发到 `PushMessageHandler`：
- `messageReceived` → `onMessageReceived(PushMessage)`
- `messageClicked` → `onMessageClicked(PushMessage)`
- `tokenUpdated` → `onTokenUpdated(String token, String vendor)`
- `tokenError`/`error` → `onError(String error, String? vendor)`
- `permissionGranted/Denied` → `onPermissionChanged(bool granted, String? vendor)`

### 厂商要点

- iOS：开启 Push/Background Modes，APNs 真机测试；首次授权由 SDK 代为请求
- 华为/荣耀/小米/VIVO：控制台参数（appId/appKey/appSecret）齐全，包名与签名匹配
- OPPO：
  - 清单元数据：`com.heytap.mcs.appkey/appsecret`（建议同时写 `com.coloros.mcs.*` 兼容键）
  - 权限：`RECEIVE_MCS_MESSAGE`（heytap 与 coloros 两套都加）
  - 包名与签名(SHA1)需与 Heytap 平台绑定一致；否则日志常见 `appBean is null`，无法获得 RegisterID
  - 设备端打开通知、自启动、后台运行免限制；必要时登录 OPPO 账号
  - 正式发布前，请确认已获批“通知栏推送”权限，否则可能导致回调不触发或消息不展示。参见《[OPPO 推送权限与上线指引](../doc/OPPO_PUSH_PERMISSION_GUIDE.md)》。

### 常见问题与排错

- 拿不到 Token：
  - 检查“包名 + 签名(SHA1) + 密钥”与安装包完全一致（尤其 OPPO）
  - Android 13+ 授予通知权限
  - 设备设置中允许通知/自启动/后台运行，关闭极限省电/数据限制
- 通知不弹：
  - iOS：检查 APNs、Entitlements、Provision Profile
  - Android：检查通知权限与 ROM 通道/样式开关
- 事件未回调：
  - 确保尽早调用 `initialize`，检查自定义 `PushMessageHandler` 是否正确覆写

## �🐛 问题反馈

如果遇到问题，请提交 Issue 并包含以下信息：
- Flutter 版本
- 设备型号和系统版本
- 错误日志
- 复现步骤

## 📄 许可证

MIT License
