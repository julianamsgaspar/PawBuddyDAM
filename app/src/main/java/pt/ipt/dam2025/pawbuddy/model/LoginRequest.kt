package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("Email")
    val Email: String,
    @SerializedName("Password")
    val Password: String
)