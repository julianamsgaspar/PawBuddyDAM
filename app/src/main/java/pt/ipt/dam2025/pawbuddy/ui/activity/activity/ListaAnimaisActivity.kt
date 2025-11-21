package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import AnimalAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import pt.ipt.dam2025.pawbuddy.repository.AnimalRepository
import pt.ipt.dam2025.pawbuddy.databinding.ActivityListaAnimaisBinding
import pt.ipt.dam2025.pawbuddy.model.Animal


class ListaAnimaisActivity :  AppCompatActivity() {

    private lateinit var binding: ActivityListaAnimaisBinding
    private val repository = AnimalRepository()
    private val adapter = AnimalAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaAnimaisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        carregarAnimais()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewAnimais.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAnimais.adapter = adapter
    }

    private fun carregarAnimais() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val listaAnimais: List<Animal> = repository.listarAnimais()
                runOnUiThread {
                    adapter.atualizarLista(listaAnimais)
                }
            } catch (e: Exception) {
                Log.e("ListaAnimais", "Erro ao carregar animais: ${e.message}")
            }
        }
    }
}