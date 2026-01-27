package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

/**
 * Representa uma adoção no sistema PawBuddy.
 *
 * Esta classe modela a relação de adoção entre um Utilizador e um Animal,
 * correspondendo a uma entidade proveniente da API REST do backend.
 * É utilizada para desserialização de objetos JSON através da biblioteca Gson.
 *
 * Cada instância de [Adotam] contém referências diretas (FK) e objetos associados,
 * permitindo flexibilidade no consumo de dados conforme o contexto da aplicação.
 *
 * @property id Identificador único da adoção.
 * @property dateA Data em que a adoção foi efetuada, no formato String conforme recebido da API.
 * @property utilizadorFK Identificador do utilizador associado à adoção (chave estrangeira).
 * @property utilizador Objeto [Utilizador] associado à adoção. Pode ser nulo caso não seja incluído na resposta da API.
 * @property animalFK Identificador do animal associado à adoção (chave estrangeira).
 * @property animal Objeto [Animal] associado à adoção. Pode ser nulo caso não seja incluído na resposta da API.
 */
class Adotam(

    /** Identificador único da adoção */
    @SerializedName("id")
    val id: Int,

    /** Data da adoção conforme definida pelo backend */
    @SerializedName("dateA")
    val dateA: String,

    /** Chave estrangeira do utilizador que efetuou a adoção */
    @SerializedName("utilizadorFK")
    val utilizadorFK: Int,

    /** Utilizador associado à adoção (opcional) */
    @SerializedName("utilizador")
    val utilizador: Utilizador?,

    /** Chave estrangeira do animal adotado */
    @SerializedName("animalFK")
    val animalFK: Int,

    /** Animal associado à adoção (opcional) */
    @SerializedName("animal")
    val animal: Animal?
)
