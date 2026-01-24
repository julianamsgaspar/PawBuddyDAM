package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import android.text.InputFilter
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
import pt.ipt.dam2025.pawbuddy.model.CriarIntencaoRequest
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import retrofit2.HttpException

class AdotarFragment : Fragment() {

    private var _binding: FragmentAdotarBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitProvider.intencaoService
    private val session by lazy { SessionManager(requireContext()) }

    private var animalId: Int = -1

    companion object {
        private const val MAX_50 = 50
        private const val MAX_MOTIVO = 500
    }

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

        // Proteção: precisa de login e animal válido
        if (!session.isLogged() || animalId <= 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_login_required),
                Toast.LENGTH_SHORT
            ).show()
            navegarParaLoginReturnToAdotar()
            return
        }

        applyInputLimits()
        setupDropdownTemAnimais()
        setupLiveValidation()

        binding.btnSubmeter.setOnClickListener { enviarIntencao() }
    }

    private fun applyInputLimits() {
        binding.etProfissao.filters = arrayOf(InputFilter.LengthFilter(MAX_50))
        binding.etResidencia.filters = arrayOf(InputFilter.LengthFilter(MAX_50))
        binding.etQuaisAnimais.filters = arrayOf(InputFilter.LengthFilter(MAX_50))
        binding.etMotivo.filters = arrayOf(InputFilter.LengthFilter(MAX_MOTIVO))
    }

    private fun setupDropdownTemAnimais() {
        val opcoes = listOf(getString(R.string.option_no), getString(R.string.option_yes))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, opcoes)

        binding.ddTemAnimais.setAdapter(adapter)
        binding.ddTemAnimais.setText(opcoes[0], false)
        binding.tilQuaisAnimais.visibility = View.GONE

        binding.ddTemAnimais.setOnItemClickListener { _, _, position, _ ->
            binding.tilTemAnimais.error = null

            val escolheuSim = (position == 1)
            binding.tilQuaisAnimais.visibility = if (escolheuSim) View.VISIBLE else View.GONE

            if (!escolheuSim) {
                binding.etQuaisAnimais.setText("")
                binding.tilQuaisAnimais.error = null
            }
        }
    }

    private fun setupLiveValidation() {
        binding.etProfissao.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilProfissao.error = null
        }
        binding.etResidencia.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilResidencia.error = null
        }
        binding.etMotivo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilMotivo.error = null
        }
        binding.etQuaisAnimais.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilQuaisAnimais.error = null
        }
    }

    private fun enviarIntencao() {
        clearErrors()

        val utilizadorId = session.userId()
        if (!session.isLogged() || utilizadorId <= 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_login_required),
                Toast.LENGTH_SHORT
            ).show()
            navegarParaLoginReturnToAdotar()
            return
        }

        val profissao = binding.etProfissao.text?.toString()?.trim().orEmpty()
        val residencia = binding.etResidencia.text?.toString()?.trim().orEmpty()
        val motivo = binding.etMotivo.text?.toString()?.trim().orEmpty()

        val temAnimais = binding.ddTemAnimais.text?.toString()?.trim().orEmpty()
        val sim = getString(R.string.option_yes)
        val nao = getString(R.string.option_no)

        val quaisAnimais = if (temAnimais == sim) {
            binding.etQuaisAnimais.text?.toString()?.trim().orEmpty()
        } else {
            ""
        }

        // ---------- Validação ----------
        var ok = true

        if (profissao.isBlank()) {
            binding.tilProfissao.error = getString(R.string.error_required_field)
            ok = false
        } else if (profissao.length > MAX_50) {
            binding.tilProfissao.error = getString(R.string.error_max_chars, MAX_50)
            ok = false
        }

        if (residencia.isBlank()) {
            binding.tilResidencia.error = getString(R.string.error_required_field)
            ok = false
        } else if (residencia.length > MAX_50) {
            binding.tilResidencia.error = getString(R.string.error_max_chars, MAX_50)
            ok = false
        }

        if (motivo.isBlank()) {
            binding.tilMotivo.error = getString(R.string.error_required_field)
            ok = false
        } else if (motivo.length > MAX_MOTIVO) {
            binding.tilMotivo.error = getString(R.string.error_max_chars, MAX_MOTIVO)
            ok = false
        }

        // backend exige "Sim" ou "Não"
        if (temAnimais != sim && temAnimais != nao) {
            binding.tilTemAnimais.error = getString(R.string.error_select_option)
            ok = false
        }

        if (temAnimais == sim) {
            if (quaisAnimais.isBlank()) {
                binding.tilQuaisAnimais.error = getString(R.string.error_required_field)
                ok = false
            } else if (quaisAnimais.length > MAX_50) {
                binding.tilQuaisAnimais.error = getString(R.string.error_max_chars, MAX_50)
                ok = false
            }
        }

        if (!ok) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_fix_form_fields),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        setLoading(true)

        val body = CriarIntencaoRequest(
            animalFK = animalId,
            profissao = profissao,
            residencia = residencia,
            motivo = motivo,
            temAnimais = temAnimais,
            quaisAnimais = if (temAnimais == sim) quaisAnimais else null
        )

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // ✅ Criar intenção (backend deve devolver 409 se já existir)
                api.criar(body)

                withContext(Dispatchers.Main) {
                    // ✅ Vai para "Minhas Intenções" e remove o formulário da backstack
                    // para o botão físico "Voltar" não regressar ao formulário.
                    val b = Bundle().apply {
                        putBoolean("showBanner", true)
                        // putInt("intencaoId", idCriado) // opcional, se o endpoint devolver id
                    }

                    findNavController().navigate(
                        R.id.listaIntencoesFragment,
                        b,
                        navOptions {
                            // ✅ Back deve ir, no máximo, para a lista de animais
                            popUpTo(R.id.listaAnimaisFragment) { inclusive = false }
                            launchSingleTop = true
                        }
                    )
                }

            } catch (e: HttpException) {
                val backendMsg = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                val msg = backendMsg?.takeIf { it.isNotBlank() }

                withContext(Dispatchers.Main) {
                    when (e.code()) {
                        401, 403 -> {
                            session.logout()
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.error_login_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            navegarParaLoginReturnToAdotar()
                        }

                        409 -> {
                            // ✅ Já existe intenção para este animal (1 intenção por utilizador/animal)
                            Toast.makeText(
                                requireContext(),
                                msg ?: getString(R.string.error_intent_already_exists),
                                Toast.LENGTH_LONG
                            ).show()

                            // Opcional: encaminhar o user para ver o estado
                            findNavController().navigate(
                                R.id.listaIntencoesFragment,
                                null,
                                navOptions {
                                    popUpTo(R.id.listaAnimaisFragment) { inclusive = false }
                                    launchSingleTop = true
                                }
                            )
                        }

                        else -> {
                            Toast.makeText(
                                requireContext(),
                                getString(
                                    R.string.error_submit_intent,
                                    msg ?: e.message() ?: getString(R.string.error_generic)
                                ),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
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
            } finally {
                withContext(Dispatchers.Main) { setLoading(false) }
            }
        }
    }

    private fun navegarParaLoginReturnToAdotar() {
        val b = Bundle().apply {
            putBoolean("returnToPrevious", true)
            putString("origin", "adotar")
            putInt("originId", animalId)
        }

        findNavController().navigate(
            R.id.loginFragment,
            b,
            navOptions {
                popUpTo(R.id.adotarFragment) { inclusive = true }
                launchSingleTop = true
            }
        )
    }

    private fun clearErrors() {
        binding.tilProfissao.error = null
        binding.tilResidencia.error = null
        binding.tilMotivo.error = null
        binding.tilTemAnimais.error = null
        binding.tilQuaisAnimais.error = null
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSubmeter.isEnabled = !loading
        binding.etProfissao.isEnabled = !loading
        binding.etResidencia.isEnabled = !loading
        binding.etMotivo.isEnabled = !loading
        binding.ddTemAnimais.isEnabled = !loading
        binding.etQuaisAnimais.isEnabled = !loading

        binding.btnSubmeter.text =
            if (loading) getString(R.string.loading) else getString(R.string.action_submit_intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
