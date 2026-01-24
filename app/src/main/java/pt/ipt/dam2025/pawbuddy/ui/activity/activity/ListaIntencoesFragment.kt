package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
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
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaIntencoesBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.IntencaoAdapter
import retrofit2.HttpException
import pt.ipt.dam2025.pawbuddy.session.SessionManager

class ListaIntencoesFragment : Fragment() {

    private var _binding: FragmentListaIntencoesBinding? = null
    private val binding get() = _binding!!
    private val session by lazy { SessionManager(requireContext()) }
    private val api = RetrofitProvider.intencaoService

    // Cache local para permitir alternar filtros sem pedir ao servidor
    private var allUserIntents: List<IntencaoDeAdocao> = emptyList()
    private var showFinalizadas: Boolean = false

    // Ajusta se o teu "Concluído" for outro número
    private val ESTADO_CONCLUIDO = 3

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaIntencoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvIntencoes.layoutManager = LinearLayoutManager(requireContext())

        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isLogged = prefs.getBoolean("isLogged", false)
        val isAdmin = prefs.getBoolean("isAdmin", false)
        val utilizadorId = prefs.getInt("utilizadorId", -1)

        if (!isLogged) {
            Toast.makeText(requireContext(), getString(R.string.error_login_required), Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // ✅ Filtro só para user normal (não-admin)
        setupFiltroUserOnly(isAdmin)

        val showBanner = arguments?.getBoolean("showBanner", false) ?: false
        val intencaoId = arguments?.getInt("intencaoId", -1) ?: -1

        if (showBanner) {
            arguments?.putBoolean("showBanner", false)

            com.google.android.material.snackbar.Snackbar
                .make(binding.root, getString(R.string.banner_intent_submitted), com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setAction(getString(R.string.action_view_status)) {
                    if (intencaoId > 0) {
                        val b = Bundle().apply { putInt("id", intencaoId) }
                        findNavController().navigate(R.id.intencaoDetalheFragment, b)
                    }
                }
                .show()
        }

        carregarIntencoes(isAdmin, utilizadorId)

    }

    override fun onResume() {
        super.onResume()
        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        carregarIntencoes(
            prefs.getBoolean("isAdmin", false),
            prefs.getInt("utilizadorId", -1)
        )
    }

    private fun setupFiltroUserOnly(isAdmin: Boolean) {
        if (isAdmin) {
            binding.chipGroupFiltro.visibility = View.GONE
            return
        }

        binding.chipGroupFiltro.visibility = View.VISIBLE

        // default: "Em curso"
        binding.chipEmCurso.isChecked = true
        showFinalizadas = false

        binding.chipGroupFiltro.setOnCheckedStateChangeListener { _, checkedIds ->
            showFinalizadas = checkedIds.contains(R.id.chipFinalizadas)
            aplicarFiltroEAtualizarUI(isAdmin = false)
        }
    }

    private fun carregarIntencoes(isAdmin: Boolean, utilizadorId: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lista: List<IntencaoDeAdocao> =
                    if (isAdmin) {
                        api.getAtivas()
                    } else {
                        if (utilizadorId <= 0) emptyList()
                        else api.getByUtilizadorId(utilizadorId)
                    }

                withContext(Dispatchers.Main) {
                    if (isAdmin) {
                        // Admin: mostra como vem do endpoint
                        mostrarLista(lista, isAdmin = true)
                    } else {
                        // User: guardar cache e aplicar filtro atual
                        allUserIntents = lista
                        aplicarFiltroEAtualizarUI(isAdmin = false)
                    }
                }

            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    if (e.code() == 401 || e.code() == 403) {
                        session.logout()
                        Toast.makeText(requireContext(), "Sessão expirada. Faz login novamente.", Toast.LENGTH_LONG).show()

                        val b = Bundle().apply {
                            putBoolean("returnToPrevious", true)
                            putString("origin", "intencoes")
                            putInt("originId", -1)
                        }
                        findNavController().navigate(R.id.loginFragment, b)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_load_intents, e.message ?: getString(R.string.error_generic)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_load_intents, e.message ?: getString(R.string.error_generic)),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun aplicarFiltroEAtualizarUI(isAdmin: Boolean) {
        val filtrada = if (showFinalizadas) {
            allUserIntents.filter { it.estado == ESTADO_CONCLUIDO }
        } else {
            allUserIntents.filter { it.estado != ESTADO_CONCLUIDO }
        }
        mostrarLista(filtrada, isAdmin)
    }

    private fun mostrarLista(lista: List<IntencaoDeAdocao>, isAdmin: Boolean) {
        if (lista.isEmpty()) {
            binding.rvIntencoes.visibility = View.GONE
            binding.txtSemIntencoes.visibility = View.VISIBLE
        } else {
            binding.txtSemIntencoes.visibility = View.GONE
            binding.rvIntencoes.visibility = View.VISIBLE

            binding.rvIntencoes.adapter = IntencaoAdapter(
                lista = lista,
                isAdmin = isAdmin,
                onClick = { intencao ->
                    val b = Bundle().apply { putInt("id", intencao.id) }
                    findNavController().navigate(R.id.intencaoDetalheFragment, b)
                },
                onEliminar = { intencao -> eliminarIntencao(intencao.id) },
                onEditarEstado = { intencao -> abrirEditarEstado(intencao.id, intencao.estado) }
            )
        }
    }

    private fun eliminarIntencao(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.deleteIntencao(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), getString(R.string.success_intent_deleted), Toast.LENGTH_SHORT).show()

                    val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
                    carregarIntencoes(
                        prefs.getBoolean("isAdmin", false),
                        prefs.getInt("utilizadorId", -1)
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_delete_intent, e.message ?: getString(R.string.error_generic)),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun abrirEditarEstado(id: Int, estadoAtual: Int) {
        val bundle = Bundle().apply {
            putInt("id", id)
            putInt("estado", estadoAtual)
        }
        findNavController().navigate(R.id.editarEstadoIntencaoFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
