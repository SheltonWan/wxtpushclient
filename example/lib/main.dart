import 'package:flutter/material.dart';
import 'package:wxtpush_client/wxtpush_client.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '推送测试客户端',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const PushTestPage(),
    );
  }
}

class PushTestPage extends StatefulWidget {
  const PushTestPage({super.key});

  @override
  State<PushTestPage> createState() => _PushTestPageState();
}

class _PushTestPageState extends State<PushTestPage> {
  final List<String> _messages = [];
  bool _isInitialized = false;
  List<PushToken> _tokens = [];
  int _badgeCount = 0; // 角标计数器

  @override
  void initState() {
    super.initState();
    _initPush();
  }

  Future<void> _initPush() async {
    try {
      setState(() {
        _messages.add('🚀 开始初始化多厂商推送服务...');
        _messages.add('📱 支持设备：荣耀、华为、小米、OPPO、VIVO、苹果');
        _messages.add('� 自动检测当前设备厂商并启用对应推送服务');
      });

      // ✨ 使用简化配置 - 自动从manifest读取Android配置！
      final config = await PushConfig.fromManifest(
        // iOS配置（Android会忽略此参数）
        apple: const AppleConfig(
          bundleId: 'com.ephnic.withyou',
          useSandbox: true, // 使用沙盒环境进行测试
        ),
        debugMode: true,
        // 可选：手动覆盖某些配置
        // oppoOverride: OppoConfig(...),
      );

      // 创建消息处理器
      final messageHandler = _ExamplePushMessageHandler(
        onTokenUpdateCallback: (token, vendor) {
          setState(() {
            _messages.add('🔑 Token更新 [$vendor]: $token');
            
            // 更新本地Token列表
            final existingIndex = _tokens.indexWhere((t) => t.vendor.id == vendor);
            final vendorEnum = PushVendor.values.firstWhere(
              (v) => v.id == vendor,
              orElse: () => PushVendor.apple, // 默认值
            );
            final newToken = PushToken(
              token: token,
              vendor: vendorEnum,
              createdAt: DateTime.now(),
            );
            
            if (existingIndex >= 0) {
              _tokens[existingIndex] = newToken;
            } else {
              _tokens.add(newToken);
            }
          });
        },
        onErrorCallback: (error, vendor) {
          setState(() {
            _messages.add('❌ 错误 [${vendor ?? 'Unknown'}]: $error');
          });
        },
        onMessageCallback: (message) {
          setState(() {
            _messages.add('📨 收到消息: ${message.title} - ${message.body}');
          });
        },
        onPermissionChangeCallback: (granted, vendor) {
          final vendorText = vendor != null ? ' [$vendor]' : '';
          setState(() {
            if (granted) {
              _messages.add('✅ 通知权限已授予$vendorText');
            } else {
              _messages.add('❌ 通知权限被拒绝$vendorText');
            }
          });
        },
      );

      // 初始化推送服务
      await WxtpushClient.instance.initialize(config, messageHandler: messageHandler);
      
      setState(() {
        _messages.add('✅ 推送服务初始化成功');
        _isInitialized = true;
      });

      // 等待一下让Token更新事件有时间触发
      // await Future.delayed(const Duration(seconds: 2));
      
      // // 获取所有Token
      // await _refreshTokens();

    } catch (e) {
      setState(() {
        _messages.add('❌ 初始化失败: $e');
      });
    }
  }

  Future<void> _refreshTokens() async {
    if (!_isInitialized) return;
    
    try {
      final tokens = await WxtpushClient.instance.getTokens();
      setState(() {
        _tokens = tokens;
        _messages.add('📋 获取到${tokens.length}个推送Token');
        for (final token in tokens) {
          _messages.add('  └─ ${token.vendor}: ${token.token.substring(0, 20)}...');
        }
      });
    } catch (e) {
      setState(() {
        _messages.add('❌ 获取Token失败: $e');
      });
    }
  }

  // 设置角标
  Future<void> _setBadge(int count) async {
    if (!_isInitialized) return;
    
    try {
      final success = await WxtpushClient.instance.setBadge(count);
      setState(() {
        if (success) {
          _badgeCount = count;
          _messages.add('🔔 角标已设置为: $count');
        } else {
          _messages.add('❌ 设置角标失败');
        }
      });
    } catch (e) {
      setState(() {
        _messages.add('❌ 设置角标异常: $e');
      });
    }
  }

