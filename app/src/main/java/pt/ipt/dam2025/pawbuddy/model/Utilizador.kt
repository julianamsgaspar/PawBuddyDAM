package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName
import java.util.Date

class Utilizador {

    /// <summary>
    /// Identificação única do utilizador
    /// </summary>
    @SerializedName("id") val id: Int = 0

    /// <summary>
    /// nome do utilizador
    /// </summary>
    @SerializedName("nome") val nome: String = ""

    /// <summary>
    /// data de nascimento do utilizador
    /// </summary>
    @SerializedName("dataNascimento")
    val dataNascimento: Date =TODO() // ou java.util.Date com TypeAdapter

    /// <summary>
    /// número de identificação fiscal
    /// </summary>
    @SerializedName("nif")
    val nif: String = ""

    /// <summary>
    /// número de telemóvel do utilizador
    /// </summary>
    @SerializedName("telemovel")
    val telemovel: String = ""

    /// <summary>
    /// morada do utilizador
    /// </summary>
    @SerializedName("morada")
    val morada: String = ""

    /// <summary>
    /// Código Postal da morada do utilizador
    /// </summary>
    @SerializedName("codPostal")
    val codPostal: String = ""

    /// <summary>
    /// email do utilizador
    /// </summary>
    @SerializedName("email")
    val email: String = ""

    /// <summary>
    /// país de origem do utilizador
    /// </summary>
    @SerializedName("pais")
    val pais: String = ""

    /// <summary>
    /// Lista de animais que o utilizador tem intenção de adotar
    /// </summary>
    @SerializedName("intencaoDeAdocao")
    val intencaoDeAdocao: List<IntencaoDeAdocao>? = null

}