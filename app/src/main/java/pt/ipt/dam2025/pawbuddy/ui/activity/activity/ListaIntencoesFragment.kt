package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaIntencoesBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.IntencaoAdapter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ListaIntencoesFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ListaIntencoesFragment  : Fragment() {

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
        val isAdmin = prefs.getBoolean("isAdmin", false)
        val utilizadorId = prefs.getInt("utilizadorId", -1)

        carregarIntencoes(isAdmin, utilizadorId)

        binding.btnVoltarHome.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    if (isAdmin) GestaoFragment() else HomeFragment()
                )
                .commit()
        }
    }

    private fun carregarIntencoes(isAdmin: Boolean, utilizadorId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lista: List<IntencaoDeAdocao> = if (isAdmin) {
                    api.getAll() // admin vê todas
                } else {
                    try {
                        listOf(api.getByIntencaoId(utilizadorId))
                    } catch (e: Exception) {
                        emptyList() // se não houver intenções para o user
                    }
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
                            onClick = { },
                            onEliminar = { intencao ->
                                eliminarIntencao(intencao.id)
                            },
                            onEditarEstado = { intencao ->
                                abrirEditarEstado(intencao.id, intencao.estado)
                            }
                        )

                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro ao carregar intenções: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

        }
    }

    private fun eliminarIntencao(id: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.deleteIntencao(id)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Intenção eliminada", Toast.LENGTH_SHORT).show()

                    // recarrega a lista
                    val prefs = requireContext()
                        .getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)

                    carregarIntencoes(
                        prefs.getBoolean("isAdmin", false),
                        prefs.getInt("utilizadorId", -1)
                    )
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro ao eliminar", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    private fun abrirEditarEstado(id: Int, estadoAtual: Int) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                EditarEstadoIntencaoFragment().apply {
                    arguments = Bundle().apply {
                        putInt("id", id)
                        putInt("estado", estadoAtual)
                    }
                }
            )
            .addToBackStack(null)
            .commit()
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}