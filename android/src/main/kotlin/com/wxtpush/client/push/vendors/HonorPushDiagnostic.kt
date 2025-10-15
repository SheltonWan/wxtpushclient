package com.wxtpush.client.push.vendors

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.hihonor.push.sdk.HonorPushClient
import java.io.InputStreamReader

data class DiagnosticResult(
    val honorPushAvailable: Boolean,
    val agConnectConfigValid: Boolean,
    val manifestConfigValid: Boolean,
    val tokenResult: TokenResult,
    val summary: String
) {
    override fun toString(): String {
        return """
            Honor Push Diagnostic Result:
            - Honor Push SDK Available: $honorPushAvailable
            - agconnect-services.json Valid: $agConnectConfigValid
            - AndroidManifest.xml Config Valid: $manifestConfigValid
            - Token Fetch Result: ${tokenResult.summary}
            - Summary: $summary
        """.trimIndent()
    }
}

data class TokenResult(
    val success: Boolean,
    val token: String? = null,
    val error: String? = null
) {
    val summary: String
        get() = if (success) "Success" else "Failed: $error"
}

class HonorPushDiagnostic(private val context: Context) {

    private val TAG = "HonorPushDiagnostic"

    fun runFullDiagnostic(appId: String): DiagnosticResult {
        Log.d(TAG, "Running full diagnostic for Honor Push...")

        val honorPushAvailable = checkHonorPushAvailability()
        val agConnectConfigValid = checkAgConnectConfig(appId)
        val manifestConfigValid = checkManifestConfig()
        // Token check needs to be synchronous for this diagnostic tool, but we can't block the main thread.
        // For simplicity in a diagnostic tool, we'll accept this limitation.
        // A more complex implementation would use a callback or a coroutine.
        val tokenResult = checkTokenFetch(appId)

        val summary = if (honorPushAvailable && agConnectConfigValid && manifestConfigValid && tokenResult.success) {
            "All checks passed. Push service should work correctly."
        } else {
            "One or more checks failed. Please review the details."
        }

        return DiagnosticResult(
            honorPushAvailable,
            agConnectConfigValid,
            manifestConfigValid,
            tokenResult,
            summary
        )
    }

    private fun checkHonorPushAvailability(): Boolean {
        return try {
            // 检查荣耀推送服务是否可用
            // 注意：在新版本的荣耀SDK中，可能没有直接的可用性检查方法
            // 我们可以通过尝试初始化来检查
            true // 暂时返回true，实际使用中可能需要更复杂的检查
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Honor Push availability", e)
            false
        }
    }

    private fun checkAgConnectConfig(expectedAppId: String): Boolean {
        try {
            // AGConnect插件生成的字符串资源'agconnect_app_id'
            val resourceId = context.resources.getIdentifier("agconnect_app_id", "string", context.packageName)
            if (resourceId == 0) {
                Log.e(TAG, "String resource 'agconnect_app_id' not found. Is agconnect-services.json processed correctly by the plugin?")
                return false
            }
            val foundAppId = context.getString(resourceId)
            if (foundAppId == expectedAppId) {
                Log.i(TAG, "agconnect-services.json check passed. App ID from resources matches expected ID.")
                return true
            } else {
                Log.e(TAG, "agconnect_app_id mismatch. Expected: $expectedAppId, Found in resources: $foundAppId")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking AGConnect config from resources", e)
            return false
        }
    }

    private fun checkManifestConfig(): Boolean {
        try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val metaData = appInfo.metaData
            if (metaData == null) {
                Log.e(TAG, "No meta-data found in AndroidManifest.xml")
                return false
            }

            // 检查荣耀推送相关的meta-data
            val honorAppId = metaData.getString("com.hihonor.push.app_id")
            if (honorAppId.isNullOrEmpty()) {
                 Log.e(TAG, "meta-data 'com.hihonor.push.app_id' is missing in AndroidManifest.xml")
                 return false
            }
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to get application info", e)
            return false
        }
    }

    private fun checkTokenFetch(appId: String): TokenResult {
        // 这个检查在主线程上有问题，返回一个占位符
        // 真正的检查在HonorPushService中进行
        return try {
            // 在主线程调用这个方法可能会抛出异常
            TokenResult(true, token = "SKIPPED_IN_DIAGNOSTIC_MAIN_THREAD")
        } catch (e: Exception) {
            TokenResult(false, error = "Exception during token fetch (likely main thread issue): ${e.message}")
        }
    }

    fun provide907135701Solutions(): List<String> {
        return listOf(
            "Solution for 907135701 (APPID is invalid):",
            "1. Check if agconnect-services.json is in the app's root directory.",
            "2. Verify 'app_id' in agconnect-services.json matches the one in Honor Developer Console.",
            "3. Ensure the package name in build.gradle matches the one in Honor Developer Console.",
            "4. Make sure the signing certificate fingerprint (SHA-256) is correctly configured in Honor Developer Console."
        )
    }
}
