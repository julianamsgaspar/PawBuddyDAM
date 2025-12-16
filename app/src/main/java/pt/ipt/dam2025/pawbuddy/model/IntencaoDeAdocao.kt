package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName
import java.util.Date

class IntencaoDeAdocao(

    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("estado")
    val estado: Int,

    @SerializedName("profissao")
    val profissao: String,

    @SerializedName("residencia")
    val residencia: String,

    @SerializedName("motivo")
    val motivo: String,

    @SerializedName("temAnimais")
    val temAnimais: String,

    @SerializedName("quaisAnimais")
    val quaisAnimais: String? = null,

    @SerializedName("dataIA")
    val dataIA: String, // 👈 STRING (mais simples)

    @SerializedName("utilizadorFK")
    val utilizadorFK: Int,

    @SerializedName("utilizador")
    val utilizador: Utilizador? = null,

    @SerializedName("animalFK")
    val animalFK: Int,

    @SerializedName("animal")
    val animal: Animal? = null

){


    fun getEstadoNome(): String = when (estado) {
        0 -> "Reservado"
        1 -> "Em Processo"
        2 -> "Em Validação"
        3 -> "Concluído"
        4 -> "Rejeitado"
        else -> "Desconhecido"
    }
}

