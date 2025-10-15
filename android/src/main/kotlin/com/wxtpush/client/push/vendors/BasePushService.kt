package com.wxtpush.client.push.vendors

import android.content.Context
import com.wxtpush.client.push.PushEventCallback

abstract class BasePushService(
    protected val context: Context,
    protected val eventCallback: PushEventCallback
) {
    abstract fun initialize(config: Map<String, Any>)
    abstract fun getToken(): String?
    abstract fun enableNotification()
    abstract fun disableNotification()
    abstract fun setAlias(alias: String)
    abstract fun setTags(tags: List<String>)
    
    protected fun sendEvent(event: Map<String, Any?>) {
        eventCallback(event)
    }
}
