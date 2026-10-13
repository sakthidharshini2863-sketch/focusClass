package com.example.service

import com.example.data.model.FocusPolicy
import com.example.data.model.RestrictedAppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FocusPolicyManager {

    val AVAILABLE_APPS: List<RestrictedAppInfo> = listOf(
        RestrictedAppInfo("com.instagram.android", "Instagram", "Social Media", defaultRestricted = true),
        RestrictedAppInfo("com.google.android.youtube", "YouTube", "Video / Streaming", defaultRestricted = true),
        RestrictedAppInfo("com.snapchat.android", "Snapchat", "Social / Chat", defaultRestricted = true),
        RestrictedAppInfo("com.zhiliaoapp.musically", "TikTok", "Short Video", defaultRestricted = false),
        RestrictedAppInfo("com.facebook.katana", "Facebook", "Social Media", defaultRestricted = false),
        RestrictedAppInfo("com.twitter.android", "X (Twitter)", "Social Media", defaultRestricted = false),
        RestrictedAppInfo("com.reddit.frontpage", "Reddit", "Forums & Media", defaultRestricted = false),
        RestrictedAppInfo("com.netflix.mediaclient", "Netflix", "Entertainment", defaultRestricted = false),
        RestrictedAppInfo("com.kiloo.subwaysurf", "Subway Surfers (Games)", "Games", defaultRestricted = false)
    )

    private val _currentPolicy = MutableStateFlow(FocusPolicy())
    val currentPolicy: StateFlow<FocusPolicy> = _currentPolicy.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun updatePolicy(newPolicy: FocusPolicy) {
        _currentPolicy.value = newPolicy
    }

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    fun isPackageRestricted(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val policy = _currentPolicy.value
        // Only restrict if the session is ACTIVE (not BREAK and not INACTIVE)
        if (!policy.isEnforced) return false

        return policy.restrictedApps.any { it.equals(packageName, ignoreCase = true) }
    }

    fun getAppNameForPackage(packageName: String): String {
        return AVAILABLE_APPS.firstOrNull { it.packageName.equals(packageName, ignoreCase = true) }?.appName
            ?: packageName.substringAfterLast('.')
    }
}
