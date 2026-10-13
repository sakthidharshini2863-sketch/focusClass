package com.example.ui.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FocusPolicy
import com.example.data.model.StudentDeviceRecord
import com.example.data.model.User
import com.example.service.FocusPolicyManager
import com.example.ui.components.DeviceRestrictionBadge
import com.example.ui.components.StatusBadge
import com.example.ui.theme.FocusBlueDark
import com.example.ui.theme.FocusBluePrimary
import com.example.ui.theme.FocusStatusAmber
import com.example.ui.theme.FocusStatusGreen
import com.example.ui.theme.FocusStatusRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherDashboard(
    user: User,
    policy: FocusPolicy,
    enrolledStudents: List<StudentDeviceRecord>,
    isBackendConnected: Boolean,
    onClassChanged: (String) -> Unit,
    onManageSession: (
        className: String,
        action: String,
        restrictedApps: List<String>,
        startTime: String,
        endTime: String
    ) -> Unit,
    onOpenServerSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedClass by remember { mutableStateOf("CSE-A") }
    val availableClasses = listOf("CSE-A", "CSE-B", "ECE-A", "IT-A")

    // Default restricted apps: Instagram, YouTube, Snapchat
    val selectedApps = remember {
        mutableStateListOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.snapchat.android"
        )
    }

    var startTimeText by remember { mutableStateOf("09:00 AM") }
    var endTimeText by remember { mutableStateOf("10:00 AM") }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_dashboard"),
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
                            .background(FocusBlueDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
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
                            text = "Faculty • ${user.department}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenServerSettings,
                        modifier = Modifier.testTag("teacher_server_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Backend Settings",
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

            // Class-wide Rule Notice Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "⚡ Class-Wide Automatic Restriction:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusBluePrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Starting a focus session for $selectedClass immediately broadcasts and applies the restriction policy to all enrolled $selectedClass student devices. Students in CSE-B or other sections remain unaffected.",
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A8A),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Session Configuration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_control_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TARGET CLASSROOM SECTION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Class Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableClasses.forEach { cls ->
                            val isSelected = (cls == selectedClass)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) FocusBluePrimary else Color(0xFFF1F5F9),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedClass = cls
                                        onClassChanged(cls)
                                    }
                                    .padding(vertical = 10.dp)
                                    .testTag("class_chip_$cls"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cls,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "SELECT DISTRACTING APPS TO RESTRICT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // App Selection Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FocusPolicyManager.AVAILABLE_APPS.forEach { appInfo ->
                            val isChecked = selectedApps.contains(appInfo.packageName)
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isChecked) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isChecked) FocusBluePrimary else Color(0xFFE2E8F0),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (isChecked) selectedApps.remove(appInfo.packageName)
                                        else selectedApps.add(appInfo.packageName)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("app_chip_${appInfo.appName}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedApps.add(appInfo.packageName)
                                        else selectedApps.remove(appInfo.packageName)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = FocusBluePrimary),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = appInfo.appName,
                                    fontSize = 13.sp,
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isChecked) FocusBlueDark else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Start and End Times
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startTimeText,
                            onValueChange = { startTimeText = it },
                            label = { Text("Start Time") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_time_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = endTimeText,
                            onValueChange = { endTimeText = it },
                            label = { Text("End Time") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("end_time_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Control Buttons (Start Focus, Allow Break, Resume Focus, End Focus)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onManageSession(
                                    selectedClass,
                                    "START",
                                    selectedApps.toList(),
                                    startTimeText,
                                    endTimeText
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_start_focus"),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start Focus", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onManageSession(
                                    selectedClass,
                                    "BREAK",
                                    selectedApps.toList(),
                                    startTimeText,
                                    endTimeText
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_allow_break"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFEF3C7),
                                contentColor = Color(0xFF92400E)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Coffee, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Allow Break", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onManageSession(
                                    selectedClass,
                                    "RESUME",
                                    selectedApps.toList(),
                                    startTimeText,
                                    endTimeText
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_resume_focus"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDCFCE7),
                                contentColor = Color(0xFF166534)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume Focus", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                onManageSession(
                                    selectedClass,
                                    "END",
                                    selectedApps.toList(),
                                    startTimeText,
                                    endTimeText
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_end_focus"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFEE2E2),
                                contentColor = Color(0xFF991B1B)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("End Focus", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Enrolled Students and Device Status Monitor
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("enrolled_students_card"),
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
                                text = "ENROLLED STUDENT DEVICES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$selectedClass (${enrolledStudents.size} Registered)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusBlueDark
                            )
                        }

                        StatusBadge(status = policy.status)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (enrolledStudents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No students currently enrolled in section $selectedClass.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            enrolledStudents.forEach { student ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = student.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "${student.registerNo} • ${student.email}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = "Device ID: ${student.deviceId.take(18)}...",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    DeviceRestrictionBadge(
                                        isEnforced = student.isRestricted,
                                        isReady = student.isAccessibilityEnabled
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
