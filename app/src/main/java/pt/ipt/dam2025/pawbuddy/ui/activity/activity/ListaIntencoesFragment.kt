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

        /*
         * ✅ Regra do projeto:
         * "Minhas Intenções" só faz sentido para utilizador autenticado.
         * Se não estiver logado, enviamos para Login.
         */
        if (!isLogged) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_login_required),
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigate(R.id.loginFragment)
            return
        }
        val showBanner = arguments?.getBoolean("showBanner", false) ?: false
        val intencaoId = arguments?.getInt("intencaoId", -1) ?: -1

        if (showBanner) {
            // limpar para não repetir quando roda/reentra
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

        // Carregar intenções (Admin: ativas / User: as suas)
        carregarIntencoes(isAdmin, utilizadorId)

        binding.btnVoltarHome.setOnClickListener {
            if (isAdmin) {
                findNavController().navigate(R.id.gestaoFragment)
            } else {
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // ✅ Ao voltar (ex.: depois de editar estado), recarrega a lista
        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        carregarIntencoes(
            prefs.getBoolean("isAdmin", false),
            prefs.getInt("utilizadorId", -1)
        )
    }

    private fun carregarIntencoes(isAdmin: Boolean, utilizadorId: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lista: List<IntencaoDeAdocao> =
                    if (isAdmin) {
                        // ✅ Admin: usar endpoint /ativas (Concluido já não vem)
                        api.getAtivas()
                    } else {
                        if (utilizadorId <= 0) emptyList()
                        else api.getByUtilizadorId(utilizadorId)
                    }

                withContext(Dispatchers.Main) {
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

            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    if (e.code() == 401 || e.code() == 403) {
                        session.logout()
                        Toast.makeText(
                            requireContext(),
                            "Sessão expirada. Faz login novamente.",
                            Toast.LENGTH_LONG
                        ).show()

                        val b = Bundle().apply {
                            putBoolean("returnToPrevious", true)
                            putString("origin", "intencoes")
                            putInt("originId", -1)
                        }
                        findNavController().navigate(R.id.loginFragment, b)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.error_load_intents,
                                e.message ?: getString(R.string.error_generic)
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_load_intents,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun eliminarIntencao(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.deleteIntencao(id)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_intent_deleted),
                        Toast.LENGTH_SHORT
                    ).show()

                    // ✅ Recarregar lista após eliminar
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
                        getString(
                            R.string.error_delete_intent,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ✅ estadoAtual agora é Int (enum vindo do backend normalmente é numérico)
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
