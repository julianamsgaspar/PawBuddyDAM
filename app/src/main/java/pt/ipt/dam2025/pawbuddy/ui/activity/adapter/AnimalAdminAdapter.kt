package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemAnimalAdminBinding
import pt.ipt.dam2025.pawbuddy.model.Animal

/**
 * Adapter para gestão administrativa de animais numa [RecyclerView].
 *
 * Enquadramento académico:
 * - Este adapter destina-se a um contexto de backoffice/administração, no qual é útil expor metadados
 *   completos do registo (incluindo o identificador [Animal.id]) e disponibilizar ações diretas por item
 *   (editar e remover).
 * - Aplica o padrão ViewHolder (nativo da RecyclerView) e usa ViewBinding ([ItemAnimalAdminBinding])
 *   para acesso tipado e seguro às views.
 * - As ações (editar/remover) são delegadas ao exterior via *callbacks* ([onEdit], [onDelete]),
 *   garantindo separação de responsabilidades: o adapter não executa lógica de negócio nem chamadas de rede.
 *
 * @param onEdit Callback invocado quando o utilizador solicita editar um animal.
 * @param onDelete Callback invocado quando o utilizador solicita remover um animal.
 */
class AnimalAdminAdapter(
    private val onEdit: (Animal) -> Unit,
    private val onDelete: (Animal) -> Unit
) : RecyclerView.Adapter<AnimalAdminAdapter.VH>() {

    /**
     * Lista interna de itens apresentada pela RecyclerView.
     *
     * Nota:
     * - Inicializada como lista vazia para evitar nullability.
     * - Mantida como [List] para restringir alterações fora do adapter.
     */
    private var items: List<Animal> = emptyList()

    /**
     * Atualiza a lista de itens e força redesenho total.
     *
     * Observação académica:
     * - notifyDataSetChanged() é simples e funcional, mas menos eficiente para alterações parciais.
     * - Em cenários com muitas atualizações, recomenda-se DiffUtil/ListAdapter para atualizações incrementais.
     *
     * @param list Nova lista de [Animal] a apresentar.
     */
    fun submitList(list: List<Animal>) {
        items = list
        notifyDataSetChanged()
    }

    /**
     * ViewHolder que encapsula as vistas de um item administrativo, via [ItemAnimalAdminBinding].
     *
     * Responsabilidades:
     * - Efetuar binding dos dados do modelo [Animal] para a UI.
     * - Registar listeners de eventos (botões de editar e remover) e encaminhá-los via callbacks.
     */
    inner class VH(private val b: ItemAnimalAdminBinding) : RecyclerView.ViewHolder(b.root) {

        /**
         * Associa os dados do animal às views do item.
         *
         * Estratégia de apresentação:
         * - Linha principal: nome + identificador (útil para referência inequívoca no contexto admin).
         * - Linha secundária: metadados concatenados ("Espécie · Raça · Género · Idade · Cor").
         *
         *
         * @param a Instância de [Animal] a apresentar no item.
         */
        fun bind(a: Animal) {
            /**
             * Apresenta o nome do animal e o seu ID persistente.
             * Em cenários administrativos, o ID facilita operações (auditoria, depuração, suporte).
             */
            b.tvNome.text = "${a.nome} (ID: ${a.id})"

            /**
             * Apresenta uma linha compacta de atributos.
             * O separador "·" melhora legibilidade e reduz ocupação de espaço vertical.
             */
            b.tvMeta.text = "${a.especie} · ${a.raca} · ${a.genero} · ${a.idade} · ${a.cor}"

            /**
             * Eventos de interação:
             * - Encaminha a intenção do utilizador para a camada superior (Fragment/Activity/ViewModel).
             * - Evita acoplamento do adapter a navegação, persistência ou chamadas ao backend.
             */
            b.btnEditar.setOnClickListener { onEdit(a) }
            b.btnRemover.setOnClickListener { onDelete(a) }
        }
    }

    /**
     * Infla o layout do item administrativo e cria o respetivo [VH].
     *
     * @param parent Contentor da RecyclerView.
     * @param viewType Tipo de vista (não utilizado para múltiplos tipos neste adapter).
     * @return ViewHolder inicializado com binding.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAnimalAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    /**
     * Número de itens disponíveis para apresentação.
     */
    override fun getItemCount() = items.size

    /**
     * Realiza o binding do item na posição [position] ao ViewHolder.
     *
     * Nota:
     * - O método delega a lógica de binding para [VH.bind], promovendo coesão.
     */
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
}
