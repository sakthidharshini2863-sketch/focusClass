package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.api.FocusApiService
import com.example.data.model.ClassStudentsResponse
import com.example.data.model.FocusPolicy
import com.example.data.model.FocusSessionDto
import com.example.data.model.LoginRequest
import com.example.data.model.RegisterStudentRequest
import com.example.data.model.SessionActionRequest
import com.example.data.model.StudentDeviceRecord
import com.example.data.model.User
import com.example.service.DevicePermissionHelper
import com.example.service.FocusPolicyManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class FocusRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("focusclass_repo_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _currentPolicy = MutableStateFlow(FocusPolicy())
    val currentPolicy: StateFlow<FocusPolicy> = _currentPolicy.asStateFlow()

    private val _teacherStudents = MutableStateFlow<List<StudentDeviceRecord>>(emptyList())
    val teacherStudents: StateFlow<List<StudentDeviceRecord>> = _teacherStudents.asStateFlow()

    private val _isBackendConnected = MutableStateFlow(false)
    val isBackendConnected: StateFlow<Boolean> = _isBackendConnected.asStateFlow()

    private val _serverBaseUrl = MutableStateFlow(
        prefs.getString("server_base_url", "http://10.0.2.2:5000/") ?: "http://10.0.2.2:5000/"
    )
    val serverBaseUrl: StateFlow<String> = _serverBaseUrl.asStateFlow()

    private val _statusMessage = MutableStateFlow("Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var apiService: FocusApiService? = null

    // Local in-memory synchronized store (ensures offline reliability & instant testing)
    private val localUsers = ConcurrentHashMap<String, User>()
    private val localPasswords = ConcurrentHashMap<String, String>()
    private val localSessions = ConcurrentHashMap<String, FocusPolicy>()
    private val localDeviceStatuses = ConcurrentHashMap<String, StudentDeviceRecord>()

    init {
        initLocalSeedData()
        rebuildRetrofitClient(_serverBaseUrl.value)
    }

    private fun rebuildRetrofitClient(baseUrl: String) {
        try {
            val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val okHttp = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(formattedUrl)
                .client(okHttp)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()

            apiService = retrofit.create(FocusApiService::class.java)
        } catch (e: Exception) {
            Log.e("FocusRepository", "Error initializing Retrofit: ${e.message}")
        }
    }

    fun setServerBaseUrl(newUrl: String) {
        val sanitized = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        _serverBaseUrl.value = sanitized
        prefs.edit().putString("server_base_url", sanitized).apply()
        rebuildRetrofitClient(sanitized)
    }

    private fun initLocalSeedData() {
        // Pre-authorized teachers
        val teacher1 = User(
            id = 1,
            name = "Prof. John Smith",
            email = "prof.smith@college.edu",
            role = "teacher",
            department = "Computer Science & Engineering"
        )
        val teacher2 = User(
            id = 2,
            name = "Dr. S. Raj",
            email = "dr.raj@college.edu",
            role = "teacher",
            department = "Computer Science & Engineering"
        )
        localUsers[teacher1.email.lowercase()] = teacher1
        localPasswords[teacher1.email.lowercase()] = "admin123"

        localUsers[teacher2.email.lowercase()] = teacher2
        localPasswords[teacher2.email.lowercase()] = "focus2026"

        // Pre-enrolled students
        val student1 = User(
            id = 3,
            name = "Alex Kumar",
            email = "alex.cse@college.edu",
            role = "student",
            registerNo = "21CS001",
            department = "Computer Science",
            year = "3rd Year",
            className = "CSE-A"
        )
        val student2 = User(
            id = 4,
            name = "Priya Sharma",
            email = "priya.cse@college.edu",
            role = "student",
            registerNo = "21CS002",
            department = "Computer Science",
            year = "3rd Year",
            className = "CSE-A"
        )
        val student3 = User(
            id = 5,
            name = "Rahul Verma",
            email = "rahul.cse@college.edu",
            role = "student",
            registerNo = "21CS045",
            department = "Computer Science",
            year = "3rd Year",
            className = "CSE-B"
        )
        listOf(student1, student2, student3).forEach { s ->
            localUsers[s.email.lowercase()] = s
            localPasswords[s.email.lowercase()] = "student123"
            localDeviceStatuses[s.id.toString()] = StudentDeviceRecord(
                id = s.id,
                name = s.name,
                email = s.email,
                registerNo = s.registerNo,
                department = s.department,
                year = s.year,
                className = s.className,
                deviceId = "device-${s.registerNo.lowercase()}",
                isAccessibilityEnabled = true,
                isOverlayEnabled = true,
                isRestricted = false,
                lastPing = "Active"
            )
        }
    }

    suspend fun login(email: String, pass: String): Result<User> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass.trim()

        // 1. Try remote API first
        val service = apiService
        if (service != null) {
            try {
                val call = service.login(LoginRequest(cleanEmail, cleanPass))
                val response = call.execute()
                if (response.isSuccessful && response.body()?.user != null) {
                    val user = response.body()!!.user!!
                    _isBackendConnected.value = true
                    _currentUser.value = user
                    return@withContext Result.success(user)
                }
            } catch (e: Exception) {
                _isBackendConnected.value = false
                Log.d("FocusRepository", "Remote server unreachable, falling back to local: ${e.message}")
            }
        }

        // 2. Local fallback
        val user = localUsers[cleanEmail]
        val expectedPass = localPasswords[cleanEmail]
        if (user != null && expectedPass == cleanPass) {
            _currentUser.value = user
            return@withContext Result.success(user)
        }

        Result.failure(Exception("Invalid email or password."))
    }

    suspend fun registerStudent(
        name: String,
        email: String,
        registerNo: String,
        department: String,
        year: String,
        className: String,
        pass: String
    ): Result<User> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()

        // Attempt remote registration first
        val service = apiService
        if (service != null) {
            try {
                val call = service.registerStudent(
                    RegisterStudentRequest(
                        name = name.trim(),
                        email = cleanEmail,
                        registerNo = registerNo.trim(),
                        department = department.trim(),
                        year = year.trim(),
                        className = className.trim().uppercase(),
                        password = pass.trim()
                    )
                )
                val response = call.execute()
                if (response.isSuccessful && response.body()?.user != null) {
                    val user = response.body()!!.user!!
                    _isBackendConnected.value = true
                    _currentUser.value = user
                    return@withContext Result.success(user)
                }
            } catch (e: Exception) {
                _isBackendConnected.value = false
            }
        }

        // Local registration fallback
        if (localUsers.containsKey(cleanEmail)) {
            return@withContext Result.failure(Exception("An account with this email already exists."))
        }

        val newUser = User(
            id = (localUsers.size + 1).toLong(),
            name = name.trim(),
            email = cleanEmail,
            role = "student",
            registerNo = registerNo.trim().uppercase(),
            department = department.trim(),
            year = year.trim(),
            className = className.trim().uppercase()
        )
        localUsers[cleanEmail] = newUser
        localPasswords[cleanEmail] = pass.trim()
        _currentUser.value = newUser

        localDeviceStatuses[newUser.id.toString()] = StudentDeviceRecord(
            id = newUser.id,
            name = newUser.name,
            email = newUser.email,
            registerNo = newUser.registerNo,
            department = newUser.department,
            year = newUser.year,
            className = newUser.className,
            deviceId = DevicePermissionHelper.getOrCreateDeviceId(context),
            isAccessibilityEnabled = DevicePermissionHelper.isAccessibilityServiceEnabled(context),
            isOverlayEnabled = DevicePermissionHelper.canDrawOverlays(context),
            isRestricted = false,
            lastPing = "Just now"
        )

        Result.success(newUser)
    }

    fun logout() {
        _currentUser.value = null
        _currentPolicy.value = FocusPolicy()
        FocusPolicyManager.updatePolicy(FocusPolicy())
    }

    suspend fun syncStudentPolicy(className: String) = withContext(Dispatchers.IO) {
        val targetClass = className.trim().uppercase()
        val service = apiService
        if (service != null) {
            try {
                val call = service.getStudentPolicy(targetClass)
                val response = call.execute()
                if (response.isSuccessful && response.body() != null) {
                    val policy = response.body()!!
                    _isBackendConnected.value = true
                    _currentPolicy.value = policy
                    FocusPolicyManager.updatePolicy(policy)
                    return@withContext
                }
            } catch (e: Exception) {
                _isBackendConnected.value = false
            }
        }

        // Local fallback
        val policy = localSessions[targetClass] ?: FocusPolicy(className = targetClass, status = "INACTIVE")
        _currentPolicy.value = policy
        FocusPolicyManager.updatePolicy(policy)
    }

    suspend fun manageTeacherSession(
        className: String,
        action: String,
        restrictedApps: List<String>,
        startTime: String,
        endTime: String,
        teacherId: Long = 1,
        teacherName: String = "Prof. John Smith"
    ): Result<FocusPolicy> = withContext(Dispatchers.IO) {
        val targetClass = className.trim().uppercase()

        // 1. Remote call
        val service = apiService
        if (service != null) {
            try {
                val call = service.manageSession(
                    SessionActionRequest(
                        className = targetClass,
                        action = action,
                        restrictedApps = restrictedApps,
                        startTime = startTime,
                        endTime = endTime,
                        teacherId = teacherId,
                        teacherName = teacherName
                    )
                )
                val response = call.execute()
                if (response.isSuccessful && response.body()?.session != null) {
                    val s = response.body()!!.session!!
                    val updatedPolicy = FocusPolicy(
                        className = s.className,
                        hasActiveSession = s.status.equals("ACTIVE", ignoreCase = true),
                        status = s.status,
                        restrictedApps = s.restrictedApps,
                        startTime = s.startTime,
                        endTime = s.endTime,
                        teacherName = s.teacherName
                    )
                    _isBackendConnected.value = true
                    localSessions[targetClass] = updatedPolicy
                    _currentPolicy.value = updatedPolicy
                    FocusPolicyManager.updatePolicy(updatedPolicy)
                    refreshTeacherClassStudents(targetClass)
                    return@withContext Result.success(updatedPolicy)
                }
            } catch (e: Exception) {
                _isBackendConnected.value = false
            }
        }

        // 2. Local fallback
        val currentStatus = when (action.uppercase()) {
            "START", "RESUME" -> "ACTIVE"
            "BREAK" -> "BREAK"
            "END" -> "ENDED"
            else -> "INACTIVE"
        }

        val updated = FocusPolicy(
            className = targetClass,
            hasActiveSession = currentStatus == "ACTIVE",
            status = currentStatus,
            restrictedApps = if (currentStatus == "ACTIVE") restrictedApps else (localSessions[targetClass]?.restrictedApps ?: emptyList()),
            startTime = startTime,
            endTime = endTime,
            teacherName = teacherName
        )
        localSessions[targetClass] = updated
        _currentPolicy.value = updated
        FocusPolicyManager.updatePolicy(updated)

        // Update local device statuses for this class only
        localDeviceStatuses.values.forEach { rec ->
            if (rec.className.equals(targetClass, ignoreCase = true)) {
                val isBlocked = (currentStatus == "ACTIVE" && rec.isAccessibilityEnabled)
                localDeviceStatuses[rec.id.toString()] = rec.copy(isRestricted = isBlocked)
            }
        }

        refreshTeacherClassStudents(targetClass)
        Result.success(updated)
    }

    suspend fun refreshTeacherClassStudents(className: String) = withContext(Dispatchers.IO) {
        val targetClass = className.trim().uppercase()
        val service = apiService
        if (service != null) {
            try {
                val call = service.getClassStudents(targetClass)
                val response = call.execute()
                if (response.isSuccessful && response.body() != null) {
                    _isBackendConnected.value = true
                    _teacherStudents.value = response.body()!!.students
                    return@withContext
                }
            } catch (_: Exception) {
                _isBackendConnected.value = false
            }
        }

        // Local fallback: filter students strictly belonging to this class
        val matchingStudents = localUsers.values
            .filter { it.isStudent && it.className.equals(targetClass, ignoreCase = true) }
            .map { s ->
                val cached = localDeviceStatuses[s.id.toString()]
                val isSessionActive = localSessions[targetClass]?.isEnforced == true
                StudentDeviceRecord(
                    id = s.id,
                    name = s.name,
                    email = s.email,
                    registerNo = s.registerNo,
                    department = s.department,
                    year = s.year,
                    className = s.className,
                    deviceId = cached?.deviceId ?: "device-${s.registerNo.lowercase()}",
                    isAccessibilityEnabled = cached?.isAccessibilityEnabled ?: true,
                    isOverlayEnabled = cached?.isOverlayEnabled ?: true,
                    isRestricted = isSessionActive && (cached?.isAccessibilityEnabled ?: true),
                    lastPing = "Just now"
                )
            }
            .sortedBy { it.registerNo }

        _teacherStudents.value = matchingStudents
    }

    suspend fun reportCurrentDeviceStatus(
        user: User,
        isAccessibility: Boolean,
        isOverlay: Boolean,
        isEnforcing: Boolean
    ) = withContext(Dispatchers.IO) {
        val deviceId = DevicePermissionHelper.getOrCreateDeviceId(context)
        val service = apiService
        if (service != null) {
            try {
                val call = service.reportDeviceStatus(
                    com.example.data.model.DeviceStatusReport(
                        deviceId = deviceId,
                        studentId = user.id,
                        studentName = user.name,
                        registerNo = user.registerNo,
                        className = user.className,
                        isAccessibilityEnabled = isAccessibility,
                        isOverlayEnabled = isOverlay,
                        isRestricted = isEnforcing
                    )
                )
                call.execute()
            } catch (_: Exception) {
                // Ignore network errors in reporting
            }
        }

        // Also update local record
        val existing = localDeviceStatuses[user.id.toString()]
        localDeviceStatuses[user.id.toString()] = StudentDeviceRecord(
            id = user.id,
            name = user.name,
            email = user.email,
            registerNo = user.registerNo,
            department = user.department,
            year = user.year,
            className = user.className,
            deviceId = deviceId,
            isAccessibilityEnabled = isAccessibility,
            isOverlayEnabled = isOverlay,
            isRestricted = isEnforcing,
            lastPing = "Online"
        )
    }
}
