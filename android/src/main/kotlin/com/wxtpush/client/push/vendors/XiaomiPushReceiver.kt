package com.wxtpush.client.push.vendors

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.util.Log

/**
 * 小米推送广播接收器
 * 继承自BroadcastReceiver，用于接收小米推送的各种事件
 */
class XiaomiPushReceiver : BroadcastReceiver() {

    private val TAG = "XiaomiPushReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "收到小米推送广播: ${intent.action}")
        
        try {
            when (intent.action) {
                "com.xiaomi.mipush.RECEIVE_MESSAGE" -> {
                    handleReceiveMessage(context, intent)
                }
                "com.xiaomi.mipush.MESSAGE_ARRIVED" -> {
                    handleMessageArrived(context, intent)
                }
                "com.xiaomi.mipush.ERROR" -> {
                    handleError(context, intent)
                }
                else -> {
                    Log.d(TAG, "未知的推送Action: ${intent.action}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理小米推送广播失败", e)
        }
    }

    private fun handleReceiveMessage(context: Context, intent: Intent) {
        Log.d(TAG, "处理小米推送注册结果")
        
        try {
            // 检查是否有小米推送SDK
            val hasMiPushSDK = try {
                Class.forName("com.xiaomi.mipush.sdk.MiPushCommandMessage")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
            
            if (hasMiPushSDK) {
                // 使用反射处理真实SDK的消息
                handleRealSDKMessage(context, intent)
            } else {
                // 处理模拟消息
                handleMockMessage(context, intent)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "处理注册结果失败", e)
        }
    }

    private fun handleRealSDKMessage(context: Context, intent: Intent) {
        try {
            // 使用反射获取MiPushCommandMessage
            val messageClass = Class.forName("com.xiaomi.mipush.sdk.MiPushCommandMessage")
            val message = intent.getSerializableExtra("key_message")
            
            if (message != null) {
                val getCommandMethod = messageClass.getMethod("getCommand")
                val getResultCodeMethod = messageClass.getMethod("getResultCode")
                val getCommandArgumentsMethod = messageClass.getMethod("getCommandArguments")
                
                val command = getCommandMethod.invoke(message) as? String
                val resultCode = getResultCodeMethod.invoke(message) as? Long ?: -1L
                val arguments = getCommandArgumentsMethod.invoke(message) as? List<String>
                
                Log.d(TAG, "真实SDK - 命令: $command, 结果码: $resultCode")
                
                // 检查是否是注册命令
                if (command == "register" && resultCode == 0L) {
                    val regId = arguments?.getOrNull(0)
                    if (!regId.isNullOrEmpty()) {
                        Log.d(TAG, "真实SDK - 获取到Token: ${regId.take(10)}...")
                        notifyTokenReceived(context, regId)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理真实SDK消息失败", e)
        }
    }

    private fun handleMockMessage(context: Context, intent: Intent) {
        Log.d(TAG, "模拟SDK - 处理模拟注册广播")
        
        // 检查是否是注册命令
        val command = intent.getStringExtra("key_command")
        val resultCode = intent.getLongExtra("key_result_code", -1L)
        val regId = intent.getStringExtra("key_reg_id")
        
        Log.d(TAG, "模拟SDK - 命令: $command, 结果码: $resultCode, regId: ${regId?.take(20)}")
        
        if (command == "register" && resultCode == 0L && !regId.isNullOrEmpty()) {
            Log.d(TAG, "模拟SDK - 注册成功，Token: ${regId.take(10)}...")
            notifyTokenReceived(context, regId)
        } else {
            // 生成默认模拟Token
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver, 
                android.provider.Settings.Secure.ANDROID_ID
            )
            val mockToken = "xiaomi_mock_${deviceId}_${System.currentTimeMillis()}"
            
            Log.d(TAG, "模拟SDK - 生成默认Token: ${mockToken.take(20)}...")
            notifyTokenReceived(context, mockToken)
        }
    }

    private fun handleMessageArrived(context: Context, intent: Intent) {
        Log.d(TAG, "处理消息到达事件")
        
        try {
            // 检查是否有小米推送SDK
            val hasMiPushSDK = try {
                Class.forName("com.xiaomi.mipush.sdk.MiPushMessage")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
            
            if (hasMiPushSDK) {
                handleRealSDKMessageArrived(context, intent)
            } else {
                handleMockMessageArrived(context, intent)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "处理消息到达失败", e)
        }
    }

    private fun handleRealSDKMessageArrived(context: Context, intent: Intent) {
        try {
            val messageClass = Class.forName("com.xiaomi.mipush.sdk.MiPushMessage")
            val message = intent.getSerializableExtra("key_message")
            
            if (message != null) {
                val getContentMethod = messageClass.getMethod("getContent")
                val getTitleMethod = messageClass.getMethod("getTitle")
                val getDescriptionMethod = messageClass.getMethod("getDescription")
                
                val content = getContentMethod.invoke(message) as? String ?: ""
                val title = getTitleMethod.invoke(message) as? String ?: ""
                val description = getDescriptionMethod.invoke(message) as? String ?: ""
                
                Log.d(TAG, "真实SDK - 收到消息: title=$title, content=$content")
                
                notifyMessageReceived(context, title, content, description)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理真实SDK消息到达失败", e)
        }
    }

    private fun handleMockMessageArrived(context: Context, intent: Intent) {
        Log.d(TAG, "模拟SDK - 收到模拟消息")
        val title = "测试消息"
        val content = "这是一条小米推送测试消息"
        val description = "模拟推送消息描述"
        
        notifyMessageReceived(context, title, content, description)
    }

    private fun handleError(context: Context, intent: Intent) {
        Log.e(TAG, "小米推送发生错误")
        
        val error = intent.getStringExtra("key_error") ?: "未知错误"
        Log.e(TAG, "错误信息: $error")
        
        // TODO: 通知错误事件到Flutter层
    }

    private fun notifyTokenReceived(context: Context, token: String) {
        Log.d(TAG, "收到小米推送Token: ${token.take(20)}...")
        // TODO: 将token上报到Flutter层
    }

    private fun notifyMessageReceived(context: Context, title: String, content: String, description: String) {
        Log.d(TAG, "收到小米推送消息: title=$title, content=$content")
        // TODO: 将消息上报到Flutter层
    }
}
