import 'package:flutter/material.dart';
import 'package:wxtpush_client/wxtpush_client.dart';

void main() {
  runApp(const MyApp());
}

/// 自定义推送消息处理器，用于诊断
class DiagnosticPushMessageHandler implements PushMessageHandler {
  final Function(String, String) onTokenReceivedCallback;
  final Function(String, String) onTokenErrorCallback;
  final Function(PushMessage, String) onMessageReceivedCallback;
  final Function(PushMessage, String) onNotificationClickedCallback;

  DiagnosticPushMessageHandler({
    required this.onTokenReceivedCallback,
    required this.onTokenErrorCallback,
    required this.onMessageReceivedCallback,
    required this.onNotificationClickedCallback,
  });

  @override
  Future<void> onMessageReceived(PushMessage message) async {
    onMessageReceivedCallback(message, 'unknown');
  }

  @override
  Future<void> onMessageClicked(PushMessage message) async {
    onNotificationClickedCallback(message, 'unknown');
  }

  @override
  Future<void> onTokenUpdated(String token, String vendor) async {
    onTokenReceivedCallback(token, vendor);
  }

  @override
  Future<void> onPermissionChanged(bool granted, String? vendor) async {
    // 可以添加权限变更的处理
  }

  @override
  Future<void> onError(String error, String? vendor) async {
    onTokenErrorCallback(error, vendor ?? 'unknown');
  }
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  MyAppState createState() => MyAppState();
}

class MyAppState extends State<MyApp> {
  String _token = '未获取';
  String _status = '准备中...';
  String _diagnostics = '';
  WxtpushClient? _client;

  @override
  void initState() {
    super.initState();
    _initPushWithDiagnostics();
  }

  Future<void> _initPushWithDiagnostics() async {
    try {
      setState(() {
        _status = '正在初始化增强诊断的OPPO推送...';
      });

      _client = WxtpushClient.instance;

      // 配置OPPO推送，包含诊断参数
      const config = PushConfig(
        oppo: OppoConfig(
          appId: '31692033', // 测试用的appId
          appKey: '85f05c636062439bb5b5a997312f61de',
          appSecret: '649aff727c6340a7b47c1588cbcd9c7a',
          // 设置较短的超时时间以便快速诊断
          registerTimeoutMs: 3000, // 3秒超时
        ),
      );

      // 自定义消息处理器，捕获详细诊断信息
      final messageHandler = DiagnosticPushMessageHandler(
        onTokenReceivedCallback: (token, vendor) {
          setState(() {
            _token = token;
            _status = '✅ Token获取成功！';
            _diagnostics +=
                '\n[${DateTime.now().toString().substring(11, 19)}] ✅ Token: ${token.substring(0, 12)}...\n';
          });
        },
        onTokenErrorCallback: (error, vendor) {
          setState(() {
            _status = '❌ Token获取失败';
            _diagnostics += '\n[${DateTime.now().toString().substring(11, 19)}] ❌ 错误: $error\n';
          });

          // 分析错误并提供建议
          _analyzeError(error);
        },
        onMessageReceivedCallback: (message, vendor) {
          setState(() {
            _diagnostics += '\n[${DateTime.now().toString().substring(11, 19)}] 📨 收到消息: ${message.title}\n';
          });
        },
        onNotificationClickedCallback: (message, vendor) {
          setState(() {
            _diagnostics += '\n[${DateTime.now().toString().substring(11, 19)}] 👆 通知点击: ${message.title}\n';
          });
        },
      );

      // 初始化推送服务
      await _client!.initialize(config, messageHandler: messageHandler);

      setState(() {
        _status = '初始化完成，开始诊断...';
        _diagnostics += '[${DateTime.now().toString().substring(11, 19)}] 🔧 OPPO推送增强诊断启动\n';
        _diagnostics += '配置参数:\n';
        _diagnostics += '  - AppID: ${config.oppo!.appId}\n';
        _diagnostics += '  - AppKey: ${config.oppo!.appKey.substring(0, 8)}***\n';
        _diagnostics += '  - 超时设置: ${config.oppo!.registerTimeoutMs ?? 5000}ms\n';
        _diagnostics += '等待SDK回调...\n';
      });

      // 获取Token（会触发诊断流程）
      final tokens = await _client!.getTokens();
      if (tokens.isNotEmpty) {
        final oppoToken = tokens.where((t) => t.vendor == PushVendor.oppo).firstOrNull;
        if (oppoToken != null) {
          setState(() {
            _token = oppoToken.token;
            _status = '✅ Token已存在';
            _diagnostics +=
                '\n[${DateTime.now().toString().substring(11, 19)}] 📋 现有Token: ${oppoToken.token.substring(0, 12)}...\n';
          });
        }
      }
    } catch (e) {
      setState(() {
        _status = '初始化失败: $e';
        _diagnostics += '\n[${DateTime.now().toString().substring(11, 19)}] 💥 初始化异常: $e\n';
      });
    }
  }

