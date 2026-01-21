package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentRegisterBinding
import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.model.LoginResponse
import pt.ipt.dam2025.pawbuddy.model.RegisterRequest
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitInitializer().authService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnRegister.setOnClickListener {

            val nome = binding.etNome.text.toString()
            val password = binding.etPassword.text.toString()
            val dataNascimento = binding.etDataNascimento.text.toString()
            val nif = binding.etNif.text.toString()
            val telemovel = binding.etTelemovel.text.toString()
            val morada = binding.etMorada.text.toString()
            val codPostal = binding.etCodPostal.text.toString()
            val email = binding.etEmail.text.toString()
            val pais = binding.etPais.text.toString()

            if (nome.isEmpty() || email.isEmpty() || password.isEmpty() || dataNascimento.isEmpty() ||
                nif.isEmpty() || telemovel.isEmpty() || morada.isEmpty() || codPostal.isEmpty() || pais.isEmpty()
            ) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_fill_all_fields),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

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
                    val response: LoginResponse = api.register(request)

                    // Se queres login automático após registo:
                    val log = LoginRequest(
                        Email = email,      // CORRIGIDO (antes estava Nome)
                        Password = password
                    )
                    api.login(log)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.register_success, email),
                            Toast.LENGTH_LONG
                        ).show()

                        val shared = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
                        shared.edit().apply {
                            putBoolean("isLogged", true)
                            putInt("utilizadorId", response.id)
                            putBoolean("isAdmin", email.equals("admin@pawbuddy.com", true))
                            apply()
                        }

                        // Navega para Home
                        findNavController().navigate(R.id.homeFragment)
                    }
                } catch (e: Exception) {
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
                }
            }
        }

           }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
