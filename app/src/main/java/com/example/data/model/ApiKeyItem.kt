package com.example.data.model

import java.util.UUID

enum class KeyStatus {
    WORKING,
    UNTESTED,
    RATE_LIMITED,
    INVALID,
    ERROR
}

data class ApiKeyItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val key: String,
    val isPrimary: Boolean = false,
    val isEnabled: Boolean = true,
    val status: KeyStatus = KeyStatus.UNTESTED,
    val lastTestedTime: Long = 0L,
    val lastResponseTimeMs: Long = 0L,
    val lastError: String? = null
) {
    val maskedKey: String
        get() {
            val trimmed = key.trim()
            if (trimmed.length <= 8) return "••••••••"
            val prefix = if (trimmed.startsWith("AIza")) "AIza" else trimmed.take(3)
            val suffix = trimmed.takeLast(4)
            return "${prefix}••••••••$suffix"
        }
}
