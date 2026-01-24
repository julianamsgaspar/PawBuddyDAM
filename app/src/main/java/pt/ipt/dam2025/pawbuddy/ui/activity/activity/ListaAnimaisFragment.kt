package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaAnimaisBinding
import pt.ipt.dam2025.pawbuddy.model.Animal
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.AnimalAdapter
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.AnimalAdminAdapter

class ListaAnimaisFragment : Fragment() {

    private var _binding: FragmentListaAnimaisBinding? = null
    private val binding get() = _binding!!

    private val animalApi = RetrofitProvider.animalService


    private var allAnimais: List<Animal> = emptyList()

    private lateinit var userAdapter: AnimalAdapter
    private lateinit var adminAdapter: AnimalAdminAdapter

    private var isAdmin: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaAnimaisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        isAdmin = prefs.getBoolean("isAdmin", false)

        // Layout + Adapter conforme perfil
        if (isAdmin) {
            binding.rvAnimais.layoutManager = LinearLayoutManager(requireContext())

            adminAdapter = AnimalAdminAdapter(
                onEdit = { animal -> abrirEditar(animal.id) },
                onDelete = { animal -> confirmarEliminar(animal) }
            )
            binding.rvAnimais.adapter = adminAdapter
        } else {
            val spanCount = if (resources.configuration.smallestScreenWidthDp >= 600) 4 else 3
            binding.rvAnimais.layoutManager = GridLayoutManager(requireContext(), spanCount)

            userAdapter = AnimalAdapter { animal -> abrirDetalhes(animal.id) }
            binding.rvAnimais.adapter = userAdapter
        }

        // Pull to refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadAnimais(showSpinner = false)
        }

        // Pesquisa local
        binding.etSearch.doAfterTextChanged {
            applyFilter(it?.toString().orEmpty())
        }

        // Inicial
        loadAnimais(showSpinner = true)

        // Voltar
        binding.btnVoltarHome.setOnClickListener {
            if (isAdmin) findNavController().navigate(R.id.gestaoFragment)
            else findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun loadAnimais(showSpinner: Boolean) {
        if (showSpinner) binding.progress.visibility = View.VISIBLE
        binding.txtErro.visibility = View.GONE
        binding.emptyState.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val animais = if (isAdmin) {
                    animalApi.listarAnimais()      // admin vê tudo
                } else {
                    animalApi.getDisponiveis()     // público vê só disponíveis
                }

                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    allAnimais = animais
                    applyFilter(binding.etSearch.text?.toString().orEmpty())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    binding.txtErro.visibility = View.VISIBLE
                    binding.txtErro.text = getString(
                        R.string.error_load_animals,
                        e.message ?: getString(R.string.error_generic)
                    )

                    submitList(emptyList())
                    binding.emptyState.visibility = View.GONE
                }
            }
        }
    }


    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()

        val filtered = if (q.isBlank()) {
            allAnimais
        } else {
            allAnimais.filter { a ->
                a.nome.lowercase().contains(q) ||
                        a.especie.lowercase().contains(q) ||
                        a.raca.lowercase().contains(q)
            }
        }

        submitList(filtered)

        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.txtErro.visibility = View.GONE
    }

    private fun submitList(list: List<Animal>) {
        if (isAdmin) adminAdapter.submitList(list) else userAdapter.submitList(list)
    }

    private fun abrirDetalhes(id: Int) {
        val bundle = Bundle().apply { putInt("animalId", id) }
        findNavController().navigate(R.id.animalDetailFragment, bundle)
    }

    private fun abrirEditar(id: Int) {
        val bundle = Bundle().apply { putInt("animalId", id) }
        findNavController().navigate(R.id.alterarAnimalFragment, bundle)
    }

    private fun confirmarEliminar(animal: Animal) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.action_delete))
            .setMessage("Eliminar o animal \"${animal.nome}\" (ID: ${animal.id})? Esta ação é irreversível.")
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                eliminarAnimal(animal.id)
            }
            .show()
    }

    private fun eliminarAnimal(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                animalApi.eliminarAnimal(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), getString(R.string.success_animal_deleted), Toast.LENGTH_SHORT).show()
                    loadAnimais(showSpinner = false)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_delete_animal, e.message ?: getString(R.string.error_generic)),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
