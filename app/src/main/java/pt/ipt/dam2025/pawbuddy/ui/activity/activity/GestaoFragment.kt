package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentGestaoBinding

class GestaoFragment : Fragment() {

    private var _binding: FragmentGestaoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ADICIONAR ANIMAL
        binding.btnAdicionarAnimal.setOnClickListener {
            findNavController().navigate(R.id.adicionarAnimalFragment)
        }

        // LISTAR ANIMAIS
        binding.btnListarAnimais.setOnClickListener {
            findNavController().navigate(R.id.listaAnimaisFragment)
        }

        // LISTAR UTILIZADORES
        binding.btnListarUtilizadores.setOnClickListener {
            findNavController().navigate(R.id.listaUtilizadoresFragment)
        }

        // LISTAR INTENÇÕES
        binding.btnListarIntencoes.setOnClickListener {
            findNavController().navigate(R.id.listaIntencoesFragment)
        }

        // LISTAR ADOÇÕES FINAIS
        binding.btnListarAdocoes.setOnClickListener {
            findNavController().navigate(R.id.listarAdocoesFinaisFragment)
        }

           }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
