package pt.ipt.dam2025.pawbuddy.retrofit.service

import pt.ipt.dam2025.pawbuddy.model.Animal
import retrofit2.Call
import retrofit2.http.GET

interface NoteService {
    @GET("API/getNotes.php")
    fun list(): Call<List<Animal>>
}