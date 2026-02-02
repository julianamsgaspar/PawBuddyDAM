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
import pt.ipt.dam2025.pawbuddy.databinding.FragmentLoginBinding
import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.model.LoginResponse
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import retrofit2.HttpException

/**
 * Fragment responsável pelo processo de autenticação (Login) na aplicação PawBuddy.
 *
 * Objetivos funcionais:
 * - Permitir que um utilizador introduza credenciais (email e password) e autentique via backend.
 * - Persistir a sessão localmente (SessionManager) após login bem sucedido.
 * - Implementar lógica de navegação pós-login:
 *   - (i) fluxo padrão: Admin -> Gestão; Utilizador -> Home
 *   - (ii) fluxo especial: retornar ao ecrã de adoção (adotarFragment) caso o login tenha sido
 *        requerido para concluir a intenção e exista contexto de retorno.
 *
 * Componentes e tecnologias:
 * - View Binding para acesso seguro aos elementos da UI.
 * - Retrofit (authService) para invocar endpoint de login.
 * - Coroutines com Dispatchers.IO/Main para rede e UI.
 * - Navigation Component (NavController + navOptions) para controlar destino e backstack.
 * - SessionManager para guardar e limpar informações de sessão.
 *
 * Considerações académicas:
 * - Validação de entrada é feita no cliente para reduzir chamadas inválidas ao backend.
 * - Tratamento de erros HTTP distingue códigos (400 vs 401/403 vs outros) para feedback adequado.
 * - Uso de popUpTo(inclusive=true) remove o Login da backstack, evitando retorno acidental
 *   ao ecrã de autenticação após login bem sucedido.
 */
class LoginFragment : Fragment() {

    /**
     * Binding nullable válido entre onCreateView e onDestroyView.
     * Deve ser libertado para evitar memory leaks.
     */
    private var _binding: FragmentLoginBinding? = null

    /**
     * Getter não-null do binding. Assume-se acesso apenas enquanto a View existir.
     */
    private val binding get() = _binding!!

    /**
     * Serviço Retrofit para autenticação.
     */
    private val authApi = RetrofitProvider.authService

    /**
     * Gestor de sessão instanciado lazy para garantir Context válido.
     * Responsável por:
     * - verificar se o utilizador está autenticado
     * - guardar sessão após login
     * - limpar sessão (logout)
     */
    private val session by lazy { SessionManager(requireContext()) }

