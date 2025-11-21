package pt.ipt.dam2025.pawbuddy.repository

import pt.ipt.dam2025.pawbuddy.model.Adotam
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

class AdotamRepository {
    private val service = RetrofitInitializer().adotamService()

    suspend fun listarAdocoes(): List<Adotam> = service.listarAdocoes()
    suspend fun obterAdocaoPorId(id: Int): Adotam = service.obterAdocaoPorId(id)
    suspend fun deletarAdocao(id: Int) = service.deletarAdocao(id)
}