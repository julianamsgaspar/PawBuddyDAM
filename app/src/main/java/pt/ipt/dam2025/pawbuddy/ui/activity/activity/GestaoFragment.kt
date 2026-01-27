package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentGestaoBinding

/**
 * Fragment responsável pela área de Gestão da aplicação PawBuddy.
 *
 * Enquadramento funcional:
 * - Atua como um ecrã de menu/portal administrativo.
 * - Disponibiliza atalhos para as principais operações de gestão do sistema:
 *   - Animais
 *   - Utilizadores
 *   - Intenções de adoção
 *   - Adoções finais
 *
 * Enquadramento arquitetural:
 * - Implementado como Fragment, integrando-se no Navigation Component.
 * - Utiliza View Binding para acesso seguro aos elementos da interface gráfica.
 * - Cada botão desencadeia uma navegação explícita para um Fragment de destino
 *   definido no nav_graph.
 *
 * Responsabilidade única:
 * - Orquestrar a navegação entre ecrãs de gestão, sem conter lógica de negócio
 *   ou acesso a dados (princípio de separação de responsabilidades).
 */
class GestaoFragment : Fragment() {

    /**
     * Referência nullable ao binding da View.
     * É válida apenas entre onCreateView e onDestroyView.
     */
    private var _binding: FragmentGestaoBinding? = null

    /**
     * Getter não-null para o binding.
     * Assume-se que só é acedido quando a View está criada.
     */
    private val binding get() = _binding!!

    /**
     * Infla o layout do Fragment e inicializa o View Binding.
     *
     * @param inflater responsável por inflar o layout XML.
     * @param container ViewGroup pai.
     * @param savedInstanceState estado previamente guardado (se existir).
     * @return View raiz associada ao Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGestaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configuração da interface e definição dos listeners após a View estar criada.
     *
     * Cada botão representa uma ação de navegação para um módulo específico
     * da aplicação. A navegação é realizada através do NavController,
     * garantindo coerência com o grafo de navegação definido.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ADICIONAR ANIMAL
        // Navega para o Fragment responsável pela criação de um novo animal.
        binding.btnAdicionarAnimal.setOnClickListener {
            findNavController().navigate(R.id.adicionarAnimalFragment)
        }

        // LISTAR ANIMAIS
        // Navega para o Fragment que apresenta a lista de animais existentes.
        binding.btnListarAnimais.setOnClickListener {
            findNavController().navigate(R.id.listaAnimaisFragment)
        }

        // LISTAR UTILIZADORES
        // Navega para o Fragment que apresenta a lista de utilizadores registados.
        binding.btnListarUtilizadores.setOnClickListener {
            findNavController().navigate(R.id.listaUtilizadoresFragment)
        }

        // LISTAR INTENÇÕES
        // Navega para o Fragment que apresenta as intenções de adoção.
        binding.btnListarIntencoes.setOnClickListener {
            findNavController().navigate(R.id.listaIntencoesFragment)
        }

        // LISTAR ADOÇÕES FINAIS
        // Navega para o Fragment que apresenta as adoções concluídas.
        binding.btnListarAdocoes.setOnClickListener {
            findNavController().navigate(R.id.listarAdocoesFinaisFragment)
        }
    }

    /**
     * Método invocado quando a View do Fragment é destruída.
     *
     * Boa prática:
     * - Libertar explicitamente o binding para evitar memory leaks,
     *   dado que o Fragment pode sobreviver à destruição da View.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
