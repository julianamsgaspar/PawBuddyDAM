package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemUtilizadorBinding
import pt.ipt.dam2025.pawbuddy.model.Utilizador

/**
 * Adapter para apresentar uma lista de [Utilizador] num RecyclerView.
 *
 * Responsabilidades:
 * - Fazer bind dos dados do utilizador para o layout item (ItemUtilizadorBinding).
 * - Expor callbacks para:
 *   - clique no item (abrir detalhe / editar / ver perfil)
 *   - clique no botão de eliminar
 *
 * Nota:
 * - Este adapter usa uma lista interna simples e chama notifyDataSetChanged() em submitList().
 *   Para listas grandes, o ideal seria usar ListAdapter + DiffUtil para updates eficientes.
 *
 * @param onClick Callback chamado quando o utilizador clica no item (card/linha).
 * @param onDeleteClick Callback chamado quando o utilizador clica no botão "delete".
 */
class UtilizadorAdapter(
    private val onClick: (Utilizador) -> Unit,
    private val onDeleteClick: (Utilizador) -> Unit
) : RecyclerView.Adapter<UtilizadorAdapter.VH>() {

    /**
     * Lista interna de utilizadores atualmente apresentada.
     * Inicialmente vazia.
     */
    private var items: List<Utilizador> = emptyList()

    /**
     * Substitui a lista atual por uma nova lista e atualiza a UI.
     *
     * @param list Lista de [Utilizador] a apresentar.
     */
    fun submitList(list: List<Utilizador>) {
        items = list
        // Atualização "bruta": redesenha tudo.
        // Para melhor performance (e animações), usa DiffUtil.
        notifyDataSetChanged()
    }

    /**
     * ViewHolder responsável por mapear um [Utilizador] para as views do item.
     *
     * @property b Binding gerado para o layout item_utilizador.xml
     */
    inner class VH(private val b: ItemUtilizadorBinding) : RecyclerView.ViewHolder(b.root) {

        /**
         * Atribui os valores do [Utilizador] às views do layout e configura listeners.
         *
         * Regras de apresentação:
         * - Nome e email: mostram string vazia se vier null.
         * - Meta: mostra ID e NIF, usando "—" quando o NIF é null.
         *
         * Interação:
         * - Clique no item -> chama [onClick]
         * - Clique no botão delete -> chama [onDeleteClick]
         *
         * @param u Utilizador a apresentar no item.
         */
        fun bind(u: Utilizador) {
            // Nome (fallback para string vazia caso null)
            b.tvUserName.text = u.nome ?: ""

            // Email (fallback para string vazia caso null)
            b.tvUserEmail.text = u.email ?: ""

            // Informação adicional (ID e NIF)
            b.tvUserMeta.text = "ID: ${u.id} · NIF: ${u.nif ?: "—"}"

            // Clique no item (ex.: abrir detalhes do utilizador)
            b.root.setOnClickListener { onClick(u) }

            // Clique no botão de eliminar
            b.btnDeleteUser.setOnClickListener { onDeleteClick(u) }
        }
    }

    /**
     * Cria e devolve o ViewHolder.
     *
     * @param parent ViewGroup que contém os itens do RecyclerView.
     * @param viewType Tipo de view (não usado neste adapter, pois há apenas 1 tipo de item).
     * @return Instância de [VH] com o binding inflado.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemUtilizadorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    /**
     * @return Número total de itens atualmente na lista.
     */
    override fun getItemCount(): Int = items.size

    /**
     * Faz bind do item na posição [position] ao [holder].
     *
     * @param holder ViewHolder reutilizado pelo RecyclerView.
     * @param position Posição do item na lista interna [items].
     */
    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }
}
