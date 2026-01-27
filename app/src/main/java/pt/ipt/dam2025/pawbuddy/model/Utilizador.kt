package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

/**
 * Representa um utilizador registado no sistema PawBuddy.
 *
 * Esta classe modela a entidade Utilizador conforme definida no backend,
 * sendo utilizada na aplicação Android para desserialização de respostas
 * JSON provenientes da API REST.
 *
 * Contém informação de identificação, contacto e dados pessoais do utilizador,
 * respeitando a nulabilidade de campos opcionais que podem não ser fornecidos
 * ou não ser necessários em todos os contextos da aplicação.
 *
 * @property id Identificador único do utilizador.
 * @property nome Nome completo do utilizador.
 * @property email Endereço de correio eletrónico do utilizador.
 * @property password Palavra-passe do utilizador (fornecida apenas em contextos controlados).
 * @property dataNascimento Data de nascimento do utilizador, em formato ISO (yyyy-MM-dd).
 * @property nif Número de Identificação Fiscal do utilizador.
 * @property telemovel Número de telemóvel do utilizador.
 * @property morada Morada de residência do utilizador.
 * @property codPostal Código postal da residência.
 * @property pais País de residência do utilizador.
 */
class Utilizador(

    /** Identificador único do utilizador */
    @SerializedName("id")
    val id: Int = 0,

    /** Nome completo do utilizador */
    @SerializedName("nome")
    val nome: String,

    /** Endereço de correio eletrónico do utilizador */
    @SerializedName("email")
    val email: String,

    /**
     * Palavra-passe do utilizador.
     *
     * Este campo deve ser utilizado apenas em contextos de autenticação
     * ou registo, não devendo ser persistido localmente em texto claro.
     */
    @SerializedName("password")
    val password: String,

    /**
     * Data de nascimento do utilizador.
     *
     * Representada em formato ISO (yyyy-MM-dd) conforme definido pelo backend.
     */
    @SerializedName("dataNascimento")
    val dataNascimento: String,

    /** Número de Identificação Fiscal (NIF) */
    @SerializedName("nif")
    val nif: String? = null,

    /** Número de telemóvel do utilizador */
    @SerializedName("telemovel")
    val telemovel: String? = null,

    /** Morada de residência do utilizador */
    @SerializedName("morada")
    val morada: String? = null,

    /** Código postal da residência */
    @SerializedName("codPostal")
    val codPostal: String? = null,

    /** País de residência do utilizador */
    @SerializedName("pais")
    val pais: String? = null
)
