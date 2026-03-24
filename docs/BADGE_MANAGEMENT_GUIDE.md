# 应用角标管理功能指南

## 概述

WXTPush Client 插件现已支持跨平台的应用角标管理功能，允许您在 iOS 和 Android 设备上设置、清除和查询应用图标上的角标数字。

## 功能特性

- ✅ **统一API**：提供跨平台一致的角标管理接口
- ✅ **多厂商支持**：自动适配 iOS、华为、荣耀、小米、OPPO、VIVO、三星
- ✅ **自动降级**：不支持角标的设备会自动降级，不影响其他功能
- ✅ **简单易用**：仅需 3 个核心方法即可完成所有操作

## API 说明

### 1. 设置角标 `setBadge(int count)`

设置应用图标上显示的角标数字。

```dart
// 设置角标为 5
final success = await WxtpushClient.instance.setBadge(5);
if (success) {
  print('角标设置成功');
}

// 设置角标为 0（等同于清除角标）
await WxtpushClient.instance.setBadge(0);
```

**参数：**
- `count` (int): 要显示的角标数字，传入 0 表示清除角标
- `vendor` (PushVendor?, 可选): 指定厂商，不指定则设置所有已初始化的厂商

**返回值：** `Future<bool>` - 操作是否成功

### 2. 清除角标 `clearBadge()`

清除应用图标上的角标，等同于 `setBadge(0)`。

```dart
final success = await WxtpushClient.instance.clearBadge();
```

**参数：**
- `vendor` (PushVendor?, 可选): 指定厂商

**返回值：** `Future<bool>` - 操作是否成功

### 3. 获取角标 `getBadge()`

获取当前应用角标数字（部分厂商可能不支持读取）。

```dart
final count = await WxtpushClient.instance.getBadge();
print('当前角标: $count');
```

**参数：**
- `vendor` (PushVendor?, 可选): 指定厂商

**返回值：** `Future<int>` - 当前角标数字，获取失败返回 0

## 使用示例

### 基础示例

```dart
import 'package:wxtpush_client/wxtpush_client.dart';

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  int _unreadCount = 0;

  @override
  void initState() {
    super.initState();
    _initPush();
  }

  Future<void> _initPush() async {
    // 初始化推送服务
    final config = await PushConfig.fromManifest(
      apple: const AppleConfig(
        bundleId: 'com.example.app',
        useSandbox: true,
      ),
    );
    
    await WxtpushClient.instance.initialize(
      config,
      messageHandler: MyPushMessageHandler(
        onMessageReceived: _onMessageReceived,
      ),
    );
  }

  Future<void> _onMessageReceived(PushMessage message) async {
    // 收到新消息，更新未读数
    setState(() {
      _unreadCount++;
    });
    
    // 更新应用角标
    await WxtpushClient.instance.setBadge(_unreadCount);
  }

  Future<void> _markAllAsRead() async {
    // 标记所有消息已读
    setState(() {
      _unreadCount = 0;
    });
    
    // 清除应用角标
    await WxtpushClient.instance.clearBadge();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('消息 ($_unreadCount)'),
        actions: [
          IconButton(
            icon: Icon(Icons.done_all),
            onPressed: _markAllAsRead,
            tooltip: '全部已读',
          ),
        ],
      ),
      body: MessageList(),
    );
  }
}
```

### 完整示例

参考 `example/lib/main.dart` 中的示例代码，包含：
- 角标增加按钮
- 角标清除按钮
- 角标查询按钮
- 实时角标显示

## 平台支持情况

| 平台 | 支持状态 | 说明 |
|------|---------|------|
| **iOS** | ✅ 完全支持 | 原生 API，完美支持设置、清除、读取 |
| **华为** | ✅ 支持 | 通过 Launcher Provider 实现 |
| **荣耀** | ✅ 支持 | 通过 Launcher Provider 实现 |
| **小米** | ✅ 支持 | 通过通知管理器 API |
| **OPPO** | ⚠️ 部分支持 | 部分机型支持，需要申请权限 |
| **VIVO** | ⚠️ 部分支持 | 部分机型支持 |
| **三星** | ✅ 支持 | 通过广播机制 |
| **其他** | ❌ 不支持 | 会返回失败，不影响其他功能 |

