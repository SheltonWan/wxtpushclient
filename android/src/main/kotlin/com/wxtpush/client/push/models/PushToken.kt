package com.wxtpush.client.push.models

data class PushToken(
    val token: String,
    val vendor: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "token" to token,
            "vendor" to vendor,
            "timestamp" to timestamp
        )
    }

    companion object {
        fun create(token: String, vendor: String): PushToken {
            return PushToken(token, vendor)
        }
    }
}
