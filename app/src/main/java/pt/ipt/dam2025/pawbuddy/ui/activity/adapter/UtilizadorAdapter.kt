package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.databinding.FragmentDetalhesUtilizadorBinding
import pt.ipt.dam2025.pawbuddy.databinding.ItemUtilizadorBinding
import pt.ipt.dam2025.pawbuddy.model.Animal
import pt.ipt.dam2025.pawbuddy.model.RegisterRequest
import pt.ipt.dam2025.pawbuddy.model.Utilizador
import pt.ipt.dam2025.pawbuddy.ui.activity.activity.DetalhesUtilizadorFragment

class UtilizadorAdapter(
    private val lista: MutableList<Utilizador> = mutableListOf(),
    private val onClick: (Utilizador) -> Unit
) : RecyclerView.Adapter<UtilizadorAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemUtilizadorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(u: Utilizador) {
            binding.tvNome.text = u.nome
            binding.tvEmail.text = u.email

            binding.root.setOnClickListener { onClick(u) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUtilizadorBinding.inflate(
            LayoutInflater.from(
            parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount(): Int = lista.size

    fun submitList(novaLista: List<Utilizador>) {
        lista.clear()
        lista.addAll(novaLista)
        notifyDataSetChanged()
    }
}