package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ReplyRule
import kotlinx.coroutines.flow.Flow

@Dao
interface ReplyRuleDao {
    @Query("SELECT * FROM reply_rules ORDER BY priority DESC, id DESC")
    fun getAllRules(): Flow<List<ReplyRule>>

    @Query("SELECT * FROM reply_rules WHERE isEnabled = 1 ORDER BY priority DESC, id DESC")
    fun getEnabledRules(): Flow<List<ReplyRule>>

    @Query("SELECT * FROM reply_rules WHERE isEnabled = 1 ORDER BY priority DESC, id DESC")
    suspend fun getEnabledRulesSync(): List<ReplyRule>

    @Query("SELECT * FROM reply_rules WHERE id = :id LIMIT 1")
    suspend fun getRuleById(id: Long): ReplyRule?

    @Query("SELECT COUNT(*) FROM reply_rules")
    fun countRules(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reply_rules WHERE isEnabled = 1")
    fun countEnabledRules(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ReplyRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<ReplyRule>): List<Long>

    @Update
    suspend fun updateRule(rule: ReplyRule)

    @Delete
    suspend fun deleteRule(rule: ReplyRule)

    @Query("DELETE FROM reply_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
}
