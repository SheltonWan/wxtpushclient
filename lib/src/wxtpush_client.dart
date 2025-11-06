import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

import 'models/push_config.dart';
import 'models/push_token.dart';
import 'models/push_message.dart';
import 'models/push_vendor.dart';
import 'handlers/push_message_handler.dart';
import 'utils/device_utils.dart';

/// WXTPush客户端SDK主类
class WxtpushClient {
  static const MethodChannel _channel = MethodChannel('wxtpush_client');
  static const EventChannel _eventChannel = EventChannel('wxtpush_client/events');

  static WxtpushClient? _instance;
  static WxtpushClient get instance {
    _instance ??= WxtpushClient._();
    return _instance!;
  }

  WxtpushClient._();

  PushConfig? _config;
  PushMessageHandler? _messageHandler;
  StreamSubscription? _eventSubscription;

  /// 初始化推送服务
  Future<void> initialize(PushConfig config, {PushMessageHandler? messageHandler}) async {
    _config = config;
    _messageHandler = messageHandler ?? DefaultPushMessageHandler();

    // 输出设备信息
    // final deviceInfo = await DeviceUtils.getDeviceInfo();
 

    // 先取消之前的订阅（如果存在）
    await _eventSubscription?.cancel();
    _eventSubscription = null;

    // 重新监听推送事件
    _listenToPushEvents();

    // 请求通知权限
    await _requestNotificationPermission();

    // 延迟一下确保事件监听器已设置
    await Future.delayed(const Duration(milliseconds: 500));

    // 初始化原生推送服务
    await _initializeNativePush();
  }

  /// 主动请求通知权限
  ///
  /// 返回权限是否被授予，同时会触发[PushMessageHandler.onPermissionChanged]回调
  Future<bool> requestPermission() async {
    try {
      final granted = await _requestNotificationPermission();

      // 触发权限变更回调
      _messageHandler?.onPermissionChanged(granted, null);

      return granted;
    } catch (e) {
      _messageHandler?.onError('请求通知权限失败: $e', null);
      return false;
    }
  }

  /// 检查当前通知权限状态
  ///
  /// 返回权限是否已被授予
  Future<bool> isPermissionGranted() async {
    try {
      final status = await Permission.notification.status;
      return status == PermissionStatus.granted;
    } catch (e) {
      _messageHandler?.onError('检查通知权限状态失败: $e', null);
      return false;
    }
  }

  /// 请求通知权限（内部方法）
  Future<bool> _requestNotificationPermission() async {
    final status = await Permission.notification.request();
    return status == PermissionStatus.granted;
  }
  /// 初始化原生推送服务
  Future<void> _initializeNativePush() async {
    try {
      final supportedVendors = await DeviceUtils.getSupportedVendors();

      debugPrint('🔍 设备支持的推送厂商: ${supportedVendors.map((v) => v.displayName).join(", ")}');

      if (supportedVendors.isEmpty) {
        debugPrint('⚠️ 未检测到匹配的设备厂商，推送服务将无法初始化');
        _messageHandler?.onError('未检测到支持的推送厂商', null);
        return;
      }

      for (final vendor in supportedVendors) {
        final vendorConfig = _config!.getVendorConfig(vendor);
        if (vendorConfig != null) {
          debugPrint('🚀 初始化 ${vendor.displayName} 推送服务...');
          try {
            await _channel.invokeMethod('initializePush', {
              'vendor': vendor.id,
              'config': _configToMap(vendor, vendorConfig),
            });
            debugPrint('✅ ${vendor.displayName} 推送服务初始化完成');
          } catch (e) {
            debugPrint('❌ ${vendor.displayName} 推送服务初始化失败: $e');
          }
        } else {
          debugPrint('⏭️ ${vendor.displayName} 未配置，跳过初始化');
        }
      }
    } on PlatformException catch (e) {
      _messageHandler?.onError('初始化推送服务失败: ${e.message}', null);
    }
  }

