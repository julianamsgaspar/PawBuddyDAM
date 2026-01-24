package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentIntencaoDetalheBinding
import pt.ipt.dam2025.pawbuddy.session.SessionManager
import pt.ipt.dam2025.pawbuddy.utils.EstadoAdocaoMapper
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
            Toast.makeText(
                requireContext(),
                getString(R.string.error_login_required),
                Toast.LENGTH_SHORT
            ).show()
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

                    // Valores (sem prefixos)
                    binding.txtEstado.text =
                        EstadoAdocaoMapper.toText(requireContext(), intencao.estado)

                    binding.txtData.text = intencao.dataIA ?: "-"

                    binding.txtAnimal.text = intencao.animal?.nome ?: "Desconhecido"
                    binding.txtProfissao.text = intencao.profissao ?: "-"
                    binding.txtResidencia.text = intencao.residencia ?: "-"

                    // temAnimais é String -> normalizar
                    binding.txtTemAnimais.text = formatTemAnimais(intencao.temAnimais)

                    binding.txtQuaisAnimais.text = intencao.quaisAnimais ?: "-"
                    binding.txtMotivo.text = intencao.motivo ?: "-"

                    val isAdmin = session.isAdmin()

                    // Utilizador (Admin)
                    binding.lblUtilizador.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    binding.txtUtilizador.visibility = if (isAdmin) View.VISIBLE else View.GONE

                    // ID do Utilizador (Admin)
                    binding.lblUtilizadorId.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    binding.txtUtilizadorId.visibility = if (isAdmin) View.VISIBLE else View.GONE

                    if (isAdmin) {
                        val utilizador = intencao.utilizador

                        val nomeUtilizador = when {
                            !utilizador?.nome.isNullOrBlank() -> utilizador?.nome!!
                            !utilizador?.email.isNullOrBlank() -> utilizador?.email!!
                            else -> "Desconhecido"
                        }

                        binding.txtUtilizador.text = nomeUtilizador
                        binding.txtUtilizadorId.text = utilizador?.id?.toString() ?: "-"

                        // Copiar ID com long click (apenas Admin)
                        binding.txtUtilizadorId.setOnLongClickListener {
                            val clipboard = requireContext()
                                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                            val clip = ClipData.newPlainText("User ID", binding.txtUtilizadorId.text)
                            clipboard.setPrimaryClip(clip)

                            Toast.makeText(requireContext(), "ID copiado", Toast.LENGTH_SHORT).show()
                            true
                        }
                    } else {
                        // Higiene: limpar valores escondidos
                        binding.txtUtilizador.text = ""
                        binding.txtUtilizadorId.text = ""
                        binding.txtUtilizadorId.setOnLongClickListener(null)
                    }
                }

            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    if (e.code() == 401 || e.code() == 403) {
                        session.logout()
                        Toast.makeText(
                            requireContext(),
                            "Sessão expirada. Faz login novamente.",
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigate(R.id.loginFragment)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_intent_not_found),
                            Toast.LENGTH_LONG
                        ).show()
                        findNavController().navigateUp()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_intent_not_found),
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun formatTemAnimais(value: String?): String {
        val v = value?.trim()?.lowercase()
        if (v.isNullOrBlank()) return "-"

        return when (v) {
            "sim", "s", "true", "1", "yes", "y" -> "Sim"
            "nao", "não", "n", "false", "0", "no" -> "Não"
            else -> value.trim()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
