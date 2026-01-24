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
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import retrofit2.HttpException

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authApi = RetrofitProvider.authService
    private val session by lazy { SessionManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val returnToPrevious = arguments?.getBoolean("returnToPrevious", false) ?: false
        val origin = arguments?.getString("origin", "") ?: ""
        val originId = arguments?.getInt("originId", -1) ?: -1

        // (Opcional/polido) Pre-fill do email vindo do Register
        arguments?.getString("prefillEmail")?.let { pre ->
            if (pre.isNotBlank()) binding.etEmail.setText(pre)
        }

        // Se já está logado, aplica regra
        if (session.isLogged()) {
            handlePostLoginNavigation(
                isAdmin = session.isAdmin(),
                returnToPrevious = returnToPrevious,
                origin = origin,
                originId = originId
            )
            return
        }

        // Limpa erros ao focar
        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilEmail.error = null
        }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.tilPassword.error = null
        }

        binding.btnLogin.setOnClickListener {
            clearErrors()

            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()

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

            setLoading(true)

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val req = LoginRequest(Email = email, Password = password)
                    val resposta: LoginResponse = authApi.login(req)

                    withContext(Dispatchers.Main) {
                        session.saveLogin(resposta.id, resposta.isAdmin)

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.success_login, resposta.email),
                            Toast.LENGTH_LONG
                        ).show()

                        handlePostLoginNavigation(
                            isAdmin = resposta.isAdmin,
                            returnToPrevious = returnToPrevious,
                            origin = origin,
                            originId = originId
                        )
                    }

                } catch (e: HttpException) {
                    val backendMsg = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                    val msg = backendMsg?.takeIf { it.isNotBlank() }

                    withContext(Dispatchers.Main) {
                        when (e.code()) {
                            400 -> {
                                // credenciais inválidas (o teu backend usa muito 400 para falha de login)
                                binding.tilPassword.error = getString(R.string.error_login_invalid)
                                Toast.makeText(
                                    requireContext(),
                                    msg ?: getString(R.string.error_login_invalid),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            401, 403 -> {
                                // aqui entra “Conta desativada” ou “sem autorização”
                                binding.tilPassword.error = msg ?: getString(R.string.error_unauthorized)
                                Toast.makeText(
                                    requireContext(),
                                    msg ?: getString(R.string.error_unauthorized),
                                    Toast.LENGTH_LONG
                                ).show()

                                // Se quiseres, podes forçar logout defensivo:
                                session.logout()
                            }
                            else -> {
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
                    withContext(Dispatchers.Main) {
                        // fallback genérico
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
                    withContext(Dispatchers.Main) { setLoading(false) }
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnRegister.isEnabled = !loading
        binding.btnLogin.text = if (loading) getString(R.string.loading) else getString(R.string.action_sign_in)
    }

    private fun handlePostLoginNavigation(
        isAdmin: Boolean,
        returnToPrevious: Boolean,
        origin: String,
        originId: Int
    ) {
        if (returnToPrevious) {

            if (origin == "adotar" && originId > 0) {
                if (isAdmin) {
                    findNavController().navigate(
                        R.id.gestaoFragment,
                        null,
                        navOptions {
                            popUpTo(R.id.loginFragment) { inclusive = true }
                            launchSingleTop = true
                        }
                    )
                } else {
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

            if (!findNavController().navigateUp()) {
                val target = if (isAdmin) R.id.gestaoFragment else R.id.homeFragment
                findNavController().navigate(target)
            }
            return
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
