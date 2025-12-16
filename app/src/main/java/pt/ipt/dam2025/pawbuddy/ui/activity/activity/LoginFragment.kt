package pt.ipt.dam2025.pawbuddy.ui.activity.activity


import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentLoginBinding
import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.model.LoginResponse
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val retrofit = RetrofitInitializer()



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()


            if (email.isBlank() || password.isBlank()) {
                Toast.makeText(requireContext(), "Preencha email e password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val log = LoginRequest(
                        Email = email,
                        Password = password
                    )
                    val resposta: LoginResponse = retrofit.authService().login(log )


                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Login feito! ${log.Email}",
                            Toast.LENGTH_LONG
                        ).show()

                        val isAdmin = log.Email == "admin@pawbuddy.com"
                        val shared = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
                        shared.edit().apply {
                            putBoolean("isLogged", true)
                            putInt("utilizadorId", resposta.id) // ID do utilizador
                            putBoolean("isAdmin", isAdmin) // se tiveres flag admin
                            apply() // grava persistentemente
                        }

                        // Redireciona para lista de animais
                        parentFragmentManager.beginTransaction()
                            .replace(
                                requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                                HomeFragment()
                            )
                            .addToBackStack(null)
                            .commit()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "Erro ao fazer login: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        binding.btnVoltarHome.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    HomeFragment()
                )
                .commit()
        }
        // Botão para Register
        binding.btnRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    RegisterFragment()
                )
                .addToBackStack(null)
                .commit()
        }

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}