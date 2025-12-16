package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentGestaoBinding

/**
 * A simple [Fragment] subclass.
 * Use the [GestaoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class GestaoFragment : Fragment() {

    private lateinit var binding: FragmentGestaoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGestaoBinding.inflate(inflater, container, false)

        // BOTÃO ADICIONAR ANIMAL
        binding.btnAdicionarAnimal.setOnClickListener {
            abrirFragment(AdicionarAnimalFragment())
        }

        // LISTAR ANIMAIS
        binding.btnListarAnimais.setOnClickListener {
            abrirFragment(ListaAnimaisFragment())
        }

        // LISTAR UTILIZADORES
        binding.btnListarUtilizadores.setOnClickListener {
            abrirFragment(ListaUtilizadoresFragment())
        }

        // LISTAR INTENÇÕES
        binding.btnListarIntencoes.setOnClickListener {
           abrirFragment(ListaIntencoesFragment())
        }

        // LISTAR ADOÇÕES FINAIS
        binding.btnListarAdocoes.setOnClickListener {
            abrirFragment(ListarAdocoesFinaisFragment())
        }

        // LOGOUT
        binding.btnVoltarHome.setOnClickListener {
            abrirFragment(HomeFragment())
        }

        return binding.root
    }

    private fun abrirFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}