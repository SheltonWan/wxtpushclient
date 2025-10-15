package com.wxtpush.client.push

enum class PushVendor(val id: String) {
    HUAWEI("huawei"),
    HONOR("honor"),
    XIAOMI("xiaomi"),
    OPPO("oppo"),
    VIVO("vivo"),
    APPLE("apple");

    companion object {
        fun fromString(value: String): PushVendor {
            return values().find { it.id == value }
                ?: throw IllegalArgumentException("Unknown push vendor: $value")
        }
    }
}
