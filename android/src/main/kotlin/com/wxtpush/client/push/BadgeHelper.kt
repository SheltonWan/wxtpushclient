package com.wxtpush.client.push

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.lang.reflect.Method

/**
 * 应用角标管理辅助类
 * 
 * 支持多厂商角标设置：
 * - 华为/荣耀：通过 Launcher Provider
 * - 小米：通过通知 API
 * - OPPO：通过 Launcher Provider (部分机型)
 * - VIVO：通过 Launcher Provider (部分机型)
 * 
 * 注意：Android 8.0+ 角标功能由系统通知渠道控制，
 * 本工具类提供的是厂商定制的角标 API
 */
object BadgeHelper {
    private const val TAG = "BadgeHelper"
    
    // 缓存角标数字，用于不支持读取的厂商
    private var cachedBadgeCount: Int = 0
    
    /**
     * 设置应用角标
     * 
     * @param context 上下文
     * @param count 角标数字，0 表示清除角标
     * @return 操作是否成功
     */
    fun setBadge(context: Context, count: Int): Boolean {
        Log.d(TAG, "设置角标: count=$count, manufacturer=${Build.MANUFACTURER}")
        
        // 缓存角标数字
        cachedBadgeCount = count
        
        return try {
            when (Build.MANUFACTURER.uppercase()) {
                "HUAWEI", "HONOR" -> setHuaweiBadge(context, count)
                "XIAOMI", "REDMI" -> setXiaomiBadge(context, count)
                "OPPO" -> setOppoBadge(context, count)
                "VIVO" -> setVivoBadge(context, count)
                "SAMSUNG" -> setSamsungBadge(context, count)
                else -> {
                    Log.w(TAG, "不支持的厂商: ${Build.MANUFACTURER}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "设置角标失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 获取当前角标数字
     * 
     * @param context 上下文
     * @return 当前角标数字
     */
    fun getBadge(context: Context): Int {
        // 大部分厂商不支持读取角标，返回缓存值
        return cachedBadgeCount
    }
    
    /**
     * 华为/荣耀角标设置
     */
    private fun setHuaweiBadge(context: Context, count: Int): Boolean {
        return try {
            val bundle = android.os.Bundle().apply {
                putString("package", context.packageName)
                putString("class", getLauncherClassName(context))
                putInt("badgenumber", count)
            }
            
            context.contentResolver.call(
                android.net.Uri.parse("content://com.huawei.android.launcher.settings/badge/"),
                "change_badge",
                null,
                bundle
            )
            
            Log.d(TAG, "华为/荣耀角标设置成功: $count")
            true
        } catch (e: Exception) {
            Log.e(TAG, "华为/荣耀角标设置失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 小米角标设置
     */
    private fun setXiaomiBadge(context: Context, count: Int): Boolean {
        return try {
            // 小米使用通知管理器设置角标
            val notificationClass = Class.forName("android.app.NotificationManager")
            val setAppBadgeCountMethod: Method = notificationClass.getDeclaredMethod(
                "setAppBadgeCount",
                Int::class.java
            )
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            setAppBadgeCountMethod.invoke(notificationManager, count)
            
            Log.d(TAG, "小米角标设置成功: $count")
            true
        } catch (e: Exception) {
            Log.e(TAG, "小米角标设置失败: ${e.message}", e)
            
            // 尝试备用方案：广播
            try {
                val intent = Intent("android.intent.action.APPLICATION_MESSAGE_UPDATE").apply {
                    putExtra("android.intent.extra.update_application_component_name", 
                        "${context.packageName}/${getLauncherClassName(context)}")
                    putExtra("android.intent.extra.update_application_message_text", 
                        if (count > 0) count.toString() else "")
                }
                context.sendBroadcast(intent)
                Log.d(TAG, "小米角标广播发送成功: $count")
                true
            } catch (e2: Exception) {
                Log.e(TAG, "小米角标广播发送失败: ${e2.message}", e2)
                false
            }
        }
    }
    
    /**
     * OPPO角标设置
     */
    private fun setOppoBadge(context: Context, count: Int): Boolean {
        return try {
            // OPPO 使用 Intent 广播
            val intent = Intent("com.oppo.unsettledevent").apply {
                putExtra("pakeageName", context.packageName)
                putExtra("number", count)
                putExtra("upgradeNumber", count)
            }
            context.sendBroadcast(intent)
            
            Log.d(TAG, "OPPO角标设置成功: $count")
            true
        } catch (e: Exception) {
            Log.e(TAG, "OPPO角标设置失败: ${e.message}", e)
            
            // 尝试备用方案
            try {
                val bundle = android.os.Bundle().apply {
                    putInt("app_badge_count", count)
                }
                context.contentResolver.call(
                    android.net.Uri.parse("content://com.android.badge/badge"),
                    "setAppBadgeCount",
                    null,
                    bundle
                )
                Log.d(TAG, "OPPO角标备用方案成功: $count")
                true
            } catch (e2: Exception) {
                Log.e(TAG, "OPPO角标备用方案失败: ${e2.message}", e2)
                false
            }
        }
    }
    
    /**
     * VIVO角标设置
     */
    private fun setVivoBadge(context: Context, count: Int): Boolean {
        return try {
            val intent = Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM").apply {
                putExtra("packageName", context.packageName)
                putExtra("className", getLauncherClassName(context))
                putExtra("notificationNum", count)
            }
            context.sendBroadcast(intent)
            
            Log.d(TAG, "VIVO角标设置成功: $count")
            true
        } catch (e: Exception) {
            Log.e(TAG, "VIVO角标设置失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 三星角标设置
     */
    private fun setSamsungBadge(context: Context, count: Int): Boolean {
        return try {
            val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
                putExtra("badge_count", count)
                putExtra("badge_count_package_name", context.packageName)
                putExtra("badge_count_class_name", getLauncherClassName(context))
            }
            context.sendBroadcast(intent)
            
            Log.d(TAG, "三星角标设置成功: $count")
            true
        } catch (e: Exception) {
            Log.e(TAG, "三星角标设置失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 获取启动器Activity类名
     */
    private fun getLauncherClassName(context: Context): String {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(context.packageName)
        }
        
        val resolveInfo = context.packageManager.queryIntentActivities(intent, 0).firstOrNull()
        return resolveInfo?.activityInfo?.name ?: ""
    }
}
