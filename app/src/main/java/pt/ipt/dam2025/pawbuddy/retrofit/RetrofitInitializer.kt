package pt.ipt.dam2025.pawbuddy.retrofit

import pt.ipt.dam2025.pawbuddy.retrofit.service.AdotamService
import pt.ipt.dam2025.pawbuddy.retrofit.service.AnimalService
import pt.ipt.dam2025.pawbuddy.retrofit.service.AuthService
import pt.ipt.dam2025.pawbuddy.retrofit.service.IntencaoDeAdocaoService
import pt.ipt.dam2025.pawbuddy.retrofit.service.UtilizadorService

class RetrofitInitializer {

    companion object {
        const val BASE_URL = RetrofitProvider.BASE_URL

        fun fullImageUrl(imageName: String): String {
            return RetrofitProvider.fullImageUrl(imageName)
        }
    }

    fun animalService(): AnimalService = RetrofitProvider.animalService
    fun intencaoService(): IntencaoDeAdocaoService = RetrofitProvider.intencaoService
    fun utilizadorService(): UtilizadorService = RetrofitProvider.utilizadorService
    fun adotamService(): AdotamService = RetrofitProvider.adotamService
    fun authService(): AuthService = RetrofitProvider.authService
}
