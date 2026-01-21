package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val message: String,
    val id: Int,
    val user: String,
    val email: String,
    val isAdmin: Boolean
)
