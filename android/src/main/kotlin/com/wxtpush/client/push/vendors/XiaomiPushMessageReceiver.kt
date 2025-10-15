package com.wxtpush.client.push.vendors

import android.content.Context
import android.util.Log
import com.xiaomi.mipush.sdk.PushMessageReceiver
import com.xiaomi.mipush.sdk.MiPushMessage
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.ErrorCode

/**
 * 小米推送消息接收器 - 完全封装实现
 * 直接继承小米SDK的PushMessageReceiver，在插件内部处理所有推送逻辑
 * 客户端应用无需任何配置或继承
 */
class XiaomiPushMessageReceiver : PushMessageReceiver() {
    
    companion object {
        private const val TAG = "XiaomiPushReceiver"
    }

    override fun onReceiveRegisterResult(context: Context?, message: MiPushCommandMessage?) {
        Log.d(TAG, "onReceiveRegisterResult: $message")
        
        if (context == null || message == null) return
        
        if (message.resultCode == ErrorCode.SUCCESS.toLong()) {
            val regId = message.commandArguments?.get(0)
            if (!regId.isNullOrEmpty()) {
                Log.i(TAG, "小米推送注册成功，RegId: ${regId.take(10)}...")
                
                // 通知插件层Token获取成功
                notifyPluginTokenReceived(context, regId)
            }
        } else {
            Log.e(TAG, "小米推送注册失败，错误码: ${message.resultCode}")
            notifyPluginError(context, "小米推送注册失败: ${message.resultCode}")
        }
    }

    override fun onReceivePassThroughMessage(context: Context?, message: MiPushMessage?) {
        Log.d(TAG, "onReceivePassThroughMessage: $message")
        
        if (context == null || message == null) return
        
        // 处理透传消息
        val content = message.content ?: ""
        val title = message.title ?: ""
        
        Log.i(TAG, "收到小米透传消息: title=$title, content=$content")
        
        // 通知插件层收到消息
        notifyPluginMessageReceived(context, "xiaomi", title, content, message.extra)
    }

    override fun onNotificationMessageClicked(context: Context?, message: MiPushMessage?) {
        Log.d(TAG, "onNotificationMessageClicked: $message")
        
        if (context == null || message == null) return
        
        // 处理通知点击
        val title = message.title ?: ""
        val content = message.content ?: ""
        
        Log.i(TAG, "小米推送通知被点击: title=$title, content=$content")
        
        // 通知插件层通知被点击
        notifyPluginNotificationClicked(context, "xiaomi", title, content, message.extra)
    }

    override fun onNotificationMessageArrived(context: Context?, message: MiPushMessage?) {
        Log.d(TAG, "onNotificationMessageArrived: $message")
        
        if (context == null || message == null) return
        
        // 处理通知到达
        val title = message.title ?: ""
        val content = message.content ?: ""
        
        Log.i(TAG, "收到小米推送通知: title=$title, content=$content")
        
        // 通知插件层收到通知
        notifyPluginNotificationReceived(context, "xiaomi", title, content, message.extra)
    }

    override fun onCommandResult(context: Context?, message: MiPushCommandMessage?) {
        Log.d(TAG, "onCommandResult: $message")
        
        if (context == null || message == null) return
        
        // 处理命令结果
        Log.i(TAG, "小米推送命令结果: ${message.command}, 结果码: ${message.resultCode}")
    }

    override fun onReceiveMessage(context: Context?, message: MiPushMessage?) {
        Log.d(TAG, "onReceiveMessage: $message")
        super.onReceiveMessage(context, message)
    }
    
    /**
     * 通知插件层Token获取成功
     */
    private fun notifyPluginTokenReceived(context: Context, token: String) {
        try {
            Log.d(TAG, "向插件层报告Token: ${token.take(10)}...")
            
            val intent = android.content.Intent("com.wxtpush.client.TOKEN_RECEIVED")
            intent.putExtra("vendor", "xiaomi")
            intent.putExtra("token", token)
            context.sendBroadcast(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "通知插件层Token失败", e)
        }
    }

    /**
     * 通知插件层收到透传消息
     */
    private fun notifyPluginMessageReceived(context: Context, vendor: String, title: String, content: String, extra: Map<String, String>?) {
        try {
            Log.d(TAG, "向插件层报告透传消息: title=$title")
            
            val intent = android.content.Intent("com.wxtpush.client.MESSAGE_RECEIVED")
            intent.putExtra("vendor", vendor)
            intent.putExtra("title", title)
            intent.putExtra("content", content)
            extra?.forEach { (key, value) ->
                intent.putExtra("extra_$key", value)
            }
            context.sendBroadcast(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "通知插件层消息失败", e)
        }
    }

    /**
     * 通知插件层收到通知
     */
    private fun notifyPluginNotificationReceived(context: Context, vendor: String, title: String, content: String, extra: Map<String, String>?) {
        try {
            Log.d(TAG, "向插件层报告通知到达: title=$title")
            
            val intent = android.content.Intent("com.wxtpush.client.NOTIFICATION_RECEIVED")
            intent.putExtra("vendor", vendor)
            intent.putExtra("title", title)
            intent.putExtra("content", content)
            extra?.forEach { (key, value) ->
                intent.putExtra("extra_$key", value)
            }
            context.sendBroadcast(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "通知插件层通知失败", e)
        }
    }

    /**
     * 通知插件层通知被点击
     */
    private fun notifyPluginNotificationClicked(context: Context, vendor: String, title: String, content: String, extra: Map<String, String>?) {
        try {
            Log.d(TAG, "向插件层报告通知点击: title=$title")
            
            val intent = android.content.Intent("com.wxtpush.client.NOTIFICATION_CLICKED")
            intent.putExtra("vendor", vendor)
            intent.putExtra("title", title)
            intent.putExtra("content", content)
            extra?.forEach { (key, value) ->
                intent.putExtra("extra_$key", value)
            }
            context.sendBroadcast(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "通知插件层点击失败", e)
        }
    }

    /**
     * 通知插件层发生错误
     */
    private fun notifyPluginError(context: Context, error: String) {
        try {
            Log.d(TAG, "向插件层报告错误: $error")
            
            val intent = android.content.Intent("com.wxtpush.client.PUSH_ERROR")
            intent.putExtra("vendor", "xiaomi")
            intent.putExtra("error", error)
            context.sendBroadcast(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "通知插件层错误失败", e)
        }
    }
}
