package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.ItemAnimalBinding
import pt.ipt.dam2025.pawbuddy.model.Animal
import android.view.LayoutInflater
import android.net.Uri
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

class AnimalAdapter(
    private val lista: List<Animal>,
    private val onClick: (Animal) -> Unit
) : RecyclerView.Adapter<AnimalAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAnimalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(animal: Animal) {
            binding.tvNome.text = animal.nome
            binding.tvRaca.text = "Raça: ${animal.raca}"
            binding.tvIdade.text = "Idade: ${animal.idade}"
            binding.tvCor.text = "Cor: ${animal.cor}"
            binding.tvGenero.text = "Gênero: ${animal.genero}"
            binding.tvEspecie.text = "Espécie: ${animal.especie}"

            /*val baseUrl = "http://10.0.2.2:5053/"  // substitua pelo seu domínio real
            val imageUrl = baseUrl + animal.imagem    // animal.imagem = "images/animal14.jpg"*/

            // -----------------------------
            // Carregar imagem da API com Glide
            // -----------------------------
            Glide.with(binding.ivAnimal.context)
                .load(RetrofitInitializer.fullImageUrl(animal.imagem.toString())) // URL da API
                .placeholder(R.drawable.animal0)
                .error(R.drawable.ic_pet_placeholder) // se falhar
                .centerCrop()
                .into(binding.ivAnimal)

            binding.root.setOnClickListener {
                onClick(animal)
            }
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