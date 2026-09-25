package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ReplyLogDao
import com.example.data.dao.ReplyRuleDao
import com.example.data.model.MatchType
import com.example.data.model.ReplyLog
import com.example.data.model.ReplyRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ReplyRule::class, ReplyLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun replyRuleDao(): ReplyRuleDao
    abstract fun replyLogDao(): ReplyLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wa_autoreply_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate default helpful rules
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getInstance(context).replyRuleDao()
                                dao.insertRules(defaultStarterRules)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val defaultStarterRules = listOf(
            ReplyRule(
                name = "Greeting & Welcome",
                incomingPattern = "hi, hello, hey, সালাম, হ্যালো",
                matchType = MatchType.CONTAINS,
                replyText = "Hello {sender}! Thanks for reaching out. I'm currently away from my phone, but I'll reply to your message as soon as possible.",
                isEnabled = true,
                isGroupAllowed = false,
                cooldownSeconds = 30,
                priority = 10
            ),
            ReplyRule(
                name = "Assalamu Alaikum",
                incomingPattern = "salam, assalam, salamualaykum, আসসালামু আলাইকুম",
                matchType = MatchType.CONTAINS,
                replyText = "Wa Alaikum Assalam! Thanks for texting. I will get back to you shortly.",
                isEnabled = true,
                isGroupAllowed = false,
                cooldownSeconds = 30,
                priority = 9
            ),
            ReplyRule(
                name = "Price / Inquiries",
                incomingPattern = "price, cost, rate, কত, দাম",
                matchType = MatchType.CONTAINS,
                replyText = "Hello! Thanks for asking about our pricing. Please let us know the exact product or service name, and we will send you the price details.",
                isEnabled = true,
                isGroupAllowed = true,
                cooldownSeconds = 30,
                priority = 8
            ),
            ReplyRule(
                name = "Emergency / Urgent",
                incomingPattern = "urgent, emergency, জরুরি",
                matchType = MatchType.CONTAINS,
                replyText = "Noted your message as urgent! I am currently away, but I will check this within 15 minutes.",
                isEnabled = true,
                isGroupAllowed = false,
                cooldownSeconds = 30,
                priority = 15
            ),
            ReplyRule(
                name = "Default Away Fallback",
                incomingPattern = "*",
                matchType = MatchType.FALLBACK_DEFAULT,
                replyText = "Hello {sender}! I'm currently away from my phone and cannot read messages right now. This is an automated response.",
                isEnabled = true,
                isGroupAllowed = false,
                cooldownSeconds = 30,
                priority = 0
            )
        )
    }
}
