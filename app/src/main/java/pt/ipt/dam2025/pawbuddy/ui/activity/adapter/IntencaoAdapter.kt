package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemIntencaoBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import pt.ipt.dam2025.pawbuddy.utils.EstadoAdocaoMapper

/**
 * Adapter responsável por apresentar uma lista de intenções de adoção ([IntencaoDeAdocao])
 * numa [RecyclerView].
 *
 * Enquadramento académico:
 * - Aplica o padrão *ViewHolder* (RecyclerView) para reciclagem eficiente das vistas, reduzindo
 *   overhead de criação e melhorando desempenho em listas com scroll.
 * - Usa *ViewBinding* ([ItemIntencaoBinding]) para acesso tipado às views, reduzindo risco de erros
 *   e aumentando manutenibilidade.
 * - Implementa controlo de visibilidade por perfil (*admin vs não-admin*), materializando um requisito
 *   típico de sistemas com controlo de acesso (RBAC/ABAC no nível de UI).
 * - Mantém separação de responsabilidades ao expor ações via callbacks:
 *   - [onClick] (abrir detalhe read-only)
 *   - [onEliminar] (pedido de remoção)
 *   - [onEditarEstado] (pedido de alteração de estado)
 *
 * Nota:
 * - Este adapter recebe uma lista imutável ([lista]) por construção; atualizações exigem reinstanciar
 *   o adapter ou reatribuir outro na RecyclerView. Isto simplifica o estado interno, mas reduz flexibilidade
 *   para atualizações incrementais.
 *
 * @property lista Lista de intenções a apresentar.
 * @property onClick Callback para clique no item (abrir detalhe).
 * @property isAdmin Indica se o utilizador atual tem permissões administrativas na UI.
 * @property onEliminar Callback invocado ao solicitar eliminação (apenas admin).
 * @property onEditarEstado Callback invocado ao solicitar edição de estado (apenas admin).
 */
class IntencaoAdapter(
    private val lista: List<IntencaoDeAdocao>,
    private val onClick: (IntencaoDeAdocao) -> Unit,
    private val isAdmin: Boolean,
    private val onEliminar: (IntencaoDeAdocao) -> Unit,
    private val onEditarEstado: (IntencaoDeAdocao) -> Unit
) : RecyclerView.Adapter<IntencaoAdapter.IntencaoViewHolder>() {

    /**
     * ViewHolder que encapsula e gere o *binding* de um item de intenção.
     *
     * Responsabilidades:
     * - Mapear atributos do modelo [IntencaoDeAdocao] para a UI.
     * - Aplicar regras de visibilidade conforme [isAdmin].
     * - Encaminhar eventos de interação para o exterior via callbacks.
     */
    inner class IntencaoViewHolder(val binding: ItemIntencaoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Associa os dados da intenção de adoção às views do layout.
         *
         * Boas práticas e decisões de implementação:
         * - Tratamento defensivo de valores nulos (safe calls + Elvis operator).
         * - Tradução/normalização do estado através de [EstadoAdocaoMapper], reduzindo acoplamento
         *   entre o enum/inteiro persistido e a representação textual na UI (i18n-friendly).
         * - Visibilidade de campos sensíveis (identificação do utilizador e ações administrativas)
         *   controlada por [isAdmin].
         * - Clique no card invoca [onClick] para navegação/detalhe read-only, mantendo o adapter
         *   agnóstico a navegação e ciclo de vida.
         *
         *
         * @param item Intenção de adoção a apresentar.
         */
        fun bind(item: IntencaoDeAdocao) = with(binding) {
            // Textos
            /**
             * Nome do animal associado:
             * - Mostra o nome quando disponível.
             * - Caso contrário, apresenta "Desconhecido" como fallback.
             */
            txtAnimal.text = "Animal: ${item.animal?.nome ?: "Desconhecido"}"

            /**
             * Estado:
             * - Converte o valor persistido (ex.: enum/int) para texto amigável através do mapper.
             * - Esta abordagem melhora manutenção e facilita internacionalização futura.
             */
            binding.txtEstado.text =
                "Estado: ${EstadoAdocaoMapper.toText(binding.root.context, item.estado)}"

            /**
             * Data:
             * - Usa o valor do payload quando existe, caso contrário apresenta "-".
             * - Não há formatação/normalização de data aqui; assume-se que o backend fornece formato já legível.
             */
            txtDataIA.text = "Data: ${item.dataIA ?: "-"}"

            // Mostrar utilizador só para Admin (e só se existir no payload)
            /**
             * Identificação do utilizador (admin):
             *
             * Estratégia de fallback em cascata:
             * 1) nome
             * 2) id
             * 3) email
             * 4) "Desconhecido"
             *
             * Justificação:
             * - Em APIs com payloads parciais, o utilizador pode vir incompleto; a cascata reduz
             *   ocorrência de campos vazios e melhora rastreabilidade no contexto administrativo.
             *
             * Nota:
             * - A escolha do identificador a apresentar é uma decisão de UX e privacidade; aqui assume-se
             *   que em modo admin é aceitável apresentar email/id conforme disponibilidade.
             */
            val nomeUtilizador =
                item.utilizador?.nome
                    ?: item.utilizador?.id
                    ?: item.utilizador?.email
                    ?: "Desconhecido"

            /**
             * Visibilidade do campo "Utilizador":
             * - Admin: VISIBLE
             * - Não-admin: GONE (não ocupa espaço)
             */
            txtUtilizador.visibility = if (isAdmin) View.VISIBLE else View.GONE
            txtUtilizador.text = "Utilizador: $nomeUtilizador"

            // Ações só Admin
            /**
             * Visibilidade da linha de ações administrativas:
             * - Admin: VISIBLE
             * - Não-admin: GONE
             *
             */
            rowAcoesAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE

            /**
             * Listeners das ações:
             * - Edição de estado (btnEditar)
             * - Eliminação (btnEliminar)
             *
             * O adapter apenas sinaliza a intenção; a execução real (API, confirmação, tratamento de erros)
             * deve residir no Fragment/ViewModel.
             */
            btnEditar.setOnClickListener { onEditarEstado(item) }
            btnEliminar.setOnClickListener { onEliminar(item) }

            // Clique no card abre detalhe read-only
            /**
             * Clique no item:
             * - Encaminha a intenção de abrir detalhe read-only.
             * - Mantém o adapter desacoplado de navegação.
             */
            root.setOnClickListener { onClick(item) }
        }
    }

    /**
     * Infla o layout do item e cria o respetivo [IntencaoViewHolder].
     *
     * @param parent Contentor onde o item será inserido.
     * @param viewType Tipo de vista (não utilizado para múltiplos tipos neste adapter).
     * @return ViewHolder inicializado com ViewBinding.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntencaoViewHolder {
        val binding = ItemIntencaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IntencaoViewHolder(binding)
    }

    /**
     * Devolve o número total de intenções apresentadas.
     */
    override fun getItemCount(): Int = lista.size

    /**
     * Realiza o binding do item na posição [position] ao [holder].
     */
    override fun onBindViewHolder(holder: IntencaoViewHolder, position: Int) {
        holder.bind(lista[position])
    }
}
