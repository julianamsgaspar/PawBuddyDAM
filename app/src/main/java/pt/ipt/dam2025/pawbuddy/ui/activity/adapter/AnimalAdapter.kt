package pt.ipt.dam2025.pawbuddy.ui.activity

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemAnimalBinding
import pt.ipt.dam2025.pawbuddy.model.Animal

class AnimalAdapter : RecyclerView.Adapter<AnimalAdapter.AnimalViewHolder>() {

    private val animais = mutableListOf<Animal>()

    inner class AnimalViewHolder(private val binding: ItemAnimalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(animal: Animal) {
            binding.tvNome.text = animal.nome
            binding.tvEspecie.text = animal.especie
            binding.tvIdade.text = animal.idade
            // Se tiver imagem, usar Glide ou Picasso
            // Glide.with(binding.ivAnimal.context).load(animal.imagem).into(binding.ivAnimal)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimalViewHolder {
        val binding = ItemAnimalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AnimalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AnimalViewHolder, position: Int) {
        holder.bind(animais[position])
    }

    override fun getItemCount(): Int = animais.size

    fun atualizarLista(novaLista: List<Animal>) {
        animais.clear()
        animais.addAll(novaLista)
        notifyDataSetChanged()
    }
}