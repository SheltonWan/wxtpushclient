// 编译测试文件 - 用于检查SDK的基本编译问题
import 'package:wxtpush_client/wxtpush_client.dart';

void main() {
  print('开始编译测试...');
  
  // 测试单例访问
  final client = WxtpushClient.instance;
  print('客户端实例: $client');
  
  // 测试枚举
  final vendor = PushVendor.huawei;
  print('推送厂商: ${vendor.displayName}');
  
  // 测试配置类
  const config = PushConfig(
    huawei: HuaweiConfig(appId: 'test', appSecret: 'secret'),
    debugMode: true,
  );
  print('配置创建完成: ${config.debugMode}');
  
  // 测试消息处理器
  final handler = DefaultPushMessageHandler();
  print('消息处理器: $handler');
  
  print('编译测试完成！');
}
