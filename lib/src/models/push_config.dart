import 'package:flutter/foundation.dart';

import 'push_vendor.dart';
import 'dart:io' show Platform;
import 'package:flutter/services.dart';

/// 推送服务配置
class PushConfig {
  final HuaweiConfig? huawei;
  final XiaomiConfig? xiaomi;
  final OppoConfig? oppo;
  final VivoConfig? vivo;
  final HonorConfig? honor;
  final AppleConfig? apple;
  final bool debugMode;

  const PushConfig({
    this.huawei,
    this.xiaomi,
    this.oppo,
    this.vivo,
    this.honor,
    this.apple,
    this.debugMode = false,
  });

  /// 从Android Manifest自动读取配置（简化集成）
  ///
  /// Android端会自动从 manifestPlaceholders 中读取配置
  /// iOS端需要手动传入配置参数
  static Future<PushConfig> fromManifest({
    // iOS端必需参数
    AppleConfig? apple,
    // 可选的调试模式
    bool debugMode = false,
    // 可选的手动覆盖配置
    HuaweiConfig? huaweiOverride,
    XiaomiConfig? xiaomiOverride,
    OppoConfig? oppoOverride,
    VivoConfig? vivoOverride,
    HonorConfig? honorOverride,
  }) async {
    if (Platform.isAndroid) {
      // Android端从manifest读取配置
      try {
        const channel = MethodChannel('wxtpush_client');
        final manifestConfig = await channel.invokeMethod<Map>('getManifestConfig');

        if (manifestConfig != null) {
          final config = Map<String, dynamic>.from(manifestConfig);
          debugPrint('从Manifest读取到配置: $config');
          return PushConfig(
            huawei: huaweiOverride ?? _createHuaweiConfig(config),
            xiaomi: xiaomiOverride ?? _createXiaomiConfig(config),
            oppo: oppoOverride ?? _createOppoConfig(config),
            vivo: vivoOverride ?? _createVivoConfig(config),
            honor: honorOverride ?? _createHonorConfig(config),
            apple: apple, // Android端不使用
            debugMode: debugMode,
          );
        }
      } catch (e) {
        if (debugMode) {
          debugPrint('从Manifest读取配置失败，使用手动覆盖配置: $e');
        }
      }

      // 如果读取失败，使用手动覆盖配置
      return PushConfig(
        huawei: huaweiOverride,
        xiaomi: xiaomiOverride,
        oppo: oppoOverride,
        vivo: vivoOverride,
        honor: honorOverride,
        apple: apple,
        debugMode: debugMode,
      );
    } else {
      // iOS端使用传入的配置
      return PushConfig(
        apple: apple,
        debugMode: debugMode,
        // iOS端不支持其他厂商
      );
    }
  }

  static HuaweiConfig? _createHuaweiConfig(Map<String, dynamic> config) {
    final appId = config['huawei_app_id'] as String?;
    final appSecret = config['huawei_app_secret'] as String?;

    if (appId != null && appId.isNotEmpty) {
      return HuaweiConfig(
        appId: appId,
        appSecret: appSecret ?? '', // appSecret可选
      );
    }
    return null;
  }

  static XiaomiConfig? _createXiaomiConfig(Map<String, dynamic> config) {
    final appId = config['xiaomi_app_id'] as String?;
    final appKey = config['xiaomi_app_key'] as String?;
    final appSecret = config['xiaomi_app_secret'] as String?;

    if (appId != null && appId.isNotEmpty && appKey != null && appKey.isNotEmpty) {
      return XiaomiConfig(
        appId: appId,
        appKey: appKey,
        appSecret: appSecret ?? '', // 小米可能不需要secret
      );
    }
    return null;
  }

  static OppoConfig? _createOppoConfig(Map<String, dynamic> config) {
    final appKey = config['oppo_app_key'] as String?;
    final appSecret = config['oppo_app_secret'] as String?;

    if (appKey != null && appKey.isNotEmpty && appSecret != null && appSecret.isNotEmpty) {
      return OppoConfig(
        appId: appKey, // OPPO使用AppKey作为AppId
        appKey: appKey,
        appSecret: appSecret,
      );
    }
    return null;
  }

  static VivoConfig? _createVivoConfig(Map<String, dynamic> config) {
    final appId = config['vivo_app_id'] as String?;
    final appKey = config['vivo_app_key'] as String?;

    if (appId != null && appId.isNotEmpty && appKey != null && appKey.isNotEmpty) {
      return VivoConfig(
        appId: appId,
        appKey: appKey,
        appSecret: '', // VIVO通常不需要secret
      );
    }
    return null;
  }

  static HonorConfig? _createHonorConfig(Map<String, dynamic> config) {
    final appId = config['honor_app_id'] as String?;
    final appSecret = config['honor_app_secret'] as String?;

    if (appId != null && appId.isNotEmpty) {
      return HonorConfig(
        appId: appId,
        appSecret: appSecret ?? '', // appSecret可选
      );
    }
    return null;
  }

  dynamic getVendorConfig(PushVendor vendor) {
    switch (vendor) {
      case PushVendor.huawei:
        return huawei;
      case PushVendor.xiaomi:
        return xiaomi;
      case PushVendor.oppo:
        return oppo;
      case PushVendor.vivo:
        return vivo;
      case PushVendor.honor:
        return honor;
      case PushVendor.apple:
        return apple;
    }
  }
}

/// 华为HMS推送配置
class HuaweiConfig {
  final String appId;
  final String appSecret; // appSecret对于Token获取来说是可选的

  const HuaweiConfig({
    required this.appId,
    this.appSecret = '', // 默认为空字符串
  });
}

/// 小米推送配置
class XiaomiConfig {
  final String appId;
  final String appKey;
  final String appSecret;

  const XiaomiConfig({
    required this.appId,
    required this.appKey,
    required this.appSecret,
  });
}

/// OPPO推送配置（增加 demoCompat 与 registerTimeoutMs）
class OppoConfig {
  final String appId;
  final String appKey;
  final String appSecret;
  final int? registerTimeoutMs; // 自定义注册超时
  final bool demoCompat; // 是否启用官方 demo 极简模式

  const OppoConfig({
    required this.appId,
    required this.appKey,
    required this.appSecret,
    this.registerTimeoutMs,
    this.demoCompat = false,
  });

  Map<String, dynamic> toMap() => {
        'appId': appId,
        'appKey': appKey,
        'appSecret': appSecret,
        if (registerTimeoutMs != null) 'registerTimeoutMs': registerTimeoutMs,
        'demoCompat': demoCompat,
      };
}

/// VIVO推送配置
class VivoConfig {
  final String appId;
  final String appKey;
  final String appSecret;

  const VivoConfig({
    required this.appId,
    required this.appKey,
    required this.appSecret,
  });
}

/// 荣耀推送配置
class HonorConfig {
  final String appId;
  final String appSecret; // appSecret对于Token获取来说是可选的

  const HonorConfig({
    required this.appId,
    this.appSecret = '', // 默认为空字符串
  });
}

/// 苹果 APNs 推送配置
class AppleConfig {
  final String bundleId;
  final bool useSandbox;

  const AppleConfig({
    required this.bundleId,
    this.useSandbox = false,
  });
}
