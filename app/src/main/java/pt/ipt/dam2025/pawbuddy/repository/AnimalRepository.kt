package pt.ipt.dam2025.pawbuddy.repository

import pt.ipt.dam2025.pawbuddy.model.Animal
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

class AnimalRepository {

    private val service = RetrofitInitializer().animalService()

    suspend fun listarAnimais(): List<Animal> {
        return service.listarAnimais()
    }

    suspend fun obterAnimalPorId(id: Int): Animal {
        return service.getAnimal(id)
    }

    suspend fun criarAnimal(animal: Animal): Animal {
        return service.criarAnimal(animal)
    }

    suspend fun atualizarAnimal(id: Int, animal: Animal): Animal {
        return service.atualizarAnimal(id, animal)
    }

    suspend fun deletarAnimal(id: Int) {
        service.deletarAnimal(id)
    }

    // Exemplo: animais de um utilizador específico
    suspend fun listarAnimaisPorUtilizador(utilizadorId: Int): List<Animal> {
        return service.listarAnimaisPorUtilizador(utilizadorId)
    }
}