package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAdotarBinding
import pt.ipt.dam2025.pawbuddy.model.IntencaoDeAdocao
import pt.ipt.dam2025.pawbuddy.retrofit.RetrofitInitializer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AdotarFragment.newInstance] factory method to
 * create an instance of this fragment.
 */

class AdotarFragment : Fragment() {

    private var _binding: FragmentAdotarBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitInitializer().intencaoService()

    private var animalId: Int = -1
    private var utilizadorId: Int = -1

    companion object {
        fun newInstance(animalId: Int, utilizadorId: Int): AdotarFragment {
            val fragment = AdotarFragment()
            fragment.animalId = animalId
            fragment.utilizadorId = utilizadorId
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdotarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Spinner "Tem Animais"
        val opcoes = listOf("Não", "Sim")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opcoes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spTemAnimais.adapter = adapter

        // Mostrar campo "Quais Animais" se selecionou "Sim"
        binding.spTemAnimais.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.etQuaisAnimais.visibility = if (position == 1) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                binding.etQuaisAnimais.visibility = View.GONE
            }
        }

        binding.btnSubmeter.setOnClickListener { enviarIntencao() }
        binding.btnVoltar.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun enviarIntencao() {
        val profissao = binding.etProfissao.text.toString()
        val residencia = binding.etResidencia.text.toString()
        val motivo = binding.etMotivo.text.toString()
        val temAnimais = binding.spTemAnimais.selectedItem.toString()
        val quaisAnimais = if (temAnimais == "Sim") binding.etQuaisAnimais.text.toString() else null

        if (profissao.isBlank() || residencia.isBlank() || motivo.isBlank() || (temAnimais == "Sim" && quaisAnimais.isNullOrBlank())) {
            Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
            return
        }

        val dataIA = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

        val intencao = IntencaoDeAdocao(
            estado = 2, // ou outro estado inicial
            profissao = profissao,
            residencia = residencia,
            motivo = motivo,
            temAnimais = temAnimais,
            quaisAnimais = quaisAnimais,
            dataIA = dataIA,
            utilizadorFK = utilizadorId,
            animalFK = animalId
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.criar(intencao)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Intenção de adoção enviada!", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Erro ao enviar: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}