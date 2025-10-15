package com.wxtpush.client.push.vendors

import android.util.Log
import com.hihonor.push.sdk.HonorMessageService
import com.hihonor.push.sdk.HonorPushDataMsg
import com.wxtpush.client.push.PushManager

class HonorMessageService : HonorMessageService() {

    companion object {
        private const val TAG = "HonorMessageService"
    }

    /**
     * 当收到透传消息时调用
     */
    override fun onMessageReceived(msg: HonorPushDataMsg?) {
        super.onMessageReceived(msg)
        if (msg == null) {
            Log.w(TAG, "收到的透传消息为空")
            return
        }
        Log.i(TAG, "收到荣耀透传消息: ${msg.data}")
        
        // 将消息转发给PushManager进行统一处理
        // 注意：这里需要确保PushManager有相应的静态方法
        try {
            val messageData = mapOf(
                "payload" to msg.data,
                "vendor" to "honor"
            )
            // 这里假设PushManager有处理消息的静态方法
            // PushManager.onReceiveMessage(applicationContext, messageData, "honor")
        } catch (e: Exception) {
            Log.e(TAG, "处理透传消息时发生异常", e)
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
        Log.i(TAG, "荣耀推送Token更新: ${token.take(20)}...")

        // 将新Token转发给PushManager进行统一处理
        try {
            // 这里假设PushManager有处理Token的静态方法
            // PushManager.onNewToken(token, "honor")
        } catch (e: Exception) {
            Log.e(TAG, "处理新Token时发生异常", e)
        }
    }
}
