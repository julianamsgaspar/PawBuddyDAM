package pt.ipt.dam2025.pawbuddy.retrofit.service

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import pt.ipt.dam2025.pawbuddy.model.Animal
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface AnimalService {
    // Listar todos os animais
    @GET("api/Animal")
    suspend fun listarAnimais(): List<Animal>

    // Detalhes de um animal
    @GET("api/Animal/{id}")
    suspend fun getAnimal(@Path("id") id: Int): Animal

    @Multipart
    @POST("api/Animal")
    suspend fun criarAnimal(
        @Part("nome") nome: RequestBody,
        @Part("raca") raca: RequestBody,
        @Part("especie") especie: RequestBody,
        @Part("idade") idade: RequestBody,
        @Part("genero") genero: RequestBody,
        @Part("cor") cor: RequestBody,
        @Part imagem: MultipartBody.Part
    ): Animal

    // PUT: atualizar animal com imagem opcional
    @Multipart
    @PUT("api/Animal/{id}")
    suspend fun atualizarAnimal(
        @Path("id") id: Int,
        @Part("id") idPart: RequestBody,
        @Part("nome") nome: RequestBody,
        @Part("raca") raca: RequestBody,
        @Part("idade") idade: RequestBody,
        @Part("genero") genero: RequestBody,
        @Part("especie") especie: RequestBody,
        @Part("cor") cor: RequestBody,
        @Part imagem: MultipartBody.Part? = null // opcional
    )

    // Deletar um animal
    @DELETE("api/Animal/{id}")
    suspend fun eliminarAnimal(@Path("id") id: Int)

    // animais de um utilizador específico
    @GET("utilizadores/{id}/Animal")
    suspend fun listarAnimaisPorUtilizador(@Path("id") utilizadorId: Int): List<Animal>
}