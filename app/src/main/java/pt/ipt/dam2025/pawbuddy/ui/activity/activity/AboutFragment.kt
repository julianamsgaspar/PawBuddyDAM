package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import pt.ipt.dam2025.pawbuddy.BuildConfig
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentAboutBinding

/**
 * AboutFragment
 *
 * Fragment responsável pela apresentação da secção "Sobre" da aplicação PawBuddy.
 *
 * Este fragmento disponibiliza informação institucional da aplicação e
 * permite o acesso externo à API do backend através de um URL configurado
 * nos recursos (strings.xml), garantindo desacoplamento entre código e
 * dados configuráveis.
 *
 * Enquadramento académico:
 * - Implementa o ciclo de vida padrão de um Fragment Android
 * - Utiliza ViewBinding para acesso seguro e tipado à interface gráfica
 * - Demonstra boas práticas de gestão de memória ao libertar referências
 *   da view no método onDestroyView()
 */
class AboutFragment : Fragment() {

    /**
     * Referência nullable ao binding da view.
     *
     * É inicializada em onCreateView() e libertada em onDestroyView(),
     * prevenindo memory leaks associados ao ciclo de vida dos fragments.
     */
    private var _binding: FragmentAboutBinding? = null

    /**
     * Propriedade auxiliar não-nullable para acesso ao binding.
     *
     * Deve ser utilizada apenas entre onCreateView() e onDestroyView().
     */
    private val binding get() = _binding!!

    /**
     * Criação e inflação da interface gráfica do fragmento.
     *
     * @param inflater Responsável por inflar o layout XML
     * @param container ViewGroup pai onde a view será inserida
     * @param savedInstanceState Estado previamente guardado do fragmento
     * @return View raiz do fragmento
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Método invocado após a criação da view.
     *
     * É utilizado para inicialização de lógica de interface e definição
     * de listeners, respeitando a separação entre criação da view e
     * comportamento interativo.
     *
     * @param view View raiz do fragmento
     * @param savedInstanceState Estado previamente guardado
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * URL da API obtido a partir dos recursos da aplicação.
         *
         * Esta abordagem facilita:
         *  - Internacionalização
         *  - Alteração do endpoint sem recompilação do código
         *  - Separação entre lógica e configuração
         */
        val apiUrl = BuildConfig.BASE_URL

        /**
         * Listener associado ao botão que abre o endpoint da API
         * no navegador externo do sistema.
         */
        binding.btnOpenApi.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(apiUrl))
            startActivity(intent)
        }

    }

    /**
     * Método chamado quando a view do fragmento é destruída.
     *
     * A referência ao binding é libertada explicitamente para evitar
     * fugas de memória, uma vez que o fragmento pode sobreviver à view.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
