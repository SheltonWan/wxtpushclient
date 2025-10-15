package com.wxtpush.client.push.vendors

import android.util.Log
import java.lang.reflect.Proxy

/**
 * 兼容 Heytap ICallBackResultService 的回调桥接。
 * 优先使用现有的动态代理方案；如需 Binder Stub，可在确认接口可达后切换为显式实现。
 */
object HeytapCallbackBridge {
    private const val TAG = "HeytapCallbackBridge"

    /**
     * 创建 ICallBackResultService 动态代理实例。
     * @param iface ICallBackResultService 接口 Class
     * @param handler 回调处理函数（methodName, args）
     */
    fun createProxy(iface: Class<*>, handler: (String, Array<out Any?>?) -> Unit): Any {
        return Proxy.newProxyInstance(
            iface.classLoader,
            arrayOf(iface)
        ) { _, method, args ->
            try {
                handler(method.name, args)
            } catch (e: Throwable) {
                Log.e(TAG, "回调处理异常: ${e.message}", e)
            }
            null
        }
    }
}
