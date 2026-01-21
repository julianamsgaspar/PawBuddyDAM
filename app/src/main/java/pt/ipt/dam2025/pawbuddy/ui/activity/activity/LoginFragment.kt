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
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.session.SessionManager

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val retrofit = RetrofitInitializer()

    // ✅ SessionManager só quando o Fragment já tem context
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

        // ✅ Se já está logado, não faz sentido mostrar Login
        if (session.isLogged()) {
            if (!findNavController().navigateUp()) {
                findNavController().navigate(R.id.homeFragment)
            }
            return
        }

        // Redirect args
        val redirectToAdotar = arguments?.getBoolean("redirectToAdotar", false) ?: false
        val redirectAnimalId = arguments?.getInt("redirectAnimalId", -1) ?: -1

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
                    val resposta: LoginResponse = retrofit.authService().login(req)

                    withContext(Dispatchers.Main) {
                        // ✅ Guardar sessão centralizada
                        session.saveLogin(resposta.id, resposta.isAdmin)

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.success_login, resposta.email),
                            Toast.LENGTH_LONG
                        ).show()

                        if (redirectToAdotar && redirectAnimalId > 0) {
                            val bundle = Bundle().apply { putInt("animalId", redirectAnimalId) }

                            findNavController().navigate(
                                R.id.adotarFragment,
                                bundle,
                                navOptions {
                                    popUpTo(R.id.loginFragment) { inclusive = true }
                                    launchSingleTop = true
                                }
                            )
                        } else {
                            findNavController().navigate(
                                R.id.homeFragment,
                                null,
                                navOptions {
                                    popUpTo(R.id.loginFragment) { inclusive = true }
                                    launchSingleTop = true
                                }
                            )
                        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