    /**
     * Infla o layout do Fragment e inicializa o View Binding.
     *
     * @return View raiz do Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura a UI e o fluxo de autenticação após a View estar criada.
     *
     * Fluxo global:
     * 1) Lê argumentos para suporte de retorno ao ecrã anterior (returnToPrevious/origin/originId).
     * 2) (Opcional) preenche email vindo do registo.
     * 3) Se já existe sessão ativa, executa navegação pós-login sem repetir autenticação.
     * 4) Configura listeners:
     *    - limpeza de erros ao focar campos
     *    - botão login: validação local + chamada ao backend
     *    - botão registo: navegação para registerFragment
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Argumentos usados para suportar "retorno ao contexto" após login obrigatório.
        val returnToPrevious = arguments?.getBoolean("returnToPrevious", false) ?: false
        val origin = arguments?.getString("origin", "") ?: ""
        val originId = arguments?.getInt("originId", -1) ?: -1

        // (Opcional) prefill vindo do Register: melhora UX evitando reintrodução do email.
        arguments?.getString("prefillEmail")?.let { pre ->
            if (pre.isNotBlank()) binding.etEmail.setText(pre)
        }

        // Se já existe sessão ativa, não faz sentido permanecer no ecrã de login.
        if (session.isLogged()) {
            navigateAfterLogin(
                isAdmin = session.isAdmin(),
                returnToPrevious = returnToPrevious,
                origin = origin,
                originId = originId
            )
            return
        }

        // Limpeza de mensagens de erro quando o utilizador volta a focar os campos.
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilEmail.error = null
        }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilPassword.error = null
        }

        // Botão Login: valida credenciais e invoca endpoint remoto.
        binding.btnLogin.setOnClickListener {
            clearErrors()

            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()

            // Validação local: evita requests desnecessários e fornece feedback imediato.
            var ok = true
            if (email.isBlank()) {
                binding.tilEmail.error = getString(R.string.error_email_required)
                ok = false
            }
            if (password.isBlank()) {
                binding.tilPassword.error = getString(R.string.error_password_required)
                ok = false
            }
            if (!ok) return@setOnClickListener

            // UI: entra em modo loading (desativa botões e muda texto do botão login).
            setLoading(true)

            // Execução assíncrona do login em background (I/O).
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Construção do payload esperado pelo backend.
                    val req = LoginRequest(Email = email, Password = password)

                    // Chamada Retrofit: em caso de sucesso devolve LoginResponse.
                    val resposta: LoginResponse = authApi.login(req)

                    withContext(Dispatchers.Main) {

                        // Persistência de sessão local (agora inclui nome).
                        session.saveLogin(
                            userId = resposta.id,
                            userName = resposta.user,
                            isAdmin = resposta.isAdmin
                        )

                        // Feedback ao utilizador (UX).
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.success_login, resposta.email),
                            Toast.LENGTH_LONG
                        ).show()

                        // Navegação pós-login segundo papel e contexto de retorno.
                        navigateAfterLogin(
                            isAdmin = resposta.isAdmin,
                            returnToPrevious = returnToPrevious,
                            origin = origin,
                            originId = originId
                        )
                    }


                } catch (e: HttpException) {
                    // Tentativa de obter uma mensagem do backend (quando devolvida no errorBody).
                    val backendMsg = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                    val msg = backendMsg?.takeIf { it.isNotBlank() }

                    withContext(Dispatchers.Main) {
                        // Tratamento diferenciado por código HTTP, permitindo feedback mais preciso.
                        when (e.code()) {
                            400 -> {
                                // Tipicamente: credenciais inválidas ou request inválido.
                                binding.tilPassword.error = getString(R.string.error_login_invalid)
                                Toast.makeText(
                                    requireContext(),
                                    msg ?: getString(R.string.error_login_invalid),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            401, 403 -> {
                                // Não autenticado / sem permissão: reforça sessão limpa.
                                binding.tilPassword.error = msg ?: getString(R.string.error_unauthorized)
                                Toast.makeText(
                                    requireContext(),
                                    msg ?: getString(R.string.error_unauthorized),
                                    Toast.LENGTH_LONG
                                ).show()
                                session.logout()
                            }
                            else -> {
                                // Outros códigos: erro não esperado; mostra mensagem genérica contextualizada.
                                Toast.makeText(
                                    requireContext(),
                                    getString(
                                        R.string.error_login_failed,
                                        msg ?: e.message() ?: getString(R.string.error_generic)
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                } catch (e: Exception) {
                    // Erros genéricos (ex.: rede, parsing, timeout).
                    withContext(Dispatchers.Main) {
                        binding.tilPassword.error = getString(R.string.error_login_invalid)
                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.error_login_failed,
                                e.message ?: getString(R.string.error_generic)
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } finally {
                    // Garante reposição do estado de loading, independentemente do resultado.
                    withContext(Dispatchers.Main) { setLoading(false) }
                }
            }
        }

        // Botão Registar: navega para o ecrã de registo.
        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    /**
     * Determina e executa a navegação após login (ou quando já existe sessão).
     *
     * Caso especial:
     * - Se o login foi exigido para concluir um fluxo (origin == "adotar") e originId é válido:
     *   - Admin: redireciona para Gestão (não faz sentido "adotar" como admin).
     *   - Utilizador: retorna ao formulário de adoção (adotarFragment) para o animal originId.
     *
     * Caso default:
     * - Admin -> GestaoFragment
     * - Utilizador -> HomeFragment
     *
     * Gestão de backstack:
     * - popUpTo(loginFragment) inclusive = true remove o Login do histórico de navegação.
     * - launchSingleTop evita criar múltiplas instâncias do mesmo destino.
     *
     * @param isAdmin perfil do utilizador.
     * @param returnToPrevious indica se deve tentar regressar ao contexto anterior.
     * @param origin identificador lógico do ecrã/origem (ex.: "adotar").
     * @param originId id associado à origem (ex.: animalId).
     */
    private fun navigateAfterLogin(
        isAdmin: Boolean,
        returnToPrevious: Boolean,
        origin: String,
        originId: Int
    ) {
        // Caso especial: voltar para o formulário de adoção (apenas utilizador normal).
        if (returnToPrevious && origin == "adotar" && originId > 0) {
            if (isAdmin) {
                // Admin é encaminhado para gestão.
                findNavController().navigate(
                    R.id.gestaoFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.loginFragment) { inclusive = true }
                        launchSingleTop = true
                    }
                )
            } else {
                // Utilizador retorna ao fluxo de adoção com o animalId original.
                val b = Bundle().apply { putInt("animalId", originId) }
                findNavController().navigate(
                    R.id.adotarFragment,
                    b,
                    navOptions {
                        popUpTo(R.id.loginFragment) { inclusive = true }
                        launchSingleTop = true
                    }
                )
            }
            return
        }

        // Caso default: encaminhar para destino principal e remover Login da backstack.
        val target = if (isAdmin) R.id.gestaoFragment else R.id.homeFragment
        findNavController().navigate(
            target,
            null,
            navOptions {
                popUpTo(R.id.loginFragment) { inclusive = true }
                launchSingleTop = true
            }
        )
    }

    /**
     * Remove erros visíveis nos TextInputLayouts.
     * Chamado antes de uma nova tentativa de login.
     */
    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }

    /**
     * Controla o estado de loading da UI.
     *
     * Efeitos:
     * - Desativa botões para prevenir múltiplos pedidos simultâneos.
     * - Altera o texto do botão Login para informar o utilizador.
     *
     * @param loading true para ativar modo loading; false para reverter.
     */
    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnRegister.isEnabled = !loading
        binding.btnLogin.text = if (loading) getString(R.string.loading) else getString(R.string.action_sign_in)
    }

    /**
     * Libertação do binding para evitar memory leaks quando a View é destruída.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
