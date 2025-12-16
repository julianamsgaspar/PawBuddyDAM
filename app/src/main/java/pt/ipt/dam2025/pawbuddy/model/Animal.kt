package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

class Animal(

    /// <summary>
    /// identificação do animal
    /// </summary>
    @SerializedName("id")
   val id: Int = 0,

    /// <summary>
    /// nome do animal
    /// </summary>
    @SerializedName("nome")
    val nome: String,

    /// <summary>
    /// raça do animal
    /// </summary>
    @SerializedName("raca")
    val raca: String,

    /// <summary>
    /// idade do animal
    /// </summary>
    @SerializedName("idade")
    val idade: String,

    /// <summary>
    /// genero do animal
    /// </summary>
    @SerializedName("genero")
    val genero: String,

    /// <summary>
    /// especie do animal (gato, cão, etc)
    /// </summary>
    @SerializedName("especie")
    val especie: String,

    /// <summary>
    /// cor do animal
    /// </summary>
    @SerializedName("cor")
    val cor: String,

    /// <summary>
    /// imagem associada ao animal
    /// </summary>
    @SerializedName("imagem")
    val imagem: String? = null , // pode ser nulo

    /// <summary>
    /// Lista de animais que o utilizador tem intenção de adotar
    /// </summary>
    @SerializedName("intencaoDeAdocao")
    val intencaoDeAdocao: List<IntencaoDeAdocao>? = null, // pode ser nulo

    @SerializedName("doa")
    val doa: Any? = null

)