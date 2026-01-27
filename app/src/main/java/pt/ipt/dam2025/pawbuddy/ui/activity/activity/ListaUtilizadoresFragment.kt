package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListaUtilizadoresBinding
import pt.ipt.dam2025.pawbuddy.model.Utilizador
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.UtilizadorAdapter
import pt.ipt.dam2025.pawbuddy.session.SessionManager

/**
 * Fragment responsável pela listagem e gestão (admin-only) de Utilizadores.
 *
 * Objetivos funcionais:
 * - Apresentar a lista de utilizadores obtida do backend.
 * - Permitir pesquisa local (filtragem client-side) por ID, nome, email e NIF.
 * - Permitir navegação para o detalhe do utilizador (DetalhesUtilizadorFragment) com contexto "fromList".
 * - Permitir eliminação de utilizadores, com confirmação e salvaguarda para impedir
 *   eliminação da própria conta do administrador autenticado.
 *
 * Enquadramento arquitetural:
 * - View Binding para acesso seguro a componentes do layout.
 * - RecyclerView (LinearLayoutManager) para listagem vertical.
 * - Retrofit (RetrofitProvider.utilizadorService) para operações CRUD remotas.
 * - SessionManager para:
 *   - validação de autenticação e permissões (admin-only)
 *   - logout em caso de sessão expirada (conforme política existente no código)
 * - Coroutines (lifecycleScope) para chamadas assíncronas e atualizações seguras de UI.
 *
 * Notas académicas (segurança e consistência):
 * - A restrição "admin-only" é aplicada no cliente por SessionManager, mas deve existir também
 *   validação server-side (autorização no backend) para garantir segurança efetiva.
 * - A filtragem é feita localmente sobre a cache allUsers; reduz carga de rede e melhora responsividade.
 */
class ListaUtilizadoresFragment : Fragment() {

    /**
     * Binding nullable, válido apenas entre onCreateView e onDestroyView.
     * Deve ser libertado em onDestroyView para prevenir memory leaks.
     */
    private var _binding: FragmentListaUtilizadoresBinding? = null

    /**
     * Getter não-null do binding; assume-se acesso apenas enquanto a View existir.
     */
    private val binding get() = _binding!!

    /**
     * Serviço Retrofit responsável por endpoints de Utilizador.
     */
    private val api = RetrofitProvider.utilizadorService

    /**
     * Adapter do RecyclerView. É inicializado em onViewCreated.
     */
    private lateinit var adapter: UtilizadorAdapter

    /**
     * Gestor de sessão instanciado lazily para garantir Context válido.
     * Usado para verificar autenticação, papel (admin) e para logout.
     */
    private val session by lazy { SessionManager(requireContext()) }

    /**
     * Cache local com a lista completa de utilizadores (fonte de filtragem).
     * Permite pesquisar sem novas chamadas ao backend.
     */
    private var allUsers: List<Utilizador> = emptyList()

    /**
     * Infla o layout do Fragment e inicializa o View Binding.
     *
     * @return View raiz do Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListaUtilizadoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura UI, valida permissões e inicia carregamento de dados.
     *
     * Passos:
     * 1) Validação de segurança: apenas administradores podem aceder.
     * 2) Configuração do adapter com callbacks:
     *    - onClick: abre detalhe do utilizador, indicando origem "fromList=true".
     *    - onDeleteClick: abre diálogo de confirmação de eliminação.
     * 3) Configuração do RecyclerView (LinearLayoutManager).
     * 4) Configuração de refresh manual (SwipeRefreshLayout).
     * 5) Configuração de pesquisa local (TextWatcher via addTextChangedListener).
     * 6) Inicialização de estados visuais e carregamento inicial.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Segurança: este ecrã é admin-only.
        // Se o utilizador não estiver autenticado ou não for admin, impede acesso e regressa.
        if (!session.isLogged() || !session.isAdmin()) {
            Toast.makeText(requireContext(), "Acesso reservado a administradores.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        // Adapter com comportamento diferenciado: abrir detalhe e confirmar eliminação.
        adapter = UtilizadorAdapter(
            onClick = { utilizador ->
                // Encapsula parâmetros de navegação para permitir ao detalhe adaptar UI
                // (ex.: mostrar botão eliminar).
                val bundle = Bundle().apply {
                    putInt("userId", utilizador.id)
                    putBoolean("fromList", true)
                }
                findNavController().navigate(R.id.detalhesUtilizadorFragment, bundle)
            },
            onDeleteClick = { utilizador ->
                confirmarApagar(utilizador)
            }
        )

        // RecyclerView: listagem vertical.
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter

        // Swipe refresh: força recarregamento da lista.
        binding.swipeRefreshUsers.setOnRefreshListener {
            carregarUtilizadores()
        }

        // Pesquisa (admin): filtra a cache local conforme o utilizador escreve.
        binding.etSearchUsers.addTextChangedListener { editable ->
            aplicarFiltro(editable?.toString())
        }

        // Estado inicial da UI.
        binding.txtErroUsers.visibility = View.GONE
        binding.emptyUsers.visibility = View.GONE
        binding.progressUsers.visibility = View.GONE

        // Carregamento inicial.
        carregarUtilizadores()
    }

    /**
     * Carrega a lista de utilizadores a partir do backend e atualiza a UI.
     *
     * Observação relevante:
     * - Aqui é usada lifecycleScope.launch sem Dispatchers.IO explícito.
     *   Por defeito, lifecycleScope corre no Main dispatcher; a chamada api.ListaDeUtilizadores()
     *   deve ser suspensa e executada por Retrofit de forma assíncrona.
     *   Ainda assim, em contexto académico, é comum explicitar Dispatchers.IO para clareza
     *   (não se altera código, apenas se documenta a implicação).
     *
     * Fluxo:
     * - Mostra progress.
     * - Faz GET de utilizadores.
     * - Atualiza cache (allUsers).
     * - Aplica filtro corrente (se existir).
     * - Oculta progress e termina swipeRefresh.
     *
     * Tratamento de erro:
     * - Termina loading.
     * - Faz logout (política herdada do projeto) e redireciona para login com bundle de contexto.
     */
    private fun carregarUtilizadores() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // UI: loading.
                binding.txtErroUsers.visibility = View.GONE
                binding.emptyUsers.visibility = View.GONE
                binding.progressUsers.visibility = View.VISIBLE

