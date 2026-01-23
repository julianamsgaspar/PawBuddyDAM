package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.Adotam
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

interface AdotamService {

    @GET("api/Adotam")
    suspend fun getAdocoes(): List<Adotam>

    @DELETE("api/Adotam/{animalId}")
    suspend fun deleteAdocao(@Path("animalId") animalId: Int)
}

