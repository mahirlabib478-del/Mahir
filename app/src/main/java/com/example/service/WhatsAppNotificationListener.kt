package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.net.Uri
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
    private val recentProcessedMessages = ConcurrentHashMap<String, Long>()

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        Log.i(TAG, "WhatsApp NotificationListenerService connected successfully")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
        Log.w(TAG, "WhatsApp NotificationListenerService disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // Target WhatsApp, WhatsApp Business, or GBWhatsApp
        val isWhatsApp = packageName == WHATSAPP_PACKAGE
        val isWhatsAppBusiness = packageName == WHATSAPP_BUSINESS_PACKAGE
        val isGBWhatsApp = packageName == GB_WHATSAPP_PACKAGE

        if (!isWhatsApp && !isWhatsAppBusiness && !isGBWhatsApp) {
            return
        }

        val app = try {
            AutoReplyApp.instance
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get AutoReplyApp instance", e)
            return
        }

        val prefs = app.preferenceManager
        if (!prefs.isMasterEnabled.value) {
            Log.d(TAG, "Auto-reply ignored: Master switch is OFF")
            return
        }

        if (isWhatsAppBusiness && !prefs.supportWhatsAppBusiness.value) {
            Log.d(TAG, "Auto-reply ignored: WhatsApp Business support disabled")
            return
        }

        val notification = sbn.notification ?: return

        // 1. Avoid answering ongoing notifications like active voice/video calls or media
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) {
            Log.d(TAG, "Ignored ongoing call or media notification")
            return
        }

        // 2. Avoid WhatsApp group summary notifications (e.g. "WhatsApp: 2 new messages")
        // Summary notifications do not have direct message remote inputs.
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            Log.d(TAG, "Ignored WhatsApp group summary notification")
            return
        }

        val extras = notification.extras ?: return

        // 3. Extract Message Text and Sender reliably (handles modern MessagingStyle)
        var sender: String? = null
        var incomingMessage: String? = null
        var isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)

        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        if (messagingStyle != null) {
            val messages = messagingStyle.messages
            if (messages.isNotEmpty()) {
                val latestMessage = messages.last()
                val textCandidate = latestMessage.text?.toString()?.trim()

                // Check if message is from the user (self reply)
                val person = latestMessage.person
                val personName = person?.name?.toString()?.trim()
                if (person == null && textCandidate?.startsWith("[Auto-Reply]", ignoreCase = true) == true) {
                    Log.d(TAG, "Ignored self outgoing message in MessagingStyle")
                    return
                }

                incomingMessage = textCandidate

                val convTitle = messagingStyle.conversationTitle?.toString()?.trim()
                if (!convTitle.isNullOrEmpty()) {
                    isGroup = true
                    sender = convTitle
                } else if (!personName.isNullOrEmpty()) {
                    sender = personName
                }
            }
        }

        // Fallback sender extraction
        if (sender.isNullOrEmpty()) {
            sender = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
                ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        }

        // Fallback message text extraction
        if (incomingMessage.isNullOrEmpty()) {
            incomingMessage = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
                ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.lastOrNull()?.toString()?.trim()
        }

        if (sender.isNullOrEmpty() || incomingMessage.isNullOrEmpty()) {
            Log.d(TAG, "Sender or incoming message is empty")
            return
        }

        // 4. Filter WhatsApp system notifications
        if (sender.equals("WhatsApp", ignoreCase = true) || sender.equals("WhatsApp Business", ignoreCase = true)) {
            if (incomingMessage.contains("Checking for new messages", ignoreCase = true) ||
                incomingMessage.contains("WhatsApp Web is active", ignoreCase = true) ||
                incomingMessage.contains("Backup in progress", ignoreCase = true) ||
                incomingMessage.contains("new messages", ignoreCase = true)) {
                return
            }
        }

        // 5. Ignore automated replies or self messages
        if (incomingMessage.startsWith("[Auto-Reply]", ignoreCase = true) ||
            incomingMessage.startsWith("You: ", ignoreCase = true)) {
            Log.d(TAG, "Ignored self/outgoing message")
            return
        }

        if (!isGroup) {
            isGroup = extras.containsKey(Notification.EXTRA_SUB_TEXT) || sender.contains(":")
        }

        // 6. De-duplicate rapid duplicate notifications for the exact same message
        val dedupKey = "$sender:$incomingMessage"
        val now = System.currentTimeMillis()
        val lastSeen = recentProcessedMessages[dedupKey]
        if (lastSeen != null && (now - lastSeen) < 5000) {
            Log.d(TAG, "Ignored duplicate notification event within 5s for: $dedupKey")
            return
        }
        recentProcessedMessages[dedupKey] = now

        // Check if contact is blacklisted
        val isBlacklisted = prefs.isContactBlacklisted(sender)
        val replyToGroups = prefs.replyToGroups.value
        val prependTag = prefs.prependTag.value
        val lastTimestamp = senderLastRepliedMap[sender]

        val targetSender = sender
        val targetText = incomingMessage

        serviceScope.launch {
            try {
                val db = app.database
                val rules = db.replyRuleDao().getEnabledRulesSync()

                val result = RuleMatcher.evaluate(
                    incomingMessage = targetText,
                    sender = targetSender,
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
                        val replyAction = findReplyAction(notification)
                        if (replyAction != null) {
                            val (actionIntent, remoteInputs) = replyAction
                            val sent = sendReply(
                                context = applicationContext,
                                pendingIntent = actionIntent,
                                remoteInputs = remoteInputs,
                                replyText = result.formattedReply
                            )
                            if (sent) {
                                senderLastRepliedMap[targetSender] = System.currentTimeMillis()
                                db.replyLogDao().insertLog(
                                    ReplyLog(
                                        sender = targetSender,
                                        incomingMessage = targetText,
                                        repliedText = result.formattedReply,
                                        ruleMatchedName = result.rule.name,
                                        status = "SENT",
                                        isGroup = isGroup
                                    )
                                )
                                Log.i(TAG, "Successfully auto-replied to $targetSender: ${result.formattedReply}")
                            } else {
                                db.replyLogDao().insertLog(
                                    ReplyLog(
                                        sender = targetSender,
                                        incomingMessage = targetText,
                                        repliedText = "[Failed to trigger reply PendingIntent]",
                                        ruleMatchedName = result.rule.name,
                                        status = "SEND_FAILED",
                                        isGroup = isGroup
                                    )
                                )
                            }
                        } else {
                            Log.w(TAG, "No quick-reply action found on notification from $targetSender")
                            db.replyLogDao().insertLog(
                                ReplyLog(
                                    sender = targetSender,
                                    incomingMessage = targetText,
                                    repliedText = "[No quick-reply action on notification. Ensure WhatsApp is closed/locked.]",
                                    ruleMatchedName = result.rule.name,
                                    status = "NO_REPLY_ACTION",
                                    isGroup = isGroup
                                )
                            )
                        }
                    }
                    is MatchResult.CooldownSkipped -> {
                        db.replyLogDao().insertLog(
                            ReplyLog(
                                sender = targetSender,
                                incomingMessage = targetText,
                                repliedText = "[Skipped: ${result.remainingSeconds}s cooldown left]",
                                ruleMatchedName = result.rule.name,
                                status = "SKIPPED_COOLDOWN",
                                isGroup = isGroup
                            )
                        )
                        Log.i(TAG, "Skipped reply to $targetSender due to cooldown")
                    }
                    is MatchResult.Blacklisted -> {
                        db.replyLogDao().insertLog(
                            ReplyLog(
                                sender = targetSender,
                                incomingMessage = targetText,
                                repliedText = "[Ignored: Sender is in blacklist]",
                                ruleMatchedName = "Blacklist",
                                status = "BLACKLISTED",
                                isGroup = isGroup
                            )
                        )
                    }
                    is MatchResult.GroupIgnored -> {
                        db.replyLogDao().insertLog(
                            ReplyLog(
                                sender = targetSender,
                                incomingMessage = targetText,
                                repliedText = "[Ignored: Group auto-reply disabled in settings]",
                                ruleMatchedName = "Group Setting",
                                status = "GROUP_IGNORED",
                                isGroup = isGroup
                            )
                        )
                    }
                    is MatchResult.NoRuleMatched -> {
                        db.replyLogDao().insertLog(
                            ReplyLog(
                                sender = targetSender,
                                incomingMessage = targetText,
                                repliedText = "[No rule matched: \"$targetText\"]",
                                ruleMatchedName = "No Match",
                                status = "NO_RULE_MATCH",
                                isGroup = isGroup
                            )
                        )
                        Log.d(TAG, "No rule matched for message: $targetText from $targetSender")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating auto reply", e)
            }
        }
    }

    /**
     * Finds the quick reply action and its RemoteInputs across native actions, WearableExtender,
     * and NotificationCompat actions.
     */
    private fun findReplyAction(notification: Notification): Pair<PendingIntent, Array<android.app.RemoteInput>>? {
        // 1. Check native notification.actions directly (Modern Android 7.0 - 15)
        notification.actions?.forEach { action ->
            val inputs = action.remoteInputs
            if (inputs != null && inputs.isNotEmpty()) {
                val hasReplyKey = inputs.any {
                    it.resultKey.contains("reply", ignoreCase = true) ||
                    it.resultKey.contains("text", ignoreCase = true) ||
                    it.resultKey.contains("message", ignoreCase = true)
                }
                val isReplyTitle = action.title?.toString()?.contains("reply", ignoreCase = true) == true
                val isReplySemantic = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) &&
                        action.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY

                if (hasReplyKey || isReplyTitle || isReplySemantic || inputs.isNotEmpty()) {
                    action.actionIntent?.let { intent ->
                        return Pair(intent, inputs)
                    }
                }
            }
        }

        // 2. Check WearableExtender actions
        val wearExtender = NotificationCompat.WearableExtender(notification)
        for (action in wearExtender.actions) {
            val inputs = action.remoteInputs
            if (inputs != null && inputs.isNotEmpty()) {
                val nativeInputs = inputs.map { compatInput ->
                    android.app.RemoteInput.Builder(compatInput.resultKey)
                        .setLabel(compatInput.label)
                        .setChoices(compatInput.choices)
                        .setAllowFreeFormInput(compatInput.allowFreeFormInput)
                        .addExtras(compatInput.extras)
                        .build()
                }.toTypedArray()
                action.actionIntent?.let { intent ->
                    return Pair(intent, nativeInputs)
                }
            }
        }

        // 3. Check NotificationCompat actions
        val actionCount = NotificationCompat.getActionCount(notification)
        for (i in 0 until actionCount) {
            val action = NotificationCompat.getAction(notification, i)
            val inputs = action?.remoteInputs
            if (inputs != null && inputs.isNotEmpty()) {
                val nativeInputs = inputs.map { compatInput ->
                    android.app.RemoteInput.Builder(compatInput.resultKey)
                        .setLabel(compatInput.label)
                        .setChoices(compatInput.choices)
                        .setAllowFreeFormInput(compatInput.allowFreeFormInput)
                        .addExtras(compatInput.extras)
                        .build()
                }.toTypedArray()
                action.actionIntent?.let { intent ->
                    return Pair(intent, nativeInputs)
                }
            }
        }

        return null
    }

    private fun sendReply(
        context: Context,
        pendingIntent: PendingIntent,
        remoteInputs: Array<android.app.RemoteInput>,
        replyText: String
    ): Boolean {
        return try {
            val intent = Intent()
            val bundle = Bundle()
            for (remoteInput in remoteInputs) {
                bundle.putCharSequence(remoteInput.resultKey, replyText)
            }
            android.app.RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)

            // Direct key fallback for WhatsApp compatibility
            bundle.putCharSequence("key_text_reply", replyText)
            intent.putExtras(bundle)

            pendingIntent.send(context, 0, intent)
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
        const val GB_WHATSAPP_PACKAGE = "com.gbwhatsapp"

        @Volatile
        var isServiceConnected: Boolean = false
            private set

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

        fun openAppDetailsSettings(context: Context) {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + context.packageName)
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }

        /**
         * Reconnects or re-binds the NotificationListenerService in case Android stopped it.
         */
        fun reconnectService(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    requestRebind(ComponentName(context, WhatsAppNotificationListener::class.java))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to call requestRebind", e)
            }

            try {
                val cn = ComponentName(context, WhatsAppNotificationListener::class.java)
                val pm = context.packageManager
                pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle component state", e)
            }
        }
    }
}
