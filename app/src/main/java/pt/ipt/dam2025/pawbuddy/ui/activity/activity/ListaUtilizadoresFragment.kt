package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaUtilizadoresBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.UtilizadorAdapter


/**
 * A simple [Fragment] subclass.
 * Use the [ListaUtilizadoresFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ListaUtilizadoresFragment : Fragment() {

    private var _binding: FragmentListaUtilizadoresBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitInitializer().utilizadorService()
    private lateinit var adapter: UtilizadorAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaUtilizadoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        adapter = UtilizadorAdapter(onClick = { utilizador ->
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(
                        pt.ipt.dam2025.pawbuddy.R.id.fragmentContainer
                    ).id,
                    DetalhesUtilizadorFragment.newInstanceFromList(utilizador.id)
                )
                .addToBackStack(null)
                .commit()
        })

        binding.recyclerViewUtilizadores.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewUtilizadores.adapter = adapter

        carregarUtilizadores()

        binding.btnVoltarGestaoo.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    GestaoFragment() // envia o id do animal
                )
                //.addToBackStack(null) // para conseguir voltar
                .commit()
        }

    }

    private fun carregarUtilizadores() {
        lifecycleScope.launch {
            try {
                val lista = api.ListaDeUtilizadores()
                adapter.submitList(lista)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}