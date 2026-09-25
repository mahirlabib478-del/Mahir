package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AutoReplyApp
import com.example.MainActivity
import com.example.R

/**
 * Foreground Service that prevents the Android OS from killing the Auto-Reply process
 * when the app is closed, cleared from recent apps, or when the phone screen is off/sleeping.
 */
class AutoReplyForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.i(TAG, "Stopping AutoReplyForegroundService via action")
            try {
                AutoReplyApp.instance.preferenceManager.setMasterEnabled(false)
            } catch (_: Exception) {}
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            isRunning = false
            return START_NOT_STICKY
        }

        val app = try {
            AutoReplyApp.instance
        } catch (_: Exception) {
            null
        }

        if (app?.preferenceManager?.isMasterEnabled?.value == false) {
            Log.d(TAG, "Master switch is OFF, shutting down foreground service")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            isRunning = false
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)
        isRunning = true
        Log.i(TAG, "AutoReplyForegroundService started in foreground successfully")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.i(TAG, "AutoReplyForegroundService destroyed")
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AutoReplyForegroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WA AutoReply is Active ⚡")
            .setContentText("Auto-responding to WhatsApp & Messenger 24/7")
            .setSmallIcon(R.drawable.ic_stat_autoreply)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_pause, "Pause Service", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto-Reply Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the auto-reply engine alive when app is closed or phone is sleeping"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "AutoReplyForeground"
        const val CHANNEL_ID = "autoreply_fg_service_channel"
        const val NOTIFICATION_ID = 9981
        const val ACTION_STOP_SERVICE = "com.example.service.STOP_AUTOREPLY_SERVICE"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, AutoReplyForegroundService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting AutoReplyForegroundService", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AutoReplyForegroundService::class.java)
            try {
                context.stopService(intent)
                isRunning = false
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AutoReplyForegroundService", e)
            }
        }
    }
}
