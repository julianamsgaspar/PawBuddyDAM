package pt.ipt.dam2025.pawbuddy.ui.activity.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.ItemIntencaoBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao


class IntencaoAdapter(
    private val lista: List<IntencaoDeAdocao>,
    private val onClick: (IntencaoDeAdocao) -> Unit,
    private val isAdmin: Boolean,
    private val onEliminar: (IntencaoDeAdocao) -> Unit,
    private val onEditarEstado: (IntencaoDeAdocao) -> Unit
) : RecyclerView.Adapter<IntencaoAdapter.IntencaoViewHolder>() {

    inner class IntencaoViewHolder(val binding: ItemIntencaoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IntencaoDeAdocao) {
            binding.txtProfissao.text = "Profissão: ${item.profissao}"
            binding.txtResidencia.text = "Residência: ${item.residencia}"
            binding.txtMotivo.text = "Motivo: ${item.motivo}"
            binding.txtTemAnimais.text = "Tem animais: ${item.temAnimais}"
            binding.txtQuaisAnimais.text = "Quais: ${item.quaisAnimais ?: "-"}"
            binding.txtDataIA.text = "Data: ${item.dataIA}"
            binding.txtEstado.text = "Estado: ${item.estado}" // usa método do modelo

            binding.txtUtilizador.text =
                "Utilizador: ${item.utilizador?.nome ?: "Desconhecido"}"


            binding.txtAnimal.text =
                "Animal: ${item.animal?.nome ?: "Desconhecido"}"


            binding.root.setOnClickListener { onClick(item) }
            binding.btnEliminar.visibility = if (isAdmin) View.VISIBLE else View.GONE
            binding.btnEliminar.visibility = if (isAdmin) View.VISIBLE else View.GONE

            binding.btnEliminar.setOnClickListener {
                onEliminar(item)
            }

            binding.btnEditar.setOnClickListener {
                onEditarEstado(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntencaoViewHolder {
        val binding = ItemIntencaoBinding.inflate(
            LayoutInflater.from(parent.context)
            , parent,
            false
        )
        return IntencaoViewHolder(binding)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: IntencaoViewHolder, position: Int) {
        holder.bind(lista[position])
    }
}