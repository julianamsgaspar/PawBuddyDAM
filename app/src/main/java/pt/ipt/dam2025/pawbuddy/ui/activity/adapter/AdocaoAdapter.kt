package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemAdocaoBinding
import pt.ipt.dam2025.pawbuddy.model.Adotam
import pt.ipt.dam2025.pawbuddy.R
import java.text.SimpleDateFormat
import java.util.Locale

class AdocaoAdapter(
    private var lista: List<Adotam>,
    private val onEliminarClick: (Adotam) -> Unit
) : RecyclerView.Adapter<AdocaoAdapter.AdocaoViewHolder>() {

    init {
        // ✅ IDs estáveis: vamos usar animalFK como identificador único da adoção
        setHasStableIds(true)
    }

    inner class AdocaoViewHolder(val binding: ItemAdocaoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemId(position: Int): Long {
        // ✅ chave única (enquanto o backend devolve id=0)
        return lista[position].animalFK.toLong()
    }

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
        val ctx = holder.itemView.context

        val dash = ctx.getString(R.string.placeholder_dash)

        holder.binding.tvNomeAnimal.text = adocao.animal?.nome ?: dash

        // Evita " • " quando campos estão vazios
        val especie = adocao.animal?.especie?.trim().orEmpty()
        val raca = adocao.animal?.raca?.trim().orEmpty()
        val info = listOf(especie, raca).filter { it.isNotBlank() }

        holder.binding.tvAnimalInfo.text =
            if (info.isEmpty()) dash else info.joinToString(ctx.getString(R.string.separator_dot))

        val nomeUser = adocao.utilizador?.nome ?: dash
        holder.binding.tvUtilizador.text = ctx.getString(R.string.label_adopted_by, nomeUser)

        val dataFmt = formatarData(adocao.dateA)
        holder.binding.tvData.text = ctx.getString(R.string.label_date, dataFmt)

        holder.binding.btnEliminar.setOnClickListener {
            // ✅ o fragment é que deve eliminar pelo animalFK
            onEliminarClick(adocao)
        }
    }

    override fun getItemCount(): Int = lista.size
    fun updateData(novaLista: List<Adotam>) {
        this.lista = novaLista
        notifyDataSetChanged()
    }

    private fun formatarData(data: String?): String {
        if (data.isNullOrBlank()) return ""

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        )

        for (pattern in formats) {
            try {
                val input = SimpleDateFormat(pattern, Locale.getDefault())
                val output = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                return output.format(input.parse(data)!!)
            } catch (_: Exception) {
                // tenta o próximo
            }
        }
        return data
    }
}
