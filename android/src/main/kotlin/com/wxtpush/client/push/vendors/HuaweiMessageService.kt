package com.wxtpush.client.push.vendors

import android.util.Log
import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import com.wxtpush.client.push.PushEventBus

class HuaweiMessageService : HmsMessageService() {

    companion object {
        private const val TAG = "HuaweiMessageService"
    }

    /**
     * 当收到推送消息时调用
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        super.onMessageReceived(remoteMessage)
        if (remoteMessage == null) {
            Log.w(TAG, "收到的推送消息为空")
            return
        }
        
        Log.i(TAG, "收到华为推送消息:")
        Log.i(TAG, "- From: ${remoteMessage.from}")
        Log.i(TAG, "- To: ${remoteMessage.to}")
        Log.i(TAG, "- MessageId: ${remoteMessage.messageId}")
        Log.i(TAG, "- MessageType: ${remoteMessage.messageType}")
        Log.i(TAG, "- TTL: ${remoteMessage.ttl}")
        Log.i(TAG, "- Data: ${remoteMessage.dataOfMap}")
        
        // 处理通知消息
        remoteMessage.notification?.let { notification ->
            Log.i(TAG, "通知消息:")
            Log.i(TAG, "- Title: ${notification.title}")
            Log.i(TAG, "- Body: ${notification.body}")
            Log.i(TAG, "- ImageUrl: ${notification.imageUrl}")
        }
        
        try {
            val dataMap = remoteMessage.dataOfMap ?: emptyMap<String, String>()
            val hasData = dataMap.isNotEmpty()
            val hasNotification = remoteMessage.notification != null
            val channel = when {
                hasData && hasNotification -> "hybrid"
                hasData -> "data"
                hasNotification -> "notification"
                else -> "unknown"
            }
            val messageData = mapOf(
                "from" to remoteMessage.from,
                "to" to remoteMessage.to,
                "messageId" to remoteMessage.messageId,
                "messageType" to remoteMessage.messageType,
                "data" to dataMap,
                "notification" to mapOf(
                    "title" to remoteMessage.notification?.title,
                    "body" to remoteMessage.notification?.body,
                    "imageUrl" to remoteMessage.notification?.imageUrl?.toString()
                ),
                "vendor" to "huawei",
                "createdAt" to System.currentTimeMillis(),
                "channel" to channel
            )
            PushEventBus.dispatch("messageReceived", messageData)
            Log.i(TAG, "处理华为推送消息完成并已分发到 Flutter")
        } catch (e: Exception) {
            Log.e(TAG, "处理华为推送消息时发生异常", e)
        }
    }

    /**
     * 当Token更新时调用
     */
    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        if (token.isNullOrEmpty()) {
            Log.w(TAG, "获取到的新Token为空")
            return
        }
        
        Log.i(TAG, "华为推送Token更新: ${token.take(20)}...")
        
        try {
            val tokenData = mapOf(
                "token" to token,
                "vendor" to "huawei",
                "origin" to "onNewToken",
                "createdAt" to System.currentTimeMillis()
            )
            PushEventBus.dispatch("tokenReceived", tokenData)
            Log.i(TAG, "华为推送Token更新处理完成并已分发到 Flutter")
        } catch (e: Exception) {
            Log.e(TAG, "处理华为推送新Token时发生异常", e)
        }
    }

    /**
     * 当发送消息到服务器失败时调用
     */
    override fun onMessageSent(msgId: String?) {
        super.onMessageSent(msgId)
        Log.i(TAG, "华为推送消息发送成功: $msgId")
    }

    /**
     * 当发送消息失败时调用
     */
    override fun onSendError(msgId: String?, exception: Exception?) {
        super.onSendError(msgId, exception)
        Log.e(TAG, "华为推送消息发送失败: $msgId", exception)
    }
}
