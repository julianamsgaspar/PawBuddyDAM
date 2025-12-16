package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.app.AlertDialog
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
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListarAdocoesFinaisBinding
import pt.ipt.dam2025.pawbuddy.model.Adotam
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.AdocaoAdapter



/**
 * A simple [Fragment] subclass.
 * Use the [ListarAdocoesFinaisFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ListarAdocoesFinaisFragment : Fragment() {

    private var _binding: FragmentListarAdocoesFinaisBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitInitializer().adotamService()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListarAdocoesFinaisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        carregarAdocoes()
        binding.btnVoltar.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, GestaoFragment())
                .addToBackStack(null)
                .commit()
        }

    }

    private fun setupRecyclerView() {
        binding.rvAdocoes.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun carregarAdocoes() {
        lifecycleScope.launch {
            try {
                val lista = api.GetAdocoes()
                binding.rvAdocoes.adapter = AdocaoAdapter(lista) { adocao ->
                    confirmarEliminar(adocao)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar adoções: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun confirmarEliminar(adocao: Adotam) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar")
            .setMessage("Eliminar adoção do animal ${adocao.animal?.nome}?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarAdocao(adocao.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarAdocao(id: Int) {
        lifecycleScope.launch {
            try {
                api.deleteAdocao(id)
                Toast.makeText(requireContext(), "Adoção eliminada", Toast.LENGTH_SHORT).show()
                carregarAdocoes()
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Erro ao eliminar: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /*private fun isAdmin(): Boolean {
        val prefs = requireContext().getSharedPreferences(
            SessionManager.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        return prefs.getBoolean(SessionManager.KEY_IS_ADMIN, false)
    }*/

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }}