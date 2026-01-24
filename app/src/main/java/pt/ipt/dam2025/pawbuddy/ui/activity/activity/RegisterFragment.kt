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
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import retrofit2.HttpException

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitProvider.authService
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

        // Se já está autenticado, não faz sentido ficar no registo
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

            val nome = binding.etNome.text?.toString()?.trim().orEmpty()
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            val dataNascimento = binding.etDataNascimento.text?.toString()?.trim().orEmpty()
            val nif = binding.etNif.text?.toString()?.trim().orEmpty()
            val telemovel = binding.etTelemovel.text?.toString()?.trim().orEmpty()
            val morada = binding.etMorada.text?.toString()?.trim().orEmpty()
            val codPostal = binding.etCodPostal.text?.toString()?.trim().orEmpty()
            val pais = binding.etPais.text?.toString()?.trim().orEmpty()

            var ok = true
            if (nome.isBlank()) { binding.tilNome.error = getString(R.string.error_required_field); ok = false }
            if (email.isBlank()) { binding.tilEmail.error = getString(R.string.error_required_field); ok = false }
            if (password.isBlank()) { binding.tilPassword.error = getString(R.string.error_required_field); ok = false }
            if (dataNascimento.isBlank()) { binding.tilDataNascimento.error = getString(R.string.error_required_field); ok = false }
            if (nif.isBlank()) { binding.tilNif.error = getString(R.string.error_required_field); ok = false }
            if (telemovel.isBlank()) { binding.tilTelemovel.error = getString(R.string.error_required_field); ok = false }
            if (morada.isBlank()) { binding.tilMorada.error = getString(R.string.error_required_field); ok = false }
            if (codPostal.isBlank()) { binding.tilCodPostal.error = getString(R.string.error_required_field); ok = false }
            if (pais.isBlank()) { binding.tilPais.error = getString(R.string.error_required_field); ok = false }
            if (!ok) return@setOnClickListener

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

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    api.register(request)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.register_success, email),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // Login automático
                    try {
                        val loginResp: LoginResponse =
                            api.login(LoginRequest(Email = email, Password = password))

                        withContext(Dispatchers.Main) {
                            session.saveLogin(loginResp.id, loginResp.isAdmin)
                            val target = if (loginResp.isAdmin) R.id.gestaoFragment else R.id.homeFragment

                            // ✅ remove Register da backstack
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
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.info_login_after_register),
                                Toast.LENGTH_LONG
                            ).show()

                            val b = Bundle().apply { putString("prefillEmail", email) }

                            // ✅ remove Register da backstack
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
                    withContext(Dispatchers.Main) {
                        when (e.code()) {
                            409 -> {
                                binding.tilEmail.error = getString(R.string.error_email_already_exists)
                                Toast.makeText(requireContext(), getString(R.string.error_email_already_exists), Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.register_error, e.message ?: getString(R.string.error_generic)),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.register_error, e.message ?: getString(R.string.error_generic)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) { setLoading(false) }
                }
            }
        }
    }

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

    private fun setLoading(loading: Boolean) {
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) getString(R.string.loading) else getString(R.string.action_register)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
