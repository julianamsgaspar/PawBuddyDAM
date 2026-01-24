package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemIntencaoBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import pt.ipt.dam2025.pawbuddy.utils.EstadoAdocaoMapper

class IntencaoAdapter(
    private val lista: List<IntencaoDeAdocao>,
    private val onClick: (IntencaoDeAdocao) -> Unit,
    private val isAdmin: Boolean,
    private val onEliminar: (IntencaoDeAdocao) -> Unit,
    private val onEditarEstado: (IntencaoDeAdocao) -> Unit
) : RecyclerView.Adapter<IntencaoAdapter.IntencaoViewHolder>() {

    inner class IntencaoViewHolder(val binding: ItemIntencaoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IntencaoDeAdocao) = with(binding) {
            // Textos
            txtAnimal.text = "Animal: ${item.animal?.nome ?: "Desconhecido"}"
            binding.txtEstado.text =
                "Estado: ${EstadoAdocaoMapper.toText(binding.root.context, item.estado)}"



            txtDataIA.text = "Data: ${item.dataIA ?: "-"}"


            // Ações só Admin
            rowAcoesAdmin.visibility = if (isAdmin) View.VISIBLE else View.GONE
            btnEditar.setOnClickListener { onEditarEstado(item) }
            btnEliminar.setOnClickListener { onEliminar(item) }

            // Clique no card abre detalhe read-only
            root.setOnClickListener { onClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntencaoViewHolder {
        val binding = ItemIntencaoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IntencaoViewHolder(binding)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: IntencaoViewHolder, position: Int) {
        holder.bind(lista[position])
    }
}
