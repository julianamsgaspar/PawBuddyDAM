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
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListarAdocoesFinaisBinding
import pt.ipt.dam2025.pawbuddy.model.Adotam
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.AdocaoAdapter

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

        binding.rvAdocoes.layoutManager = LinearLayoutManager(requireContext())

        carregarAdocoes()

           }

    private fun carregarAdocoes() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lista = api.GetAdocoes()

                withContext(Dispatchers.Main) {
                    binding.rvAdocoes.adapter = AdocaoAdapter(lista) { adocao ->
                        confirmarEliminar(adocao)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_load_adoptions,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun confirmarEliminar(adocao: Adotam) {
        val nomeAnimal = adocao.animal?.nome ?: "?"

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_confirm_title))
            .setMessage(getString(R.string.dialog_delete_adoption_message, nomeAnimal))
            .setPositiveButton(getString(R.string.dialog_delete)) { _, _ ->
                eliminarAdocao(adocao.id)
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    private fun eliminarAdocao(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.deleteAdocao(id)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_adoption_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    carregarAdocoes()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_delete_adoption,
                            e.message ?: getString(R.string.error_generic)
                        ),
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
