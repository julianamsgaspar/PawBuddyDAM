package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemAnimalAdminBinding
import pt.ipt.dam2025.pawbuddy.model.Animal

class AnimalAdminAdapter(
    private val onEdit: (Animal) -> Unit,
    private val onDelete: (Animal) -> Unit
) : RecyclerView.Adapter<AnimalAdminAdapter.VH>() {

    private var items: List<Animal> = emptyList()

    fun submitList(list: List<Animal>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemAnimalAdminBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: Animal) {
            b.tvNome.text = "${a.nome} (ID: ${a.id})"
            b.tvMeta.text = "${a.especie} · ${a.raca} · ${a.genero} · ${a.idade} · ${a.cor}"

            b.btnEditar.setOnClickListener { onEdit(a) }
            b.btnRemover.setOnClickListener { onDelete(a) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemAnimalAdminBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
}
