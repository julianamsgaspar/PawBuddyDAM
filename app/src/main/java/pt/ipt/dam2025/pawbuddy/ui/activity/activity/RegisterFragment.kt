package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

/**
 * Fragment responsável pelo registo de novos utilizadores na aplicação PawBuddy.
 *
 * Objetivos funcionais:
 * - Recolher dados de registo (identificação, contacto e morada).
 * - Validar, no cliente, a existência de campos obrigatórios antes de invocar o backend.
 * - Submeter o pedido de registo ao serviço de autenticação (authService).
 * - Após registo bem sucedido, tentar efetuar login automático:
 *   - Se o login automático funcionar: guardar sessão e navegar para o destino principal (Home/Gestão).
 *   - Se falhar: informar o utilizador e redirecionar para o Login com email pré-preenchido.
 *
 * Componentes e tecnologias:
 * - View Binding para acesso seguro aos componentes do layout.
 * - Retrofit (authService) para chamadas de registo e login.
 * - Coroutines (Dispatchers.IO/Main) para rede e atualização da UI.
 * - Navigation Component (NavController + navOptions) para controlo de backstack e destinos.
 * - SessionManager para persistência local do estado de autenticação.
 *
 * Considerações académicas:
 * - A validação no cliente reduz chamadas inválidas ao servidor, mas não substitui validações
 *   no backend (integridade e segurança).
 * - A estratégia popUpTo(inclusive=true) remove o ecrã de registo da backstack para impedir que
 *   o utilizador “volte” a um registo já concluído.
 * - O tratamento de erro HTTP 409 ilustra gestão de conflitos (email já existente).
 */
class RegisterFragment : Fragment() {

    /**
     * Binding nullable, válido apenas entre onCreateView e onDestroyView.
     */
    private var _binding: FragmentRegisterBinding? = null

    /**
     * Getter não-null do binding; assume-se acesso apenas enquanto a View existir.
     */
    private val binding get() = _binding!!

    /**
     * Serviço de autenticação (registo/login) disponibilizado por RetrofitProvider.
     */
    private val api = RetrofitProvider.authService

    /**
     * Gestor de sessão local. É usado para:
     * - verificar se o utilizador já está logado
     * - guardar sessão após login automático
     */
    private val session by lazy { SessionManager(requireContext()) }

    /**
     * Infla o layout e inicializa o View Binding.
     *
     * @return View raiz do Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura o fluxo de registo após a View estar criada.
     *
     * Fluxo:
     * 1) Se já autenticado:
     *    - redireciona para destino adequado (admin->gestão, user->home)
     *    - remove o Register da backstack
     * 2) Caso contrário:
     *    - configura listener do botão de registo
     *    - valida campos obrigatórios
     *    - envia pedido de registo para o backend
     *    - tenta login automático e navega conforme resultado
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Se já está autenticado, não faz sentido permitir novo registo nesta sessão.
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

        binding.btnRegister.setOnClickListener {
            clearErrors()

            // Recolha de campos do formulário (trim para evitar espaços acidentais).
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

            // Validação de campos obrigatórios (client-side).
            // Nota: todas as mensagens usam um texto genérico de “campo obrigatório”.
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
            if (ok && password != confirmPassword) {
                binding.tilConfirmPassword.error = getString(R.string.error_passwords_do_not_match)
                ok = false
            }
            if (!ok) return@setOnClickListener

            // UI: estado de loading (desativa botão e altera texto).
            setLoading(true)

            // Construção do request esperado pelo backend.
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

            // Execução assíncrona do registo.
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // 1) Registo no backend.
                    api.register(request)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.register_success, email),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // 2) Login automático (melhora UX reduzindo fricção pós-registo).
                    try {
                        val loginResp: LoginResponse =
                            api.login(LoginRequest(Email = email, Password = password))

                        withContext(Dispatchers.Main) {
                            // Persistência de sessão e navegação para destino principal.
                            session.saveLogin(
                                userId = loginResp.id,
                                userName = loginResp.user,
                                isAdmin = loginResp.isAdmin
                            )

                            val target =
                                if (loginResp.isAdmin) R.id.gestaoFragment else R.id.homeFragment

                            // Remove Register da backstack.
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
                        // Se o login automático falhar, redireciona para Login com email pré-preenchido.
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.info_login_after_register),
                                Toast.LENGTH_LONG
                            ).show()

                            val b = Bundle().apply { putString("prefillEmail", email) }

                            // Remove Register da backstack e abre Login.
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
                    // Tratamento de erros HTTP específicos (ex.: conflito de email).
                    withContext(Dispatchers.Main) {
                        when (e.code()) {
                            409 -> {
                                // Conflito: email já existe.
                                binding.tilEmail.error =
                                    getString(R.string.error_email_already_exists)
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_email_already_exists),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                // Erro genérico de registo.
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
                    // Erros genéricos (rede, timeout, parsing, etc.).
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
                    // Reposição do estado de loading.
                    withContext(Dispatchers.Main) { setLoading(false) }
                }
            }
        }
    }

    /**
     * Limpa mensagens de erro nos TextInputLayouts do formulário.
     * Chamado antes de uma nova tentativa de submissão.
     */
    private fun clearErrors() {
        binding.tilNome.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilDataNascimento.error = null
        binding.tilNif.error = null
        binding.tilTelemovel.error = null
        binding.tilMorada.error = null
        binding.tilCodPostal.error = null
        binding.tilPais.error = null
    }

    /**
     * Controla o estado de loading do botão de registo.
     *
     * Efeitos:
     * - Desativa o botão para impedir múltiplas submissões simultâneas.
     * - Ajusta o texto do botão para refletir o estado.
     *
     * @param loading true ativa loading; false reverte para estado normal.
     */
    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text =
            if (loading) getString(R.string.loading) else getString(R.string.action_register)
    }

    /**
     * Libertação do binding para evitar memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