  /// 配置转换为Map
  Map<String, dynamic> _configToMap(PushVendor vendor, dynamic config) {
    switch (vendor) {
      case PushVendor.huawei:
        final huaweiConfig = config as HuaweiConfig;
        return {
          'appId': huaweiConfig.appId,
          'appSecret': huaweiConfig.appSecret,
        };
      case PushVendor.xiaomi:
        final xiaomiConfig = config as XiaomiConfig;
        return {
          'appId': xiaomiConfig.appId,
          'appKey': xiaomiConfig.appKey,
          'appSecret': xiaomiConfig.appSecret,
        };
      case PushVendor.oppo:
        final oppoConfig = config as OppoConfig;
        final configMap = <String, dynamic>{
          'appId': oppoConfig.appId,
          'appKey': oppoConfig.appKey,
          'appSecret': oppoConfig.appSecret,
        };
        // 添加可选的超时配置
        if (oppoConfig.registerTimeoutMs != null) {
          configMap['registerTimeoutMs'] = oppoConfig.registerTimeoutMs; // dynamic 透传
        }
        // demo 兼容模式标记
        configMap['demoCompat'] = oppoConfig.demoCompat;
        return configMap;
      case PushVendor.vivo:
        final vivoConfig = config as VivoConfig;
        return {
          'appId': vivoConfig.appId,
          'appKey': vivoConfig.appKey,
          'appSecret': vivoConfig.appSecret,
        };
      case PushVendor.honor:
        final honorConfig = config as HonorConfig;
        return {
          'appId': honorConfig.appId,
          'appSecret': honorConfig.appSecret,
        };
      case PushVendor.apple:
        final appleConfig = config as AppleConfig;
        return {
          'bundleId': appleConfig.bundleId,
          'useSandbox': appleConfig.useSandbox,
        };
    }
  }

  /// 监听推送事件
  void _listenToPushEvents() {
    _eventSubscription = _eventChannel.receiveBroadcastStream().listen(
      (event) {
        debugPrint('📡 收到推送事件: $event');
        if (event is Map) {
          final eventMap = Map<String, dynamic>.from(event);
          _handlePushEvent(eventMap);
        }
      },
      onError: (error) {
        debugPrint('❌ 推送事件监听错误: $error');
        _messageHandler?.onError('推送事件监听错误: $error', null);
      },
    );
  }

  /// 处理推送事件
  void _handlePushEvent(Map<String, dynamic> event) {
    try {
      // Android端发送的事件格式是 {event: "tokenReceived", data: {...}}
      // 我们需要将 event 字段映射为 type 字段
      final eventType = event['event'] as String? ?? event['type'] as String?;
      final eventData = event['data'];

      debugPrint('🔄 处理推送事件: $eventType, 数据: $eventData, 数据类型: ${eventData?.runtimeType}');

      if (eventType == null) {
        debugPrint('⚠️ 事件类型为空，忽略事件: $event');
        return;
      }

      // 安全的类型转换 - 使用与getTokens相同的转换逻辑
      Map<String, dynamic>? safeEventData;
      if (eventData != null) {
        try {
          if (eventData is Map<String, dynamic>) {
            safeEventData = eventData;
          } else if (eventData is Map) {
            // 兼容 Map<Object, Object> 类型
            final map = Map<dynamic, dynamic>.from(eventData);
            final stringMap = <String, dynamic>{};

            // 安全地转换每个字段
            for (final entry in map.entries) {
              final key = entry.key.toString();
              final value = entry.value;

              // 特殊处理时间戳字段
              if (key == 'timestamp' && value is double) {
                // 将timestamp转换为createdAt字段
                stringMap['createdAt'] =
                    DateTime.fromMillisecondsSinceEpoch((value * 1000).round())
                        .toIso8601String();
              } else if (key == 'createdAt' && value is double) {
                // 处理createdAt为时间戳的情况
                stringMap['createdAt'] =
                    DateTime.fromMillisecondsSinceEpoch((value * 1000).round())
                        .toIso8601String();
              } else if (key == 'createdAt' && value is String) {
                // 已经是字符串格式的时间
                stringMap[key] = value;
              } else if (key == 'timestamp') {
                // 跳过timestamp字段，因为已经转换为createdAt
                continue;
              } else {
                // 其他字段直接赋值
                stringMap[key] = value;
              }
            }

            safeEventData = stringMap;
          } else {
            debugPrint('⚠️ 无法转换事件数据类型: ${eventData.runtimeType}, 原始数据: $eventData');
          }
        } catch (e) {
          debugPrint('❌ 事件数据类型转换失败: $e, 原始数据: $eventData');
        }
      }

      switch (eventType) {
        case 'messageReceived':
          if (safeEventData != null) {
            try {
              final message = PushMessage.fromJson(safeEventData);
              _messageHandler?.onMessageReceived(message);
            } catch (e) {
              debugPrint('❌ 解析消息接收事件失败: $e, 数据: $safeEventData');
              _messageHandler?.onError('解析消息接收事件失败: $e', null);
            }
          }
          break;
        case 'messageClicked':
          if (safeEventData != null) {
            try {
              final message = PushMessage.fromJson(safeEventData);
              _messageHandler?.onMessageClicked(message);
            } catch (e) {
              debugPrint('❌ 解析消息点击事件失败: $e, 数据: $safeEventData');
              _messageHandler?.onError('解析消息点击事件失败: $e', null);
            }
          }
          break;
        case 'tokenReceived':  // 修复：Android/iOS 发送的是 tokenReceived
        case 'tokenUpdated':   // 保留兼容
          if (safeEventData != null) {
            try {
              final token = safeEventData['token']?.toString();
              final vendor = safeEventData['vendor']?.toString();

              if (token != null && vendor != null) {
                debugPrint('📱 Token事件: $vendor - ${token.substring(0, 20)}...');
                _messageHandler?.onTokenUpdated(token, vendor);
              } else {
                debugPrint('⚠️ Token事件缺少必要字段: token=$token, vendor=$vendor');
              }
            } catch (e) {
              debugPrint('❌ 处理Token事件失败: $e, 数据: $safeEventData');
              _messageHandler?.onError('处理Token事件失败: $e', null);
            }
          }
          break;
        case 'permissionGranted':
          if (safeEventData != null) {
            try {
              final granted = _safeCastToBool(safeEventData['granted']) ?? true;
              final vendor = safeEventData['vendor']?.toString();
              _messageHandler?.onPermissionChanged(granted, vendor);
            } catch (e) {
              debugPrint('❌ 处理权限授予事件失败: $e, 数据: $safeEventData');
              _messageHandler?.onError('处理权限授予事件失败: $e', null);
            }
          }
          break;
        case 'permissionDenied':
          if (safeEventData != null) {
            try {
              final granted =
                  _safeCastToBool(safeEventData['granted']) ?? false;
              final vendor = safeEventData['vendor']?.toString();
              _messageHandler?.onPermissionChanged(granted, vendor);
            } catch (e) {
              debugPrint('❌ 处理权限拒绝事件失败: $e, 数据: $safeEventData');
              _messageHandler?.onError('处理权限拒绝事件失败: $e', null);
            }
          }
          break;
        case 'tokenError':
          if (safeEventData != null) {
            try {
              final error = safeEventData['error']?.toString() ?? '未知Token错误';
              final vendor = safeEventData['vendor']?.toString();
              _messageHandler?.onError('Token获取失败: $error', vendor);
            } catch (e) {
              debugPrint('❌ 处理Token错误事件失败: $e, 数据: $safeEventData');
              _messageHandler?.onError('处理Token错误事件失败: $e', null);
            }
          }
          break;
        case 'error':
          if (safeEventData != null) {
            try {
              final error = safeEventData['error']?.toString() ?? '未知错误';
              final vendor = safeEventData['vendor']?.toString();
              _messageHandler?.onError(error, vendor);
            } catch (e) {
              debugPrint('❌ 处理错误事件失败: $e, 数据: $safeEventData');
              _messageHandler?.onError('处理错误事件失败: $e', null);
            }
          }
          break;
      }
    } catch (e) {
      debugPrint('❌ 处理推送事件失败: $e');
      _messageHandler?.onError('处理推送事件失败: $e', null);
    }
  }

