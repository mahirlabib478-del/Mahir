package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ReplyLog
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplyLogDao {
    @Query("SELECT * FROM reply_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ReplyLog>>

    @Query("SELECT * FROM reply_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 20): Flow<List<ReplyLog>>

    @Query("SELECT COUNT(*) FROM reply_logs")
    fun countTotalReplies(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reply_logs WHERE timestamp >= :sinceTimestamp")
    fun countRepliesSince(sinceTimestamp: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReplyLog): Long

    @Delete
    suspend fun deleteLog(log: ReplyLog)

    @Query("DELETE FROM reply_logs")
    suspend fun clearAllLogs()
}
