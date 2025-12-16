package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

class RegisterRequest (
   // val id: Int = 0,
    @SerializedName("Nome")
    val nome: String,
    @SerializedName("Email")
    val email: String,
    @SerializedName("Password")
    val password: String,
    @SerializedName("DataNascimento")
    val dataNascimento: String,
    @SerializedName("Nif")
    val nif: String,
    @SerializedName("Telemovel")
    val telemovel: String,
    @SerializedName("Morada")
    val morada: String,
    @SerializedName("CodPostal")
    val codPostal: String,
    @SerializedName("Pais")
    val pais: String
)