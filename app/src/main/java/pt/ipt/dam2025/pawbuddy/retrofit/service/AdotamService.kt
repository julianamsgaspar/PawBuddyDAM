package pt.ipt.dam2025.pawbuddy.retrofit.service


import pt.ipt.dam2025.pawbuddy.model.Adotam
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Serviço Retrofit responsável pela gestão de adoções.
 *
 * Esta interface define os endpoints da API REST relacionados com
 * a entidade de adoção, permitindo a obtenção e remoção de registos
 * de adoções no sistema PawBuddy.
 *
 * Segue o padrão Service do Retrofit, onde cada método corresponde
 * a uma operação HTTP exposta pelo backend.
 */
interface AdotamService {

    /**
     * Obtém a lista de todas as adoções registadas no sistema.
     *
     * Corresponde a um pedido HTTP GET ao endpoint `/api/Adotam`.
     * O método devolve uma lista de objetos [Adotam] desserializados
     * a partir da resposta JSON da API.
     *
     * @return Lista de adoções registadas.
     */
    @GET("api/Adotam")
    suspend fun getAdocoes(): List<Adotam>

    /**
     * Remove um registo de adoção associado a um determinado animal.
     *
     * Corresponde a um pedido HTTP DELETE ao endpoint
     * `/api/Adotam/{animalId}`, onde o identificador do animal é
     * utilizado como parâmetro de rota.
     *
     * @param animalId Identificador do animal cuja adoção se pretende remover.
     */
    @DELETE("api/Adotam/{animalId}")
    suspend fun deleteAdocao(
        @Path("animalId") animalId: Int
    )
}
