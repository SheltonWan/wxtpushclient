package com.wxtpush.client.push

import android.content.Context
import com.wxtpush.client.push.PushEventBus

abstract class BasePushService(
    protected val context: Context,
    protected val eventCallback: PushEventCallback
) {
    init {
        // 注册全局事件回调，供 HuaweiMessageService / 其它独立 Service 使用
        PushEventBus.registerCallback(eventCallback)
    }
    abstract fun initialize(config: Map<String, Any>)
    abstract fun getToken(): String?
    abstract fun enableNotification()
    abstract fun disableNotification()
    abstract fun setAlias(alias: String)
    abstract fun setTags(tags: List<String>)
    
    protected fun sendEvent(event: String, data: Map<String, Any?>) {
        // 确保类型安全的事件数据
        val safeData = data.mapValues { (_, value) ->
            when (value) {
                null -> null
                is String -> value
                is Number -> value
                is Boolean -> value
                else -> value.toString()
            }
        }
        
        val eventMap = mapOf<String, Any>(
            "event" to event,
            "data" to safeData
        )
        
        // 只通过 eventCallback 发送一次
        // PushEventBus 只用于独立的系统 Service（如 HuaweiMessageService）
        eventCallback(eventMap)
    }
}