  // 增加角标
  Future<void> _increaseBadge() async {
    await _setBadge(_badgeCount + 1);
  }

  // 清除角标
  Future<void> _clearBadge() async {
    if (!_isInitialized) return;
    
    try {
      final success = await WxtpushClient.instance.clearBadge();
      setState(() {
        if (success) {
          _badgeCount = 0;
          _messages.add('✅ 角标已清除');
        } else {
          _messages.add('❌ 清除角标失败');
        }
      });
    } catch (e) {
      setState(() {
        _messages.add('❌ 清除角标异常: $e');
      });
    }
  }

  // 获取当前角标
  Future<void> _getBadge() async {
    if (!_isInitialized) return;
    
    try {
      final count = await WxtpushClient.instance.getBadge();
      setState(() {
        _badgeCount = count;
        _messages.add('📊 当前角标: $count');
      });
    } catch (e) {
      setState(() {
        _messages.add('❌ 获取角标异常: $e');
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('推送测试客户端'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            // 控制按钮
            Row(
              children: [
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isInitialized ? _refreshTokens : null,
                    child: const Text('刷新Token'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton(
                    onPressed: _isInitialized ? _clearMessages : null,
                    child: const Text('清空日志'),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            // 角标管理按钮
            Row(
              children: [
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isInitialized ? _increaseBadge : null,
                    icon: const Icon(Icons.add, size: 16),
                    label: Text('角标+1 ($_badgeCount)'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.orange,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isInitialized ? _clearBadge : null,
                    icon: const Icon(Icons.clear, size: 16),
                    label: const Text('清除角标'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.red,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: _isInitialized ? _getBadge : null,
                    icon: const Icon(Icons.info, size: 16),
                    label: const Text('查询'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.blue,
                      foregroundColor: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            // Token 信息
            if (_tokens.isNotEmpty)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Token信息 (${_tokens.length}个)',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      for (final token in _tokens)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 4),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '${token.vendor}:',
                                style: const TextStyle(fontWeight: FontWeight.bold),
                              ),
                              Text(
                                token.token,
                                style: const TextStyle(fontSize: 10, fontFamily: 'Courier'),
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            const SizedBox(height: 16),
            // 消息日志
            Expanded(
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '消息日志 (${_messages.length}条)',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      Expanded(
                        child: ListView.builder(
                          itemCount: _messages.length,
                          reverse: true, // 最新消息在顶部
                          itemBuilder: (context, index) {
                            final reversedIndex = _messages.length - 1 - index;
                            return Padding(
                              padding: const EdgeInsets.symmetric(vertical: 2),
                              child: Text(
                                '${DateTime.now().toString().substring(11, 19)} ${_messages[reversedIndex]}',
                                style: const TextStyle(fontSize: 12),
                              ),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _clearMessages() {
    setState(() {
      _messages.clear();
    });
  }
}

/// 自定义推送消息处理器
class _ExamplePushMessageHandler implements PushMessageHandler {
  final Function(String token, String vendor)? onTokenUpdateCallback;
  final Function(String error, String? vendor)? onErrorCallback;
  final Function(PushMessage message)? onMessageCallback;
  final Function(bool granted, String? vendor)? onPermissionChangeCallback;

  _ExamplePushMessageHandler({
    this.onTokenUpdateCallback,
    this.onErrorCallback,
    this.onMessageCallback,
    this.onPermissionChangeCallback,
  });

  @override
  Future<void> onMessageReceived(PushMessage message) async {
    onMessageCallback?.call(message);
  }

  @override
  Future<void> onMessageClicked(PushMessage message) async {
    onMessageCallback?.call(message);
  }

  @override
  Future<void> onTokenUpdated(String token, String vendor) async {
    onTokenUpdateCallback?.call(token, vendor);
  }

  @override
  Future<void> onPermissionChanged(bool granted, String? vendor) async {
    onPermissionChangeCallback?.call(granted, vendor);
  }

  @override
  Future<void> onError(String error, String? vendor) async {
    onErrorCallback?.call(error, vendor);
  }
}
