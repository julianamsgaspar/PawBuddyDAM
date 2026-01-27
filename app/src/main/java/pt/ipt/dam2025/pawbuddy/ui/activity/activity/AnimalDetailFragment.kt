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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAnimalDetailBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.session.SessionManager

/**
 * AnimalDetailFragment
 *
 * Fragment responsável pela visualização detalhada de um animal.
 *
 * Responsabilidades:
 *  - Ler o animalId a partir dos argumentos de navegação
 *  - Carregar os dados do animal a partir do backend (Retrofit)
 *  - Apresentar os dados na UI e carregar a imagem com Glide
 *  - Ajustar a visibilidade de ações (Adotar / Alterar / Eliminar) conforme sessão/perfil
 *  - Permitir:
 *      - Adoção (utilizador autenticado; caso contrário redireciona para login)
 *      - Alteração e eliminação (perfil admin)
 *
 * Aspetos académicos:
 *  - ViewBinding para ligação segura à interface
 *  - Corrotinas com Dispatchers.IO para rede e Dispatchers.Main para UI
 *  - Integração com Navigation Component para passagem de argumentos e gestão de backstack
 *  - Separação entre lógica de carregamento (rede) e ações de UI (listeners)
 */
class AnimalDetailFragment : Fragment() {

    /**
     * Binding nullable do fragmento.
     * Libertado em onDestroyView() para evitar memory leaks.
     */
    private var _binding: FragmentAnimalDetailBinding? = null
    private val binding get() = _binding!!

    /**
     * Serviço Retrofit para operações relacionadas com Animal.
     */
    private val animalApi = RetrofitProvider.animalService

    /**
     * Gestor de sessão para determinar autenticação e permissões.
     * É lazy para usar requireContext() apenas quando o fragmento está anexado.
     */
    private val session by lazy { SessionManager(requireContext()) }

    /**
     * ID do animal a mostrar, obtido via argumentos de navegação.
     */
    private var animalId: Int = -1

    /**
     * Obtém o animalId recebido no Bundle de argumentos.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        animalId = arguments?.getInt("animalId", -1) ?: -1
    }

    /**
     * Infla a view e inicializa o binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura a UI e carrega os dados do animal.
     *
     * Fluxo:
     *  - Determina estado de sessão (isLogged/isAdmin)
     *  - Aplica regras de visibilidade dos botões
     *  - Executa GET do animal em Dispatchers.IO
     *  - Preenche views e carrega imagem com Glide no thread principal
     *  - Define listeners: Adotar, Alterar, Eliminar
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isLogged = session.isLogged()
        val isAdmin = session.isAdmin()

        // --- Regras de UI ---
        when {
            /**
             * Caso administrador:
             *  - disponibiliza ações de gestão (alterar/eliminar)
             *  - esconde ação de adoção
             */
            isAdmin -> {
                binding.btnEliminar.visibility = View.VISIBLE
                binding.btnAlterar.visibility = View.VISIBLE
                binding.btnAdotar.visibility = View.GONE
            }

            /**
             * Caso utilizador autenticado (não admin):
             *  - permite adoção
             *  - esconde ações administrativas
             */
            isLogged -> {
                binding.btnEliminar.visibility = View.GONE
                binding.btnAlterar.visibility = View.GONE
                binding.btnAdotar.visibility = View.VISIBLE
            }

            /**
             * Caso não autenticado:
             *  - mantém botão de adoção visível, mas o fluxo exige login ao clicar
             *  - esconde ações administrativas
             */
            else -> {
                binding.btnEliminar.visibility = View.GONE
                binding.btnAlterar.visibility = View.GONE
                binding.btnAdotar.visibility = View.VISIBLE
            }
        }

        // Carregar detalhes do animal
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val animal = animalApi.getAnimal(animalId)

                withContext(Dispatchers.Main) {
                    /**
                     * Preenchimento de campos, preferindo strings parametrizadas
                     * (internacionalização e consistência).
                     */
                    binding.txtNome.text = animal.nome
                    binding.txtRaca.text = getString(R.string.label_breed, animal.raca)
                    binding.txtIdade.text = getString(R.string.label_age, animal.idade)
                    binding.txtEspecie.text = getString(R.string.label_species, animal.especie)
                    binding.txtGenero.text = getString(R.string.label_gender, animal.genero)
                    binding.txtCor.text = getString(R.string.label_color, animal.cor)

                    /**
                     * Carregamento da imagem com Glide:
                     *  - fullImageUrl constrói o URL absoluto a partir do identificador
                     *  - placeholder e error melhoram UX em cenários de latência/falha
                     *  - centerCrop garante enquadramento consistente na ImageView
                     */
                    val url = RetrofitProvider.fullImageUrl(animal.imagem ?: "")

                    Glide.with(requireContext())
                        .load(url)
                        .placeholder(R.drawable.ic_pet_placeholder)
                        .error(R.drawable.ic_pet_placeholder)
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .signature(ObjectKey(System.currentTimeMillis().toString()))
                        .into(binding.ivAnimal)

                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    /**
                     * Em caso de falha, apresenta mensagem de erro contextual.
                     * Nota: nesta implementação, o erro é atribuído ao txtNome,
                     * funcionando como fallback simples de feedback ao utilizador.
                     */
                    binding.txtNome.text = getString(
                        R.string.error_fetch_animal_details,
                        e.message ?: getString(R.string.error_generic)
                    )
                }
            }
        }

        // Adotar
        binding.btnAdotar.setOnClickListener {
            /**
             * Regra de acesso: submissão de intenção exige autenticação.
             * Se não estiver autenticado, redireciona para login e preserva contexto
             * (origem e id) para retomar o fluxo.
             */
            if (!session.isLogged()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_login_required),
                    Toast.LENGTH_SHORT
                ).show()

                val bundle = Bundle().apply {
                    putBoolean("returnToPrevious", true)
                    putString("origin", "adotar")
                    putInt("originId", animalId)
                }
                findNavController().navigate(R.id.loginFragment, bundle, navOptions { launchSingleTop = true })

                return@setOnClickListener
            }

            /**
             * Se autenticado, navega para o formulário de adoção com animalId.
             */
            val bundle = Bundle().apply { putInt("animalId", animalId) }

            findNavController().navigate(
                R.id.adotarFragment,
                bundle,
                navOptions { launchSingleTop = true }
            )
        }

        // Admin — Alterar
        binding.btnAlterar.setOnClickListener {
            val bundle = Bundle().apply { putInt("animalId", animalId) }
            findNavController().navigate(
                R.id.alterarAnimalFragment,
                bundle,
                navOptions { launchSingleTop = true }
            )
        }

        // Admin — Eliminar
        binding.btnEliminar.setOnClickListener {
            eliminarAnimal()
        }
    }

    /**
     * Elimina o animal no backend e retorna à lista de animais.
     *
     * Implementação:
     *  - Executa chamada em Dispatchers.IO
     *  - Em sucesso, notifica e navega para listaAnimaisFragment
     *  - Em erro, apresenta mensagem ao utilizador
     */
    private fun eliminarAnimal() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                animalApi.eliminarAnimal(animalId)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_animal_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.listaAnimaisFragment)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_delete_animal,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Libertação do binding ao destruir a view, prevenindo memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
