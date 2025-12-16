package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentHomeBinding
import android.widget.Toast

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment  : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        val prefs = requireContext()
            .getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val loggedUserId = prefs.getInt("utilizadorId", -1)
        val isLogged = prefs.getBoolean("isLogged", false)
        val isAdmin = prefs.getBoolean("isAdmin", false)

        binding.btnLogin.visibility = if (isLogged) View.GONE else View.VISIBLE
        binding.btnRegisterr.visibility = if (isLogged) View.GONE else View.VISIBLE
        binding.btnLogout.visibility = if (isLogged) View.VISIBLE else View.GONE

        binding.btnIrAdmin.visibility =
            if (isLogged && isAdmin) View.VISIBLE else View.GONE

        binding.btnVerPerfil.visibility =
            if (isLogged && !isAdmin) View.VISIBLE else View.GONE

        binding.btnVerIntencoes.visibility =
                if (isLogged  && !isAdmin) View.VISIBLE else View.GONE


        // Botão para Login
        binding.btnLogin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    LoginFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        // Botão para Register
        binding.btnRegisterr.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    RegisterFragment()
                )
                .addToBackStack(null)
                .commit()
        }
        binding.btnVerIntencoes.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    ListaIntencoesFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        // Botão para ver lista de animais
        binding.btnIrListaAnimais.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    ListaAnimaisFragment()
                )
                .addToBackStack(null)
                .commit()
        }
        binding.btnLogout.setOnClickListener {
            val shared = requireContext()
                .getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)

            shared.edit().clear().apply()

            Toast.makeText(requireContext(), "Logout realizado!", Toast.LENGTH_SHORT).show()

            // Redireciona para LoginFragment ou Activity
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoginFragment())
                .commit()
        }


        binding.btnIrAdmin.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .replace(
                    // ID do FrameLayout na MainActivity
                    requireActivity().findViewById<View>(
                        pt.ipt.dam2025.pawbuddy.R.id.fragmentContainer
                    ).id,
                    GestaoFragment()
                )
                .addToBackStack(null) // permite voltar atrás
                .commit()
        }

        binding.btnVerPerfil.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    requireActivity().findViewById<View>(R.id.fragmentContainer).id,
                    DetalhesUtilizadorFragment.newInstanceFromHome()
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