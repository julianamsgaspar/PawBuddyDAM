package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaAnimaisBinding
import pt.ipt.dam2025.pawbuddy.model.Animal
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.AnimalAdapter
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.AnimalAdminAdapter
import retrofit2.HttpException

/**
 * Fragment responsável pela listagem de animais.
 *
 * O comportamento do ecrã é condicionado pelo perfil da sessão:
 * - Público (anónimo + utilizador não-admin): apresenta apenas animais disponíveis para adoção;
 * - Administrador (sessão ativa com privilégios): apresenta todos os animais e disponibiliza
 *   operações de edição e eliminação.
 *
 * A camada de dados é obtida via Retrofit e executada em corrotinas, respeitando a separação
 * entre a thread de IO (rede) e a thread principal (UI).
 */
class ListaAnimaisFragment : Fragment() {

    /**
     * Binding associado ao ciclo de vida da View do Fragment (ViewBinding).
     * Deve ser libertado em [onDestroyView] para evitar retenção indevida da View.
     */
    private var _binding: FragmentListaAnimaisBinding? = null
    private val binding get() = _binding!!

    /**
     * Serviço Retrofit para operações relacionadas com animais.
     */
    private val animalApi = RetrofitProvider.animalService

    /**
     * Gestor de sessão (fonte de verdade para autenticação e permissões).
     */
    private val session by lazy { SessionManager(requireContext()) }

    /**
     * Lista completa devolvida pela API (antes de pesquisa/filtros).
     */
    private var allAnimais: List<Animal> = emptyList()

    /**
     * Adapter para modo público (apenas visualização e navegação para detalhe).
     */
    private lateinit var userAdapter: AnimalAdapter

    /**
     * Adapter para modo administrador (inclui ações de editar e eliminar).
     */
    private lateinit var adminAdapter: AnimalAdminAdapter

    /**
     * Indica se o ecrã está em modo administrador, derivado exclusivamente do estado da sessão.
     */
    private var isAdmin: Boolean = false

    /**
     * Flag para distinguir o primeiro carregamento do ecrã, evitando recarregamentos desnecessários.
     */
    private var firstLoadDone: Boolean = false

    /**
     * Filtros/Ordenação
     */

    private enum class SortMode { MOST_RECENT, OLDEST, NAME_AZ, NAME_ZA }

    private var selectedSpecies: String = "Todos"
    private var selectedGender: String = "Todos"
    private var selectedSort: SortMode = SortMode.MOST_RECENT

    /**
     * Infla o layout do Fragment e inicializa o ViewBinding.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaAnimaisBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura a UI:
     * - Determina o modo (admin/público) a partir da sessão;
     * - Inicializa o RecyclerView e o respetivo adapter;
     * - Define listeners de refresh e pesquisa;
     * - Executa o carregamento inicial.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Modo administrador deriva apenas do estado autenticado e do papel (role) na sessão.
        isAdmin = session.isLogged() && session.isAdmin()

        setupRecyclerByMode()
        setupFiltersUi()

        // Swipe-to-refresh: força nova leitura dos dados sem bloquear a UI com spinner adicional.
        binding.swipeRefresh.setOnRefreshListener {
            loadAnimais(showSpinner = false)
        }

        // Pesquisa local: aplica filtro à lista já carregada.
        binding.etSearch.doAfterTextChanged {
            applyAllFilters()
        }

        loadAnimais(showSpinner = true)
    }

    /**
     * Revalida o modo (admin/público) quando o Fragment volta ao foreground.
     *
     * Caso o utilizador tenha feito login/logout (ou alterado permissões), o RecyclerView é
     * reconfigurado para refletir o contexto atual.
     */
    override fun onResume() {
        super.onResume()
        if (!firstLoadDone) return

        val newIsAdmin = session.isLogged() && session.isAdmin()

        // Se o modo mudou, reconfigura a UI e o adapter.
        if (newIsAdmin != isAdmin) {
            isAdmin = newIsAdmin
            setupRecyclerByMode()
        } else {
            isAdmin = newIsAdmin
        }

        // Atualiza a lista após regressar ao ecrã (ex.: após editar/eliminar).
        loadAnimais(showSpinner = false)
    }

