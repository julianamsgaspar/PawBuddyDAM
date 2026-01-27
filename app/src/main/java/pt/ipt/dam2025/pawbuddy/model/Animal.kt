package pt.ipt.dam2025.pawbuddy.model

import com.google.gson.annotations.SerializedName

/**
 * Representa um animal registado no sistema PawBuddy.
 *
 * Esta classe corresponde ao modelo de domínio utilizado na aplicação Android
 * para representar animais disponíveis para adoção. É utilizada na
 * desserialização de respostas JSON provenientes da API REST do backend,
 * recorrendo à biblioteca Gson.
 *
 * A entidade Animal pode estar associada a uma ou mais intenções de adoção,
 * refletindo a relação entre utilizadores e animais no processo de adoção.
 *
 * @property id Identificador único do animal.
 * @property nome Nome do animal.
 * @property raca Raça do animal.
 * @property idade Idade do animal, representada em formato textual conforme definido pelo backend.
 * @property genero Género do animal.
 * @property especie Espécie do animal (ex.: cão, gato).
 * @property cor Cor predominante do animal.
 * @property imagem Caminho ou URL da imagem associada ao animal. Pode ser nulo.
 * @property intencaoDeAdocao Lista de intenções de adoção associadas ao animal. Pode ser nula.
 * @property doa Informação relativa à doação do animal, quando aplicável.
 */
class Animal(

 /** Identificador único do animal */
 @SerializedName("id")
 val id: Int = 0,

 /** Nome do animal */
 @SerializedName("nome")
 val nome: String,

 /** Raça do animal */
 @SerializedName("raca")
 val raca: String,

 /** Idade do animal */
 @SerializedName("idade")
 val idade: String,

 /** Género do animal */
 @SerializedName("genero")
 val genero: String,

 /** Espécie do animal (ex.: cão, gato) */
 @SerializedName("especie")
 val especie: String,

 /** Cor predominante do animal */
 @SerializedName("cor")
 val cor: String,

 /** Imagem associada ao animal (URL ou caminho). Pode ser nula */
 @SerializedName("imagem")
 val imagem: String? = null,

 /**
  * Lista de intenções de adoção associadas ao animal.
  *
  * Pode ser nula caso a API não inclua esta informação na resposta
  * ou caso o animal ainda não tenha intenções registadas.
  */
 @SerializedName("intencaoDeAdocao")
 val intencaoDeAdocao: List<IntencaoDeAdocao>? = null,

 /**
  * Informação relativa à doação do animal.
  *
  * Utiliza o tipo Any? por representar uma estrutura dinâmica
  * ou opcional fornecida pelo backend.
  */
 @SerializedName("doa")
 val doa: Any? = null
)
