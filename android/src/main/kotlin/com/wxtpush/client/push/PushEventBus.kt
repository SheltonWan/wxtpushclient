package com.wxtpush.client.push

/**
 * 全局事件总线：用于从与 BasePushService 无直接关联的组件（如厂商系统 Service）
 * 向 Flutter EventChannel 分发事件。
 */
object PushEventBus {
    @Volatile
    var callback: PushEventCallback? = null

    @Synchronized
    fun registerCallback(cb: PushEventCallback) {
        callback = cb
    }

    fun dispatchRaw(eventMap: Map<String, Any>) {
        callback?.invoke(eventMap)
    }

    fun dispatch(event: String, data: Map<String, Any?>) {
        val safeData = data.mapValues { (_, value) ->
            when (value) {
                null -> null
                is String, is Number, is Boolean -> value
                else -> value.toString()
            }
        }
        val eventMap = mapOf<String, Any>(
            "event" to event,
            "data" to safeData
        )
        dispatchRaw(eventMap)
    }
}
