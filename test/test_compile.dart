// 编译测试文件 - 用于检查SDK的基本编译问题
import 'package:flutter/foundation.dart';
import 'package:wxtpush_client/wxtpush_client.dart';

void main() {
  debugPrint('开始编译测试...');

  // 测试单例访问
  final client = WxtpushClient.instance;
  debugPrint('客户端实例: $client');

  // 测试枚举
  const vendor = PushVendor.huawei;
  debugPrint('推送厂商: ${vendor.displayName}');

  // 测试配置类
  const config = PushConfig(
    huawei: HuaweiConfig(appId: 'test', appSecret: 'secret'),
    debugMode: true,
  );
  debugPrint('配置创建完成: ${config.debugMode}');

  // 测试消息处理器
  final handler = DefaultPushMessageHandler();
  debugPrint('消息处理器: $handler');

  debugPrint('编译测试完成！');
}
