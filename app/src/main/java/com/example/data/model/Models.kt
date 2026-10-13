package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class UserRole {
    @Json(name = "teacher")
    TEACHER,
    @Json(name = "student")
    STUDENT
}

enum class SessionStatus {
    @Json(name = "ACTIVE")
    ACTIVE,
    @Json(name = "BREAK")
    BREAK,
    @Json(name = "ENDED")
    ENDED,
    @Json(name = "INACTIVE")
    INACTIVE
}

@JsonClass(generateAdapter = true)
data class User(
    val id: Long = 0,
    val name: String,
    val email: String,
    val role: String, // "teacher" or "student"
    @Json(name = "register_no")
    val registerNo: String = "",
    val department: String = "",
    val year: String = "",
    @Json(name = "class_name")
    val className: String = ""
) {
    val isTeacher: Boolean get() = role.equals("teacher", ignoreCase = true)
    val isStudent: Boolean get() = role.equals("student", ignoreCase = true)
}

data class RestrictedAppInfo(
    val packageName: String,
    val appName: String,
    val category: String = "Social Media",
    val defaultRestricted: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FocusPolicy(
    @Json(name = "class_name")
    val className: String = "",
    @Json(name = "has_active_session")
    val hasActiveSession: Boolean = false,
    val status: String = "INACTIVE", // "ACTIVE", "BREAK", "INACTIVE", "ENDED"
    @Json(name = "restricted_apps")
    val restrictedApps: List<String> = emptyList(),
    @Json(name = "start_time")
    val startTime: String = "",
    @Json(name = "end_time")
    val endTime: String = "",
    @Json(name = "teacher_name")
    val teacherName: String = ""
) {
    val isEnforced: Boolean get() = status.equals("ACTIVE", ignoreCase = true)
    val isBreak: Boolean get() = status.equals("BREAK", ignoreCase = true)
    val isEndedOrInactive: Boolean get() = !isEnforced && !isBreak
}

@JsonClass(generateAdapter = true)
data class StudentDeviceRecord(
    val id: Long = 0,
    val name: String,
    val email: String,
    @Json(name = "register_no")
    val registerNo: String,
    val department: String = "",
    val year: String = "",
    @Json(name = "class_name")
    val className: String,
    @Json(name = "device_id")
    val deviceId: String = "",
    @Json(name = "is_accessibility_enabled")
    val isAccessibilityEnabled: Boolean = false,
    @Json(name = "is_overlay_enabled")
    val isOverlayEnabled: Boolean = false,
    @Json(name = "is_restricted")
    val isRestricted: Boolean = false,
    @Json(name = "last_ping")
    val lastPing: String = ""
)

// API Request and Response DTOs

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val success: Boolean,
    val message: String? = null,
    val user: User? = null,
    val token: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterStudentRequest(
    val name: String,
    val email: String,
    @Json(name = "register_no")
    val registerNo: String,
    val department: String,
    val year: String,
    @Json(name = "class_name")
    val className: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterStudentResponse(
    val success: Boolean,
    val message: String,
    val user: User? = null
)

@JsonClass(generateAdapter = true)
data class SessionActionRequest(
    @Json(name = "class_name")
    val className: String,
    val action: String, // "START", "BREAK", "RESUME", "END"
    @Json(name = "restricted_apps")
    val restrictedApps: List<String> = emptyList(),
    @Json(name = "start_time")
    val startTime: String = "",
    @Json(name = "end_time")
    val endTime: String = "",
    @Json(name = "teacher_id")
    val teacherId: Long = 1,
    @Json(name = "teacher_name")
    val teacherName: String = ""
)

@JsonClass(generateAdapter = true)
data class SessionResponse(
    val success: Boolean,
    val message: String? = null,
    val session: FocusSessionDto? = null
)

@JsonClass(generateAdapter = true)
data class FocusSessionDto(
    @Json(name = "class_name")
    val className: String,
    val status: String,
    @Json(name = "start_time")
    val startTime: String = "",
    @Json(name = "end_time")
    val endTime: String = "",
    @Json(name = "restricted_apps")
    val restrictedApps: List<String> = emptyList(),
    @Json(name = "teacher_name")
    val teacherName: String = ""
)

@JsonClass(generateAdapter = true)
data class ClassStudentsResponse(
    @Json(name = "class_name")
    val className: String,
    val students: List<StudentDeviceRecord> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DeviceStatusReport(
    @Json(name = "device_id")
    val deviceId: String,
    @Json(name = "student_id")
    val studentId: Long?,
    @Json(name = "student_name")
    val studentName: String,
    @Json(name = "register_no")
    val registerNo: String,
    @Json(name = "class_name")
    val className: String,
    @Json(name = "is_accessibility_enabled")
    val isAccessibilityEnabled: Boolean,
    @Json(name = "is_overlay_enabled")
    val isOverlayEnabled: Boolean,
    @Json(name = "is_restricted")
    val isRestricted: Boolean
)

@JsonClass(generateAdapter = true)
data class BasicResponse(
    val success: Boolean,
    val message: String? = null
)
