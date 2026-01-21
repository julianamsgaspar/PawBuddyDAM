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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController // <- IMPORTANT: atribuir à propriedade

        // Top-level destinations (ajusta se necessário)
        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.listaAnimaisFragment,
            R.id.detalhesUtilizadorFragment
        )

        // Config inicial de AppBarConfiguration (vai ser recalculada em refreshSessionUi)
        appBarConfiguration = AppBarConfiguration(topLevelDestinations)

        // Toolbar (título e up/hamburger)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // BottomNav
        NavigationUI.setupWithNavController(binding.bottomNav, navController)

        // Listener: esconder barras em ecrãs específicos
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val hideBars = destination.id in setOf(
                R.id.loginFragment,
                R.id.registerFragment
            )

            // “App-like”: em auth escondemos toolbar + bottom nav e bloqueamos drawer
            binding.toolbar.visibility = if (hideBars) View.GONE else View.VISIBLE
            binding.bottomNav.visibility = if (hideBars) View.GONE else View.VISIBLE

            if (hideBars) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                refreshSessionUi(topLevelDestinations)
            }

            // Força redesenho do menu (para mostrar/esconder Logout)
            invalidateOptionsMenu()
        }

        // Aplicar estado inicial
        refreshSessionUi(topLevelDestinations)
    }

    override fun onResume() {
        super.onResume()
        // Se prefs mudaram após login, UI acompanha sem reiniciar a app
        refreshSessionUi(
            setOf(R.id.homeFragment, R.id.listaAnimaisFragment, R.id.detalhesUtilizadorFragment)
        )
        invalidateOptionsMenu()
    }

    private fun refreshSessionUi(topLevelDestinations: Set<Int>) {
        val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isLogged = shared.getBoolean("isLogged", false)
        val isAdmin = shared.getBoolean("isAdmin", false)

        // Drawer só para admin e logado
        val enableAdminDrawer = isLogged && isAdmin

        binding.adminDrawer.visibility = if (enableAdminDrawer) View.VISIBLE else View.GONE
        binding.drawerLayout.setDrawerLockMode(
            if (enableAdminDrawer) DrawerLayout.LOCK_MODE_UNLOCKED
            else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )

        // AppBarConfiguration: com drawer se admin
        appBarConfiguration = if (enableAdminDrawer) {
            AppBarConfiguration(topLevelDestinations, binding.drawerLayout)
        } else {
            AppBarConfiguration(topLevelDestinations)
        }

        // Reassociar action bar ao novo appBarConfiguration
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Drawer (admin) só quando ativo
        if (enableAdminDrawer) {
            NavigationUI.setupWithNavController(binding.adminDrawer, navController)
        }
    }

    // MENU (Logout)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_top_appbar, menu)

        val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isLogged = shared.getBoolean("isLogged", false)

        // Mostrar Logout apenas quando logado
        menu.findItem(R.id.action_logout)?.isVisible = isLogged
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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

        // Limpar cookie em memória (se usas cookie-based auth)
        CookieStorage.sessionCookie = null

        // Navegar para login e limpar backstack
        navController.navigate(
            R.id.loginFragment,
            null,
            navOptions {
                popUpTo(R.id.nav_graph) { inclusive = true }
                launchSingleTop = true
            }
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
