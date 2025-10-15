package com.wxtpush.client

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log

/**
 * 推送配置提供器
 * 自动从客户端应用的 build.gradle manifestPlaceholders 中读取配置
 * 简化客户端集成，无需手动配置各种 meta-data
 */
class WxtpushConfigProvider : ContentProvider() {

    companion object {
        private const val TAG = "WxtpushConfigProvider"
        private var configCache: Map<String, String>? = null

        /**
         * 获取推送配置
         */
        fun getConfig(context: Context): Map<String, String> {
            if (configCache == null) {
                configCache = loadConfigFromManifest(context)
            }
            return configCache ?: emptyMap()
        }

        /**
         * 从 AndroidManifest.xml 中读取配置
         */
        private fun loadConfigFromManifest(context: Context): Map<String, String> {
            val config = mutableMapOf<String, String>()
            try {
                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                val meta = appInfo.metaData ?: Bundle()

                // ===== 辅助函数：兼容 String/Int 类型读取 =====
                fun getMetaValue(key: String): String? {
                    return try {
                        meta.getString(key)
                    } catch (e: ClassCastException) {
                        // 如果是 Integer，转为 String
                        try {
                            meta.getInt(key, -1).takeIf { it != -1 }?.toString()
                        } catch (e2: Exception) {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                // ===== 华为 =====
                val huaweiAppId = getMetaValue("HUAWEI_APP_ID")
                    ?: getMetaValue("com.huawei.hms.client.appid")?.let { v ->
                        // 华为格式 appid=xxxx
                        if (v.startsWith("appid=")) v.substringAfter("appid=") else v
                    }
                huaweiAppId?.takeIf { it.isNotEmpty() }?.let {
                    config["huawei_app_id"] = it
                    Log.d(TAG, "加载华为配置: $it")
                }
                getMetaValue("HUAWEI_APP_SECRET")?.let {
                    config["huawei_app_secret"] = it
                }

                // ===== 荣耀 =====
                val honorAppId = getMetaValue("HONOR_APP_ID")
                    ?: getMetaValue("com.hihonor.push.app_id")
                honorAppId?.takeIf { it.isNotEmpty() }?.let {
                    config["honor_app_id"] = it
                    Log.d(TAG, "加载荣耀配置: $it")
                }
                getMetaValue("HONOR_APP_SECRET")?.let {
                    config["honor_app_secret"] = it
                }

                // ===== 小米 =====
                val xiaomiAppId = getMetaValue("XIAOMI_APP_ID")
                    ?: getMetaValue("MIPUSH_APPID")
                val xiaomiAppKey = getMetaValue("XIAOMI_APP_KEY")
                    ?: getMetaValue("MIPUSH_APPKEY")
                xiaomiAppId?.let { 
                    config["xiaomi_app_id"] = it
                    Log.d(TAG, "加载小米配置: $it")
                }
                xiaomiAppKey?.let { config["xiaomi_app_key"] = it }

                // ===== OPPO / Heytap =====
                val oppoAppKey = getMetaValue("OPPO_APP_KEY")
                    ?: getMetaValue("com.heytap.mcs.appkey")
                    ?: getMetaValue("com.coloros.mcs.appkey")
                val oppoAppSecret = getMetaValue("OPPO_APP_SECRET")
                    ?: getMetaValue("com.heytap.mcs.appsecret")
                    ?: getMetaValue("com.coloros.mcs.appsecret")
                oppoAppKey?.let { 
                    config["oppo_app_key"] = it
                    Log.d(TAG, "加载OPPO配置: $it")
                }
                oppoAppSecret?.let { config["oppo_app_secret"] = it }

                // ===== VIVO =====
                val vivoAppId = getMetaValue("VIVO_APP_ID")
                val vivoAppKey = getMetaValue("VIVO_APP_KEY")
                vivoAppId?.let { 
                    config["vivo_app_id"] = it
                    Log.d(TAG, "加载VIVO配置: $it")
                }
                vivoAppKey?.let { config["vivo_app_key"] = it }

            } catch (e: Exception) {
                Log.e(TAG, "加载推送配置失败", e)
            }
            return config
        }
    }

    override fun onCreate(): Boolean {
        context?.let { ctx ->
            // 在应用启动时预加载配置
            getConfig(ctx)
            Log.i(TAG, "推送配置提供器初始化完成")
        }
        return true
    }

    // ContentProvider 接口的其他方法（不使用，但必须实现）
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
