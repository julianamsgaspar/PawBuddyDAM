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
        navController = navHostFragment.navController

        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.listaAnimaisFragment,
            R.id.detalhesUtilizadorFragment
        )

        // ✅ Start destination dinâmico (só no primeiro arranque)
        if (savedInstanceState == null) {
            val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
            val isLogged = shared.getBoolean("isLogged", false)
            val isAdmin = shared.getBoolean("isAdmin", false)

            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            graph.setStartDestination(
                when {
                    !isLogged -> R.id.loginFragment
                    isAdmin -> R.id.gestaoFragment
                    else -> R.id.homeFragment
                }
            )
            navController.graph = graph
        }

        appBarConfiguration = AppBarConfiguration(topLevelDestinations)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.bottomNav, navController)

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
                        popUpTo(R.id.nav_graph) { inclusive = false } // mais robusto
                        launchSingleTop = true
                    }
                )
                return@addOnDestinationChangedListener
            }

            val hideAuth = destination.id in setOf(R.id.loginFragment, R.id.registerFragment)
            val hideBottomNav = hideAuth || (isLogged && isAdmin)

            binding.toolbar.visibility = if (hideAuth) View.GONE else View.VISIBLE
            binding.bottomNav.visibility = if (hideBottomNav) View.GONE else View.VISIBLE

            if (hideAuth) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                refreshSessionUi(topLevelDestinations)
            }

            invalidateOptionsMenu()
        }

        refreshSessionUi(topLevelDestinations)
    }

    override fun onResume() {
        super.onResume()
        // ✅ não forces um set "fixo"; usa o mesmo set do onCreate
        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.listaAnimaisFragment,
            R.id.detalhesUtilizadorFragment
        )
        refreshSessionUi(topLevelDestinations)
        invalidateOptionsMenu()
    }


    private fun refreshSessionUi(topLevelDestinations: Set<Int>) {
        val shared = getSharedPreferences("PawBuddyPrefs", Context.MODE_PRIVATE)
        val isLogged = shared.getBoolean("isLogged", false)
        val isAdmin = shared.getBoolean("isAdmin", false)

        val enableAdminDrawer = isLogged && isAdmin

        val adminTopLevel = setOf(
            R.id.gestaoFragment
            // adiciona outros destinos "raiz" do admin se existirem
        )

        val activeTopLevel = if (enableAdminDrawer) adminTopLevel else topLevelDestinations

        binding.adminDrawer.visibility = if (enableAdminDrawer) View.VISIBLE else View.GONE
        binding.drawerLayout.setDrawerLockMode(
            if (enableAdminDrawer) DrawerLayout.LOCK_MODE_UNLOCKED
            else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )

        // ✅ BottomNav nunca mostrar ao Admin
        binding.bottomNav.visibility = if (enableAdminDrawer) View.GONE else View.VISIBLE

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

        // Limpar cookie em memória (cookie-based auth)
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
