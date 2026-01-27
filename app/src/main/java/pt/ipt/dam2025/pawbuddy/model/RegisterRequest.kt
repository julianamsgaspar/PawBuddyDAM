package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

/**
 * Pedido de registo de um novo utilizador.
 *
 * Esta classe representa o objeto enviado no corpo (body) de um pedido HTTP
 * à API REST do backend para criação de uma nova conta de utilizador
 * na aplicação PawBuddy.
 *
 * O modelo segue o padrão DTO (Data Transfer Object), sendo utilizado
 * exclusivamente para transporte de dados entre a aplicação Android
 * e o backend, não contendo qualquer lógica de negócio.
 *
 * @property nome Nome completo do utilizador.
 * @property email Endereço de correio eletrónico do utilizador.
 * @property password Palavra-passe associada à conta.
 * @property dataNascimento Data de nascimento do utilizador, em formato textual conforme definido pelo backend.
 * @property nif Número de Identificação Fiscal do utilizador.
 * @property telemovel Número de telemóvel do utilizador.
 * @property morada Morada de residência do utilizador.
 * @property codPostal Código postal da residência.
 * @property pais País de residência do utilizador.
 */
class RegisterRequest(

    /** Nome completo do utilizador */
    @SerializedName("Nome")
    val nome: String,

    /** Endereço de correio eletrónico do utilizador */
    @SerializedName("Email")
    val email: String,

    /** Palavra-passe do utilizador */
    @SerializedName("Password")
    val password: String,

    /**
     * Data de nascimento do utilizador.
     *
     * Representada em formato String conforme especificação da API
     * (ex.: "1995-08-21").
     */
    @SerializedName("DataNascimento")
    val dataNascimento: String,

    /** Número de Identificação Fiscal (NIF) */
    @SerializedName("Nif")
    val nif: String,

    /** Número de telemóvel do utilizador */
    @SerializedName("Telemovel")
    val telemovel: String,

    /** Morada de residência do utilizador */
    @SerializedName("Morada")
    val morada: String,

    /** Código postal da residência */
    @SerializedName("CodPostal")
    val codPostal: String,

    /** País de residência do utilizador */
    @SerializedName("Pais")
    val pais: String
)