## 权限说明

### Android

华为/荣耀设备需要在 `AndroidManifest.xml` 中声明角标权限（插件已自动添加）：

```xml
<uses-permission android:name="com.huawei.android.launcher.permission.CHANGE_BADGE" />
```

### iOS

角标功能包含在通知权限中，需要在 Info.plist 中配置通知权限（插件已自动处理）。

## 注意事项

### 1. Android 系统差异

Android 8.0+ 的角标功能由系统通知渠道控制，本插件提供的是厂商定制的角标 API。不同厂商的实现存在差异：

- **华为/荣耀**：需要设备支持 EMUI/MagicUI 桌面
- **小米**：MIUI 系统内置支持
- **OPPO**：ColorOS 系统部分机型支持
- **VIVO**：OriginOS/FuntouchOS 部分机型支持

### 2. 角标读取限制

由于大部分 Android 厂商不提供角标读取 API，`getBadge()` 方法会返回插件缓存的角标数字。如果应用重启或角标被系统清除，缓存值可能不准确。

### 3. 推荐做法

```dart
// ✅ 推荐：在应用中维护未读数
class MessageState {
  int unreadCount = 0;
  
  Future<void> incrementUnread() async {
    unreadCount++;
    await WxtpushClient.instance.setBadge(unreadCount);
    await _saveToStorage(unreadCount); // 持久化存储
  }
  
  Future<void> clearUnread() async {
    unreadCount = 0;
    await WxtpushClient.instance.clearBadge();
    await _saveToStorage(0);
  }
}

// ❌ 不推荐：完全依赖 getBadge()
Future<void> incrementBadge() async {
  final current = await WxtpushClient.instance.getBadge();
  await WxtpushClient.instance.setBadge(current + 1);
}
```

### 4. 指定厂商设置

可以为特定厂商单独设置角标：

```dart
// 仅为华为设备设置角标
await WxtpushClient.instance.setBadge(5, vendor: PushVendor.huawei);

// 仅为 iOS 设置角标
await WxtpushClient.instance.setBadge(5, vendor: PushVendor.apple);

// 为所有已初始化的厂商设置角标（默认）
await WxtpushClient.instance.setBadge(5);
```

## 故障排查

### 角标设置不生效

1. **检查权限**：确认应用已获得通知权限
2. **检查厂商**：确认当前设备厂商是否支持角标
3. **查看日志**：启用 debugMode 查看详细日志

```dart
final config = await PushConfig.fromManifest(
  debugMode: true, // 启用调试日志
  apple: ...,
);
```

### iOS 角标不显示

- 确认已在 `initialize()` 时请求通知权限
- 检查设备设置中应用的通知权限是否开启
- 确认应用前台时是否设置了 `.badge` 选项

### Android 特定厂商不生效

不同厂商的角标实现差异较大，部分机型可能不支持。可以参考以下调试步骤：

1. 检查设备品牌和型号
2. 查看 Logcat 中的错误信息
3. 确认是否需要在厂商推送后台配置角标权限

## 技术实现

### iOS

使用 UIKit 的 `UIApplication.shared.applicationIconBadgeNumber` 属性。

### Android

根据设备厂商使用不同的实现：

- **华为/荣耀**：ContentProvider (`com.huawei.android.launcher.settings/badge/`)
- **小米**：NotificationManager 反射调用 `setAppBadgeCount`
- **OPPO**：广播 Intent (`com.oppo.unsettledevent`)
- **VIVO**：广播 Intent (`launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM`)
- **三星**：广播 Intent (`android.intent.action.BADGE_COUNT_UPDATE`)

所有实现都封装在 `BadgeHelper` 工具类中，确保代码复用和维护性。

## 更新日志

### v1.1.0 (2025-11-22)

- ✨ 新增应用角标管理功能
- ✨ 支持 iOS、华为、荣耀、小米、OPPO、VIVO、三星
- ✨ 提供统一的跨平台 API
- ✨ 更新示例应用，添加角标管理演示

## 支持

如有问题或建议，请提交 Issue 或 Pull Request。

---

**最后更新：** 2025年11月22日
**插件版本：** 1.1.0+
