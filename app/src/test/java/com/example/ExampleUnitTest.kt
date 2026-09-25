package com.example

import com.example.data.model.MatchType
import com.example.data.model.ReplyRule
import com.example.service.MatchResult
import com.example.service.RuleMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val sampleRules = listOf(
        ReplyRule(
            id = 1,
            name = "Greeting",
            incomingPattern = "hello, hi",
            matchType = MatchType.CONTAINS,
            replyText = "Hello {sender}!",
            isEnabled = true,
            isGroupAllowed = false,
            cooldownSeconds = 60,
            priority = 10
        ),
        ReplyRule(
            id = 2,
            name = "Price",
            incomingPattern = "price",
            matchType = MatchType.CONTAINS,
            replyText = "Our pricing is 500 BDT.",
            isEnabled = true,
            isGroupAllowed = true,
            cooldownSeconds = 30,
            priority = 5
        ),
        ReplyRule(
            id = 3,
            name = "Fallback",
            incomingPattern = "*",
            matchType = MatchType.FALLBACK_DEFAULT,
            replyText = "I will reply soon.",
            isEnabled = true,
            isGroupAllowed = false,
            cooldownSeconds = 0,
            priority = 0
        )
    )

    @Test
    fun testRuleMatcher_keywordMatch() {
        val result = RuleMatcher.evaluate(
            incomingMessage = "Can you please tell me the price of this?",
            sender = "Karim",
            isGroup = false,
            rules = sampleRules,
            isMasterEnabled = true,
            replyToGroupsAllowed = false,
            isContactBlacklisted = false,
            prependAutoReplyTag = false
        )

        assertTrue(result is MatchResult.Success)
        val success = result as MatchResult.Success
        assertEquals("Price", success.rule.name)
        assertEquals("Our pricing is 500 BDT.", success.formattedReply)
    }

    @Test
    fun testRuleMatcher_variableReplacement() {
        val result = RuleMatcher.evaluate(
            incomingMessage = "Hello friend",
            sender = "Tanvir",
            isGroup = false,
            rules = sampleRules,
            isMasterEnabled = true,
            replyToGroupsAllowed = false,
            isContactBlacklisted = false,
            prependAutoReplyTag = false
        )

        assertTrue(result is MatchResult.Success)
        val success = result as MatchResult.Success
        assertEquals("Greeting", success.rule.name)
        assertEquals("Hello Tanvir!", success.formattedReply)
    }

    @Test
    fun testRuleMatcher_masterDisabled() {
        val result = RuleMatcher.evaluate(
            incomingMessage = "Hello",
            sender = "Tanvir",
            isGroup = false,
            rules = sampleRules,
            isMasterEnabled = false,
            replyToGroupsAllowed = false,
            isContactBlacklisted = false
        )

        assertTrue(result is MatchResult.MasterDisabled)
    }

    @Test
    fun testRuleMatcher_blacklisted() {
        val result = RuleMatcher.evaluate(
            incomingMessage = "Hello",
            sender = "BlockedSpammer",
            isGroup = false,
            rules = sampleRules,
            isMasterEnabled = true,
            replyToGroupsAllowed = false,
            isContactBlacklisted = true
        )

        assertTrue(result is MatchResult.Blacklisted)
    }

    @Test
    fun testRuleMatcher_fallbackDefault() {
        val result = RuleMatcher.evaluate(
            incomingMessage = "What is the weather today?",
            sender = "Unknown",
            isGroup = false,
            rules = sampleRules,
            isMasterEnabled = true,
            replyToGroupsAllowed = false,
            isContactBlacklisted = false,
            prependAutoReplyTag = false
        )

        assertTrue(result is MatchResult.Success)
        val success = result as MatchResult.Success
        assertEquals("Fallback", success.rule.name)
        assertEquals("I will reply soon.", success.formattedReply)
    }
}
