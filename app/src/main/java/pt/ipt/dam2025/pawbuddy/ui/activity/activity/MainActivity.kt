package pt.ipt.dam2025.pawbuddy.ui.activity.activity

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import pt.ipt.dam2025.pawbuddy.R
import pt.ipt.dam2025.pawbuddy.databinding.ActivityMainBinding
import pt.ipt.dam2025.pawbuddy.network.CookieStorage
import pt.ipt.dam2025.pawbuddy.session.SessionManager

/**
 * Activity principal da aplicação (MainActivity).
 *
 * Responsabilidade:
 * - Alojar o NavHostFragment e orquestrar a navegação global (Navigation Component).
 * - Gerir a Toolbar (Top App Bar) e o menu de ações (login/logout).
 * - Gerir o Navigation Drawer (apenas para administradores).
 * - Ajustar dinamicamente o start destination do grafo de navegação no primeiro arranque.
 * - Sincronizar a UI com o estado de sessão (logged/admin), através de SharedPreferences e/ou SessionManager.
 *
 * Enquadramento arquitetural:
 * - Navigation Component: NavController, AppBarConfiguration e integração com Drawer.
 * - View Binding: ActivityMainBinding para acesso seguro a elementos do layout.
 * - Persistência leve: SharedPreferences ("PawBuddyPrefs") para estado de sessão.
 * - Cookie-based auth: CookieStorage.sessionCookie é limpo no logout, garantindo remoção de autenticação em memória.
 *
 * Considerações de engenharia:
 * - A Activity trata navegação e chrome da aplicação (toolbar/drawer), enquanto a lógica de negócio
 *   reside nos Fragments/Services.
 * - A política de "guest começa em Home" está implementada alterando o start destination e
 *   garantindo que o logout navega para Home.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Binding do layout da Activity.
     * É inicializado em onCreate após inflate.
     */
    private lateinit var binding: ActivityMainBinding

    /**
     * Controlador de navegação principal associado ao NavHostFragment.
     */
    private lateinit var navController: NavController

    /**
     * Configuração da App Bar:
     * - Define quais destinos são "top-level" (não mostram botão Up e podem mostrar hamburger).
     * - Integra com DrawerLayout quando aplicável.
     */
    private lateinit var appBarConfiguration: AppBarConfiguration

    /**
     * SessionManager mantido como dependência da Activity.
     * Nota no código indica que, embora aqui uses prefs, session pode ser útil noutros pontos.
     * O lazy garante inicialização tardia.
     */
    private val session by lazy { SessionManager(this) }

    /**
     * Conjunto de destinos top-level para utilizador comum (ou guest).
     * Tipicamente, nestes destinos a app não apresenta "Up", pois são considerados “raiz”.
     */
    private val userTopLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.listaAnimaisFragment,
        R.id.detalhesUtilizadorFragment
    )

    /**
     * Conjunto de destinos top-level para administrador.
     * Nota: pode ser expandido para incluir outras “raízes” do módulo de gestão.
     */
    private val adminTopLevelDestinations = setOf(
        R.id.gestaoFragment
        // adiciona outros destinos "raiz" do admin se existirem
    )

    /**
     * Ciclo de vida: criação da Activity.
     *
     * Responsabilidades implementadas:
     * 1) Inicialização do binding, layout e toolbar.
     * 2) Obtenção do NavController a partir do NavHostFragment.
     * 3) Configuração do start destination de forma dinâmica no primeiro arranque (savedInstanceState == null).
     * 4) Integração inicial da AppBar com o NavController.
     * 5) Listener de mudança de destino para:
     *    - aplicar guard "admin nunca fica no Home"
     *    - esconder toolbar em ecrãs de autenticação
     *    - bloquear drawer em ecrãs de autenticação
     *    - atualizar UI conforme sessão
     *    - atualizar menu login/logout
     * 6) Refresh inicial da UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa binding e aplica layout à Activity.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Define toolbar como ActionBar da Activity.
        setSupportActionBar(binding.toolbar)

        // Obtém NavController a partir do NavHostFragment no layout.
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // Start destination dinâmico (apenas no primeiro arranque).
        // Política definida:
        // - Admin autenticado: start em GestaoFragment
        // - Caso contrário (guest ou user): start em HomeFragment
        if (savedInstanceState == null) {
            val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
            val isLogged = shared.getBoolean("isLogged", false)
            val isAdmin = shared.getBoolean("isAdmin", false)

            // Infla o grafo e altera start destination programaticamente.
            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            graph.setStartDestination(
                when {
                    isLogged && isAdmin -> R.id.gestaoFragment
                    else -> R.id.homeFragment // inclui: não logado + user normal logado
                }
            )
            navController.graph = graph
        }

        // AppBar inicial (será recalculada em refreshSessionUi consoante sessão).
        appBarConfiguration = AppBarConfiguration(userTopLevelDestinations)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Listener para reagir a mudanças de destino.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
            val isLogged = shared.getBoolean("isLogged", false)
            val isAdmin = shared.getBoolean("isAdmin", false)

            // Guard: Admin nunca deve permanecer no Home (política de UX/fluxo).
            // Se por alguma razão navegar para Home, é redirecionado para Gestão.
            if (isLogged && isAdmin && destination.id == R.id.homeFragment) {
                navController.navigate(
                    R.id.gestaoFragment,
                    null,
                    navOptions {
                        popUpTo(R.id.nav_graph) { inclusive = false }
                        launchSingleTop = true
                    }
                )
                return@addOnDestinationChangedListener
            }

            // Identifica ecrãs de autenticação (onde a toolbar e drawer devem ser controlados).
            val isAuthScreen =
                destination.id == R.id.loginFragment || destination.id == R.id.registerFragment

            // Toolbar: ocultar em ecrãs de autenticação.
            binding.toolbar.visibility = if (isAuthScreen) View.GONE else View.VISIBLE

            // Drawer:
            // - bloqueia em ecrãs de autenticação
            // - caso contrário, recalcula UI conforme sessão (admin vs user/guest)
            if (isAuthScreen) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                refreshSessionUi()
            }

            // Atualiza o menu de ações (login/logout) com base no estado de sessão e destino.
            invalidateOptionsMenu()
        }

        // Refresh inicial para garantir estado correto do drawer/appbar.
        refreshSessionUi()
    }

    /**
     * Revalida UI no regresso ao foreground.
     *
     * Justificação:
     * - O estado de sessão pode mudar fora da Activity (ex.: login/logout noutro ecrã),
     *   sendo necessário refletir alterações na toolbar/drawer/menu.
     */
    override fun onResume() {
        super.onResume()
        refreshSessionUi()
        invalidateOptionsMenu()
    }

    /**
     * Atualiza Drawer + AppBarConfiguration conforme sessão.
     *
     * Política de UI:
     * - Admin autenticado:
     *   - Drawer ativo (adminDrawer visível)
     *   - DrawerLayout desbloqueado
     *   - Top-level destinations = adminTopLevelDestinations
     *   - AppBarConfiguration associada ao drawer (hamburger)
     * - User/Guest:
     *   - Drawer oculto/bloqueado
     *   - Top-level destinations = userTopLevelDestinations
     *   - AppBarConfiguration sem drawer (botão Up onde aplicável)
     *
     * Observação:
     * - Usa SharedPreferences como fonte de verdade para estado de sessão.
     * - Em casos mais complexos, recomendaria uma fonte única (SessionManager) para evitar
     *   divergências; aqui mantém-se conforme implementação existente.
     */
    private fun refreshSessionUi() {
        val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isLogged = shared.getBoolean("isLogged", false)
        val isAdmin = shared.getBoolean("isAdmin", false)

        val enableAdminDrawer = isLogged && isAdmin
        val activeTopLevel =
            if (enableAdminDrawer) adminTopLevelDestinations else userTopLevelDestinations

        // Controla visibilidade do menu drawer (apenas admin) e lock mode do DrawerLayout.
        binding.adminDrawer.visibility = if (enableAdminDrawer) View.VISIBLE else View.GONE
        binding.drawerLayout.setDrawerLockMode(
            if (enableAdminDrawer) DrawerLayout.LOCK_MODE_UNLOCKED
            else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )

        // Reconfigura AppBarConfiguration: com drawer para admin, sem drawer para user/guest.
        appBarConfiguration = if (enableAdminDrawer) {
            AppBarConfiguration(activeTopLevel, binding.drawerLayout)
        } else {
            AppBarConfiguration(activeTopLevel)
        }

        // Reassocia toolbar ao navController com nova configuração.
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Só liga NavigationView ao NavController quando o drawer está ativo (admin).
        if (enableAdminDrawer) {
            NavigationUI.setupWithNavController(binding.adminDrawer, navController)
        }
    }

    /**
     * Cria o menu da Top App Bar (ações de login/logout).
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_top_appbar, menu)
        return true
    }

    /**
     * Atualiza dinamicamente a visibilidade dos itens do menu.
     *
     * Política:
     * - Em ecrãs de autenticação (login/register): esconder ambas as ações.
     * - Se não logado: mostrar "Entrar" e esconder "Sair".
     * - Se logado: mostrar "Sair" e esconder "Entrar".
     *
     * Nota:
     * - Esta estratégia evita duplicação de itens e mantém consistência do ponto de ação.
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        super.onPrepareOptionsMenu(menu)

        val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isLogged = shared.getBoolean("isLogged", false)

        val destId = navController.currentDestination?.id
        val isAuthScreen = destId == R.id.loginFragment || destId == R.id.registerFragment

        menu.findItem(R.id.action_login)?.isVisible = !isLogged && !isAuthScreen
        menu.findItem(R.id.action_logout)?.isVisible = isLogged && !isAuthScreen

        return true
    }

    /**
     * Handler de seleção de itens no menu da Top App Bar.
     *
     * - action_login: navega para LoginFragment.
     * - action_logout: executa logout (limpa sessão e redireciona para Home).
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_login -> {
                navController.navigate(R.id.loginFragment)
                true
            }

            R.id.action_logout -> {
                performLogout()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Executa logout local e reconfigura navegação/UI.
     *
     * Operações realizadas:
     * 1) Limpa SharedPreferences (remove flags e ids relacionados com sessão).
     * 2) Limpa cookie em memória (CookieStorage.sessionCookie):
     *    - essencial em autenticação baseada em cookies para remover credenciais ativas.
     * 3) Navega para HomeFragment com popUpTo(nav_graph) inclusive:
     *    - limpa backstack e reinicia o fluxo como "guest".
     * 4) Recalcula UI (drawer/appbar) e atualiza menu.
     *
     * Consideração:
     * - O popUpTo inclusive = true efetivamente remove todos os destinos do grafo da backstack,
     *   garantindo que o utilizador não regressa a ecrãs protegidos com "Back".
     */
    private fun performLogout() {
        // Limpar prefs (estado de sessão persistido).
        getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // Limpar cookie em memória (cookie-based auth).
        CookieStorage.sessionCookie = null

        // Navegar para HOME (porque "guest" começa em Home).
        navController.navigate(
            R.id.homeFragment,
            null,
            navOptions {
                popUpTo(R.id.nav_graph) { inclusive = true }
                launchSingleTop = true
            }
        )

        // Recalcular UI e menu.
        refreshSessionUi()
        invalidateOptionsMenu()
    }

    /**
     * Integra o comportamento do botão Up (seta voltar) da App Bar com o Navigation Component.
     *
     * - Se houver Drawer configurado (admin), este método delega corretamente para abrir/fechar drawer.
     * - Caso contrário, executa navegação Up normal.
     */
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
