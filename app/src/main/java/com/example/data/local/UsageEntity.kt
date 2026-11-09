package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_usage")
data class UsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateKey: String, // e.g. "2026-10-04"
    val modelId: String,
    val voiceName: String,
    val characterCount: Int,
    val isSuccess: Boolean,
    val errorCode: Int? = null,
    val errorMessage: String? = null,
    val responseTimeMs: Long = 0L,
    val estimatedAudioDurationMs: Long = 0L
)
