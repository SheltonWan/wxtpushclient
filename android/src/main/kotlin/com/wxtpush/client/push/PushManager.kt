package com.wxtpush.client.push

import android.content.Context
import android.util.Log
import com.wxtpush.client.push.vendors.HuaweiPushService
import com.wxtpush.client.push.vendors.HonorPushService
import com.wxtpush.client.push.vendors.XiaomiPushService
import com.wxtpush.client.push.vendors.OppoPushService
import com.wxtpush.client.push.vendors.VivoPushService
import com.wxtpush.client.push.models.PushToken

typealias PushEventCallback = (Map<String, Any?>) -> Unit

class PushManager(
    private val context: Context,
    private val eventCallback: PushEventCallback
) {
    private val TAG = "WxtpushClient"
    private val vendors = mutableMapOf<PushVendor, BasePushService>()

    companion object {
        @Volatile
        private var INSTANCE: PushManager? = null
        
        fun getInstance(context: Context): PushManager? {
            return INSTANCE
        }
        
        fun setInstance(instance: PushManager) {
            INSTANCE = instance
        }
    }

    fun initializePush(vendor: PushVendor, config: Map<String, Any>) {
        Log.d(TAG, "Initializing push for vendor: ${vendor.id}")
        
        val pushService = when (vendor) {
            PushVendor.HUAWEI -> HuaweiPushService(context, eventCallback)
            PushVendor.HONOR -> HonorPushService(context, eventCallback)
            PushVendor.XIAOMI -> XiaomiPushService(context, eventCallback)
            PushVendor.OPPO -> OppoPushService(context, eventCallback)
            PushVendor.VIVO -> VivoPushService(context, eventCallback)
            PushVendor.APPLE -> throw UnsupportedOperationException("Apple push is handled by iOS platform")
        }
        
        pushService.initialize(config)
        vendors[vendor] = pushService
    }

    fun getToken(vendor: PushVendor): Map<String, Any>? {
        val token = vendors[vendor]?.getToken()
        return if (token != null) {
            PushToken.create(token, vendor.id).toMap()
        } else {
            null
        }
    }

    fun getAllTokens(): List<Map<String, Any>> {
        val tokens = mutableListOf<Map<String, Any>>()
        vendors.forEach { (vendor, service) ->
            val token = service.getToken()
            if (token != null) {
                tokens.add(PushToken.create(token, vendor.id).toMap())
            }
        }
        return tokens
    }

    fun enableNotification() {
        vendors.values.forEach { it.enableNotification() }
    }

    fun disableNotification() {
        vendors.values.forEach { it.disableNotification() }
    }

    fun setAlias(alias: String) {
        vendors.values.forEach { it.setAlias(alias) }
    }

    fun setTags(tags: List<String>) {
        vendors.values.forEach { it.setTags(tags) }
    }

    /**
     * 刷新指定厂商的推送Token
     */
    fun refreshToken(vendor: PushVendor) {
        Log.w(TAG, "Token refresh functionality not implemented for vendor: ${vendor.id}")
        // TODO: 实现各厂商的Token刷新逻辑
    }

    /**
     * 删除指定厂商的推送Token
     */
    fun deleteToken(vendor: PushVendor) {
        Log.w(TAG, "Token deletion functionality not implemented for vendor: ${vendor.id}")
        // TODO: 实现各厂商的Token删除逻辑
    }

    /**
     * 清理所有推送服务
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up all push services")
        vendors.clear()
    }
    
    /**
     * 处理推送消息接收事件
     */
    fun handleMessageReceived(messageData: Map<String, Any>) {
        Log.d(TAG, "收到推送消息: $messageData")
        eventCallback(mapOf(
            "type" to "messageReceived",
            "data" to messageData
        ))
    }
    
    /**
     * 处理推送消息点击事件
     */
    fun handleMessageClicked(messageData: Map<String, Any>) {
        Log.d(TAG, "用户点击推送消息: $messageData")
        eventCallback(mapOf(
            "type" to "messageClicked",
            "data" to messageData
        ))
    }
    
    /**
     * 处理Token更新事件
     */
    fun handleTokenUpdated(token: String, vendor: String) {
        Log.d(TAG, "Token更新: vendor=$vendor, token=$token")
        eventCallback(mapOf(
            "type" to "tokenUpdated",
            "token" to token,
            "vendor" to vendor
        ))
    }
    
    /**
     * 处理错误事件
     */
    fun handleError(error: String, vendor: String) {
        Log.e(TAG, "推送服务错误: vendor=$vendor, error=$error")
        eventCallback(mapOf(
            "type" to "error",
            "error" to error,
            "vendor" to vendor
        ))
    }
}
