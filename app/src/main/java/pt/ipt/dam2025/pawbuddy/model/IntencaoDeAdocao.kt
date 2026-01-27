package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

/**
 * Representa uma intenção de adoção registada no sistema PawBuddy.
 *
 * Esta classe modela a intenção de um utilizador em adotar um determinado animal,
 * correspondendo a uma entidade devolvida pela API REST do backend.
 *
 * A entidade contém tanto referências diretas (chaves estrangeiras) como
 * objetos associados, permitindo flexibilidade na apresentação e no consumo
 * de dados conforme o endpoint utilizado.
 *
 * @property id Identificador único da intenção de adoção.
 * @property estado Estado atual da intenção de adoção, representado como valor inteiro
 *                  correspondente a um enum definido no backend.
 * @property profissao Profissão do utilizador que submeteu a intenção.
 * @property residencia Informação relativa à residência do utilizador.
 * @property motivo Justificação apresentada para a adoção.
 * @property temAnimais Indica se o utilizador possui outros animais.
 * @property quaisAnimais Descrição dos animais que o utilizador já possui, quando aplicável.
 * @property dataIA Data de submissão da intenção de adoção, em formato ISO 8601.
 * @property utilizador Utilizador associado à intenção de adoção. Pode ser nulo.
 * @property animalFK Identificador do animal associado à intenção (chave estrangeira).
 * @property animal Animal associado à intenção de adoção. Pode ser nulo.
 */
data class IntencaoDeAdocao(

    /** Identificador único da intenção de adoção */
    @SerializedName("id")
    val id: Int = 0,

    /**
     * Estado atual da intenção de adoção.
     *
     * O backend expõe este valor como um inteiro correspondente a um enum
     * (ex.: 0 = Submetida, 1 = Em análise, 2 = Aprovada, 3 = Rejeitada).
     */
    @SerializedName("estado")
    val estado: Int,

    /** Profissão do utilizador */
    @SerializedName("profissao")
    val profissao: String,

    /** Informação sobre a residência do utilizador */
    @SerializedName("residencia")
    val residencia: String,

    /** Motivo apresentado para a adoção */
    @SerializedName("motivo")
    val motivo: String,

    /** Indica se o utilizador possui outros animais */
    @SerializedName("temAnimais")
    val temAnimais: String,

    /**
     * Descrição dos animais que o utilizador possui atualmente.
     *
     * Pode ser nulo caso o utilizador não possua outros animais
     * ou caso esta informação não seja fornecida pela API.
     */
    @SerializedName("quaisAnimais")
    val quaisAnimais: String? = null,

    /**
     * Data de submissão da intenção de adoção.
     *
     * Proveniente de um campo DateTime no backend, é recebida no formato
     * ISO 8601 (ex.: "2026-01-23T18:23:06").
     */
    @SerializedName("dataIA")
    val dataIA: String? = null,

    /**
     * Utilizador associado à intenção de adoção.
     *
     * Este campo pode ser nulo caso o backend não inclua explicitamente
     * a entidade Utilizador na resposta (ex.: ausência de Include).
     */
    @SerializedName("utilizador")
    val utilizador: Utilizador? = null,

    /** Identificador do animal associado à intenção (chave estrangeira) */
    @SerializedName("animalFK")
    val animalFK: Int,

    /**
     * Animal associado à intenção de adoção.
     *
     * Pode ser nulo caso a API apenas devolva a referência
     * (chave estrangeira) e não o objeto completo.
     */
    @SerializedName("animal")
    val animal: Animal? = null
)
