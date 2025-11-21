package pt.ipt.dam2025.pawbuddy.retrofit.service


import pt.ipt.dam2025.pawbuddy.model.Utilizador
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UtilizadorService {

    // Listar todos os utilizadores
    @GET("api/utilizadores")
    fun listarUtilizadores(): List<Utilizador>

    // Detalhes de um utilizador
    @GET("api/utilizadores/{id}")
    suspend fun getUtilizador(@Path("id") id: Int): Utilizador

    // Criar um novo utilizador
    @POST("api/utilizadores")
    suspend fun criarUtilizador(@Body utilizador: Utilizador): Utilizador

    // Atualizar um utilizador existente
    @PUT("api/utilizadores/{id}")
    suspend fun atualizarUtilizador(@Path("id") id: Int, @Body utilizador: Utilizador): Utilizador

    // Deletar um utilizador
    @DELETE("api/utilizadores/{id}")
    suspend fun deletarUtilizador(@Path("id") id: Int)
}