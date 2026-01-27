package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentEditarEstadoIntencaoBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitProvider
import pt.ipt.dam2025.pawbuddy.retrofit.service.IntencaoDeAdocaoService

/**
 * Fragment responsável por permitir a edição do estado (status) de uma Intenção de Adoção.
 *
 * Enquadramento (UI + dados):
 * - A seleção do estado é feita através de um Spinner (lista dropdown).
 * - Os labels (texto mostrado ao utilizador) e os values (códigos numéricos) são obtidos
 *   de arrays de resources (strings.xml / arrays.xml), suportando internacionalização.
 * - A persistência do novo estado é realizada por chamada a um endpoint Retrofit, numa coroutine
 *   em Dispatchers.IO, com retorno ao Dispatchers.Main para atualizar a UI.
 *
 * Contratos e pré-condições:
 * - Este Fragment espera receber via arguments:
 *   - "id": identificador da intenção (intencaoId)
 *   - "estado": estado atual (inteiro), compatível com os valores definidos em intent_status_values
 *
 * Objetivo funcional:
 * - Mostrar o estado atual pre-selecionado no Spinner.
 * - Permitir ao utilizador escolher um novo estado e confirmar (Guardar).
 * - Enviar para a API o novo estado e regressar ao ecrã anterior em caso de sucesso.
 */
class EditarEstadoIntencaoFragment : Fragment() {

    /**
     * View Binding (nullable) válido apenas entre onCreateView e onDestroyView.
     * Deve ser libertado em onDestroyView para evitar retenção indevida da View (memory leak).
     */
    private var _binding: FragmentEditarEstadoIntencaoBinding? = null

    /**
     * Getter não-null do binding. O uso de "!!" é suportado pelo ciclo de vida:
     * só deve ser acedido quando a View estiver criada.
     */
    private val binding get() = _binding!!

    /**
     * Serviço Retrofit para operações sobre Intenção de Adoção.
     * Assume-se que RetrofitProvider encapsula a configuração do cliente (baseURL, auth, etc.).
     */
    private val api = RetrofitProvider.intencaoService

    /**
     * Identificador da intenção a editar. Inicializado com valor sentinela (-1).
     */
    private var intencaoId: Int = -1

    /**
     * Estado atual da intenção, recebido via arguments.
     * Representa um código numérico (ex.: 0..4) que mapeia para labels de apresentação.
     */
    private var estadoAtual: Int = -1

    /**
     * Fase inicial do ciclo de vida do Fragment.
     * Aqui é apropriado ler os arguments (Bundle) e guardar em variáveis de instância,
     * uma vez que estes valores influenciam a configuração posterior da UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Leitura defensiva dos argumentos. Caso não existam, permanecem os valores sentinela.
        arguments?.let {
            intencaoId = it.getInt("id", -1)
            estadoAtual = it.getInt("estado", -1) // Estado agora interpretado como Int (não String)
        }
    }

    /**
     * Infla o layout do Fragment e inicializa o View Binding.
     *
     * @return View raiz associada ao binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarEstadoIntencaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configuração da UI e listeners após a criação da View.
     *
     * Passos executados:
     * 1) Carrega arrays de recursos:
     *    - labels: texto humanamente legível (ex.: "Pendente", "Aprovada", ...)
     *    - values: códigos numéricos como String (ex.: "0","1","2","3","4")
     * 2) Cria ArrayAdapter para o Spinner com base nos labels.
     * 3) Pré-seleciona o estado atual, procurando o índice em values.
     * 4) Configura botões:
     *    - Guardar: valida ID, extrai novo estado e chama atualizarEstado()
     *    - Cancelar: retorna ao ecrã anterior (popBackStack)
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Arrays de recursos:
        // - labels: o utilizador vê estes textos no Spinner
        // - values: correspondem aos códigos persistidos/enviados para a API
        val labels = resources.getStringArray(R.array.intent_status_labels)
        val values = resources.getStringArray(R.array.intent_status_values) // "0","1","2","3","4"

        // Adapter do Spinner: separa o que é mostrado (labels) do que é enviado (values).
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ).apply {
            // Layout para a lista dropdown do Spinner.
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerEstado.adapter = adapter

        // Pré-seleção do estado atual:
        // Converte estadoAtual para String e procura o índice correspondente em "values".
        // Se existir, define esse índice como seleção inicial do Spinner.
        if (estadoAtual >= 0) {
            val index = values.indexOf(estadoAtual.toString())
            if (index >= 0) binding.spinnerEstado.setSelection(index)
        }

        // Ação Guardar:
        // - Valida o identificador da intenção
        // - Obtém a posição selecionada no Spinner
        // - Converte o value dessa posição para Int
        // - Invoca atualização remota
        binding.btnGuardar.setOnClickListener {
            if (intencaoId <= 0) {
                Toast.makeText(requireContext(), "ID inválido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pos = binding.spinnerEstado.selectedItemPosition
            val novoEstadoInt = values[pos].toInt()
            atualizarEstado(novoEstadoInt)
        }

        // Ação Cancelar:
        // Volta ao ecrã anterior sem alterações.
        binding.btnCancelar.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    /**
     * Envia para o backend a atualização do estado da intenção.
     *
     * Implementação:
     * - Executa chamada de rede em Dispatchers.IO (thread adequada a I/O).
     * - Retorna ao Dispatchers.Main para:
     *   - apresentar feedback (Toast)
     *   - navegar de volta (popBackStack)
     *
     * Observação:
     * - O payload segue o contrato definido em IntencaoDeAdocaoService.EstadoRequest,
     *   encapsulando o valor inteiro do novo estado.
     *
     * @param novoEstado código numérico do estado selecionado no Spinner.
     */
    private fun atualizarEstado(novoEstado: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Chamada à API para atualização do estado.
                api.atualizarEstado(intencaoId, IntencaoDeAdocaoService.EstadoRequest(novoEstado))

                withContext(Dispatchers.Main) {
                    // Feedback de sucesso ao utilizador.
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_intent_status_updated),
                        Toast.LENGTH_SHORT
                    ).show()

                    // Regressa ao ecrã anterior; a lista pode recarregar em onResume, conforme comentário.
                    findNavController().popBackStack()
                }

            } catch (e: Exception) {
                // Tratamento genérico de erro:
                // inclui falhas de rede, erros HTTP, exceções de conversão, etc.
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_update_intent_status,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Libertação do binding quando a View é destruída.
     * Boas práticas em Fragments: a instância do Fragment pode persistir,
     * mas a View deve ser libertada para evitar leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
