package com.wxtpush.client.push.vendors

import android.content.Context
import android.util.Log
import com.hihonor.push.sdk.HonorPushClient
import com.hihonor.push.sdk.HonorPushCallback
import com.wxtpush.client.push.BasePushService
import com.wxtpush.client.push.PushEventCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HonorPushService(
    context: Context,
    eventCallback: PushEventCallback
) : BasePushService(context, eventCallback) {

    companion object {
        private const val TAG = "HonorPushService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentToken: String? = null

    override fun initialize(config: Map<String, Any>) {
        Log.d(TAG, "开始初始化荣耀原生推送服务")

        try {
            // 检查设备品牌
            val deviceBrand = android.os.Build.BRAND.lowercase()
            Log.d(TAG, "设备品牌: $deviceBrand")
            
            if (!deviceBrand.contains("honor") && !deviceBrand.contains("huawei")) {
                Log.w(TAG, "当前设备可能不支持荣耀推送服务，设备品牌: $deviceBrand")
                sendTokenError("设备不支持荣耀推送服务")
                return
            }

            // 初始化荣耀推送SDK
            HonorPushClient.getInstance().init(context, true)
            
            // 延迟一段时间后再获取Token，确保初始化完成
            scope.launch {
                kotlinx.coroutines.delay(2000) // 延迟2秒
                getTokenAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "初始化荣耀推送服务时发生异常", e)
            sendTokenError("初始化异常: ${e.message}")
        }
    }

    private fun getTokenAsync() {
        scope.launch {
            try {
                HonorPushClient.getInstance().getPushToken(object : HonorPushCallback<String> {
                    override fun onSuccess(token: String) {
                        currentToken = token
                        Log.i(TAG, "荣耀原生Token获取成功: ${currentToken?.take(20)}...")
                        sendTokenSuccess(currentToken)
                    }

                    override fun onFailure(resultCode: Int, resultInfo: String) {
                        Log.e(TAG, "获取荣耀原生Token失败: code=$resultCode, msg=$resultInfo")
                        
                        // 根据错误代码提供更详细的错误信息
                        val errorMessage = when (resultCode) {
                            8002008 -> "荣耀推送服务缺失或版本不兼容，请确保设备已安装荣耀推送服务"
                            6003 -> "网络连接错误，请检查网络连接"
                            907135701 -> "应用ID配置错误，请检查agconnect-services.json配置"
                            else -> "获取Token失败: $resultInfo (错误代码: $resultCode)"
                        }
                        
                        sendTokenError(errorMessage)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "获取荣耀原生Token时发生异常", e)
                sendTokenError("获取Token异常: ${e.message}")
            }
        }
    }

    override fun getToken(): String? {
        if (currentToken == null) {
            Log.w(TAG, "Token尚未获取，请稍后重试或确保初始化成功")
            // 尝试异步获取
            getTokenAsync()
        }
        return currentToken
    }

    override fun enableNotification() {
        Log.d(TAG, "启用荣耀推送通知")
        // 荣耀推送SDK没有专门的启用通知方法，通常在初始化时自动启用
    }

    override fun disableNotification() {
        Log.d(TAG, "禁用荣耀推送通知")
        // 荣耀推送SDK没有专门的禁用通知方法
    }

    private fun sendTokenSuccess(token: String?) {
        if (token != null) {
            scope.launch(Dispatchers.Main) {
                sendEvent("tokenReceived", mapOf(
                    "token" to token,
                    "vendor" to "honor"
                ))
            }
        }
    }

    private fun sendTokenError(errorMessage: String) {
        scope.launch(Dispatchers.Main) {
            sendEvent("tokenError", mapOf(
                "error" to errorMessage,
                "vendor" to "honor"
            ))
        }
    }

    override fun setAlias(alias: String) {
        Log.w(TAG, "荣耀推送不支持设置别名")
    }

    override fun setTags(tags: List<String>) {
        Log.w(TAG, "荣耀推送不支持设置标签")
    }
}
