import 'package:flutter/foundation.dart';

import '../models/push_message.dart';

/// 推送消息处理器
abstract class PushMessageHandler {
  /// 收到推送消息时调用
  Future<void> onMessageReceived(PushMessage message);

  /// 点击推送消息时调用
  Future<void> onMessageClicked(PushMessage message);

  /// Token更新时调用
  Future<void> onTokenUpdated(String token, String vendor);

  /// 权限状态变更时调用
  Future<void> onPermissionChanged(bool granted, String? vendor);

  /// 推送服务出错时调用
  Future<void> onError(String error, String? vendor);
}

/// 默认的推送消息处理器实现
class DefaultPushMessageHandler implements PushMessageHandler {
  @override
  Future<void> onMessageReceived(PushMessage message) async {
    debugPrint('收到推送消息: ${message.title} - ${message.body}');
  }

  @override
  Future<void> onMessageClicked(PushMessage message) async {
    debugPrint('点击推送消息: ${message.title} - ${message.body}');
  }

  @override
  Future<void> onTokenUpdated(String token, String vendor) async {
    debugPrint('Token更新: $vendor - ${token.substring(0, 8)}...');
  }

  @override
  Future<void> onPermissionChanged(bool granted, String? vendor) async {
    final vendorText = vendor != null ? ' ($vendor)' : '';
    if (granted) {
      debugPrint('✅ 通知权限已授予$vendorText');
    } else {
      debugPrint('❌ 通知权限被拒绝$vendorText');
    }
  }

  @override
  Future<void> onError(String error, String? vendor) async {
    debugPrint('推送服务错误: ${vendor ?? 'Unknown'} - $error');
  }
}
