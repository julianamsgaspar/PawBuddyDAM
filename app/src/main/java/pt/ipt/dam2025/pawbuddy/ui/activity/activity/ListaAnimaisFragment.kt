package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaAnimaisBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.AnimalAdapter

/**
 * A simple [Fragment] subclass.
 * Use the [ListaAnimaisFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
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


        binding.rvAnimais.layoutManager =
            LinearLayoutManager(requireContext())


        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isAdmin = prefs.getBoolean("isAdmin", false)


        CoroutineScope(Dispatchers.IO).launch {
            try {
                val animais = retrofit.animalService().listarAnimais()

                withContext(Dispatchers.Main) {
                    binding.rvAnimais.adapter = AnimalAdapter(animais) { animal ->
                        abrirDetalhes(animal.id)
                    }

                }




            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.txtErro.text = "Erro: ${e.message}"
                }
            }
        }

        binding.btnVoltarHome.setOnClickListener {
            val destinoFragment = if (isAdmin) {
                GestaoFragment()
            } else {
                HomeFragment()
            }

            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    destinoFragment
                )
                .commit()
        }
    }

    private fun abrirDetalhes(id: Int) {
        parentFragmentManager.beginTransaction()
            .replace(
                requireActivity().findViewById<View>(pt.ipt.dam2025.pawbuddy.R.id.fragmentContainer).id,
                AnimalDetailFragment.newInstance(id)
            )
            .addToBackStack(null)
            .commit()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}