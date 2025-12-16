package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

class LoginResponse (
    @SerializedName("message")
    val message: String,
    @SerializedName("user")
    val user: String,
    @SerializedName("id")
    val id: Int
    )