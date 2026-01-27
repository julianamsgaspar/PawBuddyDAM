package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.Utilizador
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Contrato (interface) de acesso à API REST para operações sobre a entidade Utilizador.
 *
 * Esta interface é utilizada pelo Retrofit para gerar, em tempo de execução, uma implementação
 * que executa pedidos HTTP aos endpoints definidos nas anotações (@GET, @PUT, @DELETE).
 *
 * Nota: As funções são `suspend` para suportar execução assíncrona com corrotinas, evitando
 * bloqueio da UI em Android.
 */
interface UtilizadorService {

    // =========================
    // ADMIN
    // =========================

    /**
     * Obtém a lista completa de utilizadores.
     *
     * Endpoint: GET /api/Utilizador
     * Retorno: lista de [Utilizador].
     */
    @GET("api/Utilizador")
    suspend fun ListaDeUtilizadores(): List<Utilizador>

    /**
     * Obtém um utilizador específico através do seu identificador.
     *
     * Endpoint: GET /api/Utilizador/{id}
     * @param id identificador do utilizador no backend.
     * @return instância de [Utilizador] correspondente ao identificador fornecido.
     */
    @GET("api/Utilizador/{id}")
    suspend fun getUtilizador(@Path("id") id: Int): Utilizador

    /**
     * Elimina um utilizador pelo seu identificador.
     *
     * Endpoint: DELETE /api/Utilizador/{id}
     * @param id identificador do utilizador a eliminar.
     *
     * Nota: não devolve corpo (body) na resposta, apenas o estado HTTP.
     */
    @DELETE("api/Utilizador/{id}")
    suspend fun EliminarUtilizador(@Path("id") id: Int)

    // =========================
    // USER NORMAL - PERFIL
    // =========================

    /**
     * Obtém o perfil do utilizador atualmente autenticado.
     *
     * Endpoint: GET /api/Utilizador/me
     * @return dados de perfil do utilizador autenticado como [Utilizador].
     */
    @GET("api/Utilizador/me")
    suspend fun getMe(): Utilizador

    /**
     * DTO (Data Transfer Object) para atualização do perfil do utilizador autenticado.
     *
     * Este objeto representa o corpo (JSON) enviado no pedido PUT do endpoint /me.
     * Mantém apenas os campos editáveis no contexto do perfil.
     */
    data class UpdateMyProfileRequest(
        val email: String,
        val morada: String,
        val telemovel: String,
        val codPostal: String
    )

    /**
     * Atualiza os dados do perfil do utilizador autenticado.
     *
     * Endpoint: PUT /api/Utilizador/me
     * @param body corpo do pedido com os campos de atualização, representados por [UpdateMyProfileRequest].
     *
     * Nota: não devolve corpo (body) na resposta, apenas o estado HTTP.
     */
    @PUT("api/Utilizador/me")
    suspend fun updateMe(@Body body: UpdateMyProfileRequest)
}
