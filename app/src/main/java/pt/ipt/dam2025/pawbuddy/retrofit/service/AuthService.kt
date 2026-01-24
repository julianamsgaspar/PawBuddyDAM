package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.model.LoginResponse
import pt.ipt.dam2025.pawbuddy.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST


    interface AuthService {

        @POST("api/AuthController/login")
        suspend fun login(@Body body: LoginRequest): LoginResponse

        @POST("api/AuthController/register")
        suspend fun register(@Body body: RegisterRequest): LoginResponse

        @POST("api/AuthController/logout")
        suspend fun logout(): Unit
    }

