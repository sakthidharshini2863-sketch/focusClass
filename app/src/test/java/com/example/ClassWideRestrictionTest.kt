package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.FocusRepository
import com.example.service.FocusPolicyManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ClassWideRestrictionTest {

    private lateinit var context: Context
    private lateinit var repository: FocusRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = FocusRepository(context)
    }

    @Test
    fun `test class wide restriction flow with two CSE-A students and one CSE-B student`() = runTest {
        // 1. Authenticate Teacher Prof. John Smith
        val teacherLoginResult = repository.login("prof.smith@college.edu", "admin123")
        assertTrue("Teacher login should succeed", teacherLoginResult.isSuccess)
        val teacher = teacherLoginResult.getOrThrow()
        assertTrue("User should have teacher role", teacher.isTeacher)

        // 2. Verify students exist: 2 in CSE-A, 1 in CSE-B
        repository.refreshTeacherClassStudents("CSE-A")
        val cseaStudents = repository.teacherStudents.value
        assertEquals("CSE-A must have 2 enrolled students", 2, cseaStudents.size)
        assertTrue("Alex must be in CSE-A", cseaStudents.any { it.name == "Alex Kumar" && it.className == "CSE-A" })
        assertTrue("Priya must be in CSE-A", cseaStudents.any { it.name == "Priya Sharma" && it.className == "CSE-A" })

        repository.refreshTeacherClassStudents("CSE-B")
        val csebStudents = repository.teacherStudents.value
        assertEquals("CSE-B must have 1 enrolled student", 1, csebStudents.size)
        assertTrue("Rahul must be in CSE-B", csebStudents.any { it.name == "Rahul Verma" && it.className == "CSE-B" })

        // 3. Teacher starts Focus Session for CSE-A with Instagram, YouTube, Snapchat
        val targetApps = listOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.snapchat.android"
        )
        val sessionResult = repository.manageTeacherSession(
            className = "CSE-A",
            action = "START",
            restrictedApps = targetApps,
            startTime = "10:00 AM",
            endTime = "11:00 AM",
            teacherId = teacher.id,
            teacherName = teacher.name
        )
        assertTrue("Start session should succeed", sessionResult.isSuccess)

        // 4. Verify Student 1 (Alex, CSE-A) receives active restriction policy
        repository.syncStudentPolicy("CSE-A")
        val student1Policy = repository.currentPolicy.value
        assertEquals("CSE-A", student1Policy.className)
        assertTrue("CSE-A session must be active", student1Policy.isEnforced)
        assertEquals("ACTIVE", student1Policy.status)
        assertEquals(3, student1Policy.restrictedApps.size)
        assertTrue(student1Policy.restrictedApps.contains("com.instagram.android"))
        assertTrue(student1Policy.restrictedApps.contains("com.google.android.youtube"))
        assertTrue(student1Policy.restrictedApps.contains("com.snapchat.android"))

        // Verify package matcher restricts Instagram and YouTube
        assertTrue(FocusPolicyManager.isPackageRestricted("com.instagram.android"))
        assertTrue(FocusPolicyManager.isPackageRestricted("com.google.android.youtube"))
        assertTrue(FocusPolicyManager.isPackageRestricted("com.snapchat.android"))
        assertFalse("Unrestricted apps like WhatsApp must not be blocked", FocusPolicyManager.isPackageRestricted("com.whatsapp"))

        // 5. Verify Student 2 (Priya, CSE-A) receives the exact same policy
        repository.syncStudentPolicy("CSE-A")
        val student2Policy = repository.currentPolicy.value
        assertTrue("Second CSE-A student must also receive active restriction", student2Policy.isEnforced)
        assertEquals(targetApps, student2Policy.restrictedApps)

        // 6. Verify Student 3 (Rahul, CSE-B) is COMPLETELY UNAFFECTED
        repository.syncStudentPolicy("CSE-B")
        val student3Policy = repository.currentPolicy.value
        assertEquals("CSE-B", student3Policy.className)
        assertFalse("CSE-B student must NOT have active restriction", student3Policy.isEnforced)
        assertEquals("INACTIVE", student3Policy.status)
        assertTrue("CSE-B restricted apps list must be empty", student3Policy.restrictedApps.isEmpty())
        assertFalse("Apps must not be restricted for CSE-B student", FocusPolicyManager.isPackageRestricted("com.instagram.android"))

        // 7. Teacher triggers "Allow Break" for CSE-A
        repository.manageTeacherSession(
            className = "CSE-A",
            action = "BREAK",
            restrictedApps = targetApps,
            startTime = "10:00 AM",
            endTime = "11:00 AM"
        )
        repository.syncStudentPolicy("CSE-A")
        val breakPolicy = repository.currentPolicy.value
        assertEquals("BREAK", breakPolicy.status)
        assertTrue(breakPolicy.isBreak)
        assertFalse("During break, enforcement must be paused", breakPolicy.isEnforced)
        assertFalse("Apps must not be blocked during break", FocusPolicyManager.isPackageRestricted("com.instagram.android"))

        // 8. Teacher triggers "Resume Focus" for CSE-A
        repository.manageTeacherSession(
            className = "CSE-A",
            action = "RESUME",
            restrictedApps = targetApps,
            startTime = "10:00 AM",
            endTime = "11:00 AM"
        )
        repository.syncStudentPolicy("CSE-A")
        val resumedPolicy = repository.currentPolicy.value
        assertEquals("ACTIVE", resumedPolicy.status)
        assertTrue(resumedPolicy.isEnforced)
        assertTrue(FocusPolicyManager.isPackageRestricted("com.instagram.android"))

        // 9. Teacher triggers "End Focus" for CSE-A
        repository.manageTeacherSession(
            className = "CSE-A",
            action = "END",
            restrictedApps = targetApps,
            startTime = "10:00 AM",
            endTime = "11:00 AM"
        )
        repository.syncStudentPolicy("CSE-A")
        val endedPolicy = repository.currentPolicy.value
        assertEquals("ENDED", endedPolicy.status)
        assertFalse(endedPolicy.isEnforced)
        assertFalse(FocusPolicyManager.isPackageRestricted("com.instagram.android"))
    }

    @Test
    fun `test student self registration cannot create teacher account`() = runTest {
        val regResult = repository.registerStudent(
            name = "John Doe",
            email = "john.cse@college.edu",
            registerNo = "21CS099",
            department = "CSE",
            year = "2nd Year",
            className = "CSE-A",
            pass = "secret123"
        )
        assertTrue(regResult.isSuccess)
        val student = regResult.getOrThrow()
        assertEquals("student", student.role)
        assertTrue(student.isStudent)
        assertFalse(student.isTeacher)
    }
}
