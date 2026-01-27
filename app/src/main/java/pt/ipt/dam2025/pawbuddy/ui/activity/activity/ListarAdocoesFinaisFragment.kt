package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.app.AlertDialog
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
import pt.ipt.dam2025.pawbuddy.databinding.FragmentListarAdocoesFinaisBinding
import pt.ipt.dam2025.pawbuddy.model.Adotam
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.ui.activity.adapter.AdocaoAdapter
import pt.ipt.dam2025.pawbuddy.session.SessionManager

/**
 * Fragment responsável por listar adoções finalizadas (registos em {@link Adotam}).
 *
 * Este ecrã está restrito a administradores, pois inclui ações destrutivas (eliminação de registos).
 * A listagem é obtida a partir da API via Retrofit e processada com corrotinas:
 * - chamadas de rede em {@code Dispatchers.IO}
 * - atualização de UI em {@code Dispatchers.Main}
 *
 * O RecyclerView utiliza um {@link LinearLayoutManager} e um {@link AdocaoAdapter} com callback
 * para confirmação e execução da eliminação.
 */
class ListarAdocoesFinaisFragment : Fragment() {

    /**
     * ViewBinding associado à view do fragment. Deve ser libertado em {@link #onDestroyView()}
     * para evitar manter referências à UI após destruição.
     */
    private var _binding: FragmentListarAdocoesFinaisBinding? = null
    private val binding get() = _binding!!

    /**
     * Serviço Retrofit para operações sobre adoções.
     */
    private val api = RetrofitProvider.adotamService

    /**
     * Adapter do RecyclerView para apresentação das adoções finalizadas.
     */
    private lateinit var adapter: AdocaoAdapter

    /**
     * Gestor de sessão (autenticação e permissões). Lazy para garantir acesso válido ao contexto.
     */
    private val session by lazy { SessionManager(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListarAdocoesFinaisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Controlo de acesso: apenas administradores podem visualizar e gerir adoções finalizadas.
        if (!session.isLogged() || !session.isAdmin()) {
            Toast.makeText(requireContext(), "Acesso reservado a administradores.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        // Configuração do RecyclerView (lista vertical).
        binding.rvAdocoes.layoutManager = LinearLayoutManager(requireContext())

        // Inicialização do adapter com callback para ação de eliminação (com confirmação).
        adapter = AdocaoAdapter(emptyList()) { adocao ->
            confirmarEliminar(adocao)
        }
        binding.rvAdocoes.adapter = adapter

        // Carregamento inicial dos dados.
        carregarAdocoes()
    }

    override fun onResume() {
        super.onResume()

        // Revalidação de sessão/permissões ao regressar ao ecrã (ex.: expiração enquanto a app estava em background).
        if (!session.isLogged() || !session.isAdmin()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Sessão expirada ou sem permissões.", Toast.LENGTH_LONG).show()
                findNavController().navigate(
                    R.id.loginFragment,
                    Bundle().apply {
                        putBoolean("returnToPrevious", true)
                        putString("origin", "admin_adoptions")
                        putInt("originId", -1)
                    }
                )
            }
            return
        }

        // Atualização dos dados, garantindo consistência após navegações/alterações.
        carregarAdocoes()
    }

    /**
     * Obtém a lista de adoções finalizadas a partir da API e atualiza o adapter.
     *
     * A lista é pós-processada com {@code distinctBy { it.animalFK }} para remover potenciais duplicados
     * por animal (isto é, garante no máximo um registo por identificador de animal).
     *
     * Em caso de exceção, aplica a política definida no fragment: assume sessão inválida,
     * efetua logout e redireciona para login.
     */
    private fun carregarAdocoes() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val lista = api.getAdocoes()
                    .distinctBy { it.animalFK }

                withContext(Dispatchers.Main) {
                    adapter.updateData(lista)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Política de recuperação: falha na chamada → invalida sessão local e força reautenticação.
                    session.logout()

                    Toast.makeText(
                        requireContext(),
                        "Sessão expirada. Faz login novamente.",
                        Toast.LENGTH_LONG
                    ).show()

                    if (isAdded) {
                        findNavController().navigate(
                            R.id.loginFragment,
                            Bundle().apply {
                                putBoolean("returnToPrevious", true)
                                putString("origin", "admin_adoptions")
                                putInt("originId", -1)
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Apresenta um diálogo de confirmação antes de eliminar um registo de adoção.
     *
     * Implementa uma verificação adicional de permissões no momento do clique, evitando que a ação
     * destrutiva seja executada caso a sessão tenha expirado ou o utilizador tenha perdido permissões.
     *
     * @param adocao registo de adoção selecionado.
     */
    private fun confirmarEliminar(adocao: Adotam) {
        // Hardening: valida novamente permissões antes de permitir a ação destrutiva.
        if (!session.isLogged() || !session.isAdmin()) {
            Toast.makeText(requireContext(), "Sessão expirada ou sem permissões.", Toast.LENGTH_LONG).show()
            return
        }

        val nomeAnimal = adocao.animal?.nome ?: "?"
        val animalId = adocao.animalFK

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.dialog_confirm_title))
            .setMessage(getString(R.string.dialog_delete_adoption_message, nomeAnimal))
            .setPositiveButton(getString(R.string.dialog_delete)) { _, _ ->
                eliminarAdocao(animalId)
            }
            .setNegativeButton(getString(R.string.dialog_cancel), null)
            .show()
    }

    /**
     * Executa a eliminação do registo de adoção para um dado animal.
     *
     * Em caso de sucesso, recarrega a lista para refletir a alteração.
     * Em caso de erro, aplica a política do fragment: logout e navegação para login.
     *
     * @param animalId identificador do animal associado ao registo de adoção.
     */
    private fun eliminarAdocao(animalId: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.deleteAdocao(animalId)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_adoption_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    carregarAdocoes()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Política de recuperação: falha → invalida sessão local e força reautenticação.
                    session.logout()

                    Toast.makeText(
                        requireContext(),
                        "Sessão expirada. Faz login novamente.",
                        Toast.LENGTH_LONG
                    ).show()

                    if (isAdded) {
                        findNavController().navigate(
                            R.id.loginFragment,
                            Bundle().apply {
                                putBoolean("returnToPrevious", true)
                                putString("origin", "admin_adoptions")
                                putInt("originId", -1)
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Libertação do binding para evitar memory leaks.
        _binding = null
    }
}
