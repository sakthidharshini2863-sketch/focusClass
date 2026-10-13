package com.example.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import java.util.UUID

object DevicePermissionHelper {

    private const val PREFS_NAME = "focusclass_device_prefs"
    private const val KEY_DEVICE_ID = "device_unique_id"

    /**
     * Checks if the FocusAccessibilityService is actively enabled in system settings.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedServiceName = "${context.packageName}/${FocusAccessibilityService::class.java.canonicalName}"
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false

        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        for (service in enabledServices) {
            val serviceId = service.id
            if (serviceId.contains(context.packageName) &&
                serviceId.contains(FocusAccessibilityService::class.java.simpleName)
            ) {
                return true
            }
        }

        // Fallback check through Settings.Secure string
        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            return enabledServicesSetting.contains(context.packageName) &&
                    enabledServicesSetting.contains(FocusAccessibilityService::class.java.simpleName)
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Checks if the app has permission to draw full-screen overlay shields.
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * Opens system Accessibility Settings for the user to enable FocusClass.
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Opens system Overlay Settings for the user to grant SYSTEM_ALERT_WINDOW.
     */
    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    /**
     * Retrieves or generates a persistent device UUID for student device enrollment.
     */
    fun getOrCreateDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            deviceId = "android-" + UUID.randomUUID().toString().take(12)
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }
}