    /**
     * Configura o RecyclerView consoante o modo:
     * - Admin: layout linear + adapter com ações de editar/eliminar;
     * - Público: grelha (GridLayout) com número de colunas adaptativo.
     */
    private fun setupRecyclerByMode() {
        if (isAdmin) {
            binding.rvAnimais.layoutManager = LinearLayoutManager(requireContext())
            adminAdapter = AnimalAdminAdapter(
                onEdit = { animal -> abrirEditar(animal.id) },
                onDelete = { animal -> confirmarEliminar(animal) }
            )
            binding.rvAnimais.adapter = adminAdapter
        } else {
            // Ajuste simples ao tamanho de ecrã: tablets (>=600dp) com mais colunas.
            val spanCount = if (resources.configuration.smallestScreenWidthDp >= 600) 4 else 2
            binding.rvAnimais.layoutManager = GridLayoutManager(requireContext(), spanCount)

            userAdapter = AnimalAdapter { animal -> abrirDetalhes(animal.id) }
            binding.rvAnimais.adapter = userAdapter
        }
    }

    /**
     * Configura UI dos filtros:
     * - Dropdown espécie, género e ordenação;
     * - Botões aplicar e limpar;
     * - Toggle expand/collapse do painel.
     *
     * Requisitos no XML:
     * actSpecies, actGender, actSort, btnApplyFilters, btnClearFilters,
     * btnToggleFilters, filtersContent.
     */
    private fun setupFiltersUi() {
        // Ajusta estas opções consoante os teus valores reais.
        val speciesOptions = listOf("Todos", "Cão", "Gato")
        val genderOptions = listOf("Todos", "Macho", "Fêmea")
        val sortOptions = listOf("Mais recente", "Mais antigo", "Nome A–Z", "Nome Z–A")

        // Necessita MaterialAutoCompleteTextView no layout.
        binding.actSpecies.setSimpleItems(speciesOptions.toTypedArray())
        binding.actGender.setSimpleItems(genderOptions.toTypedArray())
        binding.actSort.setSimpleItems(sortOptions.toTypedArray())

        // Defaults visuais
        binding.actSpecies.setText(selectedSpecies, false)
        binding.actGender.setText(selectedGender, false)
        binding.actSort.setText("Mais recente", false)

        binding.btnApplyFilters.setOnClickListener {
            selectedSpecies = binding.actSpecies.text?.toString().orEmpty().ifBlank { "Todos" }
            selectedGender = binding.actGender.text?.toString().orEmpty().ifBlank { "Todos" }

            selectedSort = when (binding.actSort.text?.toString().orEmpty()) {
                "Mais antigo" -> SortMode.OLDEST
                "Nome A–Z" -> SortMode.NAME_AZ
                "Nome Z–A" -> SortMode.NAME_ZA
                else -> SortMode.MOST_RECENT
            }

            applyAllFilters()
        }

        binding.btnClearFilters.setOnClickListener {
            selectedSpecies = "Todos"
            selectedGender = "Todos"
            selectedSort = SortMode.MOST_RECENT

            binding.actSpecies.setText("Todos", false)
            binding.actGender.setText("Todos", false)
            binding.actSort.setText("Mais recente", false)

            applyAllFilters()
        }

        binding.btnToggleFilters.setOnClickListener {
            val isVisible = binding.filtersContent.visibility == View.VISIBLE
            binding.filtersContent.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.btnToggleFilters.setImageResource(
                if (isVisible) R.drawable.ic_expand_more else R.drawable.ic_expand_less
            )
        }
        binding.actSpecies.setOnClickListener { binding.actSpecies.showDropDown() }
        binding.actGender.setOnClickListener { binding.actGender.showDropDown() }
        binding.actSort.setOnClickListener { binding.actSort.showDropDown() }

    }

