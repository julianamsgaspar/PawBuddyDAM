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

        // (Opcional) prefill vindo do Register
        arguments?.getString("prefillEmail")?.let { pre ->
            if (pre.isNotBlank()) binding.etEmail.setText(pre)
        }

        // Se já está logado, não faz sentido ficar aqui
        if (session.isLogged()) {
            navigateAfterLogin(
                isAdmin = session.isAdmin(),
                returnToPrevious = returnToPrevious,
                origin = origin,
                originId = originId
            )
            return
        }

        binding.etEmail.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.tilEmail.error = null }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.tilPassword.error = null }

        binding.btnLogin.setOnClickListener {
            clearErrors()

            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()

            var ok = true
            if (email.isBlank()) { binding.tilEmail.error = getString(R.string.error_email_required); ok = false }
            if (password.isBlank()) { binding.tilPassword.error = getString(R.string.error_password_required); ok = false }
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

                        navigateAfterLogin(
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
                                binding.tilPassword.error = getString(R.string.error_login_invalid)
                                Toast.makeText(requireContext(), msg ?: getString(R.string.error_login_invalid), Toast.LENGTH_LONG).show()
                            }
                            401, 403 -> {
                                binding.tilPassword.error = msg ?: getString(R.string.error_unauthorized)
                                Toast.makeText(requireContext(), msg ?: getString(R.string.error_unauthorized), Toast.LENGTH_LONG).show()
                                session.logout()
                            }
                            else -> {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.error_login_failed, msg ?: e.message() ?: getString(R.string.error_generic)),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.tilPassword.error = getString(R.string.error_login_invalid)
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_login_failed, e.message ?: getString(R.string.error_generic)),
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

    private fun navigateAfterLogin(
        isAdmin: Boolean,
        returnToPrevious: Boolean,
        origin: String,
        originId: Int
    ) {
        // Caso especial: voltar para o formulário de adoção (user normal)
        if (returnToPrevious && origin == "adotar" && originId > 0) {
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

        // Default: ir para destino correto e remover Login da backstack
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

    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnRegister.isEnabled = !loading
        binding.btnLogin.text = if (loading) getString(R.string.loading) else getString(R.string.action_sign_in)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
