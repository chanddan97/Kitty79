package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)

    @Query("DELETE FROM chapters WHERE projectId = :projectId")
    suspend fun deleteChaptersForProject(projectId: Long)

    @Query("SELECT * FROM chapters WHERE projectId = :projectId ORDER BY chapterIndex ASC")
    fun getChaptersForProjectFlow(projectId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE projectId = :projectId ORDER BY chapterIndex ASC")
    suspend fun getChaptersForProject(projectId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    // Multi-Speaker Story Methods
    @Query("SELECT * FROM story_speakers WHERE projectId = :projectId ORDER BY id ASC")
    fun getSpeakersForProjectFlow(projectId: Long): Flow<List<StorySpeakerEntity>>

    @Query("SELECT * FROM story_speakers WHERE projectId = :projectId ORDER BY id ASC")
    suspend fun getSpeakersForProject(projectId: Long): List<StorySpeakerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeaker(speaker: StorySpeakerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeakers(speakers: List<StorySpeakerEntity>)

    @Update
    suspend fun updateSpeaker(speaker: StorySpeakerEntity)

    @Query("DELETE FROM story_speakers WHERE id = :id")
    suspend fun deleteSpeaker(id: Long)

    @Query("SELECT * FROM story_script_sections WHERE projectId = :projectId ORDER BY sectionIndex ASC")
    fun getScriptSectionsForProjectFlow(projectId: Long): Flow<List<StoryScriptSectionEntity>>

    @Query("SELECT * FROM story_script_sections WHERE projectId = :projectId ORDER BY sectionIndex ASC")
    suspend fun getScriptSectionsForProject(projectId: Long): List<StoryScriptSectionEntity>

    @Query("SELECT * FROM story_script_sections WHERE id = :id")
    suspend fun getScriptSectionById(id: Long): StoryScriptSectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScriptSections(sections: List<StoryScriptSectionEntity>)

    @Update
    suspend fun updateScriptSection(section: StoryScriptSectionEntity)

    @Query("DELETE FROM story_script_sections WHERE projectId = :projectId")
    suspend fun deleteScriptSectionsForProject(projectId: Long)
}
