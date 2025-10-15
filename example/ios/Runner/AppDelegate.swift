import Flutter
import UIKit
import wxtpush_client

@main
@objc class AppDelegate: FlutterAppDelegate {
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    GeneratedPluginRegistrant.register(with: self)
    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }
  
  // APNs Token注册成功回调
  override func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    print("📱 AppDelegate: APNs Token注册成功")
    
    // 将Token传递给WxtpushClientPlugin
    if let plugin = WxtpushClientPlugin.shared {
      plugin.didRegisterForRemoteNotificationsWithDeviceToken(deviceToken)
    } else {
      print("⚠️ WxtpushClientPlugin.shared 为 nil")
    }
  }
  
  // APNs Token注册失败回调
  override func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
    print("❌ AppDelegate: APNs Token注册失败: \(error.localizedDescription)")
    
    // 将错误传递给WxtpushClientPlugin
    if let plugin = WxtpushClientPlugin.shared {
      plugin.didFailToRegisterForRemoteNotificationsWithError(error)
    } else {
      print("⚠️ WxtpushClientPlugin.shared 为 nil")
    }
  }
}
