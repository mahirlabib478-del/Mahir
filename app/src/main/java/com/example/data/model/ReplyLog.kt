package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reply_logs")
data class ReplyLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sender: String,
    val incomingMessage: String,
    val repliedText: String,
    val ruleMatchedName: String,
    val status: String = "SENT", // "SENT", "SIMULATED", "SKIPPED_COOLDOWN"
    val isGroup: Boolean = false,
    val platform: String = "WhatsApp" // "WhatsApp", "Messenger"
)
