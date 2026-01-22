package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemUtilizadorBinding
import pt.ipt.dam2025.pawbuddy.model.Utilizador

class UtilizadorAdapter(
    private val onClick: (Utilizador) -> Unit,
    private val onDeleteClick: (Utilizador) -> Unit
) : RecyclerView.Adapter<UtilizadorAdapter.VH>() {

    private var items: List<Utilizador> = emptyList()

    fun submitList(list: List<Utilizador>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemUtilizadorBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(u: Utilizador) {
            b.tvUserName.text = u.nome ?: ""
            b.tvUserEmail.text = u.email ?: ""
            b.tvUserMeta.text = "ID: ${u.id} · NIF: ${u.nif ?: "—"}"

            b.root.setOnClickListener { onClick(u) }
            b.btnDeleteUser.setOnClickListener { onDeleteClick(u) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemUtilizadorBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }
}
