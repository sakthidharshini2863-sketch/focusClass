package com.example.data.api

import com.example.data.model.BasicResponse
import com.example.data.model.ClassStudentsResponse
import com.example.data.model.FocusPolicy
import com.example.data.model.LoginRequest
import com.example.data.model.LoginResponse
import com.example.data.model.RegisterStudentRequest
import com.example.data.model.RegisterStudentResponse
import com.example.data.model.SessionActionRequest
import com.example.data.model.SessionResponse
import com.example.data.model.DeviceStatusReport
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FocusApiService {

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/auth/register-student")
    fun registerStudent(@Body request: RegisterStudentRequest): Call<RegisterStudentResponse>

    @GET("api/teacher/class/{className}/students")
    fun getClassStudents(@Path("className") className: String): Call<ClassStudentsResponse>

    @POST("api/teacher/session")
    fun manageSession(@Body request: SessionActionRequest): Call<SessionResponse>

    @GET("api/teacher/session/{className}")
    fun getTeacherSession(@Path("className") className: String): Call<SessionResponse>

    @GET("api/student/policy/{className}")
    fun getStudentPolicy(@Path("className") className: String): Call<FocusPolicy>

    @POST("api/student/device-status")
    fun reportDeviceStatus(@Body request: DeviceStatusReport): Call<BasicResponse>
}
