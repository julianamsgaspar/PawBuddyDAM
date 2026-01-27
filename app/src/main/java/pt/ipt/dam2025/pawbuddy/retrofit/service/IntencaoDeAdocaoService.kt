package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.CriarIntencaoRequest
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Serviço Retrofit responsável pela gestão das intenções de adoção.
 *
 * Esta interface define os endpoints da API REST relacionados com a entidade
 * Intenção de Adoção, permitindo operações de consulta, criação, atualização
 * de estado e remoção de intenções no sistema PawBuddy.
 *
 * Os métodos encontram-se organizados de acordo com o perfil de utilização
 * (Admin vs Utilizador), refletindo as regras de negócio implementadas
 * no backend.
 */
interface IntencaoDeAdocaoService {

    /**
     * Obtém todas as intenções de adoção registadas no sistema.
     *
     * Endpoint reservado a utilizadores com permissões administrativas.
     *
     * @return Lista completa de intenções de adoção.
     */
    @GET("api/Intencao")
    suspend fun getAll(): List<IntencaoDeAdocao>

    /**
     * Obtém todas as intenções de adoção ativas.
     *
     * Consideram-se ativas as intenções que ainda se encontram
     * em análise ou pendentes de decisão.
     *
     * @return Lista de intenções de adoção ativas.
     */
    @GET("api/Intencao/ativas")
    suspend fun getAtivas(): List<IntencaoDeAdocao>

    /**
     * Obtém o detalhe de uma intenção de adoção específica.
     *
     * @param id Identificador único da intenção de adoção.
     * @return Objeto [IntencaoDeAdocao] correspondente ao identificador fornecido.
     */
    @GET("api/Intencao/{id}")
    suspend fun getByIntencaoId(
        @Path("id") id: Int
    ): IntencaoDeAdocao

    /**
     * DTO utilizado para atualização do estado de uma intenção de adoção.
     *
     * Encapsula o novo estado da intenção, representado como valor inteiro
     * correspondente a um enum definido no backend.
     *
     * @property estado Novo estado da intenção de adoção.
     */
    data class EstadoRequest(
        val estado: Int
    )

    /**
     * Atualiza o estado de uma intenção de adoção.
     *
     * Corresponde a um pedido HTTP PUT ao endpoint
     * `/api/Intencao/{id}/estado`.
     *
     * @param id Identificador da intenção de adoção.
     * @param body Objeto [EstadoRequest] contendo o novo estado.
     */
    @PUT("api/Intencao/{id}/estado")
    suspend fun atualizarEstado(
        @Path("id") id: Int,
        @Body body: EstadoRequest
    ): Unit

    /**
     * Obtém todas as intenções de adoção associadas a um utilizador específico.
     *
     * @param utilizadorId Identificador do utilizador.
     * @return Lista de intenções de adoção do utilizador.
     */
    @GET("api/Intencao/utilizador/{utilizadorId}")
    suspend fun getByUtilizadorId(
        @Path("utilizadorId") utilizadorId: Int
    ): List<IntencaoDeAdocao>

    /**
     * Cria uma nova intenção de adoção.
     *
     * Corresponde a um pedido HTTP POST ao endpoint `/api/Intencao`,
     * utilizando um DTO específico para criação.
     *
     * @param body Objeto [CriarIntencaoRequest] com os dados da intenção.
     * @return Intenção de adoção criada.
     */
    @POST("api/Intencao")
    suspend fun criar(
        @Body body: CriarIntencaoRequest
    ): IntencaoDeAdocao

    /**
     * Atualiza os dados de uma intenção de adoção existente.
     *
     * @param id Identificador da intenção de adoção.
     * @param intencao Objeto [IntencaoDeAdocao] com os dados atualizados.
     * @return Intenção de adoção atualizada.
     */
    @PUT("api/Intencao/{id}")
    suspend fun atualizarIntencao(
        @Path("id") id: Int,
        @Body intencao: IntencaoDeAdocao
    ): IntencaoDeAdocao

    /**
     * Remove uma intenção de adoção do sistema.
     *
     * Corresponde a um pedido HTTP DELETE ao endpoint `/api/Intencao/{id}`.
     *
     * @param id Identificador da intenção de adoção a remover.
     */
    @DELETE("api/Intencao/{id}")
    suspend fun deleteIntencao(
        @Path("id") id: Int
    )
}