  /// 安全地将值转换为bool类型
  bool? _safeCastToBool(dynamic value) {
    if (value == null) return null;
    if (value is bool) return value;
    if (value is String) {
      final lowercaseValue = value.toLowerCase();
      if (lowercaseValue == 'true' || lowercaseValue == '1') return true;
      if (lowercaseValue == 'false' || lowercaseValue == '0') return false;
    }
    if (value is int) {
      return value != 0;
    }
    if (value is double) {
      return value != 0.0;
    }
    return null;
  }

  /// 获取所有可用的推送Token
  Future<List<PushToken>> getTokens() async {
    try {
      final result = await _channel.invokeMethod('getAllTokens');
      if (result is List) {
        return result
            .whereType<Map>()
            .map((tokenData) {
              try {
                // Android端已经返回正确的数据结构，直接转换
                final stringMap = Map<String, dynamic>.from(tokenData);
                return PushToken.fromJson(stringMap);
              } catch (e) {
                debugPrint('⚠️ Token数据解析失败: $e, 原始数据: $tokenData');
                return null;
              }
            })
            .whereType<PushToken>()
            .toList();
      } else {
        debugPrint('⚠️ getAllTokens返回类型错误: ${result.runtimeType}');
        return [];
      }
    } on PlatformException catch (e) {
      _messageHandler?.onError('获取推送Token失败: ${e.message}', null);
      return [];
    }
  }
  /// 获取指定厂商的推送Token
  Future<PushToken?> getToken(PushVendor vendor) async {
    try {
      final result = await _channel.invokeMethod('getToken', {
        'vendor': vendor.id,
      });

      if (result != null && result is Map) {
        try {
          // Android端已经返回正确的数据结构，直接转换
          final stringMap = Map<String, dynamic>.from(result);
          return PushToken.fromJson(stringMap);
        } catch (e) {
          debugPrint('⚠️ Token数据解析失败: $e, 原始数据: $result');
          return null;
        }
      }
      return null;
    } on PlatformException catch (e) {
      _messageHandler?.onError('获取${vendor.displayName}推送Token失败: ${e.message}', vendor.id);
      return null;
    }
  }

