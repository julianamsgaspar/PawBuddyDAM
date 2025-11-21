package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface IntencaoDeAdocaoService {
    @GET("api/intencaodeadocao")
    suspend fun listarIntencoes(): List<IntencaoDeAdocao>

    @GET("api/intencaodeadocao/{id}")
    suspend fun getIntencao(@Path("id") id: Int): IntencaoDeAdocao

    @POST("api/intencaodeadocao")
    suspend fun criarIntencao(@Body intencao: IntencaoDeAdocao): IntencaoDeAdocao

    @PUT("api/intencaodeadocao/{id}")
    suspend fun atualizarIntencao(@Path("id") id: Int, @Body intencao: IntencaoDeAdocao): IntencaoDeAdocao

    @DELETE("api/intencaodeadocao/{id}")
    suspend fun deletarIntencao(@Path("id") id: Int)
}