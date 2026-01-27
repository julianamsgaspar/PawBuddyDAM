package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentIntencaoDetalheBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import pt.ipt.dam2025.pawbuddy.utils.EstadoAdocaoMapper
import retrofit2.HttpException

/**
 * Fragment responsável pela apresentação detalhada de uma Intenção de Adoção.
 *
 * Objetivo:
 * - Consultar e apresentar os atributos de uma intenção (estado, data, animal, dados do candidato, etc.).
 * - Ajustar a UI em função do papel do utilizador (Admin vs Utilizador comum).
 * - Para Admin, expor informação adicional (identificação do utilizador) e permitir copiar o ID.
 *
 * Enquadramento arquitetural:
 * - View Binding para acesso seguro aos componentes do layout.
 * - Retrofit (via RetrofitProvider) para acesso a dados remotos.
 * - Coroutines + viewLifecycleOwner.lifecycleScope para executar rede em background,
 *   garantindo cancelamento quando a View é destruída.
 * - SessionManager para controlo do estado de autenticação e permissões (isLogged/isAdmin/logout).
 * - Mapper utilitário (EstadoAdocaoMapper) para conversão do estado numérico/enum em texto localizado.
 *
 * Considerações de robustez:
 * - Valida pré-condições (sessão ativa e id válido) antes de fazer chamadas ao backend.
 * - Trata erros HTTP (401/403) com logout e redirecionamento para login.
 * - Trata falhas gerais com feedback ao utilizador e regressa ao ecrã anterior.
 */
class IntencaoDetalheFragment : Fragment() {

    /**
     * Referência nullable ao binding, válida apenas entre onCreateView e onDestroyView.
     */
    private var _binding: FragmentIntencaoDetalheBinding? = null

    /**
     * Getter não-null do binding; assume acesso apenas com a View ativa.
     */
    private val binding get() = _binding!!

    /**
     * Serviço Retrofit para operações sobre Intenção de Adoção.
     */
    private val api = RetrofitProvider.intencaoService

    /**
     * Gestor de sessão. Criado de forma lazy para adiar a inicialização até ser necessário
     * e para garantir um Context válido (requireContext()).
     */
    private val session by lazy { SessionManager(requireContext()) }

    /**
     * Identificador da intenção a consultar. Inicializado com valor sentinela (-1).
     */
    private var intencaoId: Int = -1

    /**
     * Leitura de argumentos na fase inicial do ciclo de vida.
     * O Fragment espera receber "id" via arguments (Bundle) aquando da navegação.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intencaoId = arguments?.getInt("id", -1) ?: -1
    }

    /**
     * Infla o layout e inicializa o View Binding.
     *
     * @return View raiz do Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntencaoDetalheBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configuração inicial após a View estar criada.
     *
     * Pré-condições:
     * - Utilizador deve estar autenticado (session.isLogged()).
     * - intencaoId deve ser válido (> 0).
     *
     * Se as pré-condições falharem:
     * - Apresenta mensagem informativa
     * - Redireciona para o ecrã de login
     *
     * Caso contrário, inicia o carregamento dos dados.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!session.isLogged() || intencaoId <= 0) {
            Toast.makeText(
                requireContext(),
                getString(R.string.error_login_required),
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigate(R.id.loginFragment)
            return
        }

        carregarDetalhe()
    }

    /**
     * Carrega os detalhes da intenção a partir do backend e atualiza a UI.
     *
     * Implementação:
     * - Coroutine em Dispatchers.IO para chamada de rede.
     * - withContext(Dispatchers.Main) para atualizar componentes visuais.
     *
     * Regras de apresentação:
     * - Campos nulos/ausentes são substituídos por placeholders ("-" ou "Desconhecido").
     * - Campo estado é traduzido para texto legível através de EstadoAdocaoMapper.
     * - Informação do utilizador e ID só são visíveis para Admin.
     * - Admin pode copiar o ID do utilizador via long click (ClipboardManager).
     *
     * Tratamento de erros:
     * - HttpException 401/403: termina sessão e força novo login (sessão expirada/sem permissões).
     * - Outros HTTP: assume intenção inexistente/erro e volta ao ecrã anterior.
     * - Exceções genéricas: feedback e navigateUp().
     */
    private fun carregarDetalhe() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obtenção do detalhe da intenção pelo seu identificador.
                val intencao = api.getByIntencaoId(intencaoId)