  /// 订阅主题
  Future<bool> subscribeToTopic(String topic, {PushVendor? vendor}) async {
    try {
      final result = await _channel.invokeMethod('subscribeToTopic', {
        'topic': topic,
        'vendor': vendor?.id,
      });
      return result as bool;
    } on PlatformException catch (e) {
      _messageHandler?.onError('订阅主题失败: ${e.message}', vendor?.id);
      return false;
    }
  }

  /// 取消订阅主题
  Future<bool> unsubscribeFromTopic(String topic, {PushVendor? vendor}) async {
    try {
      final result = await _channel.invokeMethod('unsubscribeFromTopic', {
        'topic': topic,
        'vendor': vendor?.id,
      });
      return result as bool;
    } on PlatformException catch (e) {
      _messageHandler?.onError('取消订阅主题失败: ${e.message}', vendor?.id);
      return false;
    }
  }

  /// 设置别名（用户标识）
  Future<bool> setAlias(String alias, {PushVendor? vendor}) async {
    try {
      final result = await _channel.invokeMethod('setAlias', {
        'alias': alias,
        'vendor': vendor?.id,
      });
      return result as bool;
    } on PlatformException catch (e) {
      _messageHandler?.onError('设置别名失败: ${e.message}', vendor?.id);
      return false;
    }
  }

  /// 设置标签
  Future<bool> setTags(List<String> tags, {PushVendor? vendor}) async {
    try {
      final result = await _channel.invokeMethod('setTags', {
        'tags': tags,
        'vendor': vendor?.id,
      });
      return result as bool;
    } on PlatformException catch (e) {
      _messageHandler?.onError('设置标签失败: ${e.message}', vendor?.id);
      return false;
    }
  }

  /// 启用推送服务
  Future<bool> enablePush({PushVendor? vendor}) async {
    try {
      final result = await _channel.invokeMethod('enablePush', {
        'vendor': vendor?.id,
      });
      return result as bool;
    } on PlatformException catch (e) {
      _messageHandler?.onError('启用推送服务失败: ${e.message}', vendor?.id);
      return false;
    }
  }

  /// 禁用推送服务
  Future<bool> disablePush({PushVendor? vendor}) async {
    try {
      final result = await _channel.invokeMethod('disablePush', {
        'vendor': vendor?.id,
      });
      return result as bool;
    } on PlatformException catch (e) {
      _messageHandler?.onError('禁用推送服务失败: ${e.message}', vendor?.id);
      return false;
    }
  }

  /// 获取推送服务状态
  Future<bool> isPushEnabled({PushVendor? vendor}) async {
    try {
      final result = await _channel.invokeMethod('isPushEnabled', {
        'vendor': vendor?.id,
      });
      return result as bool;
    } on PlatformException catch (e) {
      _messageHandler?.onError('获取推送服务状态失败: ${e.message}', vendor?.id);
      return false;
    }
  }

  /// 刷新指定厂商的推送Token
  Future<bool> refreshToken(PushVendor vendor) async {
    try {
      await _channel.invokeMethod('refreshToken', {
        'vendor': vendor.id,
      });
      debugPrint('🔄 刷新${vendor.displayName}推送Token成功');
      return true;
    } on PlatformException catch (e) {
      _messageHandler?.onError(
          '刷新${vendor.displayName}推送Token失败: ${e.message}', vendor.id);
      return false;
    }
  }

  /// 删除指定厂商的推送Token
  Future<bool> deleteToken(PushVendor vendor) async {
    try {
      await _channel.invokeMethod('deleteToken', {
        'vendor': vendor.id,
      });
      debugPrint('🗑️ 删除${vendor.displayName}推送Token成功');
      return true;
    } on PlatformException catch (e) {
      _messageHandler?.onError(
          '删除${vendor.displayName}推送Token失败: ${e.message}', vendor.id);
      return false;
    }
  }

  /// 销毁资源
  Future<void> dispose() async {
    await _eventSubscription?.cancel();
    _eventSubscription = null;

    try {
      await _channel.invokeMethod('dispose');
    } on PlatformException catch (e) {
      _messageHandler?.onError('销毁推送服务失败: ${e.message}', null);
    }
  }
}
