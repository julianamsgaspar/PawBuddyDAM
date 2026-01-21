package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentEditarEstadoIntencaoBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

class EditarEstadoIntencaoFragment : Fragment() {

    private var _binding: FragmentEditarEstadoIntencaoBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitInitializer().intencaoService()

    private var intencaoId: Int = -1
    private var estadoAtual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            intencaoId = it.getInt("id", -1)
            estadoAtual = it.getString("estado") // ex: "Emprocesso"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarEstadoIntencaoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val labels = resources.getStringArray(R.array.intent_status_labels)
        val values = resources.getStringArray(R.array.intent_status_values)

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerEstado.adapter = adapter

        // Selecionar o estado atual (string do backend)
        estadoAtual?.let { estado ->
            val index = values.indexOf(estado)
            if (index >= 0) binding.spinnerEstado.setSelection(index)
        }

        binding.btnGuardar.setOnClickListener {
            val pos = binding.spinnerEstado.selectedItemPosition
            val novoEstado = values[pos] // ex: "Emvalidacao"
            atualizarEstado(novoEstado)
        }

        binding.btnCancelar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun atualizarEstado(novoEstado: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val intencaoAtual = api.getByIntencaoId(intencaoId)

                val intencaoAtualizada = IntencaoDeAdocao(
                    id = intencaoAtual.id,
                    estado = novoEstado,
                    profissao = intencaoAtual.profissao,
                    residencia = intencaoAtual.residencia,
                    motivo = intencaoAtual.motivo,
                    temAnimais = intencaoAtual.temAnimais,
                    quaisAnimais = intencaoAtual.quaisAnimais,
                    dataIA = intencaoAtual.dataIA,
                    utilizadorFK = intencaoAtual.utilizadorFK,
                    animalFK = intencaoAtual.animalFK
                )

                api.atualizarIntencao(intencaoId, intencaoAtualizada)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_intent_status_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(
                            R.string.error_update_intent_status,
                            e.message ?: getString(R.string.error_generic)
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
