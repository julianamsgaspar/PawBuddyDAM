package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.Animal
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AnimalService {
    // Listar todos os animais
    @GET("api/animais")
    suspend fun listarAnimais(): List<Animal>

    // Detalhes de um animal
    @GET("api/animais/{id}")
    suspend fun getAnimal(@Path("id") id: Int): Animal

    // Criar um novo animal
    @POST("api/animais")
    suspend fun criarAnimal(@Body animal: Animal): Animal

    // Atualizar um animal existente
    @PUT("api/animais/{id}")
    suspend fun atualizarAnimal(@Path("id") id: Int, @Body animal: Animal): Animal

    // Deletar um animal
    @DELETE("api/animais/{id}")
    suspend fun deletarAnimal(@Path("id") id: Int)

    // animais de um utilizador específico
    @GET("utilizadores/{id}/animais")
    suspend fun listarAnimaisPorUtilizador(@Path("id") utilizadorId: Int): List<Animal>
}