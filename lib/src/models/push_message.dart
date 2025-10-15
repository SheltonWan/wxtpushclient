/// 接收到的推送消息
class PushMessage {
  /// 消息标题
  final String? title;
  
  /// 消息内容
  final String? body;
  
  /// 自定义数据
  final Map<String, dynamic>? data;
  
  /// 消息ID
  final String? messageId;
  
  /// 点击动作
  final String? clickAction;
  
  /// 图标
  final String? icon;
  
  /// 声音
  final String? sound;
  
  /// 角标
  final int? badge;
  
  /// 接收时间
  final DateTime receivedAt;
  
  /// 是否为点击消息
  final bool isClicked;

  const PushMessage({
    this.title,
    this.body,
    this.data,
    this.messageId,
    this.clickAction,
    this.icon,
    this.sound,
    this.badge,
    required this.receivedAt,
    this.isClicked = false,
  });

  /// 从JSON创建对象
  factory PushMessage.fromJson(Map<String, dynamic> json) {
    return PushMessage(
      title: _safeStringCast(json['title']),
      body: _safeStringCast(json['body']),
      data: _safeMapCast(json['data']),
      messageId: _safeStringCast(json['messageId']),
      clickAction: _safeStringCast(json['clickAction']),
      icon: _safeStringCast(json['icon']),
      sound: _safeStringCast(json['sound']),
      badge: _safeIntCast(json['badge']),
      receivedAt: _safeDateTimeParse(json['receivedAt']) ?? DateTime.now(),
      isClicked: _safeBoolCast(json['isClicked']) ?? false,
    );
  }

  /// 安全地转换为字符串
  static String? _safeStringCast(dynamic value) {
    if (value == null) return null;
    if (value is String) return value.isEmpty ? null : value;
    return value.toString();
  }

  /// 安全地转换为Map
  static Map<String, dynamic>? _safeMapCast(dynamic value) {
    if (value == null) return null;
    if (value is Map<String, dynamic>) return value;
    if (value is Map) {
      try {
        return Map<String, dynamic>.from(value);
      } catch (e) {
        print('⚠️ 无法转换Map类型: $e, 原始数据: $value');
        return null;
      }
    }
    return null;
  }

  /// 安全地转换为整数
  static int? _safeIntCast(dynamic value) {
    if (value == null) return null;
    if (value is int) return value;
    if (value is double) return value.round();
    if (value is String) {
      try {
        return int.parse(value);
      } catch (e) {
        try {
          return double.parse(value).round();
        } catch (e) {
          print('⚠️ 无法解析整数: $value');
          return null;
        }
      }
    }
    return null;
  }

  /// 安全地转换为布尔值
  static bool? _safeBoolCast(dynamic value) {
    if (value == null) return null;
    if (value is bool) return value;
    if (value is String) {
      final lowercaseValue = value.toLowerCase();
      if (lowercaseValue == 'true' || lowercaseValue == '1') return true;
      if (lowercaseValue == 'false' || lowercaseValue == '0') return false;
    }
    if (value is int) return value != 0;
    if (value is double) return value != 0.0;
    return null;
  }

  /// 安全地解析DateTime
  static DateTime? _safeDateTimeParse(dynamic value) {
    if (value == null) return null;
    
    try {
      // 如果是字符串，尝试解析
      if (value is String) {
        if (value.isEmpty) return null;
        return DateTime.parse(value);
      }
      
      // 如果是时间戳（毫秒）
      if (value is int) {
        return DateTime.fromMillisecondsSinceEpoch(value);
      }
      
      // 如果是时间戳（秒，转换为毫秒）
      if (value is double) {
        return DateTime.fromMillisecondsSinceEpoch((value * 1000).round());
      }
      
      return null;
    } catch (e) {
      print('⚠️ 无法解析时间: $value, 错误: $e');
      return null;
    }
  }

  /// 转换为JSON
  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'body': body,
      'data': data,
      'messageId': messageId,
      'clickAction': clickAction,
      'icon': icon,
      'sound': sound,
      'badge': badge,
      'receivedAt': receivedAt.toIso8601String(),
      'isClicked': isClicked,
    };
  }

  /// 创建点击消息副本
  PushMessage asClicked() {
    return PushMessage(
      title: title,
      body: body,
      data: data,
      messageId: messageId,
      clickAction: clickAction,
      icon: icon,
      sound: sound,
      badge: badge,
      receivedAt: receivedAt,
      isClicked: true,
    );
  }

  @override
  String toString() {
    return 'PushMessage(title: $title, body: $body, messageId: $messageId, isClicked: $isClicked)';
  }
}
