package com.example

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.service.MatchResult
import com.example.service.RuleMatcher
import com.example.service.WhatsAppNotificationListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WA AutoReply", appName)
    }

    @Test
    fun `test individual chat from Ammu is not classified as group`() {
        val extras = Bundle().apply {
            putBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
            putString(Notification.EXTRA_TITLE, "আম্মু")
            putString(Notification.EXTRA_SUB_TEXT, "WhatsApp")
        }

        val isGroup = WhatsAppNotificationListener.isGroupConversation(
            sbnTag = "8801700000000@s.whatsapp.net",
            sbnKey = "0|com.whatsapp|1|8801700000000@s.whatsapp.net|1000",
            extras = extras,
            isGroupStyle = false,
            conversationTitle = "আম্মু",
            personName = "আম্মু"
        )

        assertFalse("Individual chat from Ammu must NOT be classified as group", isGroup)
    }

    @Test
    fun `test group chat is accurately classified as group`() {
        val extras = Bundle().apply {
            putBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, true)
            putString(Notification.EXTRA_TITLE, "Family Group")
        }

        val isGroup = WhatsAppNotificationListener.isGroupConversation(
            sbnTag = "120363024829384920@g.us",
            sbnKey = "0|com.whatsapp|1|120363024829384920@g.us|1000",
            extras = extras,
            isGroupStyle = true,
            conversationTitle = "Family Group",
            personName = "Bhai"
        )

        assertTrue("Group chat must be classified as group", isGroup)
    }

    @Test
    fun `test individual chat replies when group reply is disabled in settings`() {
        val rules = AppDatabase.defaultStarterRules

        val result = RuleMatcher.evaluate(
            incomingMessage = "Hi",
            sender = "আম্মু",
            isGroup = false,
            rules = rules,
            isMasterEnabled = true,
            replyToGroupsAllowed = false, // User turned OFF group auto-reply
            isContactBlacklisted = false,
            prependAutoReplyTag = true
        )

        assertTrue("1-on-1 message must succeed even when group reply is disabled", result is MatchResult.Success)
        val success = result as MatchResult.Success
        assertEquals("Greeting & Welcome", success.rule.name)
        assertTrue(success.formattedReply.contains("Hello আম্মু!"))
    }
}
