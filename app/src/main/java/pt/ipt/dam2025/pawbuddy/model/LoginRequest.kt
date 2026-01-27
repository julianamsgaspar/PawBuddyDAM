package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

/**
 * Pedido de autenticação de utilizador.
 *
 * Esta classe representa o objeto enviado no corpo (body) de um pedido HTTP
 * aquando do processo de autenticação (login) de um utilizador na aplicação
 * PawBuddy.
 *
 * O modelo segue o padrão DTO (Data Transfer Object), sendo utilizado
 * exclusivamente para transporte de credenciais entre a aplicação Android
 * e a API REST do backend, não contendo qualquer lógica de negócio.
 *
 * @property Email Endereço de correio eletrónico do utilizador.
 * @property Password Palavra-passe associada à conta do utilizador.
 */
data class LoginRequest(

    /** Endereço de correio eletrónico utilizado para autenticação */
    @SerializedName("Email")
    val Email: String,

    /** Palavra-passe do utilizador */
    @SerializedName("Password")
    val Password: String
)
