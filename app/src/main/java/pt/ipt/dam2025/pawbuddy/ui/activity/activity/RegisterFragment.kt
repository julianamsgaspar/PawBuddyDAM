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
import pt.ipt.dam2025.pawbuddy.databinding.FragmentRegisterBinding
import pt.ipt.dam2025.pawbuddy.model.LoginRequest
import pt.ipt.dam2025.pawbuddy.model.LoginResponse
import pt.ipt.dam2025.pawbuddy.model.RegisterRequest
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

/**
 * A simple [Fragment] subclass.
 * Use the [RegisterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authService = RetrofitInitializer().authService()
    private val api = RetrofitInitializer().authService()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {






        binding.btnRegister.setOnClickListener {
            val Nome = binding.etNome.text.toString();
            val Password = binding.etPassword.text.toString();
            val DataNascimento = binding.etDataNascimento.text.toString();
            val Nif = binding.etNif.text.toString();
            val Telemovel = binding.etTelemovel.text.toString();
            val Morada = binding.etMorada.text.toString();
            val CodPostal = binding.etCodPostal.text.toString();
            val Email = binding.etEmail.text.toString();
            val Pais = binding.etPais.text.toString();

            // Validar campos obrigatórios
            if (Nome.isEmpty() || Email.isEmpty() || Password.isEmpty() || DataNascimento.isEmpty() ||
                Nif.isEmpty() || Telemovel.isEmpty() || Morada.isEmpty() || CodPostal.isEmpty() || Pais.isEmpty()
            ) {
                Toast.makeText(requireContext(), "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val request = RegisterRequest(
                nome = Nome,
                email = Email,
                password = Password,
                dataNascimento = DataNascimento,
                nif = Nif,
                telemovel = Telemovel,
                morada = Morada,
                codPostal = CodPostal,
                pais = Pais
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {

                    val response : LoginResponse = api.register(request)
                    val log = LoginRequest(
                        Email = Nome,
                        Password = Password
                    )
                    api.login(log )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Registo efetuado: ${log.Email}", Toast.LENGTH_LONG).show()
                    }
                    val shared = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
                    shared.edit().apply {
                        putBoolean("isLogged", true)
                        putInt("utilizadorId", response.id) // ID do utilizador
                        putBoolean("isAdmin", log.Email == "admin@pawbuddy.com") // se tiveres flag admin
                        apply() // grava persistentemente
                    }

                    // Redireciona para a home
                        parentFragmentManager.beginTransaction()
                            .replace(
                                requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                                HomeFragment()

                            )
                            .addToBackStack(null)
                            .commit()


                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Erro: ${e.message}", Toast.LENGTH_LONG).show()
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}