package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.repository.FocusRepository
import com.example.service.DevicePermissionHelper
import com.example.ui.auth.LoginScreen
import com.example.ui.block.FocusBlockActivity
import com.example.ui.student.StudentDashboard
import com.example.ui.teacher.TeacherDashboard
import com.example.ui.theme.FocusBluePrimary
import com.example.ui.theme.FocusClassTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    LOGIN,
    STUDENT_DASHBOARD,
    TEACHER_DASHBOARD
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: FocusRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = FocusRepository(applicationContext)

        setContent {
            FocusClassTheme {
                MainAppContent(repository)
            }
        }
    }
}

@Composable
fun MainAppContent(repository: FocusRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by repository.currentUser.collectAsState()
    val currentPolicy by repository.currentPolicy.collectAsState()
    val teacherStudents by repository.teacherStudents.collectAsState()
    val isBackendConnected by repository.isBackendConnected.collectAsState()
    val serverBaseUrl by repository.serverBaseUrl.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
    var isLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var showServerDialog by remember { mutableStateOf(false) }
    var tempServerUrl by remember { mutableStateOf(serverBaseUrl) }

    // Live device permissions
    var isAccessibilityEnabled by remember {
        mutableStateOf(DevicePermissionHelper.isAccessibilityServiceEnabled(context))
    }
    var isOverlayEnabled by remember {
        mutableStateOf(DevicePermissionHelper.canDrawOverlays(context))
    }

    // Refresh permissions whenever returning to foreground
    LaunchedEffect(Unit) {
        while (isActive) {
            isAccessibilityEnabled = DevicePermissionHelper.isAccessibilityServiceEnabled(context)
            isOverlayEnabled = DevicePermissionHelper.canDrawOverlays(context)
            delay(2000L)
        }
    }

    // Background Policy Poller (Every 4 seconds for student)
    LaunchedEffect(currentUser, currentScreen) {
        while (isActive) {
            val user = currentUser
            if (user != null && user.isStudent && user.className.isNotBlank()) {
                repository.syncStudentPolicy(user.className)
                repository.reportCurrentDeviceStatus(
                    user = user,
                    isAccessibility = isAccessibilityEnabled,
                    isOverlay = isOverlayEnabled,
                    isEnforcing = currentPolicy.isEnforced && isAccessibilityEnabled
                )
            }
            delay(4000L)
        }
    }

    // React to user state changes
    LaunchedEffect(currentUser) {
        val user = currentUser
        currentScreen = when {
            user == null -> AppScreen.LOGIN
            user.isTeacher -> AppScreen.TEACHER_DASHBOARD
            user.isStudent -> AppScreen.STUDENT_DASHBOARD
            else -> AppScreen.LOGIN
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.LOGIN -> {
                    LoginScreen(
                        isLoading = isLoading,
                        errorMessage = authError,
                        onLogin = { email, pass ->
                            authError = null
                            isLoading = true
                            coroutineScope.launch {
                                val result = repository.login(email, pass)
                                isLoading = false
                                result.onFailure {
                                    authError = it.message ?: "Authentication failed."
                                }
                            }
                        },
                        onRegisterStudent = { name, email, regNo, dept, year, className, pass ->
                            authError = null
                            isLoading = true
                            coroutineScope.launch {
                                val result = repository.registerStudent(
                                    name = name,
                                    email = email,
                                    registerNo = regNo,
                                    department = dept,
                                    year = year,
                                    className = className,
                                    pass = pass
                                )
                                isLoading = false
                                result.onFailure {
                                    authError = it.message ?: "Registration failed."
                                }
                            }
                        }
                    )
                }

                AppScreen.STUDENT_DASHBOARD -> {
                    BackHandler {
                        // Return to login / logout confirmation
                        repository.logout()
                    }

                    currentUser?.let { studentUser ->
                        StudentDashboard(
                            user = studentUser,
                            policy = currentPolicy,
                            isAccessibilityEnabled = isAccessibilityEnabled,
                            isOverlayEnabled = isOverlayEnabled,
                            isBackendConnected = isBackendConnected,
                            onRefreshPolicy = {
                                coroutineScope.launch {
                                    repository.syncStudentPolicy(studentUser.className)
                                    snackbarHostState.showSnackbar("Restriction policy refreshed.")
                                }
                            },
                            onOpenAccessibilitySettings = {
                                DevicePermissionHelper.openAccessibilitySettings(context)
                            },
                            onOpenOverlaySettings = {
                                DevicePermissionHelper.openOverlaySettings(context)
                            },
                            onTestAppInterception = { testPackage ->
                                FocusBlockActivity.launch(
                                    context = context,
                                    packageName = testPackage,
                                    appName = if (testPackage.contains("youtube")) "YouTube" else "Instagram",
                                    className = studentUser.className,
                                    teacherName = currentPolicy.teacherName.ifBlank { "Prof. John Smith" },
                                    endTime = currentPolicy.endTime.ifBlank { "11:00 AM" }
                                )
                            },
                            onLogout = {
                                repository.logout()
                            }
                        )
                    }
                }

                AppScreen.TEACHER_DASHBOARD -> {
                    BackHandler {
                        repository.logout()
                    }

                    currentUser?.let { teacherUser ->
                        TeacherDashboard(
                            user = teacherUser,
                            policy = currentPolicy,
                            enrolledStudents = teacherStudents,
                            isBackendConnected = isBackendConnected,
                            onClassChanged = { targetClass ->
                                coroutineScope.launch {
                                    repository.refreshTeacherClassStudents(targetClass)
                                    repository.syncStudentPolicy(targetClass)
                                }
                            },
                            onManageSession = { cls, action, apps, startT, endT ->
                                coroutineScope.launch {
                                    val result = repository.manageTeacherSession(
                                        className = cls,
                                        action = action,
                                        restrictedApps = apps,
                                        startTime = startT,
                                        endTime = endT,
                                        teacherId = teacherUser.id,
                                        teacherName = teacherUser.name
                                    )
                                    result.onSuccess {
                                        val verb = when (action) {
                                            "START" -> "started"
                                            "BREAK" -> "paused for break"
                                            "RESUME" -> "resumed"
                                            else -> "concluded"
                                        }
                                        snackbarHostState.showSnackbar("Focus session for $cls $verb successfully.")
                                    }
                                }
                            },
                            onOpenServerSettings = {
                                tempServerUrl = serverBaseUrl
                                showServerDialog = true
                            },
                            onLogout = {
                                repository.logout()
                            }
                        )
                    }
                }
            }
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Flask Backend Connection Settings") },
            text = {
                Column {
                    Text(
                        text = "FocusClass synchronizes live policies with the Flask + SQLite backend. Default is 10.0.2.2:5000 for standard Android Emulator or your computer LAN IP for physical device testing.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it },
                        label = { Text("Server Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.setServerBaseUrl(tempServerUrl)
                        showServerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary)
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
