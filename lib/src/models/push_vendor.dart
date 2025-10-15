/// 推送厂商枚举
enum PushVendor {
  /// 华为HMS推送
  huawei('huawei', 'HMS Push'),
  
  /// 荣耀推送
  honor('honor', 'Honor Push'),
  
  /// 小米推送
  xiaomi('xiaomi', 'Mi Push'),
  
  /// OPPO推送
  oppo('oppo', 'OPPO Push'),
  
  /// VIVO推送
  vivo('vivo', 'VIVO Push'),
  
  /// 苹果APNs推送
  apple('apple', 'Apple Push Notification service');

  const PushVendor(this.id, this.displayName);

  final String id;
  final String displayName;

  /// 根据厂商ID获取枚举值
  static PushVendor? fromId(String id) {
    for (final vendor in PushVendor.values) {
      if (vendor.id == id) {
        return vendor;
      }
    }
    return null;
  }
}
