package pt.ipt.dam2025.pawbuddy.retrofit.service


import pt.ipt.dam2025.pawbuddy.model.Utilizador
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path


interface UtilizadorService {
    // GET: api/Utilizador
    @GET("api/Utilizador")
    suspend fun ListaDeUtilizadores(): List<Utilizador>

    // GET: api/Utilizador/{id}
    @GET("api/Utilizador/{id}")
    suspend fun getUtilizador(@Path("id") id: Int): Utilizador

    // DELETE: api/Utilizador/{id}
    @DELETE("api/Utilizador/{id}")
    suspend fun EliminarUtilizador(@Path("id") id: Int)


}
