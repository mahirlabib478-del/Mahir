package com.example.service

import com.example.data.model.MatchType
import com.example.data.model.ReplyRule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class MatchResult {
    data class Success(
        val rule: ReplyRule,
        val formattedReply: String,
        val explanation: String
    ) : MatchResult()

    data class CooldownSkipped(
        val rule: ReplyRule,
        val remainingSeconds: Long,
        val reason: String
    ) : MatchResult()

    data class Blacklisted(
        val sender: String,
        val reason: String = "Sender '$sender' is on the blacklist."
    ) : MatchResult()

    object MasterDisabled : MatchResult()

    object GroupIgnored : MatchResult()

    object NoRuleMatched : MatchResult()
}

object RuleMatcher {

    fun evaluate(
        incomingMessage: String,
        sender: String,
        isGroup: Boolean,
        rules: List<ReplyRule>,
        isMasterEnabled: Boolean,
        replyToGroupsAllowed: Boolean,
        isContactBlacklisted: Boolean,
        lastReplyTimestamp: Long? = null,
        prependAutoReplyTag: Boolean = true
    ): MatchResult {
        if (!isMasterEnabled) {
            return MatchResult.MasterDisabled
        }

        if (isContactBlacklisted) {
            return MatchResult.Blacklisted(sender)
        }

        if (isGroup && !replyToGroupsAllowed) {
            return MatchResult.GroupIgnored
        }

        val cleanedIncoming = incomingMessage.trim()
        val enabledRules = rules.filter { it.isEnabled }.sortedByDescending { it.priority }

        // Find candidate rule
        for (rule in enabledRules) {
            if (isGroup && !rule.isGroupAllowed) {
                continue
            }

            val isMatch = matchesPattern(cleanedIncoming, rule.incomingPattern, rule.matchType)
            if (isMatch) {
                // Check cooldown for this sender
                val now = System.currentTimeMillis()
                if (lastReplyTimestamp != null && rule.cooldownSeconds > 0) {
                    val elapsedSeconds = (now - lastReplyTimestamp) / 1000
                    if (elapsedSeconds < rule.cooldownSeconds) {
                        val remaining = rule.cooldownSeconds - elapsedSeconds
                        return MatchResult.CooldownSkipped(
                            rule = rule,
                            remainingSeconds = remaining,
                            reason = "Cooldown active: already replied within last ${rule.cooldownSeconds}s ($remaining s remaining)."
                        )
                    }
                }

                // Format reply message with variable replacements
                val formattedReply = formatReplyText(
                    rawTemplate = rule.replyText,
                    sender = sender,
                    prependTag = prependAutoReplyTag
                )

                val explanation = when (rule.matchType) {
                    MatchType.EXACT -> "Matched exact text: \"${rule.incomingPattern}\""
                    MatchType.CONTAINS -> "Keyword \"${rule.incomingPattern}\" found in incoming message"
                    MatchType.STARTS_WITH -> "Message starts with \"${rule.incomingPattern}\""
                    MatchType.REGEX -> "Matched regex pattern: /${rule.incomingPattern}/"
                    MatchType.FALLBACK_DEFAULT -> "Triggered default fallback response"
                }

                return MatchResult.Success(
                    rule = rule,
                    formattedReply = formattedReply,
                    explanation = explanation
                )
            }
        }

        return MatchResult.NoRuleMatched
    }

    private fun matchesPattern(message: String, pattern: String, matchType: MatchType): Boolean {
        if (matchType == MatchType.FALLBACK_DEFAULT) {
            return true
        }

        val trimmedPattern = pattern.trim()
        if (trimmedPattern.isEmpty()) return false

        val normalizedMsg = message.trim().trimEnd('.', '!', '?', ',', ':', ';').trim()

        return when (matchType) {
            MatchType.EXACT -> {
                // Support multiple comma-separated exact triggers
                val triggers = trimmedPattern.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                triggers.any {
                    it.equals(message, ignoreCase = true) || it.equals(normalizedMsg, ignoreCase = true)
                }
            }
            MatchType.CONTAINS -> {
                val keywords = trimmedPattern.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }
                val lowerMsg = message.lowercase(Locale.ROOT)
                val lowerNorm = normalizedMsg.lowercase(Locale.ROOT)
                keywords.any { kw ->
                    if (kw.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }) {
                        // Use word boundary for alphanumeric words
                        Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(message)
                    } else {
                        // For Unicode/Bengali or symbols, direct substring match
                        lowerMsg.contains(kw) || lowerNorm.contains(kw)
                    }
                }
            }
            MatchType.STARTS_WITH -> {
                val prefixes = trimmedPattern.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                prefixes.any {
                    message.startsWith(it, ignoreCase = true) || normalizedMsg.startsWith(it, ignoreCase = true)
                }
            }
            MatchType.REGEX -> {
                try {
                    Regex(trimmedPattern, RegexOption.IGNORE_CASE).containsMatchIn(message)
                } catch (e: Exception) {
                    false
                }
            }
            MatchType.FALLBACK_DEFAULT -> true
        }
    }

    fun formatReplyText(rawTemplate: String, sender: String, prependTag: Boolean): String {
        val cleanSender = sender.ifBlank { "Friend" }
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val now = Date()

        var formatted = rawTemplate
            .replace("{sender}", cleanSender, ignoreCase = true)
            .replace("{name}", cleanSender, ignoreCase = true)
            .replace("{time}", timeFormat.format(now), ignoreCase = true)
            .replace("{date}", dateFormat.format(now), ignoreCase = true)

        if (prependTag && !formatted.startsWith("[Auto-Reply]", ignoreCase = true)) {
            formatted = "[Auto-Reply]\n$formatted"
        }

        return formatted
    }
}
