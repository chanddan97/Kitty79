package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: UsageEntity): Long

    @Query("SELECT * FROM api_usage ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<UsageEntity>>

    @Query("SELECT * FROM api_usage WHERE dateKey = :dateKey ORDER BY timestamp DESC")
    fun getTodayFlow(dateKey: String): Flow<List<UsageEntity>>

    @Query("SELECT COUNT(*) FROM api_usage WHERE dateKey = :dateKey")
    suspend fun getTodayCount(dateKey: String): Int

    @Query("SELECT COUNT(*) FROM api_usage WHERE dateKey = :dateKey AND isSuccess = 1")
    suspend fun getTodaySuccessCount(dateKey: String): Int

    @Query("SELECT COUNT(*) FROM api_usage WHERE dateKey = :dateKey AND isSuccess = 0")
    suspend fun getTodayFailedCount(dateKey: String): Int

    @Query("SELECT COALESCE(SUM(characterCount), 0) FROM api_usage WHERE dateKey = :dateKey")
    suspend fun getTodayCharSum(dateKey: String): Int

    @Query("DELETE FROM api_usage")
    suspend fun clearAll()

    @Query("SELECT * FROM api_usage ORDER BY timestamp DESC LIMIT 10")
    fun getRecentFlow(): Flow<List<UsageEntity>>
}
