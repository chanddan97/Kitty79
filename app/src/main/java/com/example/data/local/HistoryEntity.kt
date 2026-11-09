package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tts_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val fullText: String,
    val modelId: String,
    val modelName: String,
    val voiceName: String,
    val language: String,
    val style: String,
    val emotion: String,
    val speed: Float,
    val durationMs: Long,
    val audioFilePath: String,
    val fileSize: Long,
    val isSavedToDevice: Boolean = false
)
