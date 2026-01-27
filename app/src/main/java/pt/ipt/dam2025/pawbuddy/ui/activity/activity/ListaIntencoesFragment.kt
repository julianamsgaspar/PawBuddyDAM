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
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaIntencoesBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.IntencaoAdapter
import retrofit2.HttpException
import pt.ipt.dam2025.pawbuddy.session.SessionManager

/**
 * Fragment responsável por apresentar a lista de Intenções de Adoção.
 *
 * Cenários de utilização:
 * - Utilizador comum (não-admin):
 *   - Visualiza apenas as suas intenções (por utilizadorId).
 *   - Pode alternar entre intenções "Em curso" e "Finalizadas" sem nova chamada ao servidor,
 *     graças a uma cache local (allUserIntents).
 * - Administrador:
 *   - Visualiza intenções ativas (endpoint getAtivas()).
 *   - Tem acesso a ações administrativas no adapter (eliminar, editar estado), dependendo
 *     da implementação do IntencaoAdapter.
 *
 * Componentes e tecnologias:
 * - RecyclerView + LinearLayoutManager para listagem vertical.
 * - SharedPreferences para leitura de estado de sessão (isLogged, isAdmin, utilizadorId).
 * - SessionManager para operações de sessão (logout) em caso de expiração/erro 401/403.
 * - Retrofit + Coroutines para chamadas de rede:
 *   - Dispatchers.IO para I/O
 *   - Dispatchers.Main para atualizar a UI
 * - Navigation Component para navegação para:
 *   - detalhe da intenção
 *   - edição do estado (admin)
 *   - login (quando necessário)
 *
 * Gestão de estados da UI:
 * - Mostra/oculta lista e mensagem "sem intenções" com base no tamanho da lista.
 * - Apresenta Snackbar opcional (banner) após submissão de intenção, permitindo salto direto
 *   para o detalhe.
 */
class ListaIntencoesFragment : Fragment() {

    /**
     * Binding nullable, válido apenas entre onCreateView e onDestroyView.
     */
    private var _binding: FragmentListaIntencoesBinding? = null

    /**
     * Getter não-null do binding; assume acesso apenas com a View ativa.
     */
    private val binding get() = _binding!!

    /**
     * Gestor de sessão, instanciado lazy para garantir Context válido.
     * É utilizado em particular para logout em caso de 401/403.
     */
    private val session by lazy { SessionManager(requireContext()) }

    /**
     * Serviço Retrofit associado a intenções de adoção.
     */
    private val api = RetrofitProvider.intencaoService

    /**
     * Cache local para utilizador não-admin: permite alternar filtros (em curso/finalizadas)
     * sem necessidade de nova chamada ao servidor.
     */
    private var allUserIntents: List<IntencaoDeAdocao> = emptyList()

    /**
     * Estado do filtro de UI:
     * - false: mostrar intenções "em curso"
     * - true: mostrar intenções "finalizadas"
     */
    private var showFinalizadas: Boolean = false

    /**
     * Código do estado considerado "Concluído".
     * Nota: este valor deve estar alinhado com o backend (enum/constante do servidor).
     * O comentário indica que pode necessitar de ajuste.
     */
    private val ESTADO_CONCLUIDO = 3

