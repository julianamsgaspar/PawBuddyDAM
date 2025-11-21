package pt.ipt.dam2025.pawbuddy.repository

import pt.ipt.dam2025.pawbuddy.model.Utilizador
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

class UtilizadorRepository {

    private val service = RetrofitInitializer().utilizadorService()

    suspend fun listarUtilizadores(): List<Utilizador> {
        return service.listarUtilizadores()
    }

    suspend fun obterUtilizadorPorId(id: Int): Utilizador {
        return service.getUtilizador(id)
    }

    suspend fun criarUtilizador(utilizador: Utilizador): Utilizador {
        return service.criarUtilizador(utilizador)
    }

    suspend fun atualizarUtilizador(id: Int, utilizador: Utilizador): Utilizador {
        return service.atualizarUtilizador(id, utilizador)
    }

    suspend fun deletarUtilizador(id: Int) {
        service.deletarUtilizador(id)
    }
}
