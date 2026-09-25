package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.AutoReplyApp
import com.example.service.AutoReplyForegroundService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.i("BootReceiver", "Device boot completed, checking auto-reply status")
            try {
                val app = AutoReplyApp.instance
                if (app.preferenceManager.isMasterEnabled.value) {
                    AutoReplyForegroundService.startService(context)
                    Log.i("BootReceiver", "AutoReplyForegroundService launched after reboot")
                }
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error restoring auto-reply after boot", e)
            }
        }
    }
}
