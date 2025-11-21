package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName
import java.util.Date

class IntencaoDeAdocao {
    /// <summary>
    /// identificação da intenção de adoção
    /// </summary>
    @SerializedName("id")
    val id: Int = 0

    /// <summary>
    /// Estado da adoção
    /// </summary>
    @SerializedName("estado")
    val estado: EstadoAdocao = TODO()

    /// <summary>
    /// profissão do utilizador
    /// </summary>
    @SerializedName("profissao")
    val profissao: String = ""

    /// <summary>
    /// que tipo de residência onde o utilizador vive
    /// </summary>
    @SerializedName("residencia")
    val residencia: String = ""

    /// <summary>
    /// motivo da adoção
    /// </summary>
    @SerializedName("motivo")
    val motivo: String = ""

    /// <summary>
    /// Pergunta se tem outros animais
    /// </summary>
    @SerializedName("temAnimais")
    val temAnimais: String = ""

    /// <summary>
    /// se tiver animais, quais?
    /// </summary>
    @SerializedName("quaisAnimais")
    val quaisAnimais: String? = null

    /// <summary>
    /// data da submissão do formulário
    /// </summary>
    @SerializedName("dataIA")
    val dataIA: Date =TODO()

    /// <summary>
    /// FK para referenciar o utilizador que tem a intenção de adotar um animal
    /// </summary>
    @SerializedName("utilizadorFK")
    val utilizadorFK: Int = 0

    /// <summary>
    /// Referência para o utilizador (objeto)
    /// </summary>
    @SerializedName("utilizador")
    val utilizador: Utilizador? = null// pode ser nulo se não estiver carregado

    /// <summary>
    /// FK para referenciar o animal que o utilizador tem a intenção de adotar
    /// </summary>
    @SerializedName("animalFK")
    val animalFK: Int = 0

    /// <summary>
    /// Referência para o animal (objeto)
    /// </summary>
    @SerializedName("animal")
    val animal: Animal? = null // pode ser nulo se não estiver carregado

    enum class EstadoAdocao {
        Reservado,
        EmProcesso,
        EmValidacao,
        Concluido,
        Rejeitado
    }
}
