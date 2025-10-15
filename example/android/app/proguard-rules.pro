# Keep the placeholder Service for OPPO/Heytap integration
-keep class com.ephnic.withyou.component.service.AppPushMessageService {
    <init>(...);
    *;
}

# Keep Flutter generated registrant and plugin classes
-keep class io.flutter.** { *; }
-dontwarn io.flutter.**

# Keep Huawei/Honor push service classes referenced via manifest/intents
-keep class com.wxtpush.client.push.vendors.** { *; }

# Gson model keep (if needed)
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Commons-codec is safe, but keep to be explicit
-keep class org.apache.commons.codec.** { *; }
-dontwarn org.apache.commons.codec.**
