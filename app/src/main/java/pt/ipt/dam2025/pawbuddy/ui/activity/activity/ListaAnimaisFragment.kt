package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaAnimaisBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.AnimalAdapter

class ListaAnimaisFragment : Fragment() {

    private var _binding: FragmentListaAnimaisBinding? = null
    private val binding get() = _binding!!

    private val retrofit = RetrofitInitializer()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaAnimaisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvAnimais.layoutManager = LinearLayoutManager(requireContext())

        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("isAdmin", false)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val animais = retrofit.animalService().listarAnimais()

                withContext(Dispatchers.Main) {
                    binding.txtErro.visibility = View.GONE

                    binding.rvAnimais.adapter = AnimalAdapter(animais) { animal ->
                        abrirDetalhes(animal.id)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtErro.visibility = View.VISIBLE
                    binding.txtErro.text = getString(
                        R.string.error_load_animals,
                        e.message ?: getString(R.string.error_generic)
                    )
                }
            }
        }

        binding.btnVoltarHome.setOnClickListener {
            if (isAdmin) {
                findNavController().navigate(R.id.gestaoFragment)
            } else {
                findNavController().navigate(R.id.homeFragment)
            }
            // Alternativa mais "natural" se o botão for mesmo "voltar":
            // findNavController().navigateUp()
        }
    }

    private fun abrirDetalhes(id: Int) {
        val bundle = Bundle().apply { putInt("animalId", id) }
        findNavController().navigate(R.id.animalDetailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
