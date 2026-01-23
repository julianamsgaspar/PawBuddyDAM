package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentDetalhesUtilizadorBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

class DetalhesUtilizadorFragment : Fragment() {

    private var _binding: FragmentDetalhesUtilizadorBinding? = null
    private val binding get() = _binding!!
    private val api = RetrofitInitializer().utilizadorService()
    private var userId: Int = -1
    private var fromList: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalhesUtilizadorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments
        val argUserId = args?.getInt("userId", -1) ?: -1
        fromList = args?.getBoolean("fromList", false) ?: false

        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val loggedUserId = prefs.getInt("utilizadorId", -1)

        userId = if (argUserId != -1) argUserId else loggedUserId

        if (userId == -1) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_invalid_user_id),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // UI: botões consoante origem
        if (fromList) {
            binding.btnEliminar.visibility = View.VISIBLE

            binding.btnEliminar.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.dialog_confirm_title))
                    .setMessage(getString(R.string.dialog_confirm_delete_user))
                    .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                        eliminarUtilizador(userId)
                    }
                    .setNegativeButton(getString(R.string.dialog_no), null)
                    .show()
            }
        } else {
            binding.btnEliminar.visibility = View.GONE
                    }
        // Carregar dados do utilizador
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val u = api.getUtilizador(userId)

                withContext(Dispatchers.Main) {
                    binding.tvNome.text = u.nome
                    binding.tvEmail.text = u.email
                    binding.tvPais.text = u.pais
                    binding.tvMorada.text = u.morada
                    binding.tvCodPostal.text = u.codPostal
                    binding.tvTelemovel.text = u.telemovel
                    binding.tvNif.text = u.nif
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_load_user,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun eliminarUtilizador(idU: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.EliminarUtilizador(idU)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_user_deleted),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Depois de eliminar, faz sentido voltar para a lista de utilizadores
                    findNavController().navigate(R.id.listaUtilizadoresFragment)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_delete_user,
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
