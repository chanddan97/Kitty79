package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HistoryEntity): Long

    @Update
    suspend fun update(item: HistoryEntity)

    @Query("SELECT * FROM tts_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM tts_history WHERE fullText LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchFlow(query: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM tts_history WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): HistoryEntity?

    @Query("DELETE FROM tts_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tts_history")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM tts_history")
    suspend fun getCount(): Int
}
