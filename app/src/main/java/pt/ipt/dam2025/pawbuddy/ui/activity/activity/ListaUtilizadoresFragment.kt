package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaUtilizadoresBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.UtilizadorAdapter

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
            val bundle = Bundle().apply {
                putInt("userId", utilizador.id)
                putBoolean("fromList", true)
            }
            findNavController().navigate(R.id.detalhesUtilizadorFragment, bundle)
        })

        binding.recyclerViewUtilizadores.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewUtilizadores.adapter = adapter

        carregarUtilizadores()

        binding.btnVoltarGestao.setOnClickListener {
            // Se foi aberto a partir do painel, "voltar" também faz sentido:
            // findNavController().navigateUp()
            findNavController().navigate(R.id.gestaoFragment)
        }
    }

    private fun carregarUtilizadores() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val lista = api.ListaDeUtilizadores()
                adapter.submitList(lista)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    getString(
                        R.string.error_load_users,
                        e.message ?: getString(R.string.error_generic)
                    ),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
