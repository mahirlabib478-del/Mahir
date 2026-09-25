package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.dao.ReplyRuleDao
import com.example.data.model.ReplyRule
import kotlinx.coroutines.flow.Flow

class RuleRepository(private val ruleDao: ReplyRuleDao) {

    val allRules: Flow<List<ReplyRule>> = ruleDao.getAllRules()
    val enabledRules: Flow<List<ReplyRule>> = ruleDao.getEnabledRules()
    val totalRulesCount: Flow<Int> = ruleDao.countRules()
    val enabledRulesCount: Flow<Int> = ruleDao.countEnabledRules()

    suspend fun getEnabledRulesSync(): List<ReplyRule> = ruleDao.getEnabledRulesSync()

    suspend fun insertRule(rule: ReplyRule): Long = ruleDao.insertRule(rule)

    suspend fun updateRule(rule: ReplyRule) = ruleDao.updateRule(rule)

    suspend fun deleteRule(rule: ReplyRule) = ruleDao.deleteRule(rule)

    suspend fun deleteRuleById(id: Long) = ruleDao.deleteRuleById(id)

    suspend fun toggleRuleEnabled(rule: ReplyRule) {
        ruleDao.updateRule(rule.copy(isEnabled = !rule.isEnabled))
    }

    suspend fun resetToDefaultPresets() {
        ruleDao.insertRules(AppDatabase.defaultStarterRules)
    }
}
