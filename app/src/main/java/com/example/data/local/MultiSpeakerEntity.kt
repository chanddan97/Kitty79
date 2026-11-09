package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "story_speakers")
data class StorySpeakerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val speakerType: String = "CHARACTER", // NARRATOR, CHARACTER, UNKNOWN, etc.
    val voiceName: String,
    val age: String = "UNKNOWN",
    val gender: String = "UNKNOWN",
    val personality: String = "FRIENDLY",
    val style: String = "NATURAL",
    val emotion: String = "NEUTRAL",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "story_script_sections")
data class StoryScriptSectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val sectionIndex: Int,
    val speakerId: Long?, // Links to StorySpeakerEntity.id
    val speakerName: String, // Fallback/Display name
    val text: String,
    val emotion: String = "AUTO",
    val status: String = "WAITING", // WAITING, GENERATING, COMPLETED, FAILED
    val audioFilePath: String? = null,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
