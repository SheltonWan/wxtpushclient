package com.wxtpush.client.push.vendors

import android.content.Context
import android.util.Log
import com.wxtpush.client.push.BasePushService
import com.wxtpush.client.push.PushEventCallback
import com.wxtpush.client.push.BadgeHelper

/**
 * 小米推送服务实现
 * 支持真实SDK和模拟模式的自动切换
 */
class XiaomiPushService(
    context: Context,
    eventCallback: PushEventCallback
) : BasePushService(context, eventCallback) {

    private val TAG = "XiaomiPushService"
    private val USE_REAL_SDK = checkSDKAvailable() // 自动检测SDK是否可用

    /**
     * 检测小米推送SDK是否可用
     */
    private fun checkSDKAvailable(): Boolean {
        return try {
            Class.forName("com.xiaomi.mipush.sdk.MiPushClient")
            Log.d(TAG, "检测到小米推送SDK，将使用真实实现")
            true
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "未检测到小米推送SDK，将使用模拟实现")
            false
        }
    }

    override fun initialize(config: Map<String, Any>) {
        try {
            Log.d(TAG, "初始化小米推送服务")
            
            val appId = config["appId"] as? String
            val appKey = config["appKey"] as? String
            
            if (appId.isNullOrEmpty() || appKey.isNullOrEmpty()) {
                Log.e(TAG, "小米推送配置参数缺失")
                sendEvent("error", mapOf(
                    "error" to "小米推送配置参数缺失: appId或appKey为空",
                    "vendor" to "xiaomi"
                ))
                return
            }
            
            Log.d(TAG, "小米推送配置: appId=$appId, appKey=$appKey")
            
            if (USE_REAL_SDK) {
                // 真实SDK实现 - 需要小米推送SDK AAR文件
                initializeRealSDK(appId, appKey)
            } else {
                // 模拟实现 - 用于测试
                initializeMockSDK(appId, appKey)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "小米推送初始化失败", e)
            sendEvent("error", mapOf(
                "error" to "小米推送初始化失败: ${e.message}",
                "vendor" to "xiaomi"
            ))
        }
    }

    private fun initializeRealSDK(appId: String, appKey: String) {
        // 真实SDK初始化
        try {
            // 使用反射调用MiPushClient.registerPush
            val miPushClientClass = Class.forName("com.xiaomi.mipush.sdk.MiPushClient")
            val registerMethod = miPushClientClass.getMethod("registerPush", Context::class.java, String::class.java, String::class.java)
            registerMethod.invoke(null, context, appId, appKey)
            
            Log.d(TAG, "小米推送SDK初始化完成")
            
            // 尝试获取现有Token
            val getRegIdMethod = miPushClientClass.getMethod("getRegId", Context::class.java)
            val existingToken = getRegIdMethod.invoke(null, context) as? String
            
            if (!existingToken.isNullOrEmpty()) {
                Log.d(TAG, "获取到现有Token: ${existingToken.take(10)}...")
                sendEvent("tokenReceived", mapOf(
                    "token" to existingToken, 
                    "vendor" to "xiaomi"
                ))
            } else {
                Log.d(TAG, "等待Token注册回调...")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "真实SDK初始化失败，降级到模拟模式", e)
            initializeMockSDK(appId, appKey)
        }
    }

    private fun initializeMockSDK(appId: String, appKey: String) {
        // 模拟实现 - 生成基于设备的稳定Token
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        )
        val mockToken = "xiaomi_mock_${deviceId}_${appId.hashCode()}"
        
        Log.d(TAG, "小米推送模拟实现初始化完成")
        
        // 发送模拟的注册成功广播
        try {
            val intent = android.content.Intent("com.xiaomi.mipush.RECEIVE_MESSAGE")
            intent.putExtra("key_command", "register")
            intent.putExtra("key_result_code", 0L)
            intent.putExtra("key_reg_id", mockToken)
            context.sendBroadcast(intent)
            Log.d(TAG, "发送模拟注册成功广播")
        } catch (e: Exception) {
            Log.e(TAG, "发送模拟广播失败", e)
            // 降级到直接调用事件
            sendEvent("tokenReceived", mapOf(
                "token" to mockToken, 
                "vendor" to "xiaomi"
            ))
        }
    }

    override fun getToken(): String? {
        return try {
            if (USE_REAL_SDK) {
                // 使用反射获取真实Token
                val miPushClientClass = Class.forName("com.xiaomi.mipush.sdk.MiPushClient")
                val getRegIdMethod = miPushClientClass.getMethod("getRegId", Context::class.java)
                val token = getRegIdMethod.invoke(null, context) as? String
                Log.d(TAG, "获取真实Token: ${token?.take(10) ?: "null"}...")
                token
            } else {
                // 返回基于设备的稳定模拟Token
                val deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver, 
                    android.provider.Settings.Secure.ANDROID_ID
                )
                val mockToken = "xiaomi_mock_${deviceId}_stable"
                Log.d(TAG, "返回模拟Token: ${mockToken.take(20)}...")
                mockToken
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取小米推送Token失败", e)
            null
        }
    }
    
    override fun enableNotification() {
        try {
            if (USE_REAL_SDK) {
                val miPushClientClass = Class.forName("com.xiaomi.mipush.sdk.MiPushClient")
                val resumeMethod = miPushClientClass.getMethod("resumePush", Context::class.java, String::class.java)
                resumeMethod.invoke(null, context, null)
                Log.d(TAG, "启用小米推送通知 (真实实现)")
            } else {
                Log.d(TAG, "启用小米推送通知 (模拟实现)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "启用小米推送通知失败", e)
        }
    }
    
    override fun disableNotification() {
        try {
            if (USE_REAL_SDK) {
                val miPushClientClass = Class.forName("com.xiaomi.mipush.sdk.MiPushClient")
                val pauseMethod = miPushClientClass.getMethod("pausePush", Context::class.java, String::class.java)
                pauseMethod.invoke(null, context, null)
                Log.d(TAG, "禁用小米推送通知 (真实实现)")
            } else {
                Log.d(TAG, "禁用小米推送通知 (模拟实现)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "禁用小米推送通知失败", e)
        }
    }
    
    override fun setAlias(alias: String) {
        try {
            if (USE_REAL_SDK) {
                val miPushClientClass = Class.forName("com.xiaomi.mipush.sdk.MiPushClient")
                val setAliasMethod = miPushClientClass.getMethod("setAlias", Context::class.java, String::class.java, String::class.java)
                setAliasMethod.invoke(null, context, alias, null)
                Log.d(TAG, "设置小米推送别名: $alias (真实实现)")
            } else {
                Log.d(TAG, "设置小米推送别名: $alias (模拟实现)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "设置小米推送别名失败", e)
        }
    }
    
    override fun setTags(tags: List<String>) {
        try {
            if (USE_REAL_SDK) {
                val miPushClientClass = Class.forName("com.xiaomi.mipush.sdk.MiPushClient")
                val subscribeMethod = miPushClientClass.getMethod("subscribe", Context::class.java, String::class.java, String::class.java)
                tags.forEach { tag ->
                    subscribeMethod.invoke(null, context, tag, null)
                }
                Log.d(TAG, "设置小米推送标签: $tags (真实实现)")
            } else {
                Log.d(TAG, "设置小米推送标签: $tags (模拟实现)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "设置小米推送标签失败", e)
        }
    }

    override fun setBadge(count: Int): Boolean {
        Log.d(TAG, "设置小米角标: $count")
        return BadgeHelper.setBadge(context, count)
    }

    override fun getBadge(): Int {
        Log.d(TAG, "获取小米角标")
        return BadgeHelper.getBadge(context)
    }
    
    /**
     * 判断是否需要初始化推送服务
     */
    private fun shouldInit(context: Context): Boolean {
        return try {
            // val regId = MiPushClient.getRegId(context) // 临时注释
            // regId.isNullOrEmpty()
            true // 临时总是返回true
        } catch (e: Exception) {
            true
        }
    }
}
