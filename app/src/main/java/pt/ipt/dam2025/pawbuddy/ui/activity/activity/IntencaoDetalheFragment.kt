package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentIntencaoDetalheBinding
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import retrofit2.HttpException

class IntencaoDetalheFragment : Fragment() {

    private var _binding: FragmentIntencaoDetalheBinding? = null
    private val binding get() = _binding!!

    private val api = RetrofitProvider.intencaoService
    private val session by lazy { SessionManager(requireContext()) }

    private var intencaoId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intencaoId = arguments?.getInt("id", -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIntencaoDetalheBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!session.isLogged() || intencaoId <= 0) {
            Toast.makeText(requireContext(), getString(R.string.error_login_required), Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.loginFragment)
            return
        }

        carregarDetalhe()
    }

    private fun carregarDetalhe() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val intencao = api.getByIntencaoId(intencaoId)

                withContext(Dispatchers.Main) {
                    binding.txtEstado.text = "Estado: ${intencao.estado}"
                    binding.txtData.text = "Data: ${intencao.dataIA ?: "-"}"
                    binding.txtAnimal.text = "Animal: ${intencao.animal?.nome ?: "Desconhecido"}"
                    binding.txtProfissao.text = "Profissão: ${intencao.profissao}"
                    binding.txtResidencia.text = "Residência: ${intencao.residencia}"
                    binding.txtTemAnimais.text = "Tem animais: ${intencao.temAnimais}"
                    binding.txtQuaisAnimais.text = "Quais: ${intencao.quaisAnimais ?: "-"}"
                    binding.txtMotivo.text = "Motivo: ${intencao.motivo}"
                }

            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    if (e.code() == 401 || e.code() == 403) {
                        session.logout()
                        Toast.makeText(requireContext(), "Sessão expirada. Faz login novamente.", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.loginFragment)
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.error_intent_not_found), Toast.LENGTH_LONG).show()
                        findNavController().navigateUp()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), getString(R.string.error_intent_not_found), Toast.LENGTH_LONG).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
