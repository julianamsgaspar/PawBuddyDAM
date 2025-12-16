package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentDetalhesUtilizadorBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.ui.activity.activity.HomeFragment

class DetalhesUtilizadorFragment : Fragment() {

    private var _binding: FragmentDetalhesUtilizadorBinding? = null
    private val binding get() = _binding!!
    private val api = RetrofitInitializer().utilizadorService()

    private var userId: Int = -1
    private var fromList: Boolean = false  // Para saber de onde veio o fragmento

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetalhesUtilizadorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1️ Obter userId vindo da lista (se existir)
        val argUserId = arguments?.getInt("userId", -1) ?: -1

        // 2️ Obter ID do utilizador logado
        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val loggedUserId = prefs.getInt("utilizadorId", -1)

        // 3️ Identificar origem
        fromList = arguments?.getBoolean("fromList", false) ?: false

        // 4️ Escolher qual ID usar
        userId = if (argUserId != -1) argUserId else loggedUserId

        if (userId == -1) {
            Toast.makeText(requireContext(), "Erro: ID inválido", Toast.LENGTH_SHORT).show()
            return
        }

        // 5️⃣ Configurar botões com base na origem
        if (fromList) {
            binding.btnEliminar.visibility = View.VISIBLE
            binding.btnVoltar.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, ListaUtilizadoresFragment())
                    .commit()
            }
        } else {
            binding.btnEliminar.visibility = View.GONE
            binding.btnVoltar.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, HomeFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        // 6️⃣ Ação do botão eliminar (só funciona se veio da lista)
        binding.btnEliminar.setOnClickListener {
            eliminarUtilizador(userId)
        }

        // 7️⃣ Carregar dados do utilizador da API
        lifecycleScope.launch {
            try {
                val u = api.getUtilizador(userId)

                binding.tvNome.text = u.nome
                binding.tvEmail.text = u.email
                binding.tvPais.text = u.pais
                binding.tvMorada.text = u.morada
                binding.tvCodPostal.text = u.codPostal
                binding.tvTelemovel.text = u.telemovel
                binding.tvNif.text = u.nif

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar utilizador: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun eliminarUtilizador(idU: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.EliminarUtilizador(idU)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Utilizador eliminado com sucesso!", Toast.LENGTH_SHORT).show()

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ListaUtilizadoresFragment())
                        .commit()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro ao eliminar utilizador: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {

        fun newInstanceFromList(userId: Int): DetalhesUtilizadorFragment {
            val fragment = DetalhesUtilizadorFragment()
            val args = Bundle()
            args.putInt("userId", userId)
            args.putBoolean("fromList", true)
            fragment.arguments = args
            return fragment
        }

        fun newInstanceFromHome(): DetalhesUtilizadorFragment {
            val fragment = DetalhesUtilizadorFragment()
            val args = Bundle()
            args.putBoolean("fromList", false)
            fragment.arguments = args
            return fragment
        }
    }
}
