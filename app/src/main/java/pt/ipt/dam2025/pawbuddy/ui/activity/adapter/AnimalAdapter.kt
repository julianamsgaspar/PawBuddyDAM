package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.ItemAnimalBinding
import pt.ipt.dam2025.pawbuddy.model.Animal
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

class AnimalAdapter(
    private val onClick: (Animal) -> Unit
) : RecyclerView.Adapter<AnimalAdapter.ViewHolder>() {

    private var lista: List<Animal> = emptyList()

    fun submitList(novaLista: List<Animal>) {
        lista = novaLista
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemAnimalBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(animal: Animal) {
            // Nome (linha 1)
            binding.tvNome.text = animal.nome ?: ""

            // Linha 2: Espécie · Raça (compacto)
            val especie = animal.especie ?: ""
            val raca = animal.raca ?: ""
            binding.tvEspecie.text = when {
                especie.isNotBlank() && raca.isNotBlank() -> "$especie · $raca"
                especie.isNotBlank() -> especie
                raca.isNotBlank() -> raca
                else -> ""
            }

            // Linha 3: Idade · Género (compacto)
            val idade = animal.idade?.toString() ?: ""
            val genero = animal.genero ?: ""
            binding.tvIdade.text = when {
                idade.isNotBlank() && genero.isNotBlank() -> "$idade anos · $genero"
                idade.isNotBlank() -> "$idade anos"
                genero.isNotBlank() -> genero
                else -> ""
            }

            // Campos “longos” (no grid ficam feios) — só se existirem no layout
            // Se no teu novo XML estes TextViews estiverem GONE, não faz mal setar ou não.
            binding.tvRaca.text = ""    // ou remove completamente se no XML estiver gone
            binding.tvCor.text = ""
            binding.tvGenero.text = ""

            // Imagem
            Glide.with(binding.ivAnimal.context)
                .load(RetrofitInitializer.fullImageUrl(animal.imagem.toString()))
                .placeholder(R.drawable.animal0)
                .error(R.drawable.ic_pet_placeholder)
                .centerCrop()
                .into(binding.ivAnimal)

            binding.root.setOnClickListener { onClick(animal) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnimalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }
}
