package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object BatteryOptimizationHelper {

    private const val TAG = "BatteryHelper"

    /**
     * Checks if the app is excluded from Android's Doze mode / battery optimizations.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
            return pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }

    /**
     * Opens system dialog or settings to disable battery optimization for this app,
     * allowing it to run continuously and reply even when screen is locked/sleeping.
     */
    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimization(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w(TAG, "ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS failed, trying fallback", e)
            }

            try {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Cannot open battery settings", e)
            }
        }
    }

    /**
     * Opens manufacturer-specific autostart or background power settings
     * (Xiaomi/Redmi/POCO, Samsung, Oppo, Vivo, OnePlus, Huawei).
     */
    fun openOemBackgroundSettings(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intentsToTry = mutableListOf<Intent>()

        when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> {
                intentsToTry.add(Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"))
                intentsToTry.add(Intent().setClassName("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"))
            }
            manufacturer.contains("samsung") -> {
                intentsToTry.add(Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"))
                intentsToTry.add(Intent().setClassName("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"))
            }
            manufacturer.contains("oppo") || manufacturer.contains("realme") -> {
                intentsToTry.add(Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"))
                intentsToTry.add(Intent().setClassName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"))
            }
            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> {
                intentsToTry.add(Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"))
                intentsToTry.add(Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"))
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                intentsToTry.add(Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"))
                intentsToTry.add(Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"))
            }
        }

        // Generic app details fallback
        intentsToTry.add(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        )

        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
                // Try next
            }
        }
    }
}
