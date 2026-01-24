package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

data class IntencaoDeAdocao(

    @SerializedName("id")
    val id: Int = 0,

    // backend envia enum como int (0..)
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

    // backend DateTime -> chega como string ISO (ex.: "2026-01-23T18:23:06")
    @SerializedName("dataIA")
    val dataIA: String? = null,

    // backend pode devolver objeto utilizador (quando faz Include)
    @SerializedName("utilizador")
    val utilizador: Utilizador? = null,

    @SerializedName("animalFK")
    val animalFK: Int,

    @SerializedName("animal")
    val animal: Animal? = null
)
