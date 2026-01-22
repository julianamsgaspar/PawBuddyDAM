package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAdotarBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdotarFragment : Fragment() {

    private var _binding: FragmentAdotarBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitInitializer().intencaoService()

    private var animalId: Int = -1

    // ✅ SessionManager só quando o Fragment já tem context
    private val session by lazy { SessionManager(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animalId = arguments?.getInt("animalId", -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdotarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ proteção: se não há login ou animalId inválido, vai ao login
        if (!session.isLogged() || animalId <= 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_login_required),
                Toast.LENGTH_SHORT
            ).show()

            val bundle = Bundle().apply {
                putBoolean("redirectToAdotar", true)
                putInt("redirectAnimalId", animalId)
            }

            // ✅ remove este Adotar "inválido" da pilha antes de abrir o Login
            findNavController().navigate(
                R.id.loginFragment,
                bundle,
                navOptions {
                    popUpTo(R.id.adotarFragment) { inclusive = true }
                    launchSingleTop = true
                }
            )
            return
        }

        val opcoes = listOf(getString(R.string.option_no), getString(R.string.option_yes))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, opcoes)
        binding.ddTemAnimais.setAdapter(adapter)
        binding.ddTemAnimais.setText(opcoes[0], false)

        binding.ddTemAnimais.setOnItemClickListener { _, _, position, _ ->
            val escolheuSim = (position == 1)
            binding.tilQuaisAnimais.visibility = if (escolheuSim) View.VISIBLE else View.GONE
        }

        binding.btnSubmeter.setOnClickListener { enviarIntencao() }
           }

    private fun enviarIntencao() {
        // ✅ validação de sessão antes de submeter
        val utilizadorId = session.userId()
        if (!session.isLogged() || utilizadorId <= 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_login_required),
                Toast.LENGTH_SHORT
            ).show()

            val bundle = Bundle().apply {
                putBoolean("returnToPrevious", true)
                putString("origin", "adotar")
                putInt("originId", animalId)
            }

            findNavController().navigate(R.id.loginFragment, bundle)
            return

        }

        val profissao = binding.etProfissao.text.toString().trim()
        val residencia = binding.etResidencia.text.toString().trim()
        val motivo = binding.etMotivo.text.toString().trim()

        val temAnimais = binding.ddTemAnimais.text.toString().trim()
        val sim = getString(R.string.option_yes)
        val quaisAnimais =
            if (temAnimais == sim) binding.etQuaisAnimais.text.toString().trim() else null

        if (profissao.isBlank() || residencia.isBlank() || motivo.isBlank() || temAnimais.isBlank() ||
            (temAnimais == sim && quaisAnimais.isNullOrBlank())
        ) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_fill_required_fields),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val dataIA = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

        val intencao = IntencaoDeAdocao(
            estado = "Emvalidacao",
            profissao = profissao,
            residencia = residencia,
            motivo = motivo,
            temAnimais = temAnimais,
            quaisAnimais = quaisAnimais,
            dataIA = dataIA,
            utilizadorFK = utilizadorId,
            animalFK = animalId
        )

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.criar(intencao)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_intent_created),
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigate(R.id.listaIntencoesFragment)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_submit_intent,
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