  void _analyzeError(String error) {
    setState(() {
      _diagnostics += '\n📋 错误分析:\n';

      if (error.contains('超时')) {
        _diagnostics += '  • 可能原因：SDK未正确初始化或设备不支持\n';
        _diagnostics += '  • 建议：检查设备是否为OPPO/OnePlus/realme品牌\n';
        _diagnostics += '  • 建议：确认Heytap推送服务已安装且运行\n';
      }

      if (error.contains('应用信息错误') || error.contains('code=-3')) {
        _diagnostics += '  • 可能原因：AppKey或AppSecret不正确\n';
        _diagnostics += '  • 建议：检查OPPO开放平台配置\n';
      }

      if (error.contains('应用签名错误') || error.contains('code=-8')) {
        _diagnostics += '  • 可能原因：应用签名与后台配置不符\n';
        _diagnostics += '  • 建议：检查SHA1签名是否与开放平台一致\n';
      }

      if (error.contains('白名单') || error.contains('code=-100')) {
        _diagnostics += '  • 可能原因：应用未在白名单或权限问题\n';
        _diagnostics += '  • 建议：申请"通知栏推送"正式权限\n';
        _diagnostics += '  • 参考：doc/OPPO_PUSH_PERMISSION_GUIDE.md\n';
      }

      if (error.contains('设备不支持') || error.contains('code=-11')) {
        _diagnostics += '  • 可能原因：设备不支持推送或服务未启用\n';
        _diagnostics += '  • 建议：检查设备推送服务状态\n';
        _diagnostics += '  • 建议：确认ROM版本支持Heytap推送\n';
      }

      _diagnostics += '\n';
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('OPPO推送增强诊断'),
          backgroundColor: Colors.orange,
        ),
        body: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '状态',
                        style: Theme.of(context).textTheme.headlineSmall,
                      ),
                      const SizedBox(height: 8),
                      Text(_status),
                      const SizedBox(height: 16),
                      Text(
                        'Token',
                        style: Theme.of(context).textTheme.headlineSmall,
                      ),
                      const SizedBox(height: 8),
                      SelectableText(_token),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Text(
                '诊断日志',
                style: Theme.of(context).textTheme.headlineSmall,
              ),
              const SizedBox(height: 8),
              Expanded(
                child: Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16.0),
                    child: SingleChildScrollView(
                      child: SelectableText(
                        _diagnostics.isEmpty ? '等待诊断信息...' : _diagnostics,
                        style: const TextStyle(
                          fontFamily: 'monospace',
                          fontSize: 12,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () {
                        setState(() {
                          _diagnostics = '';
                          _status = '重新启动诊断...';
                        });
                        _initPushWithDiagnostics();
                      },
                      child: const Text('重新诊断'),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: ElevatedButton(
                      onPressed: () {
                        setState(() {
                          _diagnostics = '';
                        });
                      },
                      child: const Text('清空日志'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
