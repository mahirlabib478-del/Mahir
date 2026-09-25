package com.example.util

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helper to manage WakeLocks and verify Lock Screen notification visibility
 * so auto-reply works reliably when the screen is locked and device is sleeping.
 */
object WakeLockHelper {

    private const val TAG = "WakeLockHelper"

    /**
     * Acquires a CPU WakeLock to guarantee background threads and network/IPC
     * are not suspended while evaluating rules and sending the quick reply.
     */
    fun acquireCpuWakeLock(context: Context, timeoutMs: Long = 12_000L): PowerManager.WakeLock? {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
            val wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AutoReply::NotificationProcessingCpuLock"
            )
            wakeLock.setReferenceCounted(false)
            wakeLock.acquire(timeoutMs)
            wakeLock
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire CPU wake lock", e)
            null
        }
    }

    /**
     * Briefly turns on screen/CPU if device is locked so OEM lockscreen limitations
     * allow RemoteInput PendingIntent execution.
     */
    @Suppress("DEPRECATION")
    fun wakeScreenBriefly(context: Context, timeoutMs: Long = 3_000L): PowerManager.WakeLock? {
        return try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
            val flags = PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
            val screenLock = pm.newWakeLock(flags, "AutoReply::BriefWakeupLock")
            screenLock.setReferenceCounted(false)
            screenLock.acquire(timeoutMs)
            screenLock
        } catch (e: Exception) {
            Log.w(TAG, "Failed to briefly wake screen", e)
            null
        }
    }

    /**
     * Checks if the device is currently in a locked / keyguard active state.
     */
    fun isDeviceLocked(context: Context): Boolean {
        return try {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.isKeyguardLocked == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Direct intent to open Android's Lock Screen notification settings
     * so user can set "Show sensitive content" or "Show all content on lock screen".
     */
    fun openLockScreenNotificationSettings(context: Context) {
        val intents = mutableListOf<Intent>()

        // 1. Android standard lockscreen notification channel/settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intents.add(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            )
        }

        // 2. Direct lockscreen notification settings in Android
        intents.add(Intent("android.settings.NOTIFICATION_SETTINGS"))
        intents.add(Intent(Settings.ACTION_SOUND_SETTINGS))

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
    }
}
