package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

data class CriarIntencaoRequest(
    @SerializedName("animalFK") val animalFK: Int,
    @SerializedName("profissao") val profissao: String,
    @SerializedName("residencia") val residencia: String,
    @SerializedName("motivo") val motivo: String,
    @SerializedName("temAnimais") val temAnimais: String,
    @SerializedName("quaisAnimais") val quaisAnimais: String? = null
)
