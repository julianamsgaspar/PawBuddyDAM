package pt.ipt.dam2025.pawbuddy.retrofit

import pt.ipt.dam2025.pawbuddy.retrofit.service.AdotamService
import pt.ipt.dam2025.pawbuddy.retrofit.service.AnimalService
import pt.ipt.dam2025.pawbuddy.retrofit.service.IntencaoDeAdocaoService
import pt.ipt.dam2025.pawbuddy.retrofit.service.NoteService
import pt.ipt.dam2025.pawbuddy.retrofit.service.UtilizadorService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInitializer {
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:7219/") // porta do seu backend .NET local
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun animalService(): AnimalService = retrofit.create(AnimalService::class.java)
    fun intencaoService(): IntencaoDeAdocaoService = retrofit.create(IntencaoDeAdocaoService::class.java)
    fun utilizadorService(): UtilizadorService = retrofit.create(UtilizadorService::class.java)
    fun adotamService(): AdotamService = retrofit.create(AdotamService::class.java)


}