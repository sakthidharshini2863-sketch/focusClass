package com.example.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusPolicy
import com.example.data.model.User
import com.example.service.FocusPolicyManager
import com.example.ui.components.StatusBadge
import com.example.ui.theme.FocusBlueContainer
import com.example.ui.theme.FocusBlueDark
import com.example.ui.theme.FocusBluePrimary
import com.example.ui.theme.FocusStatusAmber
import com.example.ui.theme.FocusStatusGreen
import com.example.ui.theme.FocusStatusRed

@Composable
fun StudentDashboard(
    user: User,
    policy: FocusPolicy,
    isAccessibilityEnabled: Boolean,
    isOverlayEnabled: Boolean,
    isBackendConnected: Boolean,
    onRefreshPolicy: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onTestAppInterception: (packageName: String) -> Unit,
    onLogout: () -> Unit
) {
    val isActuallyEnforcing = policy.isEnforced && isAccessibilityEnabled

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("student_dashboard"),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(FocusBluePrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = user.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = FocusBlueDark
                        )
                        Text(
                            text = "${user.className} • ${user.registerNo}",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row {
                    IconButton(
                        onClick = onRefreshPolicy,
                        modifier = Modifier.testTag("refresh_policy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Policy",
                            tint = FocusBluePrimary
                        )
                    }
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Exit", fontSize = 12.sp, color = FocusBlueDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Focus Session Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_session_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CLASSROOM FOCUS SESSION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when (policy.status.uppercase()) {
                                    "ACTIVE" -> "Focus Mode Enforced"
                                    "BREAK" -> "Break Time in Progress"
                                    else -> "No Active Lecture Session"
                                },
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = FocusBlueDark
                            )
                        }
                        StatusBadge(status = policy.status)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (policy.isEnforced || policy.isBreak) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(10.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = FocusBluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Instructor: ${policy.teacherName.ifBlank { "Class Faculty" }}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = FocusBlueDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Scheduled: ${policy.startTime} — ${policy.endTime}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "You are currently enrolled in section ${user.className}. When your professor starts a focus session for this class, restrictions will apply automatically.",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Selected Restricted Apps Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_apps_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "RESTRICTED DISTRACTING APPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (policy.restrictedApps.isEmpty()) {
                        Text(
                            text = if (policy.isBreak) "All apps temporarily permitted during break." else "No apps currently restricted for your section.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            policy.restrictedApps.forEach { pkg ->
                                val appName = FocusPolicyManager.getAppNameForPackage(pkg)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (policy.isEnforced) Color(0xFFFEE2E2) else Color(0xFFE2E8F0),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (policy.isEnforced) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = null,
                                                tint = if (policy.isEnforced) FocusStatusRed else Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = appName,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = pkg,
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (policy.isEnforced) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (policy.isEnforced) "BLOCKED" else "PAUSED",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (policy.isEnforced) FocusStatusRed else FocusStatusAmber
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Device Connection and Restriction Status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("device_restriction_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "DEVICE RESTRICTION STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Enforcement Banner
                    val bannerBg = if (isActuallyEnforcing) Color(0xFFDCFCE7) else if (!policy.isEnforced) Color(0xFFEFF6FF) else Color(0xFFFEF3C7)
                    val bannerColor = if (isActuallyEnforcing) Color(0xFF166534) else if (!policy.isEnforced) Color(0xFF1E40AF) else Color(0xFF92400E)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bannerBg, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (isActuallyEnforcing) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isActuallyEnforcing) {
                                        "Device Restriction Active"
                                    } else if (!policy.isEnforced) {
                                        "Device Ready for Next Session"
                                    } else {
                                        "Restriction Incomplete (Permissions Missing)"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bannerColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isActuallyEnforcing) {
                                        "Accessibility Service is actively monitoring foreground launches and blocking ${policy.restrictedApps.size} apps."
                                    } else if (!policy.isEnforced) {
                                        "Device is connected and enrolled in ${user.className}."
                                    } else {
                                        "FocusClass requires Accessibility Service permission to detect and intercept restricted apps on this phone."
                                    },
                                    fontSize = 12.sp,
                                    color = bannerColor.copy(alpha = 0.9f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Permission Toggles / Audits
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Accessibility Restriction Service",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = if (isAccessibilityEnabled) "Active & Running" else "Permission Disabled in Settings",
                                fontSize = 11.sp,
                                color = if (isAccessibilityEnabled) FocusStatusGreen else FocusStatusRed
                            )
                        }
                        if (!isAccessibilityEnabled) {
                            Button(
                                onClick = onOpenAccessibilitySettings,
                                colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("enable_accessibility_button")
                            ) {
                                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Enabled",
                                tint = FocusStatusGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Draw Over Other Apps (Overlay)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = if (isOverlayEnabled) "Active & Running" else "Permission Disabled in Settings",
                                fontSize = 11.sp,
                                color = if (isOverlayEnabled) FocusStatusGreen else FocusStatusRed
                            )
                        }
                        if (!isOverlayEnabled) {
                            Button(
                                onClick = onOpenOverlaySettings,
                                colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp).testTag("enable_overlay_button")
                            ) {
                                Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Enabled",
                                tint = FocusStatusGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Test App Interception Simulator
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_simulator_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "🧪 Live App Blocking Simulator",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusBlueDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Test how the device intercepts and blocks distracting apps when opened by the student during active class sessions.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTestAppInterception("com.instagram.android") },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("test_block_instagram"),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Test Instagram", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onTestAppInterception("com.google.android.youtube") },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("test_block_youtube"),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Test YouTube", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
