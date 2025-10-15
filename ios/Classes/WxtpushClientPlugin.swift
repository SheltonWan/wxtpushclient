import Flutter
import UIKit
// 1. 导入框架
import UserNotifications

public class WxtpushClientPlugin: NSObject, FlutterPlugin {
    private var channel: FlutterMethodChannel?
    private var eventChannel: FlutterEventChannel?
    // FlutterEventSink 是 iOS 端 EventChannel 的事件回调闭包类型（typedef block）。
    // 原生侧通过调用该闭包，把异步事件推送到 Dart 的 Stream。
    private var eventSink: FlutterEventSink?
    private var apnsToken: String?
    
    // 静态实例用于从AppDelegate访问
    public static var shared: WxtpushClientPlugin?
    
    // 静态注册入口
    // Flutter 引擎在应用启动或插件加载时调用这个入口，从而让插件把自己“挂接”到引擎的消息通道，以便与 Dart 侧进行通信。
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "wxtpush_client", binaryMessenger: registrar.messenger())
        let eventChannel = FlutterEventChannel(name: "wxtpush_client/events", binaryMessenger: registrar.messenger())
        let instance = WxtpushClientPlugin()
        // 保存静态实例
        shared = instance

        instance.channel = channel
        instance.eventChannel = eventChannel
        
        registrar.addMethodCallDelegate(instance, channel: channel)
        eventChannel.setStreamHandler(instance)

        // 设置推送通知 - 应用启动时主动请求权限并获取Token
        instance.setupPushNotifications()
    }

    // handle 是插件实现 FlutterPlugin 协议时的核心回调，用于接收 Dart 端通过 MethodChannel 
    // 发起的同步/异步方法调用。Flutter 引擎把每次调用封装为 FlutterMethodCall 传入，包含方法名 
    // call.method 与可选参数 call.arguments；同时提供一个一次性回调闭包 result，用于把执行
    // 结果返回给 Dart（成功值、FlutterError，或 FlutterMethodNotImplemented）。
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "initializePush":
            initializePush(call: call, result: result)
        case "getToken":
            getToken(call: call, result: result)
        case "getAllTokens":
            getAllTokens(result: result)
        case "enableNotification":
            enableNotification(result: result)
        case "disableNotification":
            disableNotification(result: result)
        case "setAlias":
            setAlias(call: call, result: result)
        case "setTags":
            setTags(call: call, result: result)
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    private func initializePush(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let arguments = call.arguments as? [String: Any],
              let vendor = arguments["vendor"] as? String,
              let config = arguments["config"] as? [String: Any] else {
            result(FlutterError(code: "INVALID_ARGUMENTS", message: "Invalid arguments", details: nil))
            return
        }
        
        if vendor == "apple" {
            // 注册苹果推送
            requestNotificationPermissions()
            result(nil)
        } else {
            // iOS上其他厂商推送不适用
            result(FlutterError(code: "UNSUPPORTED", message: "Vendor \\(vendor) is not supported on iOS", details: nil))
        }
    }
    
    private func getToken(call: FlutterMethodCall, result: @escaping FlutterResult) {
        guard let args = call.arguments as? [String: Any],
              let vendor = args["vendor"] as? String else {
            result(FlutterError(code: "INVALID_ARGS", message: "Invalid arguments", details: nil))
            return
        }
        
        if vendor == "apple" {
            if let token = apnsToken {
                let tokenData: [String: Any] = [
                    "vendor": "apple",
                    "token": token,
                    "timestamp": Date().timeIntervalSince1970
                ]
                result(tokenData)
            } else {
                // 尝试重新获取Token
                requestNotificationPermissions()
                result(FlutterError(code: "TOKEN_NOT_AVAILABLE", message: "APNs token not available yet", details: nil))
            }
        } else {
            result(nil)
        }
    }
    
    private func getAllTokens(result: @escaping FlutterResult) {
        var tokens: [[String: Any]] = []
        
        if let token = apnsToken {
            tokens.append([
                "vendor": "apple",
                "token": token,
                "timestamp": Date().timeIntervalSince1970
            ])
        }
        
        result(tokens)
    }
    
    private func enableNotification(result: @escaping FlutterResult) {
        requestNotificationPermissions()
        result(nil)
    }
    
    private func disableNotification(result: @escaping FlutterResult) {
        // iOS不支持程序内禁用推送，用户需要在系统设置中关闭
        result(nil)
    }
    
    private func setAlias(call: FlutterMethodCall, result: @escaping FlutterResult) {
        // APNs不直接支持别名，需要在服务端处理
        result(nil)
    }
    
    private func setTags(call: FlutterMethodCall, result: @escaping FlutterResult) {
        // APNs不直接支持标签，需要在服务端处理
        result(nil)
    }
    
    private func setupPushNotifications() {
        // 设置推送通知代理
        UNUserNotificationCenter.current().delegate = self
        
        // 先检查当前授权状态
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            DispatchQueue.main.async {
                switch settings.authorizationStatus {
                case .authorized, .provisional:
                    // 已授权，直接注册远程通知
                    print("✅ 推送通知已授权，主动注册APNs Token")
                    UIApplication.shared.registerForRemoteNotifications()
                    
                case .notDetermined:
                    // 未决定，请求权限
                    print("❓ 推送通知未决定，请求用户授权")
                    self?.requestNotificationPermissions()
                    
                case .denied:
                    // 已拒绝
                    print("❌ 推送通知已被用户拒绝")
                    self?.sendEvent(event: "permissionDenied", data: ["granted": false, "vendor": "apple"])
                    
                default:
                    break
                }
            }
        }
        
        // 如果是模拟器，生成模拟Token用于测试
        #if targetEnvironment(simulator)
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            let simulatorToken = "simulator_token_\(UUID().uuidString.replacingOccurrences(of: "-", with: "").prefix(64))"
            self.apnsToken = simulatorToken
            
            self.sendEvent(event: "tokenReceived", data: [
                "vendor": "apple",
                "token": simulatorToken
            ])
            
            print("📱 模拟器APNs Token生成: \(simulatorToken)")
        }
        #endif
    }
    // 2. 请求用户授权
    private func requestNotificationPermissions() {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .badge, .sound]) { [weak self] granted, error in
            DispatchQueue.main.async {
                if granted {
                    UIApplication.shared.registerForRemoteNotifications()
                    print("✅ 用户授权推送通知")
                    self?.sendEvent(event: "permissionGranted", data: ["granted": true, "vendor": "apple"])
                } else {
                    print("❌ 用户拒绝推送通知")
                    self?.sendEvent(event: "permissionDenied", data: ["granted": false, "vendor": "apple"])
                }
            }
        }
    }
    // 3. 处理注册结果
    // 处理APNs Token获取成功
    public func didRegisterForRemoteNotificationsWithDeviceToken(_ deviceToken: Data) {
        let tokenString = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        apnsToken = tokenString
        
        print("📱 iOS APNs Token获取成功: \(tokenString)")
        
        sendEvent(event: "tokenReceived", data: [  // 改为 tokenReceived 与 Android 统一
            "vendor": "apple",
            "token": tokenString
        ])
    }
    
    // 处理APNs Token获取失败
    public func didFailToRegisterForRemoteNotificationsWithError(_ error: Error) {
        print("❌ iOS APNs Token获取失败: \(error.localizedDescription)")
        
        sendEvent(event: "tokenError", data: [
            "vendor": "apple",
            "error": error.localizedDescription
        ])
    }
    
    private func sendEvent(event: String, data: [String: Any]) {
        let eventData: [String: Any] = [
            "event": event,  // 修改为 "event" 以与 Android 端保持一致
            "data": data
        ]
        print("📡 发送推送事件: \(event), 数据: \(data)")
        eventSink?(eventData)
    }
}

