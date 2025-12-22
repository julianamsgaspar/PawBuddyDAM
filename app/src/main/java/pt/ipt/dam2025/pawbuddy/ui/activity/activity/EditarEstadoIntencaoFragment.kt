package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentEditarEstadoIntencaoBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [EditarEstadoIntencaoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditarEstadoIntencaoFragment : Fragment() {

    private var _binding: FragmentEditarEstadoIntencaoBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitInitializer().intencaoService()

    private var intencaoId: Int = -1
    private var estadoAtual: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            intencaoId = it.getInt("id")
            estadoAtual = it.getInt("estado")
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

        val estados = listOf(
            "Reservado",
            "Em Processo",
            "Em Validação",
            "Concluído",
            "Rejeitado"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            estados
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerEstado.adapter = adapter
        binding.spinnerEstado.setSelection(estadoAtual)

        binding.btnGuardar.setOnClickListener {
            val novoEstado = binding.spinnerEstado.selectedItemPosition
            atualizarEstado(novoEstado)
        }

        binding.btnCancelar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun atualizarEstado(novoEstado: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // vai buscar intenção completa atual
                val intencaoAtual = api.getByIntencaoId(intencaoId)

                // Criar nova intenção só com estado alterado
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

                // Enviar objeto completo
                api.atualizarIntencao(intencaoId, intencaoAtualizada)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Estado atualizado com sucesso",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Erro ao atualizar estado",
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