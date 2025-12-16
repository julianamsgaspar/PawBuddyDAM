package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemAdocaoBinding
import pt.ipt.dam2025.pawbuddy.model.Adotam
import java.text.SimpleDateFormat
import java.util.Locale

class AdocaoAdapter(
    private val lista: List<Adotam>,
    private val onEliminarClick: (Adotam) -> Unit
) : RecyclerView.Adapter<AdocaoAdapter.AdocaoViewHolder>() {

    inner class AdocaoViewHolder(val binding: ItemAdocaoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdocaoViewHolder {
        val binding = ItemAdocaoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdocaoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdocaoViewHolder, position: Int) {
        val adocao = lista[position]

        holder.binding.tvNomeAnimal.text = adocao.animal?.nome ?: "—"
        holder.binding.tvAnimalInfo.text =
            "${adocao.animal?.especie ?: ""} • ${adocao.animal?.raca ?: ""}"

        holder.binding.tvUtilizador.text =
            "Adotado por: ${adocao.utilizador?.nome ?: "—"}"

        holder.binding.tvData.text =
            "Data: ${formatarData(adocao.dateA)}"

        // Botão eliminar
        holder.binding.btnEliminar.setOnClickListener {
            onEliminarClick(adocao)
        }
    }

    override fun getItemCount(): Int = lista.size

    private fun formatarData(data: String): String {
        return try {
            val input = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            )
            val output = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            output.format(input.parse(data)!!)
        } catch (e: Exception) {
            data
        }
    }
}