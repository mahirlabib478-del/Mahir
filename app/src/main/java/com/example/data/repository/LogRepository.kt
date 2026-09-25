package com.example.data.repository

import com.example.data.dao.ReplyLogDao
import com.example.data.model.ReplyLog
import kotlinx.coroutines.flow.Flow

class LogRepository(private val logDao: ReplyLogDao) {

    val allLogs: Flow<List<ReplyLog>> = logDao.getAllLogs()
    val totalRepliesCount: Flow<Int> = logDao.countTotalReplies()

    fun getRecentLogs(limit: Int = 30): Flow<List<ReplyLog>> = logDao.getRecentLogs(limit)

    suspend fun insertLog(log: ReplyLog): Long = logDao.insertLog(log)

    suspend fun clearAllLogs() = logDao.clearAllLogs()

    suspend fun deleteLog(log: ReplyLog) = logDao.deleteLog(log)
}
