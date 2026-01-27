package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentDetalhesUtilizadorBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider

/**
 * Fragment responsável por apresentar os detalhes de um utilizador.
 *
 * Este ecrã pode ser utilizado em dois contextos:
 * 1) Perfil do utilizador autenticado (quando não existe `userId` nos argumentos);
 * 2) Detalhe de um utilizador selecionado a partir de uma lista (quando `fromList = true`),
 *    permitindo também a eliminação do registo.
 *
 * A obtenção de dados é efetuada via Retrofit e corrotinas, garantindo que chamadas de rede
 * decorrem fora da thread principal.
 */
class DetalhesUtilizadorFragment : Fragment() {

    /**
     * Binding associado ao ciclo de vida da View do Fragment (ViewBinding).
     * É libertado em [onDestroyView] para evitar memory leaks.
     */
    private var _binding: FragmentDetalhesUtilizadorBinding? = null
    private val binding get() = _binding!!

    /**
     * Serviço Retrofit para operações sobre utilizadores.
     */
    private val api = RetrofitProvider.utilizadorService

    /**
     * Identificador do utilizador cujos dados serão apresentados.
     * Inicializado com -1 para representar estado inválido/não definido.
     */
    private var userId: Int = -1

    /**
     * Indica se o Fragment foi aberto a partir da lista de utilizadores (contexto de administração).
     * Quando verdadeiro, o botão de eliminação é disponibilizado.
     */
    private var fromList: Boolean = false

    /**
     * Infla o layout do Fragment e inicializa o ViewBinding.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalhesUtilizadorBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura o ecrã após a criação da View:
     * - Determina o utilizador alvo (argumentos ou SharedPreferences);
     * - Configura a visibilidade e ação do botão de eliminação (caso aplicável);
     * - Carrega e apresenta os dados do utilizador via API.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Leitura de argumentos: userId (quando se abre o detalhe a partir de uma lista)
        // e indicador do contexto de navegação (fromList).
        val args = arguments
        val argUserId = args?.getInt("userId", -1) ?: -1
        fromList = args?.getBoolean("fromList", false) ?: false

        // Leitura do utilizador autenticado guardado localmente (fallback para modo "perfil").
        val prefs = requireContext().getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val loggedUserId = prefs.getInt("utilizadorId", -1)

        // Prioriza o ID vindo por argumentos; caso contrário, usa o ID do utilizador autenticado.
        userId = if (argUserId != -1) argUserId else loggedUserId

        // Validação mínima: impede chamadas à API com um ID inválido.
        if (userId == -1) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_invalid_user_id),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Apresenta o ID no topo do ecrã (ex.: suporte a debug e identificação do registo).
        // Nota: `tvUserId` deve existir no layout XML.
        binding.tvUserId.text = "ID: $userId"

        // Em contexto de lista (admin), ativa a funcionalidade de eliminação com confirmação explícita.
        if (fromList) {
            binding.btnEliminar.visibility = View.VISIBLE
            binding.btnEliminar.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.dialog_confirm_title))
                    .setMessage(getString(R.string.dialog_confirm_delete_user))
                    .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                        eliminarUtilizador(userId)
                    }
                    .setNegativeButton(getString(R.string.dialog_no), null)
                    .show()
            }
        } else {
            // Em contexto de perfil, não se expõe a opção de eliminação.
            binding.btnEliminar.visibility = View.GONE
        }

        // Carregamento dos detalhes do utilizador em background (IO), com atualização da UI na Main thread.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val u = api.getUtilizador(userId)
                withContext(Dispatchers.Main) {
                    // Proteção contra atualização da UI caso o Fragment já não esteja anexado.
                    if (!isAdded) return@withContext

                    // Preenchimento dos campos com os dados devolvidos pela API.
                    binding.tvNome.text = u.nome
                    binding.tvEmail.text = u.email
                    binding.tvPais.text = u.pais
                    binding.tvMorada.text = u.morada
                    binding.tvCodPostal.text = u.codPostal
                    binding.tvTelemovel.text = u.telemovel
                    binding.tvNif.text = u.nif
                }
            } catch (e: Exception) {
                // Tratamento genérico de erro (ex.: falha de rede, erro HTTP, parsing, etc.).
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_load_user,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Efetua a eliminação de um utilizador através da API.
     *
     * A operação decorre em IO e, em caso de sucesso, é apresentado um feedback ao utilizador
     * e é efetuada navegação para o ecrã de lista de utilizadores.
     *
     * @param idU identificador do utilizador a eliminar.
     */
    private fun eliminarUtilizador(idU: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.EliminarUtilizador(idU)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_user_deleted),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Após eliminação, regressa ao ecrã de listagem.
                    findNavController().navigate(R.id.listaUtilizadoresFragment)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext

                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_delete_user,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Limpa a referência do binding quando a View é destruída, respeitando o ciclo de vida do Fragment.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
