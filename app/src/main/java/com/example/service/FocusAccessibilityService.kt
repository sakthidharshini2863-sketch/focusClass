package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.example.ui.block.FocusBlockActivity

class FocusAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        FocusPolicyManager.setServiceConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val eventPackage = event.packageName?.toString() ?: return

        // Skip events coming from our own app or Android system UI
        if (eventPackage == packageName ||
            eventPackage.startsWith("com.android.systemui") ||
            eventPackage == "android"
        ) {
            return
        }

        // Check if the foreground application is restricted in the active session
        if (FocusPolicyManager.isPackageRestricted(eventPackage)) {
            val now = System.currentTimeMillis()
            // Debounce rapid successive events from the same app (within 1.5 seconds)
            if (eventPackage == lastInterceptedPackage && (now - lastInterceptTime) < 1500L) {
                return
            }

            lastInterceptedPackage = eventPackage
            lastInterceptTime = now

            // Immediately pull the user out of the restricted app to system home
            performGlobalAction(GLOBAL_ACTION_HOME)

            val policy = FocusPolicyManager.currentPolicy.value
            val appName = FocusPolicyManager.getAppNameForPackage(eventPackage)

            // Launch the Focus Shield Blocking Overlay after a brief 150ms delay
            // so system home animation does not dismiss the new task
            mainHandler.postDelayed({
                FocusBlockActivity.launch(
                    context = applicationContext,
                    packageName = eventPackage,
                    appName = appName,
                    className = policy.className.ifBlank { "Classroom" },
                    teacherName = policy.teacherName.ifBlank { "Professor" },
                    endTime = policy.endTime.ifBlank { "Class End" }
                )
            }, 150L)
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        FocusPolicyManager.setServiceConnected(false)
    }
}
