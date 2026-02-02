package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.FragmentHomeBinding
import pt.ipt.dam2025.pawbuddy.session.SessionManager

/**
 * Fragment que implementa o ecrã inicial (Home) da aplicação PawBuddy.
 *
 * Finalidade e contexto:
 * - Serve como ponto de entrada funcional para o utilizador após abrir a aplicação.
 * - Agrega chamadas para ação (CTAs) sob a forma de "cards" (ex.: explorar, conta, registo).
 * - Ajusta dinamicamente o conteúdo e a visibilidade dos elementos em função do estado
 *   de autenticação e do perfil do utilizador (admin vs não-admin).
 *
 * Dependências e mecanismos utilizados:
 * - View Binding para acesso tipado e seguro aos componentes do layout.
 * - SharedPreferences para obter o estado de sessão (isLogged) e permissões (isAdmin).
 * - Navigation Component (NavController) para navegação entre Fragments.
 * - Animação simples (scale) para feedback visual em interações com cards.
 *
 * Observação de engenharia:
 * - Este Fragment contém lógica de apresentação (UI logic) e orquestração de navegação.
 * - Não contém lógica de negócio nem acesso direto a dados remotos, respeitando a separação
 *   de responsabilidades em camadas.
 */
class HomeFragment : Fragment() {

    /**
     * Binding nullable, válido apenas entre onCreateView e onDestroyView.
     * Deve ser libertado para evitar memory leaks.
     */
    private var _binding: FragmentHomeBinding? = null

    /**
     * Getter não-null do binding; assume que apenas é acedido quando a View existe.
     */
    private val binding get() = _binding!!

    private val session by lazy { SessionManager(requireContext()) }

    /**
     * Infla o layout do Fragment e inicializa o View Binding.
     *
     * @param inflater inflador do layout XML.
     * @param container ViewGroup pai (pode ser null).
     * @param savedInstanceState estado previamente guardado (se aplicável).
     * @return View raiz associada ao binding.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Configura a interface e define o comportamento dos cards após a View estar criada.
     *
     * Estratégia aplicada:
     * 1) Ler estado de sessão e permissões via SharedPreferences:
     *    - isLogged: indica se existe sessão ativa
     *    - isAdmin: indica se o utilizador tem perfil administrativo
     * 2) Configurar CTAs:
     *    - Explorar: sempre disponível
     *    - Conta: alterna entre login e perfil consoante isLogged
     *    - Minhas Intenções: visível apenas para utilizador autenticado não-admin
     *    - Registo: visível apenas quando não autenticado
     *    - About: sempre disponível
     * 3) Aplicar animação antes de efetuar navegação, melhorando UX.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isLogged = session.isLogged()
        val isAdmin = session.isAdmin()
        val userName = session.userName()


        binding.tvWelcome.text = when {
            isLogged && !userName.isNullOrBlank() ->
                "Olá, $userName"

            isLogged ->
                "Sessão expirada"

            else ->
                "Bem-vindo ao PawBuddy"
        }

        binding.tvWelcome.visibility = View.VISIBLE


        // Card "Explorar": sempre disponível, direciona para a lista de animais.
        binding.cardExplore.setOnClickListener { v ->
            animateCard(v) {
                findNavController().navigate(R.id.listaAnimaisFragment)
            }
        }


        // Card "Minhas Intenções":
        // Visível apenas para utilizadores autenticados que não sejam administradores.
        // Nota no código: espera-se que o elemento exista no XML com visibility="gone" por defeito.
        binding.cardMyIntents.visibility = if (isLogged && !isAdmin) View.VISIBLE else View.GONE
        binding.cardMyIntents.setOnClickListener { v ->
            animateCard(v) {
                // Navegação para as intenções do utilizador (ajustável ao ID real do nav_graph).
                findNavController().navigate(R.id.listaIntencoesFragment)
            }
        }

        // Card "Registo":
        // Aparece apenas quando o utilizador não está autenticado.
        binding.cardRegister.visibility = if (isLogged) View.GONE else View.VISIBLE
        binding.cardRegister.setOnClickListener { v ->
            animateCard(v) {
                findNavController().navigate(R.id.registerFragment)
            }
        }

        // Card "About": disponibiliza informação sobre a aplicação (sempre acessível).
        binding.cardAbout.setOnClickListener { v ->
            animateCard(v) {
                findNavController().navigate(R.id.aboutFragment)
            }
        }
    }

    /**
     * Aplica uma animação curta de "press feedback" (escala) a um card e executa uma ação no final.
     *
     * Racional:
     * - Introduz feedback visual imediato ao utilizador.
     * - Mantém a navegação desacoplada da UI, delegando a ação para o callback onEnd.
     *
     * Implementação:
     * - Primeira fase: reduz ligeiramente escala (0.98) em 80ms.
     * - Segunda fase: retorna à escala normal (1.0) em 110ms.
     * - Ao terminar, executa o callback (p.ex., navegação).
     *
     * @param view View alvo da animação (tipicamente um card).
     * @param onEnd ação a executar após concluir a animação.
     */
    private fun animateCard(view: View, onEnd: () -> Unit) {
        view.animate()
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(80)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(110)
                    .withEndAction { onEnd() }
                    .start()
            }
            .start()
    }

    /**
     * Libertação do binding quando a View é destruída.
     * Essencial para evitar memory leaks em Fragments, pois o ciclo de vida do Fragment
     * pode ultrapassar o da View.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
