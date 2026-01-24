package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaUtilizadoresBinding
import pt.ipt.dam2025.pawbuddy.model.Utilizador
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.UtilizadorAdapter
import pt.ipt.dam2025.pawbuddy.session.SessionManager

class ListaUtilizadoresFragment : Fragment() {

    private var _binding: FragmentListaUtilizadoresBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitProvider.utilizadorService
    private lateinit var adapter: UtilizadorAdapter
    private val session by lazy { SessionManager(requireContext()) }

    // Lista original (fonte) para permitir filtrar sem voltar ao backend
    private var allUsers: List<Utilizador> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaUtilizadoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Segurança: este ecrã é admin-only
        if (!session.isLogged() || !session.isAdmin()) {
            Toast.makeText(requireContext(), "Acesso reservado a administradores.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

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

        // Swipe refresh
        binding.swipeRefreshUsers.setOnRefreshListener {
            carregarUtilizadores()
        }

        // Pesquisa (só Admin)
        binding.etSearchUsers.addTextChangedListener { editable ->
            aplicarFiltro(editable?.toString())
        }

        // UI inicial
        binding.txtErroUsers.visibility = View.GONE
        binding.emptyUsers.visibility = View.GONE
        binding.progressUsers.visibility = View.GONE

        carregarUtilizadores()
    }

    private fun carregarUtilizadores() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // UI: loading
                binding.txtErroUsers.visibility = View.GONE
                binding.emptyUsers.visibility = View.GONE
                binding.progressUsers.visibility = View.VISIBLE

                val lista = api.ListaDeUtilizadores()
                allUsers = lista

                // Aplica o filtro atual (se existir)
                aplicarFiltro(binding.etSearchUsers.text?.toString())

                // UI: done
                binding.progressUsers.visibility = View.GONE
                binding.swipeRefreshUsers.isRefreshing = false

            } catch (e: Exception) {
                binding.progressUsers.visibility = View.GONE
                binding.swipeRefreshUsers.isRefreshing = false

                // Aqui podes optar por manter a sessão e apenas mostrar erro.
                // Mantive o teu comportamento (logout) porque já tinhas assim.
                session.logout()

                Toast.makeText(
                    requireContext(),
                    "Sessão expirada. Faz login novamente.",
                    Toast.LENGTH_LONG
                ).show()

                if (isAdded) {
                    val b = Bundle().apply {
                        putBoolean("returnToPrevious", true)
                        putString("origin", "admin_users")
                        putInt("originId", -1)
                    }
                    findNavController().navigate(R.id.loginFragment, b)
                }
            }
        }
    }

    /**
     * Filtra a lista localmente por:
     * - ID (match exato se o input for número)
     * - Nome / Email / NIF (contains, case-insensitive)
     *
     * É chamado:
     * - sempre que o utilizador escreve no campo de pesquisa
     * - após carregar a lista do backend
     */
    private fun aplicarFiltro(queryRaw: String?) {
        val query = queryRaw?.trim().orEmpty()

        val filtrada = if (query.isBlank()) {
            allUsers
        } else {
            val qLower = query.lowercase()
            val qId = query.toIntOrNull()

            allUsers.filter { u ->
                val nome = u.nome?.lowercase().orEmpty()
                val email = u.email?.lowercase().orEmpty()
                val nif = u.nif?.lowercase().orEmpty()

                // ID exato quando query é numérica
                (qId != null && u.id == qId) ||
                        // Pesquisa textual
                        nome.contains(qLower) ||
                        email.contains(qLower) ||
                        nif.contains(qLower) ||
                        // opcional: permitir procurar por parte do ID (ex: "1" encontra 10, 21)
                        u.id.toString().contains(query)
            }
        }

        adapter.submitList(filtrada)

        // Empty state (também útil quando o filtro não encontra resultados)
        binding.emptyUsers.visibility = if (filtrada.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmarApagar(u: Utilizador) {
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
