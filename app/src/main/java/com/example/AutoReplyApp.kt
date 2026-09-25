package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.preferences.PreferenceManager

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
    }

    companion object {
        lateinit var instance: AutoReplyApp
            private set
    }
}
