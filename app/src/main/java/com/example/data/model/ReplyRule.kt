package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reply_rules")
data class ReplyRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val incomingPattern: String,
    val matchType: MatchType,
    val replyText: String,
    val isEnabled: Boolean = true,
    val isGroupAllowed: Boolean = true,
    val cooldownSeconds: Int = 180, // Default 3 min cooldown per sender
    val priority: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
