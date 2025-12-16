package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

class Utilizador(

    /// <summary>
    /// identificação do animal
    /// </summary>
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("nome")
    val nome: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("dataNascimento")
    val dataNascimento: String, // "yyyy-MM-dd"

    @SerializedName("nif")
    val nif: String? = null,

    @SerializedName("telemovel")
    val telemovel: String? = null,

    @SerializedName("morada")
    val morada: String? = null,

    @SerializedName("codPostal")
    val codPostal: String? = null,

    @SerializedName("pais")
    val pais: String? = null
)