package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.Adotam
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface AdotamService {
    /// <summary>
    /// Lista todas as adoções definitivas
    /// </summary>
    @GET("api/adotam")
    suspend fun listarAdocoes(): List<Adotam>

    /// <summary>
    /// Obter adoção por ID
    /// </summary>
    @GET("api/adotam/{id}")
    suspend fun obterAdocaoPorId(@Path("id") id: Int): Adotam

    /// <summary>
    /// Deletar uma adoção pelo ID
    /// </summary>
    @DELETE("api/adotam/{id}")
    suspend fun deletarAdocao(@Path("id") id: Int)

}