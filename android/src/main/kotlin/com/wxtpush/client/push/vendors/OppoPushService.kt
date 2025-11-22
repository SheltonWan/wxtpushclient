package com.wxtpush.client.push.vendors

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.wxtpush.client.push.BasePushService
import com.wxtpush.client.push.PushEventCallback
import com.wxtpush.client.push.BadgeHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.reflect.Proxy
import java.security.MessageDigest
import android.content.pm.Signature
import android.content.pm.PackageManager
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Parcel

/**
 * OPPO推送服务实现
 * 支持Heytap推送SDK和模拟模式的自动切换
 */
class OppoPushService(
    context: Context,
    eventCallback: PushEventCallback
) : BasePushService(context, eventCallback) {
    
    companion object {
        private const val TAG = "OppoPushService"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentToken: String? = null
    private val USE_REAL_SDK = checkSDKAvailable()
    private var registrationTimeout: Job? = null
    private var registerTimeoutMs: Long = 8000 // 默认8秒（可通过配置缩短到5秒等）
    private var demoCompat: Boolean = false // 是否启用与官方demo一致的极简流程
    
    /**
     * 获取OPPO推送错误码对应的错误信息
     */
    private fun getOppoErrorMessage(code: Int): String {
        return when (code) {
            0 -> "成功"
            -1 -> "系统错误"
            -2 -> "客户端错误"
            -3 -> "应用信息错误"
            -4 -> "TOKEN信息错误"
            -5 -> "应用KEY错误"
            -6 -> "应用SECRET错误"
            -7 -> "应用包名错误"
            -8 -> "应用签名错误"
            -9 -> "注册信息错误"
            -10 -> "推送权限被关闭"
            -11 -> "设备不支持推送功能"
            -12 -> "推送服务被禁用"
            -13 -> "网络连接失败"
            -14 -> "服务器错误"
            -15 -> "参数错误"
            -16 -> "签名验证失败"
            -17 -> "推送服务初始化失败"
            -18 -> "获取推送Token失败"
            -19 -> "推送消息发送失败"
            -20 -> "推送消息接收失败"
            -100 -> "应用未在白名单中"
            -200 -> "设备未连接到推送服务"
            -300 -> "推送服务版本过低"
            else -> "未知错误 (code=$code)"
        }
    }
    
    /**
     * 启动注册超时监控
     */
    private fun startRegistrationTimeout() {
        registrationTimeout?.cancel()
        registrationTimeout = scope.launch {
            delay(registerTimeoutMs) // 使用可配置超时
            if (currentToken == null) {
                val sec = registerTimeoutMs / 1000
                Log.w(TAG, "⏰ OPPO推送注册超时，未在${sec}秒内收到回调")
                sendTokenError("注册超时：${sec}秒内未收到SDK回调响应，可能原因：1.网络连接问题 2.应用配置错误 3.设备不支持推送")
            }
        }
    }
    
    /**
     * 取消注册超时监控
     */
    private fun cancelRegistrationTimeout() {
        registrationTimeout?.cancel()
        registrationTimeout = null
    }
    
    /**
     * 检测OPPO/Heytap推送SDK是否可用
     */
    private fun checkSDKAvailable(): Boolean {
        return try {
            Class.forName("com.heytap.msp.push.HeytapPushManager")
            Log.d(TAG, "检测到Heytap推送SDK，将使用真实实现")
            true
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "未检测到Heytap推送SDK，将使用模拟实现")
            false
        }
    }
    
    override fun initialize(config: Map<String, Any>) {
        try {
            Log.d(TAG, "开始初始化OPPO推送服务")
            
            val appId = config["appId"] as? String
            val appKey = config["appKey"] as? String
            val appSecret = config["appSecret"] as? String
            // demo 兼容模式
            demoCompat = (config["demoCompat"] as? Boolean) == true
            // 可选：自定义注册超时（毫秒）
            (config["registerTimeoutMs"] as? Number)?.toLong()?.let { customMs ->
                if (customMs in 1000..20000) { // 合理范围：1s~20s
                    registerTimeoutMs = customMs
                }
            }
            
            if (appKey.isNullOrEmpty() || appSecret.isNullOrEmpty()) {
                Log.e(TAG, "OPPO推送配置参数缺失")
                sendTokenError("OPPO推送配置参数缺失: appKey或appSecret为空")
                return
            }
            
            Log.d(TAG, "OPPO推送配置: appId=$appId, appKey=${appKey.take(8)}***, appSecret=${appSecret.take(8)}***, timeout=${registerTimeoutMs}ms, demoCompat=$demoCompat")
            
            // 获取Manifest中的配置作为备选
            val manifestConfig = getManifestConfig()
            val finalAppKey = appKey.ifEmpty { manifestConfig["appKey"] } ?: ""
            val finalAppSecret = appSecret.ifEmpty { manifestConfig["appSecret"] } ?: ""
            
            if (finalAppKey.isEmpty() || finalAppSecret.isEmpty()) {
                Log.e(TAG, "OPPO推送配置不完整")
                sendTokenError("OPPO推送配置不完整")
                return
            }
            
            if (USE_REAL_SDK) {
                // 检查设备是否支持OPPO推送
                if (isOppoDevice() || isColorOSDevice()) {
                    initializeRealSDK(appId, finalAppKey, finalAppSecret)
                } else {
                    Log.w(TAG, "非OPPO/OnePlus设备，使用模拟实现")
                    initializeMockSDK(appId, finalAppKey, finalAppSecret)
                }
            } else {
                initializeMockSDK(appId, finalAppKey, finalAppSecret)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "OPPO推送初始化失败", e)
            sendTokenError("OPPO推送初始化失败: ${e.message}")
        }
    }
    
    private fun initializeRealSDK(appId: String?, appKey: String, appSecret: String) {
        Log.d(TAG, "使用真实Heytap推送SDK初始化")
        
        scope.launch {
            try {
                // 打印关键运行时信息，便于排查包名/签名/元数据是否一致
                logRuntimeConfig(appId, appKey, appSecret)

                val heytapClass = Class.forName("com.heytap.msp.push.HeytapPushManager")
                Log.d(TAG, "✅ HeytapPushManager 类加载成功")
                
                // 详细检查所有可用方法
                logAvailableMethods(heytapClass)
                
                // 检查推送服务是否支持
                val isSupportPushMethod = heytapClass.getMethod("isSupportPush", Context::class.java)
                val isSupported = isSupportPushMethod.invoke(null, context) as? Boolean ?: false
                Log.d(TAG, "📋 isSupportPush 检查结果: $isSupported")
                
                if (!isSupported) {
                    Log.w(TAG, "❌ 当前设备不支持Heytap推送服务")
                    // 进一步诊断为什么不支持
                    performDeepDiagnostics()
                    // 降级到模拟实现
                    initializeMockSDK(appId, appKey, appSecret)
                    return@launch
                }
                
                Log.d(TAG, "✅ 设备支持Heytap推送，继续初始化...")

                // 先设置 appKey/appSecret（部分SDK/ROM要求在 init 之前设置）
                try {
                    val setKeySecret = heytapClass.getMethod("setAppKeySecret", String::class.java, String::class.java)
                    setKeySecret.invoke(null, appKey, appSecret)
                    Log.d(TAG, "✅ setAppKeySecret 调用成功（pre-init）")
                } catch (e: Exception) {
                    Log.d(TAG, "⚠️ setAppKeySecret 不可用或调用失败: ${e.message}")
                }

                // 创建注册回调（兼容不同SDK包名）并尽早设置，以便接收状态回调
                val callbackInterface = resolveCallbackInterface()
                Log.d(TAG, "✅ 回调接口解析成功: ${callbackInterface.name}")
                // 优先使用 Binder 支撑的回调；失败则内部自动回退
                val callbackProxy = createBinderBackedCallback(callbackInterface)
                
                // 提前设置回调，部分 ROM 要求在 init 前完成回调绑定
                Handler(Looper.getMainLooper()).post {
                    try {
                        val setCallback = heytapClass.getMethod("setPushCallback", callbackInterface)
                        setCallback.invoke(null, callbackProxy)
                        Log.d(TAG, "✅ setPushCallback 设置成功")
                        try {
                            val getCb = heytapClass.getMethod("getPushCallback")
                            val cb = getCb.invoke(null)
                            Log.d(TAG, "📎 当前已设置的回调对象: ${cb?.javaClass?.name}")
                        } catch (_: Exception) {}
                    } catch (e: Exception) {
                        Log.d(TAG, "⚠️ setPushCallback 不可用或调用失败: ${e.message}")
                    }
                }

                // 初始化推送服务（更详细的调试）
                try {
                    Log.d(TAG, "🔄 尝试调用 HeytapPushManager.init...")
                    val initMethod = heytapClass.getMethod("init", Context::class.java, Boolean::class.javaPrimitiveType)
                    initMethod.invoke(null, context, true)
                    Log.d(TAG, "✅ HeytapPushManager.init 调用成功")
                } catch (e: NoSuchMethodException) {
                    Log.d(TAG, "⚠️ init方法不存在，跳过初始化步骤")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ init方法调用失败", e)
                    throw e
                }

                // 某些 ROM 可能要求 init 之后再次设置 key/secret，冗余设置一次以规避边界问题
                try {
                    val setKeySecret = heytapClass.getMethod("setAppKeySecret", String::class.java, String::class.java)
                    setKeySecret.invoke(null, appKey, appSecret)
                    Log.d(TAG, "✅ setAppKeySecret 调用成功（post-init）")
                } catch (e: Exception) {
                    Log.d(TAG, "⚠️ setAppKeySecret (post-init) 调用失败: ${e.message}")
                }

                // 输出 SDK/Push 版本与接收 Action 供排查
                try {
                    val getSDKName = heytapClass.getMethod("getSDKVersionName")
                    val getSDKCode = heytapClass.getMethod("getSDKVersionCode")
                    val getPushName = heytapClass.getMethod("getPushVersionName")
                    val getPushCode = heytapClass.getMethod("getPushVersionCode")
                    Log.d(TAG, "SDK Version: ${getSDKName.invoke(null)} (${getSDKCode.invoke(null)})")
                    Log.d(TAG, "Push Service Version: ${getPushName.invoke(null)} (${getPushCode.invoke(null)})")
                } catch (_: Exception) {}
                try {
                    val recvAction = heytapClass.getMethod("getReceiveSdkAction", Context::class.java).invoke(null, context) as? String
                    if (!recvAction.isNullOrEmpty()) Log.d(TAG, "Receive SDK Action: $recvAction")
                } catch (_: Exception) {}

                // 设置回调（已提前）、激活与权限

                // 在主线程激活与恢复推送（部分 ROM 对主线程更敏感）
                Handler(Looper.getMainLooper()).post {
                    try {
                        val activeMethod = heytapClass.getMethod("active", Context::class.java)
                        activeMethod.invoke(null, context)
                        Log.d(TAG, "✅ active 调用成功")
                    } catch (e: Exception) {
                        Log.d(TAG, "⚠️ active 方法不可用或调用失败: ${e.message}")
                    }

                    try {
                        Log.d(TAG, "🔄 尝试调用 resumePush() 无参...")
                        val resumeMethod = heytapClass.getMethod("resumePush")
                        resumeMethod.invoke(null)
                        Log.d(TAG, "✅ resumePush() 调用成功")
                    } catch (e: NoSuchMethodException) {
                        Log.d(TAG, "⚠️ resumePush() 方法不存在")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ resumePush() 调用失败", e)
                    }

                    // 主动触发一次通知权限请求（部分机型需要用户授权）
                    try {
                        val reqPerm = heytapClass.getMethod("requestNotificationPermission")
                        reqPerm.invoke(null)
                        Log.d(TAG, "📣 已调用 requestNotificationPermission()")
                    } catch (_: NoSuchMethodException) {
                        // 忽略
                    } catch (e: Exception) {
                        Log.d(TAG, "requestNotificationPermission 调用失败: ${e.message}")
                    }

                    // 记录 MCS 包名（便于确认实际对接服务）
                    try {
                        val pkgMethod = heytapClass.getMethod("getMcsPackageName", Context::class.java)
                        val mcsPkg = pkgMethod.invoke(null, context) as? String
                        if (!mcsPkg.isNullOrEmpty()) Log.d(TAG, "🧩 MCS Service 包名: $mcsPkg")
                    } catch (_: Exception) {}

                    // 动态监听 SDK 广播（如包含注册结果等）
                    try {
                        val action = heytapClass.getMethod("getReceiveSdkAction", Context::class.java).invoke(null, context) as? String
                        if (!action.isNullOrEmpty()) {
                            val filter = IntentFilter(action).apply { priority = 1000 }
                            context.registerReceiver(object: BroadcastReceiver() {
                                override fun onReceive(ctx: Context?, intent: Intent?) {
                                    try {
                                        Log.d(TAG, "📡 收到 Heytap SDK 广播: ${intent?.action}, extras=${intent?.extras}")
                                        val extras = intent?.extras
                                        var rid: String? = null
                                        // 常见键名尝试
                                        val candidateKeys = listOf("registerID","registerId","register_id","rid","token","registration_id")
                                        for (k in candidateKeys) {
                                            val v = extras?.get(k)?.toString()
                                            if (!v.isNullOrEmpty()) { rid = v; break }
                                        }
                                        // 枚举所有键，打印出来辅助诊断
                                        extras?.keySet()?.forEach { key ->
                                            val v = extras.get(key)
                                            Log.d(TAG, "📦 广播 extra: $key = $v")
                                        }
                                        // 如存在 JSON 字符串，尝试解析其中的 registerId
                                        if (rid.isNullOrEmpty()) {
                                            val possibleJsonKeys = listOf("message","content","data")
                                            for (jk in possibleJsonKeys) {
                                                val raw = extras?.get(jk)?.toString()
                                                if (!raw.isNullOrEmpty()) {
                                                    try {
                                                        val json = org.json.JSONObject(raw)
                                                        val jKeys = listOf("registerID","registerId","register_id","rid","token","registration_id")
                                                        for (kk in jKeys) {
                                                            if (json.has(kk)) { rid = json.optString(kk); break }
                                                        }
                                                    } catch (_: Throwable) { /* 不是 JSON 忽略 */ }
                                                }
                                                if (!rid.isNullOrEmpty()) break
                                            }
                                        }
                                        if (!rid.isNullOrEmpty()) {
                                            Log.d(TAG, "📡 广播中提取到 RegisterID: ${rid.take(12)}...")
                                            currentToken = rid
                                            cancelRegistrationTimeout()
                                            sendTokenSuccess(rid)
                                        }
                                    } catch (e: Exception) {
                                        Log.d(TAG, "处理 SDK 广播异常: ${e.message}")
                                    }
                                }
                            }, filter)
                            Log.d(TAG, "📡 已注册 SDK 广播监听: $action")
                        }
                    } catch (_: Exception) {}
                }

                if (!demoCompat) {
                    // 检查当前推送与通知状态（此时回调已设置）
                    checkCurrentPushStatus(heytapClass)
                    // 辅助：查询应用通知总开关（有回调）
                    try { queryAppNotificationSwitch(heytapClass) } catch (_: Exception) {}
                    // 检查是否已有Token
                    checkExistingToken(heytapClass)
                    // 注册推送（增强版本）
                    performEnhancedRegistration(heytapClass, appId, appKey, appSecret, callbackProxy, callbackInterface)
                } else {
                    // demo 兼容模式：极简序列，尽快返回
                    performDemoRegistration(heytapClass, appId, appKey, appSecret, callbackProxy, callbackInterface)
                }
                
            } catch (e: Exception) {
                cancelRegistrationTimeout() // 取消超时监控
                Log.e(TAG, "❌ 真实SDK初始化失败，降级到模拟模式", e)
                initializeMockSDK(appId, appKey, appSecret)
            }
        }
    }

    /**
     * 解析 Heytap 回调接口，兼容不同 SDK 命名空间。
     */
    private fun resolveCallbackInterface(): Class<*> {
        val candidates = listOf(
            "com.heytap.msp.push.callback.ICallBackResultService", // MSP 新版
            "com.coloros.mcssdk.callback.ICallBackResultService"   // 旧版 ColorOS
        )
        for (name in candidates) {
            try {
                val cls = Class.forName(name)
                Log.d(TAG, "使用回调接口: $name")
                return cls
            } catch (_: ClassNotFoundException) {
                // try next
            }
        }
        // 默认仍抛出，以便外层捕获并降级
        throw ClassNotFoundException("未找到可用的 ICallBackResultService 接口（已尝试: ${candidates.joinToString()}})")
    }

    /**
     * 检查HeytapPushManager所有可用方法
     */
    private fun logAvailableMethods(heytapClass: Class<*>) {
        try {
            Log.d(TAG, "========== HeytapPushManager 可用方法列表 ==========")
            val methods = heytapClass.declaredMethods
            methods.forEach { method ->
                val params = method.parameterTypes.joinToString(", ") { it.simpleName }
                Log.d(TAG, "方法: ${method.name}($params) -> ${method.returnType.simpleName}")
            }
            Log.d(TAG, "========== 总计 ${methods.size} 个方法 ==========")
        } catch (e: Exception) {
            Log.w(TAG, "获取方法列表失败: ${e.message}")
        }
    }

    /**
     * 执行深度诊断：为什么设备不支持推送
     */
    private fun performDeepDiagnostics() {
        Log.d(TAG, "========== 深度诊断：设备不支持推送的原因 ==========")
        
        // 1. 检查ROM信息
        Log.d(TAG, "ROM信息:")
        Log.d(TAG, "  Brand: ${android.os.Build.BRAND}")
        Log.d(TAG, "  Manufacturer: ${android.os.Build.MANUFACTURER}")
        Log.d(TAG, "  Model: ${android.os.Build.MODEL}")
        Log.d(TAG, "  Display: ${android.os.Build.DISPLAY}")
        Log.d(TAG, "  Product: ${android.os.Build.PRODUCT}")
        Log.d(TAG, "  Device: ${android.os.Build.DEVICE}")
        Log.d(TAG, "  OS Version: ${android.os.Build.VERSION.RELEASE}")
        Log.d(TAG, "  SDK Version: ${android.os.Build.VERSION.SDK_INT}")
        
        // 2. 检查系统属性
        try {
            val systemProperties = listOf(
                "ro.build.version.opporom",
                "ro.build.version.coloros",
                "ro.vendor.oplus.rom.version",
                "ro.oppo.theme.version",
                "ro.build.display.ota"
            )
            systemProperties.forEach { prop ->
                try {
                    // 使用反射访问系统属性
                    val systemPropertiesClass = Class.forName("android.os.SystemProperties")
                    val getMethod = systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
                    val value = getMethod.invoke(null, prop, "未找到") as String
                    Log.d(TAG, "  系统属性 $prop: $value")
                } catch (e: Exception) {
                    Log.d(TAG, "  系统属性 $prop: 读取失败")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "无法读取系统属性: ${e.message}")
        }
        
        // 3. 检查推送服务包是否存在
        checkPushServicePackages()
        
        Log.d(TAG, "========== 诊断完成 ==========")
    }

    /**
     * 检查系统中的推送服务包
     */
    private fun checkPushServicePackages() {
        Log.d(TAG, "检查推送服务包:")
        val pushPackages = listOf(
            "com.heytap.mcs",
            "com.coloros.mcs",
            "com.oppo.mcs",
            "com.heytap.msp",
            "com.coloros.mcssdk"
        )
        
        val pm = context.packageManager
        pushPackages.forEach { packageName ->
            try {
                val info = pm.getPackageInfo(packageName, 0)
                Log.d(TAG, "  ✅ $packageName 已安装 (版本: ${info.versionName})")
            } catch (e: Exception) {
                Log.d(TAG, "  ❌ $packageName 未安装")
            }
        }
    }

    /**
     * 查询应用通知总开关状态并通过回调打印，用于辅助定位通知被关闭导致的无回调问题。
     */
    private fun queryAppNotificationSwitch(heytapClass: Class<*>) {
        try {
            val callbackName = "com.heytap.msp.push.callback.IGetAppNotificationCallBackService"
            val cbInterface = try {
                Class.forName(callbackName)
            } catch (_: ClassNotFoundException) {
                Class.forName("com.coloros.mcssdk.callback.IGetAppNotificationCallBackService")
            }

            val cbProxy = Proxy.newProxyInstance(cbInterface.classLoader, arrayOf(cbInterface)) { _, method, args ->
                val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                if (method.name == "onGetAppNotificationSwitch" && args != null && args.isNotEmpty()) {
                    val open = args[0] as? Boolean
                    Log.d(TAG, "[$ts] 🔐 应用通知开关: ${open}")
                    if (open == false) {
                        try {
                            val openSettings = heytapClass.getMethod("openNotificationSettings")
                            openSettings.invoke(null)
                            Log.d(TAG, "[$ts] 📣 已调用 openNotificationSettings() 引导开启通知")
                        } catch (_: Exception) {}
                    }
                } else {
                    Log.d(TAG, "[$ts] 🔐 通知开关回调方法: ${method.name}, args=${args?.toList()}")
                }
                null
            }

            val method = heytapClass.getMethod("getAppNotificationSwitch", cbInterface)
            method.invoke(null, cbProxy)
            Log.d(TAG, "已请求应用通知开关状态（getAppNotificationSwitch）")
        } catch (e: Exception) {
            Log.d(TAG, "查询应用通知开关失败: ${e.message}")
        }
    }

    /**
     * 检查当前推送状态
     */
    private fun checkCurrentPushStatus(heytapClass: Class<*>) {
        try {
            Log.d(TAG, "========== 当前推送状态检查 ==========")
            
            // 尝试获取推送状态（无参版本触发回调）
            try {
                val getPushStatusMethod = heytapClass.getMethod("getPushStatus")
                getPushStatusMethod.invoke(null)
                Log.d(TAG, "已请求推送状态检查（getPushStatus 无参）")
            } catch (e: NoSuchMethodException) {
                Log.d(TAG, "getPushStatus() 方法不存在，跳过")
            } catch (e: Exception) {
                Log.d(TAG, "请求推送状态失败: ${e.message}")
            }

            // 检查通知状态（优先无参，其次 JSONObject 版本）
            try {
                val method = heytapClass.getMethod("getNotificationStatus")
                method.invoke(null)
                Log.d(TAG, "已请求通知状态检查（getNotificationStatus 无参）")
            } catch (e: NoSuchMethodException) {
                try {
                    val jsonClass = Class.forName("org.json.JSONObject")
                    val jsonObj = jsonClass.getConstructor().newInstance()
                    val putMethod = jsonClass.getMethod("put", String::class.java, Any::class.java)
                    putMethod.invoke(jsonObj, "from_sdk", "wxtpush_client")
                    val method2 = heytapClass.getMethod("getNotificationStatus", jsonClass)
                    method2.invoke(null, jsonObj)
                    Log.d(TAG, "已请求通知状态检查（getNotificationStatus JSONObject）")
                } catch (ex: Exception) {
                    Log.d(TAG, "无法请求通知状态: ${ex.message}")
                }
            } catch (e: Exception) {
                Log.d(TAG, "无法请求通知状态: ${e.message}")
            }
            
            Log.d(TAG, "========== 状态检查完成 ==========")
        } catch (e: Exception) {
            Log.w(TAG, "推送状态检查失败: ${e.message}")
        }
    }

    /**
     * 检查现有Token
     */
    private fun checkExistingToken(heytapClass: Class<*>) {
        try {
            Log.d(TAG, "🔍 检查是否已有现有Token...")
            
            val getRegisterIdMethod = try {
                heytapClass.getMethod("getRegisterID", Context::class.java)
            } catch (e: NoSuchMethodException) {
                heytapClass.getMethod("getRegisterID")
            }
            
            val existingToken = if (getRegisterIdMethod.parameterCount > 0) {
                getRegisterIdMethod.invoke(null, context) as? String
            } else {
                getRegisterIdMethod.invoke(null) as? String
            }
            
            if (!existingToken.isNullOrEmpty()) {
                Log.d(TAG, "✅ 发现现有Token: ${existingToken.take(12)}...")
                currentToken = existingToken
                // 直接返回现有Token，但仍继续注册流程以确保状态同步
                sendTokenSuccess(existingToken)
            } else {
                Log.d(TAG, "❌ 未发现现有Token，需要重新注册")
            }
        } catch (e: Exception) {
            Log.d(TAG, "检查现有Token失败: ${e.message}")
        }
    }

    /**
     * 创建 Binder 支撑的回调实现：
     * - 通过反射读取 ICallBackResultService.Stub 的常量，构造自定义 Binder 覆盖 onTransact；
     * - 确保 SDK 以 AIDL/Binder 方式回调 onRegister 等跨进程方法时能够正确到达；
     * - 若构建失败，则回退到动态代理实现。
     */
    private fun createBinderBackedCallback(callbackInterface: Class<*>): Any {
        return try {
            // 1) 优先从接口的嵌套类中查找 Stub（更稳妥的方式，避免类加载器差异）
            var stubClass: Class<*>? = null
            try {
                val nested = callbackInterface.declaredClasses
                nested?.firstOrNull { it.simpleName == "Stub" }?.let {
                    stubClass = it
                    Log.d(TAG, "Binder Stub 命中(嵌套类): ${it.name}")
                }
            } catch (_: Throwable) {}

            // 2) 兜底：兼容 heytap 与 coloros 命名空间的 Class.forName
            if (stubClass == null) {
                val stubCandidates = listOf(
                    "${callbackInterface.name}$" + "Stub",
                    (callbackInterface.name.replace("com.heytap.msp.push", "com.coloros.mcssdk")) + "$" + "Stub"
                )
                var lastErr: Throwable? = null
                for (cn in stubCandidates) {
                    try {
                        stubClass = Class.forName(cn)
                        Log.d(TAG, "Binder Stub 命中: $cn")
                        break
                    } catch (t: Throwable) {
                        lastErr = t
                    }
                }
                if (stubClass == null) throw lastErr ?: ClassNotFoundException("No Stub found for ${callbackInterface.name}")
            }

            // 将非空的 stubClass 赋值给不可变引用，避免闭包中 smart cast 失败
            val stubKlass = stubClass!!

            val descriptor = try {
                val f = stubKlass.getDeclaredField("DESCRIPTOR")
                if (!f.isAccessible) f.isAccessible = true
                f.get(null) as? String
            } catch (_: Throwable) { null }
            if (descriptor.isNullOrEmpty()) throw IllegalStateException("No DESCRIPTOR on Stub")

            fun tx(name: String): Int? = try {
                val f = stubKlass.getDeclaredField("TRANSACTION_" + name)
                if (!f.isAccessible) f.isAccessible = true
                f.getInt(null)
            } catch (_: Throwable) { null }
            val txOnRegister = tx("onRegister")
            val txOnUnRegister = tx("onUnRegister")
            val txOnSetPushTime = tx("onSetPushTime")
            val txOnGetPushStatus = tx("onGetPushStatus")
            val txOnGetNotificationStatus = tx("onGetNotificationStatus")

            val binder = object : Binder() {
                override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
                    return try {
                        data.enforceInterface(descriptor)
                        when (code) {
                            txOnRegister -> {
                                val c = data.readInt()
                                val rid = data.readString()
                                val pkg = data.readString()
                                handleCallback("onRegister", arrayOf(c, rid, pkg))
                                reply?.writeNoException()
                                true
                            }
                            txOnUnRegister -> {
                                val c = data.readInt()
                                val pkg = data.readString()
                                handleCallback("onUnRegister", arrayOf(c, pkg))
                                reply?.writeNoException()
                                true
                            }
                            txOnSetPushTime -> {
                                val c = data.readInt()
                                val pkg = data.readString()
                                handleCallback("onSetPushTime", arrayOf(c, pkg))
                                reply?.writeNoException()
                                true
                            }
                            txOnGetPushStatus -> {
                                val c = data.readInt()
                                val status = data.readInt()
                                val pkg = data.readString()
                                handleCallback("onGetPushStatus", arrayOf(c, status, pkg))
                                reply?.writeNoException()
                                true
                            }
                            txOnGetNotificationStatus -> {
                                val c = data.readInt()
                                val status = data.readInt()
                                val pkg = data.readString()
                                handleCallback("onGetNotificationStatus", arrayOf(c, status, pkg))
                                reply?.writeNoException()
                                true
                            }
                            else -> super.onTransact(code, data, reply, flags)
                        }
                    } catch (t: Throwable) {
                        Log.d(TAG, "Binder onTransact 处理异常: ${t.message}")
                        super.onTransact(code, data, reply, flags)
                    }
                }
            }

            // 生成实现接口的代理对象，asBinder 返回我们自定义的 Binder
            Proxy.newProxyInstance(
                callbackInterface.classLoader,
                arrayOf(callbackInterface)
            ) { _, method, args ->
                if (method.name == "asBinder" && method.parameterCount == 0) return@newProxyInstance binder
                handleCallback(method.name, args)
                null
            }
        } catch (e: Throwable) {
            Log.d(TAG, "⚠️ 无法创建 Binder 支撑回调，回退到动态代理: ${e.message}")
            createEnhancedCallbackProxy(callbackInterface)
        }
    }

    /**
     * 创建增强版回调代理，提供更详细的日志（不带 Binder onTransact 支撑，作为回退方案）
     */
    private fun createEnhancedCallbackProxy(callbackInterface: Class<*>): Any {
        return Proxy.newProxyInstance(
            callbackInterface.classLoader,
            arrayOf(callbackInterface)
        ) { _, method, args ->
            // 为 AIDL 接口提供 Binder 以满足 IInterface 要求
            if (method.name == "asBinder" && method.parameterCount == 0) {
                try {
                    return@newProxyInstance Binder()
                } catch (_: Throwable) {
                    // 兜底返回 null（不理想，但不阻断后续处理）
                    return@newProxyInstance null
                }
            }
            handleCallback(method.name, args)
            null
        }
    }

    /**
     * 统一的回调处理与日志输出，供 Binder onTransact 和 代理拦截共同复用。
     */
    private fun handleCallback(methodName: String, args: Array<Any?>?) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        Log.d(TAG, "[$timestamp] 📞 收到回调: $methodName")
        Log.d(TAG, "  参数: ${args?.joinToString(", ") { it?.toString() ?: "null" }}")

        when (methodName) {
            "onRegister" -> {
                val code = (args?.getOrNull(0) as? Int) ?: -1
                val registerId = args?.getOrNull(1)?.toString()
                val packageName = args?.getOrNull(2)?.toString()

                Log.d(TAG, "  ✅ 注册回调详情:")
                Log.d(TAG, "    返回码: $code (${getOppoErrorMessage(code)})")
                Log.d(TAG, "    RegisterID: ${registerId?.take(12)}...")
                Log.d(TAG, "    包名: $packageName")
                Log.d(TAG, "    当前包名: ${context.packageName}")
                Log.d(TAG, "    包名匹配: ${packageName == context.packageName}")

                if (code == 0 && !registerId.isNullOrEmpty()) {
                    currentToken = registerId
                    cancelRegistrationTimeout()
                    Log.i(TAG, "🎉 OPPO推送注册成功: ${registerId.take(12)}...")
                    sendTokenSuccess(registerId)
                } else {
                    cancelRegistrationTimeout()
                    val errorMessage = getOppoErrorMessage(code)
                    Log.e(TAG, "💥 OPPO推送注册失败: $errorMessage (code=$code)")

                    when (code) {
                        -3 -> Log.e(TAG, "    建议：检查appKey和appSecret是否正确")
                        -8 -> Log.e(TAG, "    建议：检查应用签名是否与后台配置一致")
                        -100 -> Log.e(TAG, "    建议：检查应用是否在白名单中，或申请正式推送权限")
                        -200 -> Log.e(TAG, "    建议：检查设备网络连接和推送服务状态")
                    }

                    sendTokenError("$errorMessage (详细错误码:$code)")
                }
            }
            "onUnRegister" -> {
                val code = (args?.getOrNull(0) as? Int) ?: -1
                val packageName = args?.getOrNull(1)?.toString()
                Log.d(TAG, "  📤 取消注册回调: code=$code (${getOppoErrorMessage(code)}), packageName=$packageName")
            }
            "onSetPushTime" -> {
                val code = (args?.getOrNull(0) as? Int) ?: -1
                val packageName = args?.getOrNull(1)?.toString()
                Log.d(TAG, "  ⏰ 设置推送时间回调: code=$code (${getOppoErrorMessage(code)}), packageName=$packageName")
            }
            "onGetPushStatus" -> {
                val code = (args?.getOrNull(0) as? Int) ?: -1
                val status = (args?.getOrNull(1) as? Int) ?: -1
                val packageName = args?.getOrNull(2)?.toString()
                Log.d(TAG, "  📊 获取推送状态回调: code=$code (${getOppoErrorMessage(code)}), status=$status, packageName=$packageName")
            }
            "onGetNotificationStatus" -> {
                val code = (args?.getOrNull(0) as? Int) ?: -1
                val status = (args?.getOrNull(1) as? Int) ?: -1
                val packageName = args?.getOrNull(2)?.toString()
                Log.d(TAG, "  🔔 获取通知状态回调: code=$code (${getOppoErrorMessage(code)}), status=$status, packageName=$packageName")
            }
            else -> {
                Log.d(TAG, "  ❓ 其他回调: $methodName")
            }
        }
    }

    /**
     * 执行增强版注册流程
     */
    private fun performEnhancedRegistration(
        heytapClass: Class<*>,
        appId: String?,
        appKey: String,
        appSecret: String,
        callbackProxy: Any,
        callbackInterface: Class<*>
    ) {
        Handler(Looper.getMainLooper()).post {
            try {
                Log.d(TAG, "========== 开始增强版注册流程 ==========")
                // 如已存在 Token，直接返回以加快就绪速度（避免不必要的再次注册）
                if (!currentToken.isNullOrEmpty()) {
                    Log.d(TAG, "⏭️ 已存在 Token，跳过注册流程，token=${currentToken?.take(12)}...")
                    return@post
                }
                
                // 启动注册超时监控
                startRegistrationTimeout()
                Log.d(TAG, "🚀 启动${registerTimeoutMs/1000}秒超时监控...")

                // 仅在发现已有 Token 时才进行一次 unRegister 预清理，避免无 Token 情况下的无谓注销
                if (!currentToken.isNullOrEmpty()) {
                    try {
                        val unregNoArg = heytapClass.getMethod("unRegister")
                        unregNoArg.invoke(null)
                        Log.d(TAG, "🧹 发现旧 Token，已调用 unRegister() 进行预清理")
                    } catch (_: NoSuchMethodException) {
                        // 忽略无此方法
                    } catch (e: Exception) {
                        Log.d(TAG, "预清理 unRegister 失败: ${e.message}")
                    }
                } else {
                    Log.d(TAG, "⏭️ 无旧 Token，跳过 unRegister 预清理")
                }
                
                // 扫描可用的 register 重载，避免先尝试不存在的方法造成噪声日志
                val declared = heytapClass.declaredMethods.filter { it.name == "register" }
                fun hasOverload(paramTypeNames: List<String>): Boolean {
                    return declared.any { m ->
                        val ps = m.parameterTypes.map { it.name }
                        ps == paramTypeNames
                    }
                }
                val ctx = Context::class.java.name
                val str = String::class.java.name
                val cbName = callbackInterface.name
                val jsonName = "org.json.JSONObject"

                // 重载存在性检测
                val hasPlain = hasOverload(listOf(ctx, str, str, cbName))
                val hasJson  = hasOverload(listOf(ctx, str, str, jsonName, cbName))
                val hasAppId = hasOverload(listOf(ctx, str, str, str, cbName))

                // 优先级：常见的无 appId 纯参数 -> 含 JSONObject -> 带 appId（仅当提供）
                var registrationSuccess = false
                var usedVariant = ""
                try {
                    if (hasPlain) {
                        val m = heytapClass.getMethod(
                            "register",
                            Context::class.java,
                            String::class.java,
                            String::class.java,
                            callbackInterface
                        )
                        m.invoke(null, context, appKey, appSecret, callbackProxy)
                        usedVariant = "register(Context,String,String,ICallBackResultService)"
                        Log.d(TAG, "📞 使用重载: $usedVariant")
                        registrationSuccess = true
                    } else if (hasJson) {
                        val jsonClass = Class.forName(jsonName)
                        val jsonObj = jsonClass.getConstructor().newInstance()
                        val put = jsonClass.getMethod("put", String::class.java, Any::class.java)
                        put.invoke(jsonObj, "from_sdk", "wxtpush_client")
                        put.invoke(jsonObj, "ts", System.currentTimeMillis())
                        try { put.invoke(jsonObj, "package", context.packageName) } catch (_: Exception) {}
                        if (!appId.isNullOrEmpty()) { try { put.invoke(jsonObj, "app_id", appId) } catch (_: Exception) {} }

                        val m = heytapClass.getMethod(
                            "register",
                            Context::class.java,
                            String::class.java,
                            String::class.java,
                            jsonClass,
                            callbackInterface
                        )
                        m.invoke(null, context, appKey, appSecret, jsonObj, callbackProxy)
                        usedVariant = "register(Context,String,String,JSONObject,ICallBackResultService)"
                        Log.d(TAG, "📞 使用重载: $usedVariant")
                        registrationSuccess = true
                    } else if (hasAppId && !appId.isNullOrEmpty()) {
                        val m = heytapClass.getMethod(
                            "register",
                            Context::class.java,
                            String::class.java,
                            String::class.java,
                            String::class.java,
                            callbackInterface
                        )
                        m.invoke(null, context, appId, appKey, appSecret, callbackProxy)
                        usedVariant = "register(Context,String(appId),String,String,ICallBackResultService)"
                        Log.d(TAG, "� 使用重载: $usedVariant")
                        registrationSuccess = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ 选择的注册重载调用失败: ${e.message}")
                    registrationSuccess = false
                }
                
                if (registrationSuccess) {
                    Log.d(TAG, "✅ 注册方法调用完成，等待SDK回调...")
                    Log.d(TAG, "========== 注册流程启动完成 ==========")
                    // 在短延迟内温和“催发”一次注册流程，部分 ROM 需要主动触发 getRegister()
                    nudgeRegistration(heytapClass)
                    // 若 1.5s 内仍无回调，尝试另一个重载进行一次性回退
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (currentToken == null) {
                            try {
                                // 选择另一个可用的重载进行一次性回退
                                if (usedVariant.contains("JSONObject") && hasPlain) {
                                    val m = heytapClass.getMethod(
                                        "register",
                                        Context::class.java,
                                        String::class.java,
                                        String::class.java,
                                        callbackInterface
                                    )
                                    m.invoke(null, context, appKey, appSecret, callbackProxy)
                                    Log.d(TAG, "🪞 回退尝试: register(Context,String,String,ICallBackResultService)")
                                } else if (!usedVariant.contains("JSONObject") && hasJson) {
                                    val jsonClass2 = Class.forName(jsonName)
                                    val jsonObj2 = jsonClass2.getConstructor().newInstance()
                                    val put2 = jsonClass2.getMethod("put", String::class.java, Any::class.java)
                                    put2.invoke(jsonObj2, "from_sdk", "wxtpush_client")
                                    put2.invoke(jsonObj2, "ts", System.currentTimeMillis())
                                    try { put2.invoke(jsonObj2, "package", context.packageName) } catch (_: Exception) {}
                                    val m = heytapClass.getMethod(
                                        "register",
                                        Context::class.java,
                                        String::class.java,
                                        String::class.java,
                                        jsonClass2,
                                        callbackInterface
                                    )
                                    m.invoke(null, context, appKey, appSecret, jsonObj2, callbackProxy)
                                    Log.d(TAG, "🪞 回退尝试: register(Context,String,String,JSONObject,ICallBackResultService)")
                                } else if (hasAppId && !appId.isNullOrEmpty()) {
                                    val m = heytapClass.getMethod(
                                        "register",
                                        Context::class.java,
                                        String::class.java,
                                        String::class.java,
                                        String::class.java,
                                        callbackInterface
                                    )
                                    m.invoke(null, context, appId, appKey, appSecret, callbackProxy)
                                    Log.d(TAG, "🪞 回退尝试: register(Context,String(appId),String,String,ICallBackResultService)")
                                }
                            } catch (ex: Exception) {
                                Log.d(TAG, "回退注册尝试失败: ${ex.message}")
                            }
                        }
                    }, 1500)
                } else {
                    throw RuntimeException("所有注册方法都不可用")
                }
                
            } catch (e: Exception) {
                cancelRegistrationTimeout()
                Log.e(TAG, "💥 OPPO推送注册流程失败", e)
                sendTokenError("注册流程失败: ${e.message}")
            }
        }
    }

    /**
     * demo 兼容模式：极简注册流程
     * 与官方 heytapPushDemo 靠近：setPushCallback -> setAppKeySecret -> init -> register -> getRegister (一次)
     * 不做：状态/通知开关查询、二次重试、延迟多次 nudge。
     */
    private fun performDemoRegistration(
        heytapClass: Class<*>,
        appId: String?,
        appKey: String?,
        appSecret: String?,
        callbackProxy: Any?,
        callbackInterface: Class<*>?
    ) {
        if (appKey.isNullOrBlank() || appSecret.isNullOrBlank()) {
            Log.e(TAG, "[demoCompat] OPPO注册失败: appKey/appSecret 为空")
            return
        }
        // 如果已有 token，直接回调成功（与增强模式一致），避免重复调用
        currentToken?.let { token ->
            Log.i(TAG, "[demoCompat] 已存在OPPO token，跳过注册 -> $token")
            sendTokenSuccess(token)
            return
        }

        // 启动超时（demo 模式下若用户未配置单独超时，沿用 registerTimeoutMs；建议保持<=6000ms）
        registrationTimeout?.cancel()
        registrationTimeout = scope.launch {
            delay(registerTimeoutMs)
            if (currentToken == null) {
                Log.w(TAG, "[demoCompat] 在 ${registerTimeoutMs}ms 内未获得 onRegister 回调。请确认签名/应用在 OPPO 开放平台已审核通过/设备网络正常。")
                sendTokenError("demoCompat 超时: $registerTimeoutMs ms 内未获得 onRegister")
            }
        }
        val ctx = context
        try {
            // register - 首选标准三参方法
            val registerMethod = kotlin.runCatching {
                heytapClass.getMethod(
                    "register",
                    Context::class.java,
                    String::class.java,
                    String::class.java,
                    callbackInterface
                )
            }.getOrNull()
            if (registerMethod != null && callbackProxy != null) {
                Log.i(TAG, "[demoCompat] 调用 register(Context,String,String,ICallback)")
                registerMethod.invoke(null, ctx, appKey, appSecret, callbackProxy)
            } else {
                Log.w(TAG, "[demoCompat] 未找到标准 register 重载或回调代理为空，尝试 JSONObject 方式")
                val jsonRegister = kotlin.runCatching {
                    heytapClass.getMethod(
                        "register",
                        Context::class.java,
                        org.json.JSONObject::class.java,
                        callbackInterface
                    )
                }.getOrNull()
                if (jsonRegister != null && callbackProxy != null) {
                    val jo = org.json.JSONObject().apply {
                        put("appKey", appKey)
                        put("appSecret", appSecret)
                        if (!appId.isNullOrBlank()) put("appId", appId)
                    }
                    Log.i(TAG, "[demoCompat] 调用 register(Context,JSONObject,ICallback)")
                    jsonRegister.invoke(null, ctx, jo, callbackProxy)
                } else {
                    Log.e(TAG, "[demoCompat] 无法找到任何 register 方法，注册中止")
                    return
                }
            }

            // 单次 getRegister 轻推（官方 demo 会较快得到 token；我们模仿一次即可）
            scope.launch {
                delay(500)
                if (currentToken == null) {
                    val getRegister = kotlin.runCatching {
                        heytapClass.getMethod("getRegister", Context::class.java)
                    }.getOrNull()
                    if (getRegister != null) {
                        Log.d(TAG, "[demoCompat] 500ms 仍无 token，调用一次 getRegister() 轻推")
                        kotlin.runCatching { getRegister.invoke(null, ctx) }.
                            onFailure { e -> Log.w(TAG, "[demoCompat] getRegister 调用失败: ${e.message}") }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[demoCompat] 注册流程异常: ${e.message}", e)
            sendTokenError("demoCompat 注册异常: ${e.message}")
        }
    }

    /**
     * 在调用 register 之后，温和触发一次 getRegister()/getRegister(JSONObject)，
     * 并在短延迟后复查一次 getRegisterID()。不做循环轮询，均在超时窗口内完成。
     */
    private fun nudgeRegistration(heytapClass: Class<*>) {
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                try {
                    val method = heytapClass.getMethod("getRegister")
                    method.invoke(null)
                    Log.d(TAG, "🪄 已触发 getRegister()")
                } catch (e: NoSuchMethodException) {
                    try {
                        val jsonClass = Class.forName("org.json.JSONObject")
                        val jsonObj = jsonClass.getConstructor().newInstance()
                        val putMethod = jsonClass.getMethod("put", String::class.java, Any::class.java)
                        putMethod.invoke(jsonObj, "from_sdk", "wxtpush_client")
                        val method2 = heytapClass.getMethod("getRegister", jsonClass)
                        method2.invoke(null, jsonObj)
                        Log.d(TAG, "🪄 已触发 getRegister(JSONObject)")
                    } catch (ex: Exception) {
                        Log.d(TAG, "getRegister 触发失败: ${ex.message}")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "getRegister 调用异常: ${e.message}")
            }
        }, 600)

        // 1.6 秒后复查一次是否已有 Token（一次性）
        scope.launch {
            delay(1600)
            try {
                val getRegisterIdMethod = try {
                    heytapClass.getMethod("getRegisterID", Context::class.java)
                } catch (_: NoSuchMethodException) {
                    heytapClass.getMethod("getRegisterID")
                }
                val token = if (getRegisterIdMethod.parameterCount > 0) {
                    getRegisterIdMethod.invoke(null, context) as? String
                } else {
                    getRegisterIdMethod.invoke(null) as? String
                }
                if (!token.isNullOrEmpty()) {
                    Log.d(TAG, "🔁 复查获取到 Token: ${token.take(12)}...")
                    currentToken = token
                    cancelRegistrationTimeout()
                    sendTokenSuccess(token)
                } else {
                    Log.d(TAG, "🔁 复查仍未获得 Token（等待 SDK 回调或超时处理）")
                }
            } catch (e: Exception) {
                Log.d(TAG, "复查 Token 失败: ${e.message}")
            }
        }

        // 3.5 秒时进行第二次温和催发与 3.8 秒二次复查（仍在超时窗口内，不引入循环轮询）
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentToken != null) return@postDelayed
            try {
                try {
                    val method = heytapClass.getMethod("getRegister")
                    method.invoke(null)
                    Log.d(TAG, "🪄 第二次催发 getRegister()")
                } catch (e: NoSuchMethodException) {
                    try {
                        val jsonClass = Class.forName("org.json.JSONObject")
                        val jsonObj = jsonClass.getConstructor().newInstance()
                        val putMethod = jsonClass.getMethod("put", String::class.java, Any::class.java)
                        putMethod.invoke(jsonObj, "from_sdk", "wxtpush_client")
                        val method2 = heytapClass.getMethod("getRegister", jsonClass)
                        method2.invoke(null, jsonObj)
                        Log.d(TAG, "🪄 第二次催发 getRegister(JSONObject)")
                    } catch (ex: Exception) {
                        Log.d(TAG, "第二次 getRegister 触发失败: ${ex.message}")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "第二次 getRegister 调用异常: ${e.message}")
            }
        }, 3500)

        scope.launch {
            delay(3800)
            if (currentToken != null) return@launch
            try {
                val getRegisterIdMethod = try {
                    heytapClass.getMethod("getRegisterID", Context::class.java)
                } catch (_: NoSuchMethodException) {
                    heytapClass.getMethod("getRegisterID")
                }
                val token = if (getRegisterIdMethod.parameterCount > 0) {
                    getRegisterIdMethod.invoke(null, context) as? String
                } else {
                    getRegisterIdMethod.invoke(null) as? String
                }
                if (!token.isNullOrEmpty()) {
                    Log.d(TAG, "🔁 第二次复查获取到 Token: ${token.take(12)}...")
                    currentToken = token
                    cancelRegistrationTimeout()
                    sendTokenSuccess(token)
                } else {
                    Log.d(TAG, "🔁 第二次复查仍未获得 Token")
                }
            } catch (e: Exception) {
                Log.d(TAG, "第二次复查 Token 失败: ${e.message}")
            }
        }
    }

    /**
     * 打印运行时关键信息：包名、签名SHA1、Manifest元数据、设备信息
     */
    private fun logRuntimeConfig(appId: String?, appKey: String, appSecret: String) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName
            val appInfo = pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            val meta = appInfo.metaData

            // 读取清单中配置（如存在）
            val mdAppKey = meta?.getString("com.heytap.mcs.appkey")
                ?: meta?.getString("com.coloros.mcs.appkey")
            val mdAppSecret = meta?.getString("com.heytap.mcs.appsecret")
                ?: meta?.getString("com.coloros.mcs.appsecret")

            // 计算签名SHA1
            val sigInfo = pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
            val signatures: Array<Signature> = if (sigInfo.hasMultipleSigners()) sigInfo.apkContentsSigners else sigInfo.signingCertificateHistory
            val sha1List = signatures.map { sig ->
                val md = MessageDigest.getInstance("SHA1")
                md.update(sig.toByteArray())
                md.digest().joinToString(":") { b -> "%02X".format(b) }
            }

            // ROM 版本信息（尽力读取）
            val romProps = mutableMapOf<String, String>()
            try {
                val sp = Class.forName("android.os.SystemProperties")
                val get = sp.getMethod("get", String::class.java, String::class.java)
                fun prop(k: String): String = try { get.invoke(null, k, "").toString() } catch (_: Throwable) { "" }
                romProps["ro.build.version.opporom"] = prop("ro.build.version.opporom")
                romProps["ro.build.version.coloros"] = prop("ro.build.version.coloros")
                romProps["ro.vendor.oplus.rom.version"] = prop("ro.vendor.oplus.rom.version")
            } catch (_: Throwable) {}

            Log.i(TAG, "========== OPPO Push 运行时配置核对 ==========")
            Log.i(TAG, "包名: $pkg")
            Log.i(TAG, "签名SHA1: ${sha1List.joinToString(", ")}")
            Log.i(TAG, "传入 appId: ${appId ?: "(null)"}")
            Log.i(TAG, "传入 appKey: ${appKey.take(8)}***")
            Log.i(TAG, "传入 appSecret: ${appSecret.take(8)}***")
            Log.i(TAG, "Manifest appKey: ${mdAppKey?.take(8) ?: "(null)"}***")
            Log.i(TAG, "Manifest appSecret: ${mdAppSecret?.take(8) ?: "(null)"}***")
            Log.i(TAG, "设备: brand=${android.os.Build.BRAND}, model=${android.os.Build.MODEL}, manufacturer=${android.os.Build.MANUFACTURER}")
            if (romProps.isNotEmpty()) {
                Log.i(TAG, "ROM: ${romProps.map { it.key + "=" + it.value }.joinToString(", ")}")
            }
            Log.i(TAG, "============================================")
        } catch (e: Exception) {
            Log.w(TAG, "打印运行时配置失败: ${e.message}")
        }
    }
    
    private fun initializeMockSDK(appId: String?, appKey: String, appSecret: String) {
        Log.d(TAG, "使用模拟OPPO推送实现")
        
        scope.launch {
            delay(2000) // 模拟初始化延迟
            
            // 生成基于设备和配置的稳定Token
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val tokenSource = "oppo_${appKey}_${deviceId}"
            val token = generateStableToken(tokenSource)
            
            currentToken = token
            Log.d(TAG, "模拟OPPO Token生成成功: ${token.take(12)}...")
            sendTokenSuccess(token)
        }
    }
    
    private fun generateStableToken(source: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(source.toByteArray())
            hash.joinToString("") { "%02x".format(it) }.take(32)
        } catch (e: Exception) {
            // 降级到简单哈希
            "oppo_${Math.abs(source.hashCode()).toString().padStart(8, '0')}_${System.currentTimeMillis() % 100000}"
        }
    }
    
    private fun getManifestConfig(): Map<String, String> {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val metaData = appInfo.metaData ?: return emptyMap()
            
            mapOf(
                "appKey" to (metaData.getString("com.heytap.mcs.appkey") 
                    ?: metaData.getString("com.coloros.mcs.appkey") ?: ""),
                "appSecret" to (metaData.getString("com.heytap.mcs.appsecret") 
                    ?: metaData.getString("com.coloros.mcs.appsecret") ?: "")
            )
        } catch (e: Exception) {
            Log.w(TAG, "读取Manifest配置失败", e)
            emptyMap()
        }
    }
    
    private fun isOppoDevice(): Boolean {
        val brand = android.os.Build.BRAND?.lowercase() ?: ""
        val manufacturer = android.os.Build.MANUFACTURER?.lowercase() ?: ""
        return brand.contains("oppo") || manufacturer.contains("oppo")
    }
    
    private fun isColorOSDevice(): Boolean {
        val brand = android.os.Build.BRAND?.lowercase() ?: ""
        return brand.contains("oneplus") || brand.contains("realme")
    }
    
    override fun getToken(): String? {
        if (currentToken == null) {
            Log.w(TAG, "Token尚未获取，请稍后重试或确保初始化成功")
            
            // 如果使用真实SDK且Token为空，尝试一次性获取已有Token
            if (USE_REAL_SDK) {
                try {
                    val heytapClass = Class.forName("com.heytap.msp.push.HeytapPushManager")
                    val getRegisterIdMethod = try {
                        heytapClass.getMethod("getRegisterID", Context::class.java)
                    } catch (e: NoSuchMethodException) {
                        heytapClass.getMethod("getRegisterID")
                    }
                    
                    val token = if (getRegisterIdMethod.parameterCount > 0) {
                        getRegisterIdMethod.invoke(null, context) as? String
                    } else {
                        getRegisterIdMethod.invoke(null) as? String
                    }
                    
                    if (!token.isNullOrEmpty()) {
                        currentToken = token
                        Log.d(TAG, "📋 主动获取到已有Token: ${token.take(12)}...")
                        return token
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "主动获取Token失败: ${e.message}")
                }
            }
        }
        return currentToken
    }
    
    override fun enableNotification() {
        Log.d(TAG, "启用OPPO推送通知")
        if (USE_REAL_SDK) {
            try {
                val heytapClass = Class.forName("com.heytap.msp.push.HeytapPushManager")
                try {
                    val resumeNoArg = heytapClass.getMethod("resumePush")
                    resumeNoArg.invoke(null)
                    Log.d(TAG, "OPPO推送通知已启用（resumePush 无参）")
                } catch (e: NoSuchMethodException) {
                    val resumeWithCtx = heytapClass.getMethod("resumePush", Context::class.java)
                    resumeWithCtx.invoke(null, context)
                    Log.d(TAG, "OPPO推送通知已启用（resumePush 带 Context）")
                }
            } catch (e: Exception) {
                Log.w(TAG, "启用OPPO推送通知失败", e)
            }
        }
    }
    
    override fun disableNotification() {
        Log.d(TAG, "禁用OPPO推送通知")
        if (USE_REAL_SDK) {
            try {
                val heytapClass = Class.forName("com.heytap.msp.push.HeytapPushManager")
                try {
                    val pauseNoArg = heytapClass.getMethod("pausePush")
                    pauseNoArg.invoke(null)
                    Log.d(TAG, "OPPO推送通知已禁用（pausePush 无参）")
                } catch (e: NoSuchMethodException) {
                    val pauseWithCtx = heytapClass.getMethod("pausePush", Context::class.java)
                    pauseWithCtx.invoke(null, context)
                    Log.d(TAG, "OPPO推送通知已禁用（pausePush 带 Context）")
                }
            } catch (e: Exception) {
                Log.w(TAG, "禁用OPPO推送通知失败", e)
            }
        }
    }
    
    override fun setAlias(alias: String) {
        Log.w(TAG, "OPPO推送不直接支持设置别名，请通过服务端实现")
    }
    
    override fun setTags(tags: List<String>) {
        Log.w(TAG, "OPPO推送不直接支持设置标签，请通过服务端实现")
    }

    override fun setBadge(count: Int): Boolean {
        Log.d(TAG, "设置OPPO角标: $count")
        return BadgeHelper.setBadge(context, count)
    }

    override fun getBadge(): Int {
        Log.d(TAG, "获取OPPO角标")
        return BadgeHelper.getBadge(context)
    }
    
    private fun sendTokenSuccess(token: String) {
        scope.launch(Dispatchers.Main) {
            sendEvent("tokenReceived", mapOf(
                "token" to token,
                "vendor" to "oppo"
            ))
        }
    }
    
    private fun sendTokenError(errorMessage: String) {
        scope.launch(Dispatchers.Main) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val detailedError = "[$timestamp] OPPO推送错误: $errorMessage"
            val hint = "如持续出现超时/无回调，可能与 OPPO 权限（测试/正式）相关，参见 doc/OPPO_PUSH_PERMISSION_GUIDE.md"
            
            Log.e(TAG, "$detailedError\n$hint")
            sendEvent("tokenError", mapOf(
                "error" to detailedError,
                "vendor" to "oppo",
                "timestamp" to System.currentTimeMillis(),
                "details" to mapOf(
                    "originalError" to errorMessage,
                    "sdkAvailable" to USE_REAL_SDK,
                    "deviceInfo" to mapOf(
                        "brand" to android.os.Build.BRAND,
                        "model" to android.os.Build.MODEL,
                        "manufacturer" to android.os.Build.MANUFACTURER
                    )
                )
            ))
        }
    }
}
