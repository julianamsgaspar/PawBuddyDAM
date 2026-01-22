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

        // Args unificados
        val returnToPrevious = arguments?.getBoolean("returnToPrevious", false) ?: false
        val origin = arguments?.getString("origin", "") ?: ""
        val originId = arguments?.getInt("originId", -1) ?: -1

        // Se já está logado, aplica já a regra unificada
        if (session.isLogged()) {
            handlePostLoginNavigation(
                isAdmin = session.isAdmin(),
                returnToPrevious = returnToPrevious,
                origin = origin,
                originId = originId
            )
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_fill_email_password),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

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
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.error_login_failed,
                                e.message ?: getString(R.string.error_generic)
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    private fun handlePostLoginNavigation(
        isAdmin: Boolean,
        returnToPrevious: Boolean,
        origin: String,
        originId: Int
    ) {
        // Caso 1: queremos voltar ao contexto anterior
        if (returnToPrevious) {

            // Origem: fluxo de adoção (regra especial por perfil)
            if (origin == "adotar" && originId > 0) {
                if (isAdmin) {
                    // Admin nunca volta ao formulário de intenção
                    findNavController().navigate(
                        R.id.gestaoFragment,
                        null,
                        navOptions {
                            popUpTo(R.id.loginFragment) { inclusive = true }
                            launchSingleTop = true
                        }
                    )
                } else {
                    // Utilizador normal retoma a adoção
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

            // Outras origens: volta ao ecrã anterior
            if (!findNavController().navigateUp()) {
                // fallback se não houver back stack
                val target = if (isAdmin) R.id.gestaoFragment else R.id.homeFragment
                findNavController().navigate(target)
            }
            return
        }

        // Caso 2: login “normal”
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
