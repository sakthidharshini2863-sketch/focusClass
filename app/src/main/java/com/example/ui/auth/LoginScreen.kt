package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.theme.FocusBlueContainer
import com.example.ui.theme.FocusBlueDark
import com.example.ui.theme.FocusBluePrimary
import com.example.ui.theme.FocusStatusRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onLogin: (email: String, pass: String) -> Unit,
    onRegisterStudent: (
        name: String,
        email: String,
        regNo: String,
        dept: String,
        year: String,
        className: String,
        pass: String
    ) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Student Login, 1: Teacher Login, 2: Student Register

    // Login Form State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Registration Form State
    var regName by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regNumber by remember { mutableStateOf("") }
    var regDept by remember { mutableStateOf("Computer Science & Engineering") }
    var regYear by remember { mutableStateOf("3rd Year") }
    var regClass by remember { mutableStateOf("CSE-A") }
    var regPassword by remember { mutableStateOf("") }

    var expandedClassDropdown by remember { mutableStateOf(false) }
    val classOptions = listOf("CSE-A", "CSE-B", "ECE-A", "IT-A")

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("login_screen"),
        color = Color(0xFFF8FAFC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Brand Header (Clean Blue & White)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(FocusBluePrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "FocusClass Shield",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "FocusClass",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = FocusBlueDark
            )

            Text(
                text = "Smart Classroom Distraction Prevention",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Mode Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFFF1F5F9),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = FocusBluePrimary
                            )
                        },
                        modifier = Modifier.background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Student", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            modifier = Modifier.testTag("tab_student_login")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Teacher", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            modifier = Modifier.testTag("tab_teacher_login")
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Register", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            modifier = Modifier.testTag("tab_register_student")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = errorMessage,
                                color = FocusStatusRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    when (selectedTab) {
                        0, 1 -> {
                            // Login Form
                            val isTeacher = (selectedTab == 1)

                            Text(
                                text = if (isTeacher) "Authorized Faculty Sign In" else "Student Sign In",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusBlueDark
                            )

                            if (isTeacher) {
                                Text(
                                    text = "Teacher accounts are pre-authorized by administration.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("College Email") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = FocusBluePrimary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("email_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FocusBluePrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = FocusBluePrimary)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input"),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = FocusBluePrimary,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { onLogin(email, password) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("login_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = if (isTeacher) "Log In as Teacher" else "Log In as Student",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        2 -> {
                            // Student Registration Form
                            Text(
                                text = "Student Registration",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = FocusBlueDark
                            )
                            Text(
                                text = "Enroll your device in your classroom section.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = regName,
                                onValueChange = { regName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_name_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regEmail,
                                onValueChange = { regEmail = it },
                                label = { Text("College Email") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_email_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = regNumber,
                                    onValueChange = { regNumber = it },
                                    label = { Text("Register No") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reg_number_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                ExposedDropdownMenuBox(
                                    expanded = expandedClassDropdown,
                                    onExpandedChange = { expandedClassDropdown = !expandedClassDropdown },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = regClass,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Class / Sec") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClassDropdown) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .testTag("reg_class_input"),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expandedClassDropdown,
                                        onDismissRequest = { expandedClassDropdown = false }
                                    ) {
                                        classOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt) },
                                                onClick = {
                                                    regClass = opt
                                                    expandedClassDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = regDept,
                                    onValueChange = { regDept = it },
                                    label = { Text("Department") },
                                    modifier = Modifier.weight(1.2f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = regYear,
                                    onValueChange = { regYear = it },
                                    label = { Text("Year") },
                                    modifier = Modifier.weight(0.8f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = regPassword,
                                onValueChange = { regPassword = it },
                                label = { Text("Create Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reg_password_input"),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    onRegisterStudent(
                                        regName,
                                        regEmail,
                                        regNumber,
                                        regDept,
                                        regYear,
                                        regClass,
                                        regPassword
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("register_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isLoading &&
                                        regName.isNotBlank() &&
                                        regEmail.isNotBlank() &&
                                        regNumber.isNotBlank() &&
                                        regPassword.isNotBlank()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "Complete Student Registration",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Fill Test Credentials Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ Quick-Fill Accounts for Testing:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = FocusBluePrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedTab = 1
                                email = "prof.smith@college.edu"
                                password = "admin123"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("quick_fill_teacher"),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusBlueDark),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Prof. Smith (Teacher)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                selectedTab = 0
                                email = "alex.cse@college.edu"
                                password = "student123"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("quick_fill_student1"),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Alex (CSE-A)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                selectedTab = 0
                                email = "priya.cse@college.edu"
                                password = "student123"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("quick_fill_student2"),
                            colors = ButtonDefaults.buttonColors(containerColor = FocusBluePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Priya (CSE-A)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                selectedTab = 0
                                email = "rahul.cse@college.edu"
                                password = "student123"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("quick_fill_student3"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Rahul (CSE-B)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
