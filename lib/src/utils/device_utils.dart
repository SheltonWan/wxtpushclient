import 'dart:io';
import 'package:device_info_plus/device_info_plus.dart';
import '../models/push_vendor.dart';

/// 设备工具类
class DeviceUtils {
  static final DeviceInfoPlugin _deviceInfo = DeviceInfoPlugin();
  
  /// 获取设备信息
  static Future<Map<String, String>> getDeviceInfo() async {
    final info = <String, String>{};
    
    if (Platform.isAndroid) {
      final androidInfo = await _deviceInfo.androidInfo;
      info['platform'] = 'android';
      info['brand'] = androidInfo.brand;
      info['model'] = androidInfo.model;
      info['manufacturer'] = androidInfo.manufacturer;
      info['version'] = androidInfo.version.release;
      info['sdkInt'] = androidInfo.version.sdkInt.toString();
    } else if (Platform.isIOS) {
      final iosInfo = await _deviceInfo.iosInfo;
      info['platform'] = 'ios';
      info['name'] = iosInfo.name;
      info['model'] = iosInfo.model;
      info['systemName'] = iosInfo.systemName;
      info['systemVersion'] = iosInfo.systemVersion;
    }
    
    return info;
  }
  
  /// 根据设备信息判断应该使用的推送厂商
  static Future<List<PushVendor>> getSupportedVendors() async {
    final vendors = <PushVendor>[];
    
    if (Platform.isIOS) {
      vendors.add(PushVendor.apple);
      return vendors;
    }
    
    if (Platform.isAndroid) {
      final androidInfo = await _deviceInfo.androidInfo;
      final brand = androidInfo.brand.toLowerCase();
      final manufacturer = androidInfo.manufacturer.toLowerCase();
      final model = androidInfo.model.toLowerCase();
      
      // 根据品牌判断支持的推送服务
      if (brand.contains('huawei') || manufacturer.contains('huawei')) {
        vendors.add(PushVendor.huawei);
        // 华为设备也尝试荣耀推送（有些荣耀设备仍然显示华为品牌）
        vendors.add(PushVendor.honor);
      } else if (brand.contains('honor') || manufacturer.contains('honor')) {
        vendors.add(PushVendor.honor);
        // 荣耀设备也尝试华为推送（向下兼容）
        vendors.add(PushVendor.huawei);
      } else if (brand.contains('xiaomi') || manufacturer.contains('xiaomi') || 
                 brand.contains('redmi') || manufacturer.contains('redmi')) {
        vendors.add(PushVendor.xiaomi);
      } else if (brand.contains('oppo') || manufacturer.contains('oppo') ||
                 brand.contains('oneplus') || manufacturer.contains('oneplus') ||
                 brand.contains('realme') || manufacturer.contains('realme')) {
        vendors.add(PushVendor.oppo);
      } else if (brand.contains('vivo') || manufacturer.contains('vivo') ||
                 brand.contains('iqoo') || manufacturer.contains('iqoo')) {
        vendors.add(PushVendor.vivo);
      } else if (model.startsWith('oxf') || model.startsWith('oxy') || 
                 model.startsWith('ela') || model.startsWith('yal')) {
        // 特殊处理：通过型号代码识别荣耀设备
        // OXF = 荣耀V30系列, OXY = 荣耀Play系列, ELA = 荣耀20系列, YAL = 荣耀30系列
        print('🔍 通过型号代码识别为荣耀设备: $model');
        vendors.add(PushVendor.honor);
        vendors.add(PushVendor.huawei); // 早期荣耀设备支持华为推送
      }
      
      // 如果没有匹配的厂商，可以尝试Google FCM（如果需要的话）
      // vendors.add(PushVendor.google);
    }
    
    return vendors;
  }
  
  /// 判断设备是否支持指定厂商的推送服务
  static Future<bool> isVendorSupported(PushVendor vendor) async {
    final supportedVendors = await getSupportedVendors();
    return supportedVendors.contains(vendor);
  }
  
  /// 获取设备唯一标识
  static Future<String?> getDeviceId() async {
    if (Platform.isAndroid) {
      final androidInfo = await _deviceInfo.androidInfo;
      return androidInfo.id; // Android ID
    } else if (Platform.isIOS) {
      final iosInfo = await _deviceInfo.iosInfo;
      return iosInfo.identifierForVendor; // iOS Vendor ID
    }
    return null;
  }
  
  /// 检查是否为华为HMS设备
  static Future<bool> isHuaweiDevice() async {
    if (!Platform.isAndroid) return false;
    
    final androidInfo = await _deviceInfo.androidInfo;
    final brand = androidInfo.brand.toLowerCase();
    final manufacturer = androidInfo.manufacturer.toLowerCase();
    
    return brand.contains('huawei') || manufacturer.contains('huawei');
  }
  
  /// 检查是否为荣耀设备
  static Future<bool> isHonorDevice() async {
    if (!Platform.isAndroid) return false;
    
    final androidInfo = await _deviceInfo.androidInfo;
    final brand = androidInfo.brand.toLowerCase();
    final manufacturer = androidInfo.manufacturer.toLowerCase();
    
    return brand.contains('honor') || manufacturer.contains('honor');
  }
}