                // Chamada ao backend: obtém lista completa.
                val lista = api.ListaDeUtilizadores()
                allUsers = lista

                // Aplica o filtro atual (se existir), atualizando o adapter.
                aplicarFiltro(binding.etSearchUsers.text?.toString())

                // UI: done.
                binding.progressUsers.visibility = View.GONE
                binding.swipeRefreshUsers.isRefreshing = false

            } catch (e: Exception) {
                // UI: termina loading.
                binding.progressUsers.visibility = View.GONE
                binding.swipeRefreshUsers.isRefreshing = false

                // Política de sessão:
                // Em caso de falha (frequentemente 401/403), termina sessão e força novo login.
                // Nota: aqui a exceção é genérica; não distingue HTTP errors.
                session.logout()

                Toast.makeText(
                    requireContext(),
                    "Sessão expirada. Faz login novamente.",
                    Toast.LENGTH_LONG
                ).show()

                if (isAdded) {
                    // Bundle de contexto para eventual retorno após autenticação.
                    val b = Bundle().apply {
                        putBoolean("returnToPrevious", true)
                        putString("origin", "admin_users")
                        putInt("originId", -1)
                    }
                    findNavController().navigate(R.id.loginFragment, b)
                }
            }
        }
    }

    /**
     * Filtra a lista localmente por:
     * - ID (match exato se o input for numérico)
     * - Nome / Email / NIF (contains, case-insensitive)
     * - (Opcional) parte do ID (contains), permitindo pesquisa parcial por dígitos
     *
     * É chamado:
     * - sempre que o utilizador escreve no campo de pesquisa
     * - após carregar a lista do backend
     *
     * Justificação:
     * - Filtragem client-side melhora responsividade e reduz chamadas redundantes.
     * - A cache allUsers garante que a fonte de verdade local se mantém disponível.
     *
     * Efeito na UI:
     * - Atualiza o adapter com a lista filtrada.
     * - Mostra emptyUsers se não existirem resultados (inclusive quando filtro não encontra matches).
     *
     * @param queryRaw string introduzida pelo utilizador (pode ser null).
     */
    private fun aplicarFiltro(queryRaw: String?) {
        val query = queryRaw?.trim().orEmpty()

        val filtrada = if (query.isBlank()) {
            allUsers
        } else {
            val qLower = query.lowercase()
            val qId = query.toIntOrNull()

            allUsers.filter { u ->
                val nome = u.nome?.lowercase().orEmpty()
                val email = u.email?.lowercase().orEmpty()
                val nif = u.nif?.lowercase().orEmpty()

                // ID exato quando query é numérica.
                (qId != null && u.id == qId) ||
                        // Pesquisa textual.
                        nome.contains(qLower) ||
                        email.contains(qLower) ||
                        nif.contains(qLower) ||
                        // Pesquisa parcial por ID (ex.: "1" encontra "10", "21", etc.).
                        u.id.toString().contains(query)
            }
        }

        // Submissão ao adapter (apresentação no RecyclerView).
        adapter.submitList(filtrada)

        // Estado vazio.
        binding.emptyUsers.visibility = if (filtrada.isEmpty()) View.VISIBLE else View.GONE
    }

    /**
     * Apresenta confirmação antes de eliminar um utilizador.
     *
     * Regras:
     * - Impede que o administrador apague a sua própria conta (regra de segurança operacional).
     * - Caso permitido, mostra AlertDialog com confirmação explícita (ação irreversível).
     *
     * @param u utilizador alvo da eliminação.
     */
    private fun confirmarApagar(u: Utilizador) {
        // Salvaguarda: evita auto-eliminação da conta atualmente autenticada.
        if (session.userId() == u.id) {
            Toast.makeText(requireContext(), "Não podes apagar a tua própria conta.", Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Apagar utilizador")
            .setMessage("Tens a certeza que queres apagar \"${u.nome}\"?\nEsta ação é irreversível.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Apagar") { _, _ -> apagarUtilizador(u) }
            .show()
    }

    /**
     * Elimina o utilizador no backend e atualiza a listagem.
     *
     * Implementação:
     * - Executa em Dispatchers.IO (rede).
     * - Em sucesso: Toast e recarrega a lista a partir do servidor.
     * - Em erro: apresenta mensagem de erro.
     *
     * Nota:
     * - O método recarrega a lista inteira (carregarUtilizadores()) para garantir consistência
     *   entre cliente e servidor após eliminação.
     *
     * @param u utilizador alvo.
     */
    private fun apagarUtilizador(u: Utilizador) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.EliminarUtilizador(u.id)

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Utilizador apagado.", Toast.LENGTH_SHORT).show()
                    carregarUtilizadores()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao apagar: ${e.message ?: "erro"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Libertação do binding quando a View é destruída.
     * Previne memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
