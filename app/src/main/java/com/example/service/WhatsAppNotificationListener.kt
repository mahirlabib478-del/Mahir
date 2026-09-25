package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.AutoReplyApp
import com.example.data.model.ReplyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class WhatsAppNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val senderLastRepliedMap = ConcurrentHashMap<String, Long>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // Target WhatsApp or WhatsApp Business
        val isWhatsApp = packageName == WHATSAPP_PACKAGE
        val isWhatsAppBusiness = packageName == WHATSAPP_BUSINESS_PACKAGE

        if (!isWhatsApp && !isWhatsAppBusiness) {
            return
        }

        val app = try {
            AutoReplyApp.instance
        } catch (e: Exception) {
            return
        }

        val prefs = app.preferenceManager
        if (!prefs.isMasterEnabled.value) return
        if (isWhatsAppBusiness && !prefs.supportWhatsAppBusiness.value) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Avoid answering ongoing notifications like active audio/video calls or media playback
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: return

        // WhatsApp system notifications like "Checking for new messages..." or "WhatsApp Web is active"
        if (text.isEmpty() || title.isEmpty()) return
        if (title.equals("WhatsApp", ignoreCase = true) || title.equals("WhatsApp Business", ignoreCase = true)) {
            if (text.contains("Checking for new messages", ignoreCase = true) ||
                text.contains("WhatsApp Web is active", ignoreCase = true) ||
                text.contains("Backup in progress", ignoreCase = true)) {
                return
            }
        }

        val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
                extras.containsKey(Notification.EXTRA_SUB_TEXT) ||
                title.contains(":")

        val sender = title

        // Check if contact is blacklisted
        val isBlacklisted = prefs.isContactBlacklisted(sender)
        val replyToGroups = prefs.replyToGroups.value
        val prependTag = prefs.prependTag.value
        val lastTimestamp = senderLastRepliedMap[sender]

        serviceScope.launch {
            try {
                val db = app.database
                val rules = db.replyRuleDao().getEnabledRulesSync()

                val result = RuleMatcher.evaluate(
                    incomingMessage = text,
                    sender = sender,
                    isGroup = isGroup,
                    rules = rules,
                    isMasterEnabled = prefs.isMasterEnabled.value,
                    replyToGroupsAllowed = replyToGroups,
                    isContactBlacklisted = isBlacklisted,
                    lastReplyTimestamp = lastTimestamp,
                    prependAutoReplyTag = prependTag
                )

                when (result) {
                    is MatchResult.Success -> {
                        val replyAction = findWearReplyAction(notification)
                        if (replyAction != null) {
                            val sent = sendReply(replyAction, result.formattedReply)
                            if (sent) {
                                senderLastRepliedMap[sender] = System.currentTimeMillis()
                                db.replyLogDao().insertLog(
                                    ReplyLog(
                                        sender = sender,
                                        incomingMessage = text,
                                        repliedText = result.formattedReply,
                                        ruleMatchedName = result.rule.name,
                                        status = "SENT",
                                        isGroup = isGroup
                                    )
                                )
                                Log.d(TAG, "Successfully auto-replied to $sender: ${result.formattedReply}")
                            }
                        } else {
                            Log.w(TAG, "No quick-reply action found on notification from $sender")
                        }
                    }
                    is MatchResult.CooldownSkipped -> {
                        db.replyLogDao().insertLog(
                            ReplyLog(
                                sender = sender,
                                incomingMessage = text,
                                repliedText = "[Skipped: ${result.remainingSeconds}s cooldown left]",
                                ruleMatchedName = result.rule.name,
                                status = "SKIPPED_COOLDOWN",
                                isGroup = isGroup
                            )
                        )
                    }
                    is MatchResult.Blacklisted -> {
                        Log.i(TAG, "Skipped reply because sender $sender is blacklisted")
                    }
                    else -> {
                        // Other non-matching cases
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating auto reply", e)
            }
        }
    }

    private fun findWearReplyAction(notification: Notification): NotificationCompat.Action? {
        val wearExtender = NotificationCompat.WearableExtender(notification)
        for (action in wearExtender.actions) {
            if (action.remoteInputs != null && action.remoteInputs!!.isNotEmpty()) {
                return action
            }
        }

        // Search regular notification actions
        val actionCount = NotificationCompat.getActionCount(notification)
        for (i in 0 until actionCount) {
            val action = NotificationCompat.getAction(notification, i)
            if (action?.remoteInputs != null && action.remoteInputs!!.isNotEmpty()) {
                return action
            }
        }
        return null
    }

    private fun sendReply(action: NotificationCompat.Action, replyText: String): Boolean {
        return try {
            val intent = Intent()
            val bundle = Bundle()
            for (remoteInput in action.remoteInputs!!) {
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(
                action.remoteInputs!!.map {
                    android.app.RemoteInput.Builder(it.resultKey)
                        .setLabel(it.label)
                        .setChoices(it.choices)
                        .setAllowFreeFormInput(it.allowFreeFormInput)
                        .addExtras(it.extras)
                        .build()
                }.toTypedArray(),
                intent,
                bundle
            )
            val actionIntent = action.actionIntent ?: return false
            actionIntent.send(applicationContext, 0, intent)
            true
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "Reply PendingIntent canceled", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send auto-reply", e)
            false
        }
    }

    companion object {
        private const val TAG = "WAAutoReplyService"
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun openPermissionSettings(context: Context) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
