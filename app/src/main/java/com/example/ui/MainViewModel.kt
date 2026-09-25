package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AutoReplyApp
import com.example.data.model.MatchType
import com.example.data.model.ReplyLog
import com.example.data.model.ReplyRule
import com.example.data.repository.LogRepository
import com.example.data.repository.RuleRepository
import com.example.service.MatchResult
import com.example.service.RuleMatcher
import com.example.service.WhatsAppNotificationListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab {
    DASHBOARD,
    RULES,
    SIMULATOR,
    HISTORY,
    SETTINGS
}

data class SimChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val isIncoming: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val matchedRuleName: String? = null,
    val matchExplanation: String? = null,
    val status: String = "SUCCESS"
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AutoReplyApp
    val ruleRepository = RuleRepository(app.database.replyRuleDao())
    val logRepository = LogRepository(app.database.replyLogDao())
    val preferenceManager = app.preferenceManager

    // Current navigation tab
    private val _currentTab = MutableStateFlow(ScreenTab.DASHBOARD)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Notification Listener permission check
    private val _isPermissionGranted = MutableStateFlow(
        WhatsAppNotificationListener.isPermissionGranted(application)
    )
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    // Master enabled flow
    val isMasterEnabled: StateFlow<Boolean> = preferenceManager.isMasterEnabled

    // Rules
    val allRules: StateFlow<List<ReplyRule>> = ruleRepository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRulesCount: StateFlow<Int> = ruleRepository.totalRulesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val enabledRulesCount: StateFlow<Int> = ruleRepository.enabledRulesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Logs
    val allLogs: StateFlow<List<ReplyLog>> = logRepository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalRepliesCount: StateFlow<Int> = logRepository.totalRepliesCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Rule dialog state
    private val _editingRule = MutableStateFlow<ReplyRule?>(null)
    val editingRule: StateFlow<ReplyRule?> = _editingRule.asStateFlow()

    private val _showRuleDialog = MutableStateFlow(false)
    val showRuleDialog: StateFlow<Boolean> = _showRuleDialog.asStateFlow()

    // Simulator State
    private val _simChatHistory = MutableStateFlow<List<SimChatMessage>>(
        listOf(
            SimChatMessage(
                sender = "Assistant",
                text = "Welcome to the WhatsApp Auto-Reply Simulator! Send any message to test your rules in real-time.",
                isIncoming = false,
                status = "INFO"
            )
        )
    )
    val simChatHistory: StateFlow<List<SimChatMessage>> = _simChatHistory.asStateFlow()

    private val _isSimulatingReply = MutableStateFlow(false)
    val isSimulatingReply: StateFlow<Boolean> = _isSimulatingReply.asStateFlow()

    private val simCooldownMap = mutableMapOf<String, Long>()

    // Toast/Snackbar info message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun checkPermission() {
        _isPermissionGranted.value = WhatsAppNotificationListener.isPermissionGranted(getApplication())
    }

    fun toggleMasterSwitch() {
        val newState = !isMasterEnabled.value
        preferenceManager.setMasterEnabled(newState)
        _snackbarMessage.value = if (newState) "Auto Reply is now ACTIVE" else "Auto Reply is PAUSED"
    }

    fun toggleRule(rule: ReplyRule) {
        viewModelScope.launch {
            ruleRepository.toggleRuleEnabled(rule)
        }
    }

    fun openAddRuleDialog() {
        _editingRule.value = null
        _showRuleDialog.value = true
    }

    fun openEditRuleDialog(rule: ReplyRule) {
        _editingRule.value = rule
        _showRuleDialog.value = true
    }

    fun closeRuleDialog() {
        _showRuleDialog.value = false
        _editingRule.value = null
    }

    fun saveRule(
        name: String,
        pattern: String,
        matchType: MatchType,
        replyText: String,
        isGroupAllowed: Boolean,
        cooldownSeconds: Int,
        priority: Int
    ) {
        viewModelScope.launch {
            val existing = _editingRule.value
            if (existing == null) {
                val newRule = ReplyRule(
                    name = name.ifBlank { "Custom Rule" },
                    incomingPattern = pattern,
                    matchType = matchType,
                    replyText = replyText,
                    isEnabled = true,
                    isGroupAllowed = isGroupAllowed,
                    cooldownSeconds = cooldownSeconds,
                    priority = priority
                )
                ruleRepository.insertRule(newRule)
                _snackbarMessage.value = "New auto reply rule added!"
            } else {
                val updated = existing.copy(
                    name = name.ifBlank { "Custom Rule" },
                    incomingPattern = pattern,
                    matchType = matchType,
                    replyText = replyText,
                    isGroupAllowed = isGroupAllowed,
                    cooldownSeconds = cooldownSeconds,
                    priority = priority
                )
                ruleRepository.updateRule(updated)
                _snackbarMessage.value = "Rule updated successfully!"
            }
            closeRuleDialog()
        }
    }

    fun deleteRule(rule: ReplyRule) {
        viewModelScope.launch {
            ruleRepository.deleteRule(rule)
            _snackbarMessage.value = "Rule deleted"
        }
    }

    fun resetToStarterPresets() {
        viewModelScope.launch {
            ruleRepository.resetToDefaultPresets()
            _snackbarMessage.value = "Default rules imported!"
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            logRepository.clearAllLogs()
            _snackbarMessage.value = "Reply logs cleared"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Interactive Simulator Action
    fun simulateIncomingMessage(
        senderName: String,
        messageText: String,
        isGroup: Boolean
    ) {
        val cleanSender = senderName.ifBlank { "Test Contact" }
        val cleanMsg = messageText.trim()
        if (cleanMsg.isEmpty()) return

        // Add incoming user message to chat
        val incomingChat = SimChatMessage(
            sender = cleanSender,
            text = cleanMsg,
            isIncoming = true
        )
        _simChatHistory.value = _simChatHistory.value + incomingChat

        viewModelScope.launch {
            _isSimulatingReply.value = true
            // Realistic typing delay
            delay(600)

            val rules = allRules.value
            val isBlacklisted = preferenceManager.isContactBlacklisted(cleanSender)
            val replyToGroups = preferenceManager.replyToGroups.value
            val lastTimestamp = simCooldownMap[cleanSender]

            val result = RuleMatcher.evaluate(
                incomingMessage = cleanMsg,
                sender = cleanSender,
                isGroup = isGroup,
                rules = rules,
                isMasterEnabled = preferenceManager.isMasterEnabled.value,
                replyToGroupsAllowed = replyToGroups,
                isContactBlacklisted = isBlacklisted,
                lastReplyTimestamp = lastTimestamp,
                prependAutoReplyTag = preferenceManager.prependTag.value
            )

            when (result) {
                is MatchResult.Success -> {
                    simCooldownMap[cleanSender] = System.currentTimeMillis()
                    val responseMsg = SimChatMessage(
                        sender = "WA AutoReply",
                        text = result.formattedReply,
                        isIncoming = false,
                        matchedRuleName = result.rule.name,
                        matchExplanation = result.explanation,
                        status = "SENT"
                    )
                    _simChatHistory.value = _simChatHistory.value + responseMsg

                    // Also save to actual logs so user sees it in History
                    logRepository.insertLog(
                        ReplyLog(
                            sender = cleanSender,
                            incomingMessage = cleanMsg,
                            repliedText = result.formattedReply,
                            ruleMatchedName = result.rule.name,
                            status = "SIMULATED",
                            isGroup = isGroup
                        )
                    )
                }
                is MatchResult.CooldownSkipped -> {
                    val skippedMsg = SimChatMessage(
                        sender = "System",
                        text = "⏸️ [Cooldown Active] Already replied recently to $cleanSender. Cooldown: ${result.remainingSeconds}s remaining before next reply.",
                        isIncoming = false,
                        matchedRuleName = result.rule.name,
                        matchExplanation = result.reason,
                        status = "COOLDOWN"
                    )
                    _simChatHistory.value = _simChatHistory.value + skippedMsg
                }
                is MatchResult.Blacklisted -> {
                    val blockedMsg = SimChatMessage(
                        sender = "System",
                        text = "🚫 [Blacklisted] Sender '$cleanSender' is in your ignore list. No reply sent.",
                        isIncoming = false,
                        status = "BLOCKED"
                    )
                    _simChatHistory.value = _simChatHistory.value + blockedMsg
                }
                is MatchResult.GroupIgnored -> {
                    val groupMsg = SimChatMessage(
                        sender = "System",
                        text = "👥 [Group Ignored] Auto-reply to groups is turned off in Settings.",
                        isIncoming = false,
                        status = "BLOCKED"
                    )
                    _simChatHistory.value = _simChatHistory.value + groupMsg
                }
                is MatchResult.MasterDisabled -> {
                    val disabledMsg = SimChatMessage(
                        sender = "System",
                        text = "⚠️ [Master OFF] Auto-reply is currently paused. Turn on the Master Switch on the Dashboard.",
                        isIncoming = false,
                        status = "PAUSED"
                    )
                    _simChatHistory.value = _simChatHistory.value + disabledMsg
                }
                is MatchResult.NoRuleMatched -> {
                    val noMatchMsg = SimChatMessage(
                        sender = "System",
                        text = "ℹ️ [No Rule Matched] None of your active rules matched \"$cleanMsg\". Add a Default Fallback rule to answer all messages.",
                        isIncoming = false,
                        status = "NO_MATCH"
                    )
                    _simChatHistory.value = _simChatHistory.value + noMatchMsg
                }
            }

            _isSimulatingReply.value = false
        }
    }

    fun clearSimChat() {
        simCooldownMap.clear()
        _simChatHistory.value = listOf(
            SimChatMessage(
                sender = "Assistant",
                text = "Chat cleared. Send any test message below!",
                isIncoming = false,
                status = "INFO"
            )
        )
    }
}
