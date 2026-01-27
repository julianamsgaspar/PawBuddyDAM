package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

/**
 * Pedido de criação de uma intenção de adoção.
 *
 * Esta classe representa o objeto enviado no corpo (body) de um pedido HTTP
 * para a API REST, aquando da submissão de uma nova intenção de adoção por parte
 * de um utilizador autenticado.
 *
 * O modelo segue o padrão DTO (Data Transfer Object), sendo utilizado
 * exclusivamente para transporte de dados entre a aplicação Android
 * e o backend, não contendo qualquer lógica de negócio.
 *
 * @property animalFK Identificador do animal ao qual a intenção de adoção se refere.
 * @property profissao Profissão do utilizador que submete a intenção de adoção.
 * @property residencia Informação relativa à residência do utilizador.
 * @property motivo Justificação ou motivação apresentada para a adoção do animal.
 * @property temAnimais Indica se o utilizador possui outros animais (ex.: "Sim" / "Não").
 * @property quaisAnimais Descrição dos animais que o utilizador já possui, caso aplicável.
 */
data class CriarIntencaoRequest(

    /** Identificador do animal (chave estrangeira) */
    @SerializedName("animalFK")
    val animalFK: Int,

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
     * ou caso esta informação não seja fornecida.
     */
    @SerializedName("quaisAnimais")
    val quaisAnimais: String? = null
)
