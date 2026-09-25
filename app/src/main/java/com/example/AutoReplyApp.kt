package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.preferences.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoReplyApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferenceManager: PreferenceManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getInstance(this)
        preferenceManager = PreferenceManager(this)

        // Ensure default rules exist even on existing database migrations
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val ruleDao = database.replyRuleDao()
                val existingRules = ruleDao.getEnabledRulesSync()
                if (existingRules.isEmpty()) {
                    ruleDao.insertRules(AppDatabase.defaultStarterRules)
                }
            } catch (e: Exception) {
                android.util.Log.e("AutoReplyApp", "Error checking/seeding rules", e)
            }
        }
    }

    companion object {
        lateinit var instance: AutoReplyApp
            private set
    }
}
