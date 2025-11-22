package com.wxtpush.client

import android.content.Context
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import com.wxtpush.client.push.PushManager
import com.wxtpush.client.push.PushVendor

/** WxtpushClientPlugin */
class WxtpushClientPlugin: FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var context: Context
    private lateinit var pushManager: PushManager
    
    private var eventSink: EventChannel.EventSink? = null

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        context = flutterPluginBinding.applicationContext
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "wxtpush_client")
        channel.setMethodCallHandler(this)
        
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "wxtpush_client/events")
        eventChannel.setStreamHandler(this)
        
        pushManager = PushManager(context) { event ->
            eventSink?.success(event)
        }
        
        // 设置PushManager单例实例，供推送服务回调使用
        PushManager.setInstance(pushManager)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "initializePush" -> {
                try {
                    val vendor = call.argument<String>("vendor") ?: throw IllegalArgumentException("vendor is required")
                    val config = call.argument<Map<String, Any>>("config") ?: throw IllegalArgumentException("config is required")
                    
                    val pushVendor = PushVendor.fromString(vendor)
                    pushManager.initializePush(pushVendor, config)
                    result.success(null)
                } catch (e: Exception) {
                    result.error("INIT_ERROR", "Failed to initialize push: ${e.message}", null)
                }
            }
            "getToken" -> {
                try {
                    val vendor = call.argument<String>("vendor") ?: throw IllegalArgumentException("vendor is required")
                    val pushVendor = PushVendor.fromString(vendor)
                    val token = pushManager.getToken(pushVendor)
                    result.success(token)
                } catch (e: Exception) {
                    result.error("TOKEN_ERROR", "Failed to get token: ${e.message}", null)
                }
            }
            "getAllTokens" -> {
                try {
                    val tokens = pushManager.getAllTokens()
                    result.success(tokens)
                } catch (e: Exception) {
                    result.error("TOKENS_ERROR", "Failed to get tokens: ${e.message}", null)
                }
            }
            "enableNotification" -> {
                try {
                    pushManager.enableNotification()
                    result.success(null)
                } catch (e: Exception) {
                    result.error("ENABLE_ERROR", "Failed to enable notification: ${e.message}", null)
                }
            }
            "getManifestConfig" -> {
                try {
                    val config = WxtpushConfigProvider.getConfig(context)
                    result.success(config)
                } catch (e: Exception) {
                    result.error("CONFIG_ERROR", "Failed to get manifest config: ${e.message}", null)
                }
            }
            "disableNotification" -> {
                try {
                    pushManager.disableNotification()
                    result.success(null)
                } catch (e: Exception) {
                    result.error("DISABLE_ERROR", "Failed to disable notification: ${e.message}", null)
                }
            }
            "setAlias" -> {
                try {
                    val alias = call.argument<String>("alias") ?: throw IllegalArgumentException("alias is required")
                    pushManager.setAlias(alias)
                    result.success(null)
                } catch (e: Exception) {
                    result.error("ALIAS_ERROR", "Failed to set alias: ${e.message}", null)
                }
            }
            "setTags" -> {
                try {
                    val tags = call.argument<List<String>>("tags") ?: throw IllegalArgumentException("tags is required")
                    pushManager.setTags(tags)
                    result.success(null)
                } catch (e: Exception) {
                    result.error("TAGS_ERROR", "Failed to set tags: ${e.message}", null)
                }
            }
            "refreshToken" -> {
                try {
                    val vendor = call.argument<String>("vendor") ?: throw IllegalArgumentException("vendor is required")
                    val pushVendor = PushVendor.fromString(vendor)
                    pushManager.refreshToken(pushVendor)
                    result.success(null)
                } catch (e: Exception) {
                    result.error("REFRESH_ERROR", "Failed to refresh token: ${e.message}", null)
                }
            }
            "deleteToken" -> {
                try {
                    val vendor = call.argument<String>("vendor") ?: throw IllegalArgumentException("vendor is required")
                    val pushVendor = PushVendor.fromString(vendor)
                    pushManager.deleteToken(pushVendor)
                    result.success(null)
                } catch (e: Exception) {
                    result.error("DELETE_ERROR", "Failed to delete token: ${e.message}", null)
                }
            }
            "setBadge" -> {
                try {
                    val count = call.argument<Int>("count") ?: throw IllegalArgumentException("count is required")
                    val vendor = call.argument<String>("vendor")
                    val pushVendor = vendor?.let { PushVendor.fromString(it) }
                    val success = pushManager.setBadge(count, pushVendor)
                    result.success(success)
                } catch (e: Exception) {
                    result.error("BADGE_ERROR", "Failed to set badge: ${e.message}", null)
                }
            }
            "getBadge" -> {
                try {
                    val vendor = call.argument<String>("vendor")
                    val pushVendor = vendor?.let { PushVendor.fromString(it) }
                    val badgeCount = pushManager.getBadge(pushVendor)
                    result.success(badgeCount)
                } catch (e: Exception) {
                    result.error("BADGE_ERROR", "Failed to get badge: ${e.message}", null)
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }
}
