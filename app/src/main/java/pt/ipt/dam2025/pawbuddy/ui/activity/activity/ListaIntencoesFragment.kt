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

class ListaIntencoesFragment : Fragment() {

    private var _binding: FragmentListaIntencoesBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitInitializer().intencaoService()

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

        // Carregar intenções (user: só as suas / admin: todas — admin refinamos depois)
        carregarIntencoes(isAdmin, utilizadorId)

        binding.btnVoltarHome.setOnClickListener {
            // Para já: admin volta para gestão, user volta para home
            if (isAdmin) {
                findNavController().navigate(R.id.gestaoFragment)
            } else {
                findNavController().navigate(R.id.homeFragment)
            }
            // Alternativa se quiseres comportamento "back":
            // findNavController().navigateUp()
        }
    }

    private fun carregarIntencoes(isAdmin: Boolean, utilizadorId: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {

                // ✅ Admin vê todas; user vê apenas as suas (lista)
                val lista: List<IntencaoDeAdocao> =
                    if (isAdmin) {
                        api.getAll()
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
                            onClick = { /* opcional */ },
                            onEliminar = { intencao -> eliminarIntencao(intencao.id) },
                            onEditarEstado = { intencao -> abrirEditarEstado(intencao.id, intencao.estado) }
                        )
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

                    // Recarregar lista após eliminar
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

    private fun abrirEditarEstado(id: Int, estadoAtual: String) {
        val bundle = Bundle().apply {
            putInt("id", id)
            putString("estado", estadoAtual)
        }
        findNavController().navigate(R.id.editarEstadoIntencaoFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
