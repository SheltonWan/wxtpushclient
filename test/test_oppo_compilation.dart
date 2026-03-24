import 'package:flutter/material.dart';
import 'package:wxtpush_client/wxtpush_client.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  MyAppState createState() => MyAppState();
}

class MyAppState extends State<MyApp> {
  String _token = '未获取';
  String _status = '初始化中...';
  WxtpushClient? _client;

  @override
  void initState() {
    super.initState();
    _initPush();
  }

  Future<void> _initPush() async {
    try {
      _client = WxtpushClient.instance;

      // 配置OPPO推送
      const config = PushConfig(
        oppo: OppoConfig(
          appId: 'test_app_id',
          appKey: 'test_app_key',
          appSecret: 'test_app_secret',
        ),
      );

      // 自定义消息处理器
      final messageHandler = CustomPushMessageHandler(
        onTokenReceived: (token, vendor) {
          setState(() {
            _token = token;
            _status = 'Token获取成功 ($vendor)';
          });
        },
        onTokenError: (error, vendor) {
          setState(() {
            _status = '错误: $error ($vendor)';
          });
        },
      );

      // 初始化推送服务
      await _client!.initialize(config, messageHandler: messageHandler);

      // 获取Token
      final tokens = await _client!.getTokens();
      if (tokens.isNotEmpty) {
        final oppoToken = tokens.where((t) => t.vendor == PushVendor.oppo).firstOrNull;
        if (oppoToken != null) {
          setState(() {
            _token = oppoToken.token;
            _status = 'Token已获取 (${oppoToken.vendor.name})';
          });
        }
      }
    } catch (e) {
      setState(() {
        _status = '初始化失败: $e';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('OPPO推送测试'),
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('状态: $_status'),
              const SizedBox(height: 20),
              Text('Token: $_token'),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: _initPush,
                child: const Text('重新初始化'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _client?.dispose();
    super.dispose();
  }
}

class CustomPushMessageHandler extends DefaultPushMessageHandler {
  final Function(String, String)? onTokenReceived;
  final Function(String, String?)? onTokenError;

  CustomPushMessageHandler({
    this.onTokenReceived,
    this.onTokenError,
  });

  @override
  Future<void> onMessageReceived(PushMessage message) async {
    debugPrint('收到推送消息: ${message.title} - ${message.body}');
  }

  @override
  Future<void> onTokenUpdated(String token, String vendor) async {
    debugPrint('收到Token: $vendor - $token');
    onTokenReceived?.call(token, vendor);
  }

  @override
  Future<void> onError(String error, String? vendor) async {
    debugPrint('推送错误: $error');
    onTokenError?.call(error, vendor);
  }
}
