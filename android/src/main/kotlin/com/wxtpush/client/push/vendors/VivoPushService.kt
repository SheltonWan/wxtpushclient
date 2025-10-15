package com.wxtpush.client.push.vendors

import android.content.Context
import android.util.Log
import com.wxtpush.client.push.BasePushService
import com.wxtpush.client.push.PushEventCallback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * VIVO推送服务实现
 * 支持真实SDK和模拟模式的自动切换
 */
class VivoPushService(
    context: Context,
    eventCallback: PushEventCallback
) : BasePushService(context, eventCallback) {

    companion object {
        private const val TAG = "VivoPushService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentToken: String? = null
    private val USE_REAL_SDK = checkSDKAvailable()

    /**
     * 检测VIVO推送SDK是否可用
     */
    private fun checkSDKAvailable(): Boolean {
        return try {
            Class.forName("com.vivo.push.PushClient")
            Log.d(TAG, "检测到VIVO推送SDK，将使用真实实现")
            true
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "未检测到VIVO推送SDK，将使用模拟实现")
            false
        }
    }

    override fun initialize(config: Map<String, Any>) {
        try {
            Log.d(TAG, "初始化VIVO推送服务")
            
            val appId = config["appId"] as? String
            val appKey = config["appKey"] as? String
            
            if (appId.isNullOrEmpty() || appKey.isNullOrEmpty()) {
                Log.e(TAG, "VIVO推送配置参数缺失")
                sendEvent("error", mapOf(
                    "error" to "VIVO推送配置参数缺失: appId或appKey为空",
                    "vendor" to "vivo"
                ))
                return
            }
            
            Log.d(TAG, "VIVO推送配置: appId=$appId, appKey=$appKey")
            
            // 检查设备品牌
            val deviceBrand = android.os.Build.BRAND.lowercase()
            if (!deviceBrand.contains("vivo")) {
                Log.w(TAG, "当前设备不是VIVO品牌: $deviceBrand，可能无法正常使用VIVO推送")
            }
            
            if (USE_REAL_SDK) {
                initializeRealSDK(appId, appKey)
            } else {
                initializeMockSDK(appId, appKey)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "VIVO推送初始化失败", e)
            sendEvent("error", mapOf(
                "error" to "VIVO推送初始化失败: ${e.message}",
                "vendor" to "vivo"
            ))
        }
    }

    private fun initializeRealSDK(appId: String, appKey: String) {
        try {
            // 使用反射调用VIVO推送SDK
            val pushClientClass = Class.forName("com.vivo.push.PushClient")
            
            // 初始化VIVO推送
            val initMethod = pushClientClass.getMethod("getInstance", Context::class.java)
            val pushClient = initMethod.invoke(null, context)
            
            // 初始化推送服务
            val initPushMethod = pushClient.javaClass.getMethod("initialize")
            initPushMethod.invoke(pushClient)
            
            // 检查推送服务是否支持
            val isSupportMethod = pushClient.javaClass.getMethod("isSupport")
            val isSupported = isSupportMethod.invoke(pushClient) as Boolean
            
            if (!isSupported) {
                Log.w(TAG, "当前设备不支持VIVO推送服务，降级到模拟模式")
                initializeMockSDK(appId, appKey)
                return
            }
            
            // 开启推送服务
            val turnOnPushMethod = pushClient.javaClass.getMethod("turnOnPush", 
                Class.forName("com.vivo.push.IPushActionListener"))
            
            // 创建回调
            val listenerClass = Class.forName("com.vivo.push.IPushActionListener")
            val listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                when (method.name) {
                    "onStateChanged" -> {
                        val state = args?.get(0) as? Int ?: -1
                        if (state == 0) { // 推送开启成功
                            Log.d(TAG, "VIVO推送服务开启成功")
                            // 获取RegId
                            scope.launch {
                                delay(2000) // 等待2秒让服务完全启动
                                getRegIdFromSDK(pushClient)
                            }
                        } else {
                            Log.e(TAG, "VIVO推送服务开启失败，状态码: $state")
                            sendEvent("error", mapOf(
                                "error" to "VIVO推送服务开启失败",
                                "vendor" to "vivo"
                            ))
                        }
                    }
                }
                null
            }
            
            turnOnPushMethod.invoke(pushClient, listener)
            Log.d(TAG, "VIVO推送SDK初始化完成，等待服务启动...")
            
        } catch (e: Exception) {
            Log.e(TAG, "真实SDK初始化失败，降级到模拟模式", e)
            initializeMockSDK(appId, appKey)
        }
    }

    private fun getRegIdFromSDK(pushClient: Any) {
        try {
            val getRegIdMethod = pushClient.javaClass.getMethod("getRegId")
            val regId = getRegIdMethod.invoke(pushClient) as? String
            
            if (!regId.isNullOrEmpty()) {
                currentToken = regId
                Log.i(TAG, "VIVO推送Token获取成功: ${regId.take(10)}...")
                sendEvent("tokenReceived", mapOf(
                    "token" to regId,
                    "vendor" to "vivo"
                ))
            } else {
                Log.w(TAG, "VIVO推送Token获取失败：返回空Token")
                sendEvent("error", mapOf(
                    "error" to "获取VIVO推送Token失败",
                    "vendor" to "vivo"
                ))
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取VIVO推送Token时发生异常", e)
            sendEvent("error", mapOf(
                "error" to "获取Token异常: ${e.message}",
                "vendor" to "vivo"
            ))
        }
    }

    private fun initializeMockSDK(appId: String, appKey: String) {
        Log.d(TAG, "使用VIVO推送模拟实现")
        
        scope.launch {
            delay(2000) // 模拟初始化延迟
            
            // 生成基于设备的稳定Token
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver, 
                android.provider.Settings.Secure.ANDROID_ID
            )
            val mockToken = "VIVO_MOCK_${deviceId.take(8)}_${System.currentTimeMillis()}"
            
            currentToken = mockToken
            Log.d(TAG, "VIVO推送模拟Token生成: ${mockToken.take(15)}...")
            
            sendEvent("tokenReceived", mapOf(
                "token" to mockToken,
                "vendor" to "vivo"
            ))
        }
    }

    override fun getToken(): String? = currentToken
    
    override fun enableNotification() {
        if (USE_REAL_SDK) {
            // TODO: 实现真实的通知开启逻辑
            Log.d(TAG, "开启VIVO推送通知")
        } else {
            Log.d(TAG, "模拟开启VIVO推送通知")
        }
    }
    
    override fun disableNotification() {
        if (USE_REAL_SDK) {
            // TODO: 实现真实的通知关闭逻辑
            Log.d(TAG, "关闭VIVO推送通知")
        } else {
            Log.d(TAG, "模拟关闭VIVO推送通知")
        }
    }
    
    override fun setAlias(alias: String) {
        if (USE_REAL_SDK) {
            // TODO: 实现真实的别名设置逻辑
            Log.d(TAG, "设置VIVO推送别名: $alias")
        } else {
            Log.d(TAG, "模拟设置VIVO推送别名: $alias")
        }
    }
    
    override fun setTags(tags: List<String>) {
        if (USE_REAL_SDK) {
            // TODO: 实现真实的标签设置逻辑
            Log.d(TAG, "设置VIVO推送标签: $tags")
        } else {
            Log.d(TAG, "模拟设置VIVO推送标签: $tags")
        }
    }
}
