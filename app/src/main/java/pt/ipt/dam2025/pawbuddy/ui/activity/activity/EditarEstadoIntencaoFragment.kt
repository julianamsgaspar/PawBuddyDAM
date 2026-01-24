package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentEditarEstadoIntencaoBinding
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import pt.ipt.dam2025.pawbuddy.retrofit.service.IntencaoDeAdocaoService

class EditarEstadoIntencaoFragment : Fragment() {

    private var _binding: FragmentEditarEstadoIntencaoBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitProvider.intencaoService

    private var intencaoId: Int = -1
    private var estadoAtual: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            intencaoId = it.getInt("id", -1)
            estadoAtual = it.getInt("estado", -1) // ✅ agora é Int
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
        val values = resources.getStringArray(R.array.intent_status_values) // "0","1","2","3","4"

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spinnerEstado.adapter = adapter

        // ✅ Selecionar estado atual (match por valor numérico)
        if (estadoAtual >= 0) {
            val index = values.indexOf(estadoAtual.toString())
            if (index >= 0) binding.spinnerEstado.setSelection(index)
        }

        binding.btnGuardar.setOnClickListener {
            if (intencaoId <= 0) {
                Toast.makeText(requireContext(), "ID inválido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pos = binding.spinnerEstado.selectedItemPosition
            val novoEstadoInt = values[pos].toInt()
            atualizarEstado(novoEstadoInt)
        }

        binding.btnCancelar.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun atualizarEstado(novoEstado: Int) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                api.atualizarEstado(intencaoId, IntencaoDeAdocaoService.EstadoRequest(novoEstado))

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.success_intent_status_updated),
                        Toast.LENGTH_SHORT
                    ).show()

                    // ✅ volta à lista (que vai recarregar em onResume)
                    findNavController().popBackStack()
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
