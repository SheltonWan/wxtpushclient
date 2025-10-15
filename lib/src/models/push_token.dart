import 'push_vendor.dart';

/// 设备推送Token信息
class PushToken {
  /// 推送厂商
  final PushVendor vendor;
  
  /// 设备Token
  final String token;
  
  /// 设备ID
  final String? deviceId;
  
  /// 应用包名
  final String? packageName;
  
  /// Token创建时间
  final DateTime createdAt;
  
  /// Token是否有效
  final bool isValid;

  const PushToken({
    required this.vendor,
    required this.token,
    this.deviceId,
    this.packageName,
    required this.createdAt,
    this.isValid = true,
  });

  /// 从JSON创建对象
  factory PushToken.fromJson(Map<String, dynamic> json) {
    // 安全地解析vendor
    PushVendor vendor;
    try {
      final vendorId = json['vendor']?.toString();
      if (vendorId != null) {
        vendor = PushVendor.fromId(vendorId) ?? PushVendor.apple;
      } else {
        vendor = PushVendor.apple;
      }
    } catch (e) {
      vendor = PushVendor.apple; // 默认值
    }
    
    // 安全地解析token
    String token;
    try {
      token = json['token']?.toString() ?? '';
    } catch (e) {
      token = '';
    }
    
    // 安全地解析时间
    DateTime createdAt;
    try {
      final createdAtValue = json['createdAt'];
      if (createdAtValue is String) {
        createdAt = DateTime.parse(createdAtValue);
      } else if (createdAtValue is double) {
        // 时间戳格式
        createdAt = DateTime.fromMillisecondsSinceEpoch((createdAtValue * 1000).round());
      } else if (createdAtValue is int) {
        // 毫秒时间戳
        createdAt = DateTime.fromMillisecondsSinceEpoch(createdAtValue);
      } else {
        createdAt = DateTime.now();
      }
    } catch (e) {
      createdAt = DateTime.now();
    }
    
    return PushToken(
      vendor: vendor,
      token: token,
      deviceId: json['deviceId']?.toString(),
      packageName: json['packageName']?.toString(),
      createdAt: createdAt,
      isValid: json['isValid'] as bool? ?? true,
    );
  }

  /// 转换为JSON
  Map<String, dynamic> toJson() {
    return {
      'vendor': vendor.id,
      'token': token,
      'deviceId': deviceId,
      'packageName': packageName,
      'createdAt': createdAt.toIso8601String(),
      'isValid': isValid,
    };
  }

  @override
  String toString() {
    return 'PushToken(vendor: $vendor, token: ${token.substring(0, 8)}..., isValid: $isValid)';
  }
}
