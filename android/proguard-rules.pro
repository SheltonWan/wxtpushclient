# WXTPush 混淆规则
# 此文件会被插件自动添加到客户端应用的混淆配置中

# ===== 保留WXTPush核心类 =====
-keep class com.wxtpush.client.** { *; }

# ===== 华为推送混淆规则 =====
-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

-keep class com.hianalytics.android.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}
-keep class com.huawei.agconnect.**{*;}

# ===== 荣耀推送混淆规则 =====
-keep class com.hihonor.** { *; }
-keep class com.honor.** { *; }

# ===== 小米推送混淆规则 (基于官方AAR) =====
-keep class com.xiaomi.mipush.sdk.MiPushMessage {*;}
-keep class com.xiaomi.mipush.sdk.MiPushCommandMessage {*;}
-keep class com.xiaomi.mipush.sdk.PushMessageReceiver {*;}
-keep class com.xiaomi.mipush.sdk.MessageHandleService {*;}
-keep class com.xiaomi.push.service.XMJobService {*;}
-keep class com.xiaomi.push.service.XMPushService {*;}
-keep class com.xiaomi.mipush.sdk.PushMessageHandler {*;}
-keep class com.xiaomi.push.service.receivers.NetworkStatusReceiver {*;}
-keep class com.xiaomi.push.service.receivers.PingReceiver {*;}
-keep class com.xiaomi.mipush.sdk.NotificationClickedActivity {*;}
-keep class com.xiaomi.mipush.sdk.MiPushClient {*;}
-keep class com.xiaomi.mipush.sdk.** { *; }

# ===== OPPO/Heytap推送混淆规则 =====
-keep class com.heytap.mcs.** { *; }
-keep class com.coloros.mcs.** { *; }
-keep class com.oppo.push.** { *; }

# ===== VIVO推送混淆规则 (预留) =====
-keep class com.vivo.push.** { *; }
-keep class com.vivo.vms.** { *; }

# ===== 通用依赖混淆规则 =====
# Apache Commons Codec (OPPO依赖)
-keep class org.apache.commons.codec.** { *; }
# Gson (JSON处理)
-keep class com.google.gson.** { *; }
-keep class com.hihonor.push.**{*;}
-keep class com.hihonor.mcs.**{*;}

# ===== 小米推送混淆规则 =====
-keep class com.xiaomi.mipush.sdk.** { *; }
-keep class com.xiaomi.push.** { *; }
-keep class com.xiaomi.channel.commonutils.** { *; }

# ===== OPPO推送混淆规则 =====
-keep class com.heytap.mcs.** { *; }
-keep class com.coloros.mcs.** { *; }
-keep class com.heytap.msp.push.** { *; }
-keep class com.coloros.mcssdk.** { *; }

# ===== VIVO推送混淆规则 =====
-keep class com.vivo.push.** { *; }
-keep class com.vivo.vms.** { *; }

# ===== 通用推送相关 =====
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Service
-keep public class * extends android.content.ContentProvider

# ===== JSON序列化保护 =====
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}
