package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

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
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isLogged = prefs.getBoolean("isLogged", false)
        val isAdmin = prefs.getBoolean("isAdmin", false)

        // Explorar (sempre)
        binding.cardExplore.setOnClickListener { v ->
            animateCard(v) {
                findNavController().navigate(R.id.listaAnimaisFragment)
            }
        }

        // Conta (login ou perfil)
        if (isLogged) {
            binding.tvAccountTitle.text = getString(R.string.home_cta_profile_title)
            binding.tvAccountDesc.text = getString(R.string.home_cta_profile_desc)
        } else {
            binding.tvAccountTitle.text = getString(R.string.home_cta_login_title)
            binding.tvAccountDesc.text = getString(R.string.home_cta_login_desc)
        }

        binding.cardAccount.setOnClickListener { v ->
            animateCard(v) {
                if (isLogged) {
                    val bundle = Bundle().apply { putBoolean("fromList", false) }
                    findNavController().navigate(R.id.detalhesUtilizadorFragment, bundle)
                } else {
                    findNavController().navigate(R.id.loginFragment)
                }
            }
        }

        // Minhas Intenções (apenas user logado NÃO-admin)
        // Nota: garante que existe no XML: cardMyIntents (visibility="gone")
        binding.cardMyIntents.visibility = if (isLogged && !isAdmin) View.VISIBLE else View.GONE
        binding.cardMyIntents.setOnClickListener { v ->
            animateCard(v) {
                // Ajusta este destino ao ID real do teu nav_graph
                // Ex.: R.id.minhasIntencoesFragment ou R.id.listaIntencoesFragment (modo user)
                findNavController().navigate(R.id.listaIntencoesFragment)
            }
        }

        // Registo (só não logado)
        binding.cardRegister.visibility = if (isLogged) View.GONE else View.VISIBLE
        binding.cardRegister.setOnClickListener { v ->
            animateCard(v) {
                findNavController().navigate(R.id.registerFragment)
            }
        }
    }

    private fun animateCard(view: View, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(110)
                    .withEndAction { onEnd() }
                    .start()
            }
            .start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
