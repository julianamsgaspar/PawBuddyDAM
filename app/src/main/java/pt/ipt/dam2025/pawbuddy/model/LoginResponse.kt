package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

/**
 * Resposta ao pedido de autenticação de utilizador.
 *
 * Esta classe representa a resposta devolvida pela API REST após um
 * pedido de login bem-sucedido ou com informação de estado relevante.
 *
 * O modelo segue o padrão DTO (Data Transfer Object), sendo utilizado
 * exclusivamente para transporte de dados entre o backend e a aplicação
 * Android, não contendo qualquer lógica de negócio.
 *
 * @property message Mensagem informativa devolvida pela API (ex.: sucesso ou erro).
 * @property id Identificador único do utilizador autenticado.
 * @property user Nome ou identificador público do utilizador.
 * @property email Endereço de correio eletrónico do utilizador.
 * @property isAdmin Indica se o utilizador possui permissões de administrador.
 */
data class LoginResponse(

    /** Mensagem informativa devolvida pela API */
    @SerializedName("message")
    val message: String,

    /** Identificador único do utilizador */
    @SerializedName("id")
    val id: Int,

    /** Nome ou identificador do utilizador */
    @SerializedName("user")
    val user: String,

    /** Endereço de correio eletrónico do utilizador */
    @SerializedName("email")
    val email: String,

    /** Indica se o utilizador possui privilégios de administrador */
    @SerializedName("isAdmin")
    val isAdmin: Boolean
)
