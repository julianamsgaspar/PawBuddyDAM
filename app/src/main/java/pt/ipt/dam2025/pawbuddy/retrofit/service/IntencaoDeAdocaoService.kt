package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface IntencaoDeAdocaoService {


    @GET("api/Intencao")
    suspend fun getAll(): List<IntencaoDeAdocao>


    @GET("api/Intencao/{id}")
    suspend fun getByIntencaoId(
        @Path("id") id: Int
    ): IntencaoDeAdocao


    @POST("api/Intencao")
    suspend fun criar(
        @Body intencao: IntencaoDeAdocao
    ): IntencaoDeAdocao


    @PUT("api/Intencao/{id}")
    suspend fun atualizarIntencao(
        @Path("id") id: Int,
        @Body intencao: IntencaoDeAdocao
    ): IntencaoDeAdocao


    @DELETE("api/Intencao/{id}")
    suspend fun deleteIntencao(
        @Path("id") id: Int
    )
}