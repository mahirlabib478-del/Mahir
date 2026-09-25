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
        try {
            if (AutoReplyApp.instance.preferenceManager.isMasterEnabled.value) {
                AutoReplyForegroundService.startService(applicationContext)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not start AutoReplyForegroundService from listener", e)
        }
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

        // Target WhatsApp (standard, business, GB) and Facebook Messenger (standard, Lite)
        val isWhatsApp = packageName == WHATSAPP_PACKAGE
        val isWhatsAppBusiness = packageName == WHATSAPP_BUSINESS_PACKAGE
        val isGBWhatsApp = packageName == GB_WHATSAPP_PACKAGE
        val isMessenger = packageName == MESSENGER_PACKAGE || packageName == MESSENGER_LITE_PACKAGE

        if (!isWhatsApp && !isWhatsAppBusiness && !isGBWhatsApp && !isMessenger) {
            return
        }

        val platformName = if (isMessenger) "Messenger" else "WhatsApp"

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

        if (isMessenger && !prefs.supportMessenger.value) {
            Log.d(TAG, "Auto-reply ignored: Facebook Messenger support disabled")
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
        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val messages = messagingStyle?.messages ?: emptyList()
        val latestMessage = messages.lastOrNull()
        val textCandidate = latestMessage?.text?.toString()?.trim()
        val person = latestMessage?.person
        val personName = person?.name?.toString()?.trim()
        val convTitle = messagingStyle?.conversationTitle?.toString()?.trim()
        val isGroupStyle = messagingStyle?.isGroupConversation

        // Check if message is from the user (self reply)
        if (latestMessage != null && person == null && textCandidate?.startsWith("[Auto-Reply]", ignoreCase = true) == true) {
            Log.d(TAG, "Ignored self outgoing message in MessagingStyle")
            return
        }

        val isGroup = isGroupConversation(
            sbnTag = sbn.tag,
            sbnKey = sbn.key,
            extras = extras,
            isGroupStyle = isGroupStyle,
            conversationTitle = convTitle,
            personName = personName
        )

        var sender: String? = null
        var incomingMessage: String? = textCandidate

        if (isGroup) {
            // For group conversations: conversationTitle is the group's name
            sender = when {
                !convTitle.isNullOrEmpty() -> convTitle
                !personName.isNullOrEmpty() -> personName
                else -> null
            }
        } else {
            // For 1-on-1 private individual chat: personName is the sender/contact name
            sender = when {
                !personName.isNullOrEmpty() -> personName
                !convTitle.isNullOrEmpty() -> convTitle
                else -> null
            }
        }

        // Fallback sender extraction if MessagingStyle didn't provide one
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

        // If legacy fallback group message was formatted as "Sender: Message", strip prefix
        if (isGroup && messagingStyle == null && incomingMessage.contains(": ")) {
            incomingMessage = incomingMessage.substringAfter(": ").trim()
        }

        // 4. Filter WhatsApp & Messenger system notifications
        if (sender.equals("WhatsApp", ignoreCase = true) || sender.equals("WhatsApp Business", ignoreCase = true)) {
            if (incomingMessage.contains("Checking for new messages", ignoreCase = true) ||
                incomingMessage.contains("WhatsApp Web is active", ignoreCase = true) ||
                incomingMessage.contains("Backup in progress", ignoreCase = true) ||
                incomingMessage.contains("new messages", ignoreCase = true)) {
                return
            }
        }

        if (isMessenger) {
            if (sender.equals("Messenger", ignoreCase = true) ||
                sender.equals("Chat heads active", ignoreCase = true) ||
                incomingMessage.contains("Chat head active", ignoreCase = true) ||
                incomingMessage.contains("displaying over other apps", ignoreCase = true) ||
                incomingMessage.contains("Waiting for network", ignoreCase = true)) {
                return
            }
        }

        // 5. Ignore automated replies or self messages
        if (incomingMessage.startsWith("[Auto-Reply]", ignoreCase = true) ||
            incomingMessage.startsWith("You: ", ignoreCase = true)) {
            Log.d(TAG, "Ignored self/outgoing message")
            return
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

        val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val wakeLock = powerManager?.newWakeLock(
            android.os.PowerManager.PARTIAL_WAKE_LOCK,
            "AutoReply::NotificationProcessingWakeLock"
        )
        wakeLock?.acquire(10_000L) // Keep CPU active for up to 10s while sending

        try {
            val db = app.database
            val rules = try {
                db.replyRuleDao().getEnabledRulesDirect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load rules directly", e)
                emptyList()
            }

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

            var logStatus = ""
            var repliedTextLog = ""
            var ruleMatchedName = ""

            when (result) {
                is MatchResult.Success -> {
                    ruleMatchedName = result.rule.name
                    val replyAction = findReplyAction(notification)
                    if (replyAction != null) {
                        val (actionIntent, remoteInputs) = replyAction
                        val (sent, errorMsg) = sendReply(
                            context = this@WhatsAppNotificationListener,
                            pendingIntent = actionIntent,
                            remoteInputs = remoteInputs,
                            replyText = result.formattedReply,
                            targetPackage = packageName
                        )
                        if (sent) {
                            senderLastRepliedMap[targetSender] = System.currentTimeMillis()
                            logStatus = "SENT"
                            repliedTextLog = result.formattedReply
                            Log.i(TAG, "Successfully auto-replied on $platformName to $targetSender: ${result.formattedReply}")
                        } else {
                            logStatus = "SEND_FAILED"
                            repliedTextLog = "[Send Failed: $errorMsg]"
                        }
                    } else {
                        logStatus = "NO_REPLY_ACTION"
                        repliedTextLog = "[No quick-reply action on notification. Ensure $platformName chat is not open on screen.]"
                        Log.w(TAG, "No quick-reply action found on notification from $targetSender")
                    }
                }
                is MatchResult.CooldownSkipped -> {
                    ruleMatchedName = result.rule.name
                    logStatus = "SKIPPED_COOLDOWN"
                    repliedTextLog = "[Skipped: ${result.remainingSeconds}s cooldown left]"
                    Log.i(TAG, "Skipped reply to $targetSender due to cooldown")
                }
                is MatchResult.Blacklisted -> {
                    ruleMatchedName = "Blacklist"
                    logStatus = "BLACKLISTED"
                    repliedTextLog = "[Ignored: Sender is in blacklist]"
                }
                is MatchResult.GroupIgnored -> {
                    ruleMatchedName = "Group Setting"
                    logStatus = "GROUP_IGNORED"
                    repliedTextLog = "[Ignored: Group auto-reply disabled in settings]"
                }
                is MatchResult.NoRuleMatched -> {
                    ruleMatchedName = "No Match"
                    logStatus = "NO_RULE_MATCH"
                    repliedTextLog = "[No rule matched: \"$targetText\"]"
                    Log.d(TAG, "No rule matched for message: $targetText from $targetSender")
                }
                else -> {}
            }

            // Save log entry to DB asynchronously
            if (logStatus.isNotEmpty() && repliedTextLog.isNotEmpty()) {
                val logEntry = ReplyLog(
                    sender = targetSender,
                    incomingMessage = targetText,
                    repliedText = repliedTextLog,
                    ruleMatchedName = ruleMatchedName,
                    status = logStatus,
                    isGroup = isGroup,
                    platform = platformName
                )
                serviceScope.launch(Dispatchers.IO) {
                    try {
                        db.replyLogDao().insertLog(logEntry)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error inserting log to db", e)
                    } finally {
                        try {
                            if (wakeLock?.isHeld == true) {
                                wakeLock.release()
                            }
                        } catch (_: Exception) {}
                    }
                }
            } else {
                try {
                    if (wakeLock?.isHeld == true) {
                        wakeLock.release()
                    }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating auto reply", e)
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
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
        replyText: String,
        targetPackage: String? = null
    ): Pair<Boolean, String> {
        val replyIntent = Intent()
        val resultsBundle = Bundle()

        // 1. Add result to each declared RemoteInput
        for (remoteInput in remoteInputs) {
            resultsBundle.putCharSequence(remoteInput.resultKey, replyText)
        }
        // Direct key fallbacks for WhatsApp and Android Auto
        resultsBundle.putCharSequence("key_text_reply", replyText)
        resultsBundle.putCharSequence("android.intent.extra.TEXT", replyText)
        resultsBundle.putCharSequence(Intent.EXTRA_TEXT, replyText)

        // Attach results via RemoteInput
        android.app.RemoteInput.addResultsToIntent(remoteInputs, replyIntent, resultsBundle)
        replyIntent.putExtras(resultsBundle)

        var lastError = "Unknown error"

        // Strategy 1: Standard send with context (works on 99% of Android versions without SecurityException)
        try {
            pendingIntent.send(context, 0, replyIntent)
            Log.i(TAG, "Reply sent successfully via Strategy 1 (context.send)")
            return Pair(true, "")
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"
            Log.w(TAG, "Strategy 1 failed: $lastError")
        }

        // Strategy 2: Standard send with applicationContext
        try {
            pendingIntent.send(context.applicationContext, 0, replyIntent)
            Log.i(TAG, "Reply sent successfully via Strategy 2 (applicationContext.send)")
            return Pair(true, "")
        } catch (e: Exception) {
            lastError = "${e.javaClass.simpleName}: ${e.message}"
            Log.w(TAG, "Strategy 2 failed: $lastError")
        }

        // Strategy 3: Try with ActivityOptions for Android 14+ if BAL is enforced
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val activityOptions = android.app.ActivityOptions.makeBasic()
                activityOptions.setPendingIntentBackgroundActivityStartMode(
                    android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
                pendingIntent.send(context.applicationContext, 0, replyIntent, null, null, null, activityOptions.toBundle())
                Log.i(TAG, "Reply sent successfully via Strategy 3 (ActivityOptions)")
                return Pair(true, "")
            } catch (e: Exception) {
                lastError = "${e.javaClass.simpleName}: ${e.message}"
                Log.w(TAG, "Strategy 3 failed: $lastError")
            }
        }

        // Strategy 4: Explicit package if targetPackage is specified
        if (!targetPackage.isNullOrEmpty()) {
            try {
                val explicitIntent = Intent(replyIntent).apply {
                    setPackage(targetPackage)
                }
                pendingIntent.send(context.applicationContext, 0, explicitIntent)
                Log.i(TAG, "Reply sent successfully via Strategy 4 (explicit package)")
                return Pair(true, "")
            } catch (e: Exception) {
                lastError = "${e.javaClass.simpleName}: ${e.message}"
                Log.w(TAG, "Strategy 4 failed: $lastError")
            }
        }

        return Pair(false, lastError)
    }

    companion object {
        private const val TAG = "WAAutoReplyService"
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
        const val GB_WHATSAPP_PACKAGE = "com.gbwhatsapp"
        const val MESSENGER_PACKAGE = "com.facebook.orca"
        const val MESSENGER_LITE_PACKAGE = "com.facebook.mlite"

        @Volatile
        var isServiceConnected: Boolean = false
            private set

        /**
         * Accurately determines if a WhatsApp notification is from a group conversation or
         * an individual 1-on-1 private chat.
         */
        fun isGroupConversation(
            sbnTag: String?,
            sbnKey: String?,
            extras: Bundle?,
            isGroupStyle: Boolean?,
            conversationTitle: String?,
            personName: String?
        ): Boolean {
            // 1. WhatsApp JID format check in tag or key
            // WhatsApp group chat JIDs end with "@g.us" (e.g. 120363024829384920@g.us)
            // Individual user JIDs end with "@s.whatsapp.net" or contact phone numbers
            if (sbnTag?.contains("@g.us", ignoreCase = true) == true ||
                sbnKey?.contains("@g.us", ignoreCase = true) == true
            ) {
                return true
            }
            if (sbnTag?.contains("@s.whatsapp.net", ignoreCase = true) == true ||
                sbnKey?.contains("@s.whatsapp.net", ignoreCase = true) == true
            ) {
                return false
            }

            // 2. Android official group conversation flag (API 28+ / NotificationCompat)
            if (extras != null && extras.containsKey(Notification.EXTRA_IS_GROUP_CONVERSATION)) {
                return extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
            }

            // 3. NotificationCompat.MessagingStyle group flag
            if (isGroupStyle == true) {
                return true
            }

            // 4. In MessagingStyle: For group chats, conversationTitle represents the group name,
            // while personName represents the individual participant who sent the message.
            // In 1-on-1 private chats, conversationTitle is either null, empty, or equal to personName.
            if (!conversationTitle.isNullOrEmpty() && !personName.isNullOrEmpty() &&
                !conversationTitle.equals(personName, ignoreCase = true)
            ) {
                return true
            }

            return false
        }

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