// FlutterStreamHandler 是 Flutter 平台通道中的事件流桥接协议，配合 EventChannel 
// 将原生侧产生的异步事件推送到 Dart 侧的 Stream。它暴露两个生命周期回调：onListen 和 onCancel。
extension WxtpushClientPlugin: FlutterStreamHandler {
    // 当 Dart 端调用 receiveBroadcastStream(...).listen(...) 开始订阅时，
    // iOS 侧会回调 onListen，你可以在这里保存系统传来的 FlutterEventSink 闭包；
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = events
        return nil
    }
    // 当 Dart 端取消订阅时会回调 onCancel，此时应清空保存的 sink，表示事件通道不再可用。
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        self.eventSink = nil
        return nil
    }
}

extension WxtpushClientPlugin: UNUserNotificationCenterDelegate {
    public func userNotificationCenter(_ center: UNUserNotificationCenter, 
                                     willPresent notification: UNNotification, 
                                     withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        // 在前台显示通知
        completionHandler([.alert, .badge, .sound])
        
        sendEvent(event: "messageReceived", data: [
            "title": notification.request.content.title,
            "body": notification.request.content.body,
            "data": notification.request.content.userInfo
        ])
    }
    
    public func userNotificationCenter(_ center: UNUserNotificationCenter, 
                                     didReceive response: UNNotificationResponse, 
                                     withCompletionHandler completionHandler: @escaping () -> Void) {
        // 处理通知点击
        sendEvent(event: "messageClicked", data: [
            "title": response.notification.request.content.title,
            "body": response.notification.request.content.body,
            "data": response.notification.request.content.userInfo
        ])
        
        completionHandler()
    }
}