    /**
     * Carrega a lista de animais a partir da API.
     *
     * - Público: consulta apenas animais disponíveis;
     * - Admin: consulta todos os animais.
     *
     * @param showSpinner indica se deve mostrar o progress indicator de carregamento inicial.
     */
    private fun loadAnimais(showSpinner: Boolean) {
        if (showSpinner) binding.progress.visibility = View.VISIBLE
        binding.txtErro.visibility = View.GONE
        binding.emptyState.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Em modo público, evita endpoints que exijam permissões administrativas.
                val animais = if (isAdmin) {
                    animalApi.listarAnimais()
                } else {
                    animalApi.getDisponiveis()
                }

                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    allAnimais = animais
                    firstLoadDone = true

                    // Aplica filtro existente (se houver texto na pesquisa).
                    applyAllFilters()
                }

            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    // Só faz sentido interpretar 401/403 como “sessão expirada” em contexto admin.
                    if (isAdmin && (e.code() == 401 || e.code() == 403)) {
                        session.logout()
                        Toast.makeText(
                            requireContext(),
                            "Sessão expirada. Faz login novamente.",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigate(R.id.loginFragment)
                        return@withContext
                    }

                    // Apresentação de erro genérico de carregamento.
                    binding.txtErro.visibility = View.VISIBLE
                    binding.txtErro.text = getString(
                        R.string.error_load_animals,
                        e.message ?: getString(R.string.error_generic)
                    )

                    submitList(emptyList())
                    binding.emptyState.visibility = View.GONE
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    binding.txtErro.visibility = View.VISIBLE
                    binding.txtErro.text = getString(
                        R.string.error_load_animals,
                        e.message ?: getString(R.string.error_generic)
                    )

                    submitList(emptyList())
                    binding.emptyState.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Aplica pesquisa + filtros + ordenação sobre allAnimais e submete ao adapter.
     */
    private fun applyAllFilters() {
        val q = binding.etSearch.text?.toString().orEmpty().trim().lowercase()

        var list = allAnimais

        // 1) Pesquisa (nome/especie/raca)
        if (q.isNotBlank()) {
            list = list.filter { a ->
                a.nome.lowercase().contains(q) ||
                        a.especie.lowercase().contains(q) ||
                        a.raca.lowercase().contains(q)
            }
        }

        // 2) Filtro por espécie (assumindo a.especie = "Cão"/"Gato"/etc.)
        if (selectedSpecies != "Todos") {
            list = list.filter { a -> a.especie.equals(selectedSpecies, ignoreCase = true) }
        }

        // 3) Filtro por género
        // Ajusta conforme o teu model:
        // - Se tiveres a.genero: String? -> "Macho"/"Fêmea"
        // - Se tiveres enum/int -> traduz aqui.
        if (selectedGender != "Todos") {
            list = list.filter { a ->
                val g = a.genero?.toString().orEmpty()
                g.equals(selectedGender, ignoreCase = true)
            }
        }

        // 4) Ordenação
        list = when (selectedSort) {
            SortMode.NAME_AZ -> list.sortedBy { it.nome.lowercase() }
            SortMode.NAME_ZA -> list.sortedByDescending { it.nome.lowercase() }
            SortMode.OLDEST -> list.sortedBy { it.id } // fallback sem datas
            SortMode.MOST_RECENT -> list.sortedByDescending { it.id } // fallback sem datas
        }

        submitList(list)
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        binding.txtErro.visibility = View.GONE
    }

    /**
     * Submete a lista ao adapter ativo (admin ou público).
     *
     * @param list lista a apresentar no RecyclerView.
     */
    private fun submitList(list: List<Animal>) {
        if (isAdmin) adminAdapter.submitList(list) else userAdapter.submitList(list)
    }

    /**
     * Navega para o detalhe do animal selecionado.
     *
     * @param id identificador do animal.
     */
    private fun abrirDetalhes(id: Int) {
        val bundle = Bundle().apply { putInt("animalId", id) }
        findNavController().navigate(R.id.animalDetailFragment, bundle)
    }

    /**
     * Navega para o ecrã de edição do animal (reservado a administradores).
     *
     * @param id identificador do animal.
     */
    private fun abrirEditar(id: Int) {
        if (!session.isLogged() || !session.isAdmin()) {
            Toast.makeText(
                requireContext(),
                "Acesso reservado a administradores.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val bundle = Bundle().apply { putInt("animalId", id) }
        findNavController().navigate(R.id.alterarAnimalFragment, bundle)
    }

    /**
     * Apresenta um diálogo de confirmação antes de eliminar um animal.
     *
     * @param animal instância alvo da eliminação (utilizada para mostrar nome e ID ao utilizador).
     */
    private fun confirmarEliminar(animal: Animal) {
        if (!session.isLogged() || !session.isAdmin()) {
            Toast.makeText(
                requireContext(),
                "Acesso reservado a administradores.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.action_delete))
            .setMessage("Eliminar o animal \"${animal.nome}\" (ID: ${animal.id})? Esta ação é irreversível.")
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                eliminarAnimal(animal.id)
            }
            .show()
    }

    /**
     * Executa a eliminação de um animal via API.
     *
     * Em caso de sucesso, apresenta feedback e recarrega a lista.
     * Em caso de 401/403, assume expiração/invalidade da sessão e redireciona para login.
     *
     * @param id identificador do animal a eliminar.
     */
    private fun eliminarAnimal(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                animalApi.eliminarAnimal(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_animal_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    loadAnimais(showSpinner = false)
                }
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    if (e.code() == 401 || e.code() == 403) {
                        session.logout()
                        Toast.makeText(
                            requireContext(),
                            "Sessão expirada. Faz login novamente.",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigate(R.id.loginFragment)
                        return@withContext
                    }

                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_delete_animal,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
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
     * Liberta o binding quando a View é destruída, prevenindo memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
