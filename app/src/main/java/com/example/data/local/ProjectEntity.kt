package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val rawText: String,
    val modelId: String,
    val voiceName: String,
    val language: String,
    val style: String,
    val scene: String,
    val emotion: String,
    val status: String = "DRAFT", // DRAFT, GENERATING, PAUSED, COMPLETED, ERROR
    val projectType: String = "SINGLE", // SINGLE, MULTI_SPEAKER
    val totalChapters: Int = 0,
    val completedChapters: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val chapterIndex: Int,
    val title: String,
    val content: String,
    val status: String = "WAITING", // WAITING, GENERATING, COMPLETED, FAILED, PAUSED
    val audioFilePath: String? = null,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
