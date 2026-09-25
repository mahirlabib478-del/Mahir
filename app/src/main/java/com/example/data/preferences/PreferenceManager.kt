package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wa_autoreply_prefs", Context.MODE_PRIVATE)

    private val _isMasterEnabled = MutableStateFlow(prefs.getBoolean(KEY_MASTER_ENABLED, true))
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private val _replyToGroups = MutableStateFlow(prefs.getBoolean(KEY_REPLY_TO_GROUPS, false))
    val replyToGroups: StateFlow<Boolean> = _replyToGroups.asStateFlow()

    private val _prependTag = MutableStateFlow(prefs.getBoolean(KEY_PREPEND_TAG, true))
    val prependTag: StateFlow<Boolean> = _prependTag.asStateFlow()

    private val _supportWhatsAppBusiness = MutableStateFlow(prefs.getBoolean(KEY_SUPPORT_WA_BUSINESS, true))
    val supportWhatsAppBusiness: StateFlow<Boolean> = _supportWhatsAppBusiness.asStateFlow()

    private val _globalCooldownMinutes = MutableStateFlow(prefs.getInt(KEY_GLOBAL_COOLDOWN, 3))
    val globalCooldownMinutes: StateFlow<Int> = _globalCooldownMinutes.asStateFlow()

    private val _blacklistedContacts = MutableStateFlow(prefs.getString(KEY_BLACKLISTED_CONTACTS, "") ?: "")
    val blacklistedContacts: StateFlow<String> = _blacklistedContacts.asStateFlow()

    fun setMasterEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        _isMasterEnabled.value = enabled
    }

    fun setReplyToGroups(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REPLY_TO_GROUPS, enabled).apply()
        _replyToGroups.value = enabled
    }

    fun setPrependTag(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PREPEND_TAG, enabled).apply()
        _prependTag.value = enabled
    }

    fun setSupportWhatsAppBusiness(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SUPPORT_WA_BUSINESS, enabled).apply()
        _supportWhatsAppBusiness.value = enabled
    }

    fun setGlobalCooldownMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_GLOBAL_COOLDOWN, minutes).apply()
        _globalCooldownMinutes.value = minutes
    }

    fun setBlacklistedContacts(contacts: String) {
        prefs.edit().putString(KEY_BLACKLISTED_CONTACTS, contacts).apply()
        _blacklistedContacts.value = contacts
    }

    fun isContactBlacklisted(sender: String): Boolean {
        val list = _blacklistedContacts.value
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        val normalized = sender.trim().lowercase()
        return list.any { blocked -> normalized.contains(blocked) }
    }

    companion object {
        private const val KEY_MASTER_ENABLED = "master_enabled"
        private const val KEY_REPLY_TO_GROUPS = "reply_to_groups"
        private const val KEY_PREPEND_TAG = "prepend_tag"
        private const val KEY_SUPPORT_WA_BUSINESS = "support_wa_business"
        private const val KEY_GLOBAL_COOLDOWN = "global_cooldown"
        private const val KEY_BLACKLISTED_CONTACTS = "blacklisted_contacts"
    }
}
