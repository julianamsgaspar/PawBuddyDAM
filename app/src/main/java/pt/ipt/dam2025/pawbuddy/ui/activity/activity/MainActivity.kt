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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    // Mantido (mesmo que aqui uses prefs, podes usar session noutros pontos)
    private val session by lazy { SessionManager(this) }

    private val userTopLevelDestinations = setOf(
        R.id.homeFragment,
        R.id.listaAnimaisFragment,
        R.id.detalhesUtilizadorFragment
    )

    private val adminTopLevelDestinations = setOf(
        R.id.gestaoFragment
        // adiciona outros destinos "raiz" do admin se existirem
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        // ✅ Start destination dinâmico (apenas no primeiro arranque)
        // ALTERAÇÃO: quando NÃO está logado, começa em HOME (não em Login)
        if (savedInstanceState == null) {
            val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
            val isLogged = shared.getBoolean("isLogged", false)
            val isAdmin = shared.getBoolean("isAdmin", false)

            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            graph.setStartDestination(
                when {
                    isLogged && isAdmin -> R.id.gestaoFragment
                    else -> R.id.homeFragment // inclui: não logado + user normal logado
                }
            )
            navController.graph = graph
        }

        // AppBar inicial (vai ser recalculada em refreshSessionUi)
        appBarConfiguration = AppBarConfiguration(userTopLevelDestinations)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
            val isLogged = shared.getBoolean("isLogged", false)
            val isAdmin = shared.getBoolean("isAdmin", false)

            // ✅ Guard: Admin nunca fica no Home
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

            val isAuthScreen =
                destination.id == R.id.loginFragment || destination.id == R.id.registerFragment

            // Toolbar: esconder em auth
            binding.toolbar.visibility = if (isAuthScreen) View.GONE else View.VISIBLE

            // Drawer: bloquear em auth; caso contrário, atualizar UI conforme sessão
            if (isAuthScreen) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                refreshSessionUi()
            }

            // Atualiza visibilidade dos itens do menu (login/logout)
            invalidateOptionsMenu()
        }

        refreshSessionUi()
    }

    override fun onResume() {
        super.onResume()
        refreshSessionUi()
        invalidateOptionsMenu()
    }

    /**
     * Atualiza Drawer + AppBarConfiguration conforme sessão.
     * - Admin: Drawer ativo (adminDrawer) e top-level = adminTopLevelDestinations
     * - User/Guest: sem drawer (ou drawer bloqueado) e top-level = userTopLevelDestinations
     */
    private fun refreshSessionUi() {
        val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isLogged = shared.getBoolean("isLogged", false)
        val isAdmin = shared.getBoolean("isAdmin", false)

        val enableAdminDrawer = isLogged && isAdmin
        val activeTopLevel =
            if (enableAdminDrawer) adminTopLevelDestinations else userTopLevelDestinations

        binding.adminDrawer.visibility = if (enableAdminDrawer) View.VISIBLE else View.GONE
        binding.drawerLayout.setDrawerLockMode(
            if (enableAdminDrawer) DrawerLayout.LOCK_MODE_UNLOCKED
            else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )

        appBarConfiguration = if (enableAdminDrawer) {
            AppBarConfiguration(activeTopLevel, binding.drawerLayout)
        } else {
            AppBarConfiguration(activeTopLevel)
        }

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        if (enableAdminDrawer) {
            NavigationUI.setupWithNavController(binding.adminDrawer, navController)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_top_appbar, menu)
        return true
    }

    /**
     * Alterna o item no mesmo lugar:
     * - não logado => Entrar
     * - logado => Sair
     * E esconde ambos em ecrãs de autenticação.
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

    private fun performLogout() {
        // Limpar prefs
        getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        // Limpar cookie em memória (cookie-based auth)
        CookieStorage.sessionCookie = null

        // Navegar para HOME (porque agora "guest" começa em Home)
        navController.navigate(
            R.id.homeFragment,
            null,
            navOptions {
                popUpTo(R.id.nav_graph) { inclusive = true }
                launchSingleTop = true
            }
        )

        // Recalcular UI e menu
        refreshSessionUi()
        invalidateOptionsMenu()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
