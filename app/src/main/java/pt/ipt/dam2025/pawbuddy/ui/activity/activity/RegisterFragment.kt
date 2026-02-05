package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentRegisterBinding
import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.model.LoginResponse
import pt.ipt.dam2025.pawbuddy.model.RegisterRequest
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import retrofit2.HttpException
import java.util.Calendar

/**
 * Fragment responsável pelo registo de novos utilizadores.
 *
 * Responsabilidades principais:
 * - Recolha e validação *client-side* de dados de registo.
 * - Submissão do registo ao backend (Retrofit).
 * - Tentativa de login automático após registo bem sucedido.
 * - Navegação controlada via Navigation Component (gestão de backstack).
 *
 * Nota: as validações no cliente melhoram UX e reduzem chamadas inválidas,
 * mas não substituem validações no servidor.
 */
class RegisterFragment : Fragment() {

    /** Binding válido apenas entre onCreateView e onDestroyView (evita memory leaks). */
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    /** Serviço de autenticação (registo/login) providenciado pelo RetrofitProvider. */
    private val api = RetrofitProvider.authService

    /** Gestão de sessão local (estado de autenticação + perfil admin). */
    private val session by lazy { SessionManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Se já autenticado, redireciona diretamente para o destino apropriado.
        if (session.isLogged()) {
            val target = if (session.isAdmin()) R.id.gestaoFragment else R.id.homeFragment
            findNavController().navigate(
                target,
                null,
                navOptions {
                    popUpTo(R.id.registerFragment) { inclusive = true }
                    launchSingleTop = true
                }
            )
            return
        }

        // Configuração de restrições/UX: filtros, calendário e validação em tempo real.
        setupFieldConstraints()
        setupDatePicker()
        setupRealtimeValidation()

        binding.btnRegister.setOnClickListener {
            clearErrors()

            // Leitura do formulário (trim para reduzir erros por espaços acidentais).
            val nome = binding.etNome.text?.toString()?.trim().orEmpty()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()
            val dataNascimento = binding.etDataNascimento.text?.toString()?.trim().orEmpty()
            val nif = binding.etNif.text?.toString()?.trim().orEmpty()
            val telemovel = binding.etTelemovel.text?.toString()?.trim().orEmpty()
            val morada = binding.etMorada.text?.toString()?.trim().orEmpty()
            val codPostal = binding.etCodPostal.text?.toString()?.trim().orEmpty()
            val pais = binding.etPais.text?.toString()?.trim().orEmpty()

            // Validação final (antes de rede). Mantém a UI consistente com os TextInputLayouts.
            var ok = true
            if (nome.isBlank()) { binding.tilNome.error = getString(R.string.error_required_field); ok = false }
            if (email.isBlank()) { binding.tilEmail.error = getString(R.string.error_required_field); ok = false }
            if (password.isBlank()) { binding.tilPassword.error = getString(R.string.error_required_field); ok = false }
            if (confirmPassword.isBlank()) { binding.tilConfirmPassword.error = getString(R.string.error_required_field); ok = false }
            if (dataNascimento.isBlank()) { binding.tilDataNascimento.error = getString(R.string.error_required_field); ok = false }
            if (nif.isBlank()) { binding.tilNif.error = getString(R.string.error_required_field); ok = false }
            if (telemovel.isBlank()) { binding.tilTelemovel.error = getString(R.string.error_required_field); ok = false }
            if (morada.isBlank()) { binding.tilMorada.error = getString(R.string.error_required_field); ok = false }
            if (codPostal.isBlank()) { binding.tilCodPostal.error = getString(R.string.error_required_field); ok = false }
            if (pais.isBlank()) { binding.tilPais.error = getString(R.string.error_required_field); ok = false }

            // Regras específicas (formato/dimensões).
            if (ok && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = getString(R.string.error_invalid_email); ok = false
            }
            if (ok && password != confirmPassword) {
                binding.tilConfirmPassword.error = getString(R.string.error_passwords_do_not_match); ok = false
            }
            if (ok && nif.length != 9) {
                binding.tilNif.error = getString(R.string.error_nif_9_digits); ok = false
            }
            if (ok && telemovel.length != 9) {
                binding.tilTelemovel.error = getString(R.string.error_phone_9_digits); ok = false
            }
            val postalOk = Regex("^\\d{4}-\\d{3}$").matches(codPostal)
            if (ok && !postalOk) {
                binding.tilCodPostal.error = getString(R.string.error_postal_format); ok = false
            }

            if (!ok) return@setOnClickListener

            // UI: bloqueia submissões repetidas enquanto decorre a operação de rede.
            setLoading(true)

            val request = RegisterRequest(
                nome = nome,
                email = email,
                password = password,
                dataNascimento = dataNascimento,
                nif = nif,
                telemovel = telemovel,
                morada = morada,
                codPostal = codPostal,
                pais = pais
            )

            // Coroutines: IO para rede; Main para atualização da UI.
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 1) Registo
                    api.register(request)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.register_success, email),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // 2) Login automático (melhoria de UX)
                    try {
                        val loginResp: LoginResponse =
                            api.login(LoginRequest(Email = email, Password = password))

                        withContext(Dispatchers.Main) {
                            session.saveLogin(
                                userId = loginResp.id,
                                userName = loginResp.user,
                                isAdmin = loginResp.isAdmin
                            )

                            val target = if (loginResp.isAdmin) R.id.gestaoFragment else R.id.homeFragment
                            findNavController().navigate(
                                target,
                                null,
                                navOptions {
                                    popUpTo(R.id.registerFragment) { inclusive = true }
                                    launchSingleTop = true
                                }
                            )
                        }

                    } catch (_: Exception) {
                        // Fallback: navega para login com email pré-preenchido.
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.info_login_after_register),
                                Toast.LENGTH_LONG
                            ).show()

                            val b = Bundle().apply { putString("prefillEmail", email) }

                            findNavController().navigate(
                                R.id.loginFragment,
                                b,
                                navOptions {
                                    popUpTo(R.id.registerFragment) { inclusive = true }
                                    launchSingleTop = true
                                }
                            )
                        }
                    }

                } catch (e: HttpException) {
                    // Erros HTTP (ex.: 409 conflito de email já registado).
                    withContext(Dispatchers.Main) {
                        when (e.code()) {
                            409 -> {
                                binding.tilEmail.error = getString(R.string.error_email_already_exists)
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_email_already_exists),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                Toast.makeText(
                                    requireContext(),
                                    getString(
                                        R.string.register_error,
                                        e.message ?: getString(R.string.error_generic)
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Erros genéricos (rede/timeout/parsing).
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.register_error,
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
    }

    /**
     * Define restrições de entrada (InputFilters):
     * - limites de tamanho (nome/morada/país)
     * - apenas dígitos para NIF/telemóvel
     * - charset permitido e tamanho fixo para código postal (NNNN-NNN)
     */
    private fun setupFieldConstraints() {
        binding.etNome.filters = arrayOf(InputFilter.LengthFilter(80))
        binding.etMorada.filters = arrayOf(InputFilter.LengthFilter(120))
        binding.etPais.filters = arrayOf(InputFilter.LengthFilter(60))

        binding.etNif.filters = arrayOf(InputFilter.LengthFilter(9), DigitsOnlyFilter())
        binding.etTelemovel.filters = arrayOf(InputFilter.LengthFilter(9), DigitsOnlyFilter())

        binding.etCodPostal.filters = arrayOf(InputFilter.LengthFilter(8), PostalCodeCharsFilter())
    }

    /**
     * Associa um DatePickerDialog ao campo de data de nascimento.
     * Também impede seleção de datas futuras (integridade semântica básica).
     */
    private fun setupDatePicker() {
        binding.etDataNascimento.setOnClickListener {
            val cal = Calendar.getInstance()

            // Tenta reaproveitar valor existente (formato dd/MM/yyyy).
            val current = binding.etDataNascimento.text?.toString().orEmpty()
            val parts = current.split("/")
            if (parts.size == 3) {
                val d = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                val y = parts[2].toIntOrNull()
                if (d != null && m != null && y != null) {
                    cal.set(Calendar.DAY_OF_MONTH, d)
                    cal.set(Calendar.MONTH, m - 1)
                    cal.set(Calendar.YEAR, y)
                }
            }

            val dialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val dd = dayOfMonth.toString().padStart(2, '0')
                    val mm = (month + 1).toString().padStart(2, '0')
                    binding.etDataNascimento.setText("$dd/$mm/$year")
                    binding.tilDataNascimento.error = null
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )

            dialog.datePicker.maxDate = System.currentTimeMillis()
            dialog.show()
        }
    }

    /**
     * Validação incremental: oferece feedback imediato ao utilizador enquanto escreve,
     * reduzindo tentativas inválidas e melhorando a usabilidade.
     */
    private fun setupRealtimeValidation() {
        binding.etEmail.doAfterTextChanged {
            val email = it?.toString()?.trim().orEmpty()
            binding.tilEmail.error =
                if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches())
                    getString(R.string.error_invalid_email)
                else null
        }

        binding.etNif.doAfterTextChanged {
            val v = it?.toString().orEmpty()
            binding.tilNif.error =
                if (v.isNotBlank() && v.length != 9) getString(R.string.error_nif_9_digits) else null
        }

        binding.etTelemovel.doAfterTextChanged {
            val v = it?.toString().orEmpty()
            binding.tilTelemovel.error =
                if (v.isNotBlank() && v.length != 9) getString(R.string.error_phone_9_digits) else null
        }

        // Insere hífen após 4 dígitos e mantém formato NNNN-NNN.
        binding.etCodPostal.addTextChangedListener(PostalCodeHyphenWatcher(binding.etCodPostal))

        // Mantém o erro de confirmação atualizado.
        val passWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val p1 = binding.etPassword.text?.toString().orEmpty()
                val p2 = binding.etConfirmPassword.text?.toString().orEmpty()
                binding.tilConfirmPassword.error =
                    if (p2.isNotBlank() && p1 != p2) getString(R.string.error_passwords_do_not_match) else null
            }
        }
        binding.etPassword.addTextChangedListener(passWatcher)
        binding.etConfirmPassword.addTextChangedListener(passWatcher)
    }

    /** Remove erros anteriores para evitar “estado sujo” entre tentativas. */
    private fun clearErrors() {
        binding.tilNome.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tilDataNascimento.error = null
        binding.tilNif.error = null
        binding.tilTelemovel.error = null
        binding.tilMorada.error = null
        binding.tilCodPostal.error = null
        binding.tilPais.error = null
    }

    /** Controla estado de loading: bloqueia botão e ajusta label. */
    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text =
            if (loading) getString(R.string.loading) else getString(R.string.action_register)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // -----------------------
    // Helpers (InputFilters)
    // -----------------------

    /**
     * Filtro que rejeita qualquer carácter não numérico.
     * Útil para campos que devem conter apenas dígitos (NIF/telemóvel).
     */
    private class DigitsOnlyFilter : InputFilter {
        override fun filter(
            source: CharSequence, start: Int, end: Int,
            dest: android.text.Spanned, dstart: Int, dend: Int
        ): CharSequence? {
            for (i in start until end) {
                if (!source[i].isDigit()) return ""
            }
            return null
        }
    }

    /**
     * Permite apenas dígitos e '-' no código postal, prevenindo caracteres inválidos.
     */
    private class PostalCodeCharsFilter : InputFilter {
        override fun filter(
            source: CharSequence, start: Int, end: Int,
            dest: android.text.Spanned, dstart: Int, dend: Int
        ): CharSequence? {
            for (i in start until end) {
                val c = source[i]
                if (!(c.isDigit() || c == '-')) return ""
            }
            return null
        }
    }

    /**
     * TextWatcher que aplica formatação incremental do código postal:
     * - remove caracteres não numéricos
     * - limita a 7 dígitos (4+3)
     * - insere hífen após os primeiros 4
     */
    private class PostalCodeHyphenWatcher(
        private val editText: EditText
    ) : TextWatcher {

        private var editing = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (editing) return
            editing = true

            val raw = s?.toString().orEmpty()
            val digits = raw.filter { it.isDigit() }.take(7)

            val formatted = when {
                digits.length <= 4 -> digits
                else -> digits.substring(0, 4) + "-" + digits.substring(4)
            }

            if (formatted != raw) {
                editText.setText(formatted)
                editText.setSelection(formatted.length)
            }

            editing = false
        }
    }
}
