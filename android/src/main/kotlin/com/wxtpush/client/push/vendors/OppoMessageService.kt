package com.wxtpush.client.push.vendors

import android.content.Context
import android.util.Log
import com.wxtpush.client.push.PushManager

/**
 * OPPO/Heytap Push 消息接收服务包装
 * 说明：此类在无 Heytap SDK 时不会被系统实例化。
 * 若集成 SDK，将由其框架通过反射调用相同签名的方法。
 */
@Suppress("unused")
class OppoMessageService {
    private val TAG = "OppoMessageService"

    // 兼容旧 SDK：com.coloros.mcssdk.callback.PushAdapter#processMessage(Context, DataMessage)
    fun processMessage(context: Context?, dataMessage: Any?) {
        Log.d(TAG, "processMessage compat: $dataMessage")
        // 尝试转发基础信息
        forwardMessage(context, mapOf("vendor" to "oppo", "raw" to (dataMessage?.toString() ?: "")))
    }

    // 兼容新 SDK：可带 pushType
    fun processMessage(context: Context?, dataMessage: Any?, pushType: Int) {
        Log.d(TAG, "processMessage type=$pushType, msg=$dataMessage")
        forwardMessage(context, mapOf("vendor" to "oppo", "type" to pushType, "raw" to (dataMessage?.toString() ?: "")))
    }

    private fun forwardMessage(context: Context?, messageData: Map<String, Any>) {
        try {
            if (context == null) {
                Log.w(TAG, "forward message skipped: context is null")
                return
            }
            val pm = PushManager.getInstance(context)
            pm?.handleMessageReceived(messageData)
        } catch (e: Exception) {
            Log.w(TAG, "forward message failed: ${e.message}")
        }
    }
}