    /**
     * Infla a View e inicializa o binding.
     *
     * @return View raiz do Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaIntencoesBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura UI, valida sessão e inicia carregamento inicial.
     *
     * Fluxo:
     * 1) Configura RecyclerView (layout vertical).
     * 2) Lê dados de sessão em SharedPreferences:
     *    - isLogged: se false, redireciona para login.
     *    - isAdmin: determina endpoints e visibilidade de filtros.
     *    - utilizadorId: usado para listar intenções do utilizador comum.
     * 3) Configura filtro apenas para utilizador não-admin (chips).
     * 4) Processa "banner" opcional (Snackbar) quando vem de submissão de intenção.
     * 5) Carrega intenções pela primeira vez.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvIntencoes.layoutManager = LinearLayoutManager(requireContext())

        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isAdminPrefs = prefs.getBoolean("isAdmin", false)
        val utilizadorId = prefs.getInt("utilizadorId", -1)

        // ✅ 1) Login obrigatório (fonte de verdade: SessionManager)
        if (!session.isLogged()) {
            Toast.makeText(requireContext(), getString(R.string.error_login_required), Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // ✅ 2) Se este ecrã vai trabalhar em modo ADMIN, validar admin via SessionManager
        if (isAdminPrefs && !session.isAdmin()) {
            Toast.makeText(requireContext(), "Acesso reservado a administradores.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        // Configuração do filtro (apenas para utilizador comum).
        setupFiltroUserOnly(isAdminPrefs)

        val showBanner = arguments?.getBoolean("showBanner", false) ?: false
        val intencaoId = arguments?.getInt("intencaoId", -1) ?: -1

        if (showBanner) {
            arguments?.putBoolean("showBanner", false)

            com.google.android.material.snackbar.Snackbar
                .make(
                    binding.root,
                    getString(R.string.banner_intent_submitted),
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                )
                .setAction(getString(R.string.action_view_status)) {
                    if (intencaoId > 0) {
                        val b = Bundle().apply { putInt("id", intencaoId) }
                        findNavController().navigate(R.id.intencaoDetalheFragment, b)
                    }
                }
                .show()
        }

        // ✅ Carregamento inicial (admin ou user)
        carregarIntencoes(isAdminPrefs, utilizadorId)
    }


    /**
     * Recarregamento em onResume.
     *
     * Justificação:
     * - Permite refletir alterações feitas noutros ecrãs (ex.: edição do estado, eliminação).
     * - Garante que a lista é atualizada quando o utilizador regressa ao Fragment.
     *
     * Observação:
     * - O método recarrega sempre a partir do servidor (não depende exclusivamente da cache).
     */
    override fun onResume() {
        super.onResume()

        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isAdminPrefs = prefs.getBoolean("isAdmin", false)
        val utilizadorId = prefs.getInt("utilizadorId", -1)

        // ✅ se perdeu sessão enquanto estava fora
        if (!session.isLogged()) {
            Toast.makeText(requireContext(), "Sessão expirada. Faz login novamente.", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.loginFragment)
            return
        }

        // ✅ se prefs diz admin, mas SessionManager já não valida admin
        if (isAdminPrefs && !session.isAdmin()) {
            Toast.makeText(requireContext(), "Acesso reservado a administradores.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        carregarIntencoes(isAdminPrefs, utilizadorId)
    }


    /**
     * Configura o filtro por chips apenas para utilizador não-admin.
     *
     * Comportamento:
     * - Admin: filtro é ocultado (chipGroupFiltro GONE).
     * - User: filtro é visível e default é "Em curso".
     * - Alterações no chipGroup atualizam a variável showFinalizadas e aplicam o filtro sobre a cache.
     *
     * @param isAdmin indica se o utilizador é administrador.
     */
    private fun setupFiltroUserOnly(isAdmin: Boolean) {
        if (isAdmin) {
            binding.chipGroupFiltro.visibility = View.GONE
            return
        }

        binding.chipGroupFiltro.visibility = View.VISIBLE

        // Default: mostrar intenções "Em curso".
        binding.chipEmCurso.isChecked = true
        showFinalizadas = false

        // Listener do filtro: determina se o chip "Finalizadas" está selecionado.
        binding.chipGroupFiltro.setOnCheckedStateChangeListener { _, checkedIds ->
            showFinalizadas = checkedIds.contains(R.id.chipFinalizadas)
            aplicarFiltroEAtualizarUI(isAdmin = false)
        }
    }

    /**
     * Carrega intenções do backend consoante o perfil.
     *
     * Estratégia de obtenção:
     * - Admin: endpoint getAtivas() (intenções ativas).
     * - Utilizador: endpoint getByUtilizadorId(utilizadorId).
     *
     * Processamento:
     * - Admin: apresenta lista diretamente, sem cache e sem filtro por chips.
     * - Utilizador: guarda em allUserIntents e aplica o filtro atual (em curso/finalizadas).
     *
     * Tratamento de erros:
     * - HttpException 401/403: sessão expirada ou sem permissões -> logout e navega para login,
     *   passando bundle para eventual retorno ao contexto anterior.
     * - Outros erros: Toast com mensagem apropriada.
     *
     * @param isAdmin indica o perfil do utilizador.
     * @param utilizadorId id do utilizador (relevante para não-admin).
     */
    private fun carregarIntencoes(isAdmin: Boolean, utilizadorId: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lista: List<IntencaoDeAdocao> =
                    if (isAdmin) {
                        api.getAtivas()
                    } else {
                        if (utilizadorId <= 0) emptyList()
                        else api.getByUtilizadorId(utilizadorId)
                    }

                withContext(Dispatchers.Main) {
                    if (isAdmin) {
                        // Admin: lista é exibida como recebida do endpoint.
                        mostrarLista(lista, isAdmin = true)
                    } else {
                        // User: guarda cache e aplica filtro corrente.
                        allUserIntents = lista
                        aplicarFiltroEAtualizarUI(isAdmin = false)
                    }
                }

            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    if (e.code() == 401 || e.code() == 403) {
                        // Política: expiração/invalidade da sessão -> logout local + novo login.
                        session.logout()
                        Toast.makeText(
                            requireContext(),
                            "Sessão expirada. Faz login novamente.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Bundle de contexto para o login (suporta lógica de retorno após autenticação).
                        val b = Bundle().apply {
                            putBoolean("returnToPrevious", true)
                            putString("origin", "intencoes")
                            putInt("originId", -1)
                        }
                        findNavController().navigate(R.id.loginFragment, b)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(
                                R.string.error_load_intents,
                                e.message ?: getString(R.string.error_generic)
                            ),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_load_intents,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Aplica o filtro (em curso vs finalizadas) sobre a cache allUserIntents e atualiza a UI.
     *
     * Critério:
     * - Finalizadas: estado == ESTADO_CONCLUIDO
     * - Em curso: estado != ESTADO_CONCLUIDO
     *
     * @param isAdmin mantido como parâmetro por consistência; para user espera-se false.
     */
    private fun aplicarFiltroEAtualizarUI(isAdmin: Boolean) {
        val filtrada = if (showFinalizadas) {
            allUserIntents.filter { it.estado == ESTADO_CONCLUIDO }
        } else {
            allUserIntents.filter { it.estado != ESTADO_CONCLUIDO }
        }
        mostrarLista(filtrada, isAdmin)
    }

    /**
     * Atualiza a UI para apresentar a lista (ou estado vazio) e configura o adapter.
     *
     * Comportamento:
     * - Lista vazia: esconde RecyclerView e mostra texto "Sem intenções".
     * - Lista com elementos: mostra RecyclerView e atribui um IntencaoAdapter configurado
     *   com callbacks para:
     *   - clique (abrir detalhe)
     *   - eliminar (admin/funcionalidade conforme adapter)
     *   - editar estado (admin)
     *
     * Nota de design:
     * - O adapter é criado e atribuído a cada chamada. Em projetos maiores, pode optar-se por
     *   criar o adapter uma vez e apenas atualizar a lista (submitList), por razões de eficiência.
     *
     * @param lista intenções a apresentar.
     * @param isAdmin perfil do utilizador para adaptar ações apresentadas.
     */
    private fun mostrarLista(lista: List<IntencaoDeAdocao>, isAdmin: Boolean) {
        if (lista.isEmpty()) {
            binding.rvIntencoes.visibility = View.GONE
            binding.txtSemIntencoes.visibility = View.VISIBLE
        } else {
            binding.txtSemIntencoes.visibility = View.GONE
            binding.rvIntencoes.visibility = View.VISIBLE

            binding.rvIntencoes.adapter = IntencaoAdapter(
                lista = lista,
                isAdmin = isAdmin,
                onClick = { intencao ->
                    val b = Bundle().apply { putInt("id", intencao.id) }
                    findNavController().navigate(R.id.intencaoDetalheFragment, b)
                },
                onEliminar = { intencao -> eliminarIntencao(intencao.id) },
                onEditarEstado = { intencao -> abrirEditarEstado(intencao.id, intencao.estado) }
            )
        }
    }

    /**
     * Elimina uma intenção no backend e, em caso de sucesso, recarrega a listagem.
     *
     * Implementação:
     * - Executa a operação em Dispatchers.IO.
     * - No sucesso: apresenta Toast e recarrega intenções com base no perfil atual.
     * - No erro: apresenta Toast com mensagem de erro.
     *
     * @param id identificador da intenção a eliminar.
     */
    private fun eliminarIntencao(id: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.deleteIntencao(id)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_intent_deleted),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Recarrega intenções para refletir a eliminação no estado da UI.
                    val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
                    carregarIntencoes(
                        prefs.getBoolean("isAdmin", false),
                        prefs.getInt("utilizadorId", -1)
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_delete_intent,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Navega para o Fragment de edição do estado de uma intenção (tipicamente ação administrativa).
     *
     * Passa no Bundle:
     * - id: identificador da intenção
     * - estado: estado atual, para pré-seleção no Spinner no ecrã de edição
     *
     * @param id id da intenção.
     * @param estadoAtual estado corrente.
     */
    private fun abrirEditarEstado(id: Int, estadoAtual: Int) {
        val bundle = Bundle().apply {
            putInt("id", id)
            putInt("estado", estadoAtual)
        }
        findNavController().navigate(R.id.editarEstadoIntencaoFragment, bundle)
    }

    /**
     * Libertação do binding para evitar memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
