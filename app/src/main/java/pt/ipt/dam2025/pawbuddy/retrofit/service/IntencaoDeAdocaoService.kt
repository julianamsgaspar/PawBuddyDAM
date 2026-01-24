package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.CriarIntencaoRequest
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface IntencaoDeAdocaoService {

    // ADMIN – todas as intenções
    @GET("api/Intencao")
    suspend fun getAll(): List<IntencaoDeAdocao>

    @GET("api/Intencao/ativas")
    suspend fun getAtivas(): List<IntencaoDeAdocao>

    // ADMIN / DETALHE – uma intenção por ID
    @GET("api/Intencao/{id}")
    suspend fun getByIntencaoId(@Path("id") id: Int): IntencaoDeAdocao

    data class EstadoRequest(val estado: Int)

    @PUT("api/Intencao/{id}/estado")
    suspend fun atualizarEstado(
        @Path("id") id: Int,
        @Body body: EstadoRequest
    ): Unit

    // USER – intenções do utilizador
    @GET("api/Intencao/utilizador/{utilizadorId}")
    suspend fun getByUtilizadorId(@Path("utilizadorId") utilizadorId: Int): List<IntencaoDeAdocao>

    // ✅ CRIAR (request DTO)
    @POST("api/Intencao")
    suspend fun criar(@Body body: CriarIntencaoRequest): IntencaoDeAdocao

    @PUT("api/Intencao/{id}")
    suspend fun atualizarIntencao(
        @Path("id") id: Int,
        @Body intencao: IntencaoDeAdocao
    ): IntencaoDeAdocao

    @DELETE("api/Intencao/{id}")
    suspend fun deleteIntencao(@Path("id") id: Int)
}
