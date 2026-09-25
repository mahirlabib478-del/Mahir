package com.example.data.model

enum class MatchType(val displayName: String, val description: String) {
    EXACT("Exact Match", "Matches when the incoming message equals this exact text (case-insensitive)"),
    CONTAINS("Contains Keyword", "Matches if the message contains this word or phrase anywhere"),
    STARTS_WITH("Starts With", "Matches if the incoming message begins with this text"),
    REGEX("Regular Expression", "Advanced regex pattern matching"),
    FALLBACK_DEFAULT("Default / Fallback", "Replies to any incoming message when no other rules match")
}
