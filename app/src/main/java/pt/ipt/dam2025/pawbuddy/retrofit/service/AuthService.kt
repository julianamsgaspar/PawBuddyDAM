package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.model.LoginResponse
import pt.ipt.dam2025.pawbuddy.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthService {

    // Registo
    @Headers("Content-Type: application/json")
    @POST("api/AuthController/register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    // Login
    @Headers("Content-Type: application/json")
    @POST("api/AuthController/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/AuthController/hello")
    suspend fun hello(): String



}