                withContext(Dispatchers.Main) {

                    // Estado: convertido para texto (localizado) via mapper.
                    // Nota: evita apresentar valores "crus" (ex.: números) ao utilizador final.
                    binding.txtEstado.text =
                        EstadoAdocaoMapper.toText(requireContext(), intencao.estado)

                    // Data: usa placeholder caso a data não exista.
                    binding.txtData.text = intencao.dataIA ?: "-"

                    // Dados do animal e da intenção.
                    binding.txtAnimal.text = intencao.animal?.nome ?: "Desconhecido"
                    binding.txtProfissao.text = intencao.profissao ?: "-"
                    binding.txtResidencia.text = intencao.residencia ?: "-"

                    // temAnimais é String -> normaliza entradas heterogéneas (sim/nao/true/false/1/0, etc.).
                    binding.txtTemAnimais.text = formatTemAnimais(intencao.temAnimais)

                    binding.txtQuaisAnimais.text = intencao.quaisAnimais ?: "-"
                    binding.txtMotivo.text = intencao.motivo ?: "-"

                    val isAdmin = session.isAdmin()

                    // Elementos exclusivos do Admin (labels e valores).
                    // Para utilizador comum, ficam escondidos para evitar exposição indevida de dados.
                    binding.lblUtilizador.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    binding.txtUtilizador.visibility = if (isAdmin) View.VISIBLE else View.GONE

                    binding.lblUtilizadorId.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    binding.txtUtilizadorId.visibility = if (isAdmin) View.VISIBLE else View.GONE

                    if (isAdmin) {
                        val utilizador = intencao.utilizador

                        // Política de seleção do identificador "humano" do utilizador:
                        // 1) Preferir nome (quando disponível)
                        // 2) Caso contrário, usar email
                        // 3) Se nenhum existir, mostrar "Desconhecido"
                        val nomeUtilizador = when {
                            !utilizador?.nome.isNullOrBlank() -> utilizador?.nome!!
                            !utilizador?.email.isNullOrBlank() -> utilizador?.email!!
                            else -> "Desconhecido"
                        }

                        binding.txtUtilizador.text = nomeUtilizador
                        binding.txtUtilizadorId.text = utilizador?.id?.toString() ?: "-"

                        // Long click no ID do utilizador: copia para a área de transferência.
                        // Útil para tarefas administrativas (ex.: pesquisa por ID, auditoria, suporte).
                        binding.txtUtilizadorId.setOnLongClickListener {
                            val clipboard = requireContext()
                                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                            val clip = ClipData.newPlainText("User ID", binding.txtUtilizadorId.text)
                            clipboard.setPrimaryClip(clip)

                            Toast.makeText(requireContext(), "ID copiado", Toast.LENGTH_SHORT).show()
                            true
                        }
                    } else {
                        // Higiene de UI/estado:
                        // Mesmo estando escondidos, limpa valores e listeners para evitar inconsistências
                        // em cenários de reutilização/reattach do Fragment/View.
                        binding.txtUtilizador.text = ""
                        binding.txtUtilizadorId.text = ""
                        binding.txtUtilizadorId.setOnLongClickListener(null)
                    }
                }

            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    if (e.code() == 401 || e.code() == 403) {
                        // Falha de autenticação/autorização:
                        // Estratégia: terminar sessão local e forçar reautenticação.
                        session.logout()
                        Toast.makeText(
                            requireContext(),
                            "Sessão expirada. Faz login novamente.",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigate(R.id.loginFragment)
                    } else {
                        // Outros códigos HTTP: assume intenção inexistente ou erro de acesso.
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_intent_not_found),
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigateUp()
                    }
                }
            } catch (e: Exception) {
                // Falha genérica (ex.: timeout, parsing, problemas de conectividade, etc.).
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_intent_not_found),
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    /**
     * Normaliza o campo "temAnimais" quando este chega como String a partir do backend/UI.
     *
     * Problema típico:
     * - Em integrações, valores booleanos podem vir em múltiplos formatos (sim/não, true/false, 1/0, yes/no).
     *
     * Estratégia:
     * - Sanitiza o input: trim + lowercase.
     * - Mapeia sinónimos para "Sim" ou "Não".
     * - Se não reconhecer, devolve o valor original limpo (preserva informação inesperada).
     * - Para null ou vazio, devolve "-".
     *
     * @param value valor textual potencialmente heterogéneo.
     * @return string normalizada para apresentação ao utilizador.
     */
    private fun formatTemAnimais(value: String?): String {
        val v = value?.trim()?.lowercase()
        if (v.isNullOrBlank()) return "-"

        return when (v) {
            "sim", "s", "true", "1", "yes", "y" -> "Sim"
            "nao", "não", "n", "false", "0", "no" -> "Não"
            else -> value.trim()
        }
    }

    /**
     * Libertação do binding quando a View é destruída.
     * Boa prática essencial em Fragments para evitar retenção da árvore de Views.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
