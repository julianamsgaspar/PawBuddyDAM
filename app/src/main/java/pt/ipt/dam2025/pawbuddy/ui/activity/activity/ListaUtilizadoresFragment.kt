package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaUtilizadoresBinding
import pt.ipt.dam2025.pawbuddy.model.Utilizador
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.UtilizadorAdapter
import pt.ipt.dam2025.pawbuddy.session.SessionManager

class ListaUtilizadoresFragment : Fragment() {

    private var _binding: FragmentListaUtilizadoresBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitInitializer().utilizadorService()
    private lateinit var adapter: UtilizadorAdapter

    private val session by lazy { SessionManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaUtilizadoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = UtilizadorAdapter(
            onClick = { utilizador ->
                val bundle = Bundle().apply {
                    putInt("userId", utilizador.id)
                    putBoolean("fromList", true)
                }
                findNavController().navigate(R.id.detalhesUtilizadorFragment, bundle)
            },
            onDeleteClick = { utilizador ->
                confirmarApagar(utilizador)
            }
        )

        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter


        carregarUtilizadores()

    }

    private fun carregarUtilizadores() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val lista = api.ListaDeUtilizadores()
                adapter.submitList(lista)

            } catch (e: Exception) {
                session.logout()

                Toast.makeText(
                    requireContext(),
                    "Sessão expirada. Faz login novamente.",
                    Toast.LENGTH_LONG
                ).show()

                if (isAdded) {
                    val b = Bundle().apply {
                        putBoolean("returnToPrevious", true)
                        putString("origin", "admin_users") // origem deste login
                        putInt("originId", -1)             // opcional
                    }
                    findNavController().navigate(R.id.loginFragment, b)
                }
            }
        }
    }







    private fun confirmarApagar(u: Utilizador) {
        // proteção básica: impedir que o admin apague a si próprio
        if (session.userId() == u.id) {
            Toast.makeText(requireContext(), "Não podes apagar a tua própria conta.", Toast.LENGTH_LONG).show()
            return
        }


        AlertDialog.Builder(requireContext())
            .setTitle("Apagar utilizador")
            .setMessage("Tens a certeza que queres apagar \"${u.nome}\"?\nEsta ação é irreversível.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Apagar") { _, _ -> apagarUtilizador(u) }
            .show()
    }

    private fun apagarUtilizador(u: Utilizador) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ⚠️ aqui tens de ter o endpoint no service
                api.EliminarUtilizador(u.id)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Utilizador apagado.", Toast.LENGTH_SHORT).show()
                    carregarUtilizadores()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao apagar: ${e.message ?: "erro"}",
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
