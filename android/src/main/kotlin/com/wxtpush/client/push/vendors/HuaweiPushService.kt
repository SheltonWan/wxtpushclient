package com.wxtpush.client.push.vendors

import android.content.Context
import android.util.Log
import com.huawei.hms.api.HuaweiApiAvailability
import com.huawei.hms.api.ConnectionResult
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.wxtpush.client.push.BasePushService
import com.wxtpush.client.push.PushEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * 华为推送服务实现
 */
class HuaweiPushService(
    context: Context,
    eventCallback: PushEventCallback
) : BasePushService(context, eventCallback) {

    companion object {
        private const val TAG = "HuaweiPushService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentToken: String? = null

    override fun initialize(config: Map<String, Any>) {
        Log.d(TAG, "========== 开始初始化华为推送服务 ==========")
        Log.d(TAG, "配置参数: $config")

        // 检查HMS Core可用性
        val availability = HuaweiApiAvailability.getInstance()
        val result = availability.isHuaweiMobileServicesAvailable(context)
        
        Log.d(TAG, "HMS Core 可用性检查结果: $result")
        
        if (result != ConnectionResult.SUCCESS) {
            Log.e(TAG, "❌ HMS Core不可用，result code: $result")
            sendTokenError("HMS Core不可用")
            return
        }

        Log.i(TAG, "✅ HMS Core可用，立即获取Token")
        
        // 直接获取Token，不延迟
        scope.launch {
            getTokenAsync(config)
        }
    }    private fun getTokenAsync(config: Map<String, Any>) {
        scope.launch {
            try {
                val appId = config["appId"] as? String
                if (appId.isNullOrEmpty()) {
                    Log.e(TAG, "华为推送AppId未配置")
                    sendTokenError("AppId未配置")
                    return@launch
                }

                Log.d(TAG, "正在获取华为推送Token，AppId: $appId")
                
                // 华为推送Token获取 - 根据HMS Core版本使用不同scope
                val token = try {
                    // 华为推送标准scope，适用于大部分设备
                    Log.d(TAG, "尝试使用HCM scope获取Token")
                    HmsInstanceId.getInstance(context).getToken(appId, "HCM")
                } catch (e: ApiException) {
                    if (e.statusCode == 907135701) {
                        // 如果HCM scope失败，尝试空scope（新版HMS Core）
                        Log.w(TAG, "HCM scope失败，尝试空scope")
                        try {
                            HmsInstanceId.getInstance(context).getToken(appId, "")
                        } catch (e2: ApiException) {
                            // 最后尝试默认scope
                            Log.w(TAG, "空scope也失败，尝试默认scope")
                            HmsInstanceId.getInstance(context).getToken(appId, "DEFAULT_SCOPE")
                        }
                    } else {
                        throw e // 重新抛出其他异常
                    }
                }
                
                if (!token.isNullOrEmpty()) {
                    currentToken = token
                    Log.i(TAG, "华为推送Token获取成功: ${token.take(20)}...")
                    sendTokenSuccess(token)
                } else {
                    Log.e(TAG, "获取华为推送Token失败：返回空Token")
                    sendTokenError("获取Token失败")
                }
                
            } catch (e: ApiException) {
                Log.e(TAG, "获取华为推送Token时发生ApiException", e)
                sendTokenError("ApiException: ${e.statusCode} - ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "获取华为推送Token时发生异常", e)
                sendTokenError("异常: ${e.message}")
            }
        }
    }

    override fun getToken(): String? {
        if (currentToken == null) {
            Log.w(TAG, "Token尚未获取，请稍后重试或确保初始化成功")
        }
        return currentToken
    }

    override fun enableNotification() {
        Log.d(TAG, "启用华为推送通知")
        // 华为推送SDK没有专门的启用通知方法，通常在初始化时自动启用
    }

    override fun disableNotification() {
        Log.d(TAG, "禁用华为推送通知")
        // 可以通过删除Token来禁用推送
        scope.launch {
            try {
                HmsInstanceId.getInstance(context).deleteToken("")
                currentToken = null
                Log.i(TAG, "华为推送Token已删除")
            } catch (e: Exception) {
                Log.e(TAG, "删除华为推送Token失败", e)
            }
        }
    }

    override fun setAlias(alias: String) {
        Log.w(TAG, "华为推送不直接支持设置别名，请通过服务端实现")
    }

    override fun setTags(tags: List<String>) {
        Log.w(TAG, "华为推送不直接支持设置标签，请通过服务端实现")
    }

    private fun sendTokenSuccess(token: String) {
        scope.launch(Dispatchers.Main) {
            sendEvent("tokenReceived", mapOf(
                "token" to token,
                "vendor" to "huawei"
            ))
        }
    }

    private fun sendTokenError(errorMessage: String) {
        scope.launch(Dispatchers.Main) {
            sendEvent("tokenError", mapOf(
                "error" to errorMessage,
                "vendor" to "huawei"
            ))
        }
    }
}
