package com.upc.asistenteredidbi

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.upc.asistenteredidbi.data.notification.NotificationTokenSync
import com.upc.asistenteredidbi.databinding.ActivityMainBinding
import com.upc.asistenteredidbi.presentation.main.SessionDestination
import com.upc.asistenteredidbi.presentation.main.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val sessionViewModel: SessionViewModel by viewModels()

    @Inject
    lateinit var notificationTokenSync: NotificationTokenSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupDrawer()
        observeSession()

        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayoutRoot.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayoutRoot.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }

        if (savedInstanceState == null) {
            sessionViewModel.checkSession()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
    }

    fun openDrawer() {
        sessionViewModel.loadDrawerProfile()
        binding.drawerLayoutRoot.openDrawer(GravityCompat.START)
    }

    private fun setupDrawer() {
        val drawer = binding.navDrawerContent

        drawer.btnCloseDrawer.setOnClickListener { closeDrawer() }
        drawer.navLogout.setOnClickListener {
            closeDrawer()
            sessionViewModel.logout()
        }

        drawer.navInicio.setOnClickListener { closeDrawer() }
        drawer.navNuevaEvaluacion.setOnClickListener { navigateFromDrawer(R.id.action_home_to_chat) }
        drawer.navHistorial.setOnClickListener { navigateFromDrawer(R.id.action_home_to_historial) }
        drawer.navPropuestas.setOnClickListener { navigateFromDrawer(R.id.action_home_to_minutas) }
        drawer.navConfiguracion.setOnClickListener { navigateFromDrawer(R.id.action_home_to_config) }
        drawer.navMiPerfil.setOnClickListener { navigateFromDrawer(R.id.action_home_to_perfil) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionViewModel.drawerProfile.collect { profile ->
                    if (profile != null) {
                        drawer.tvDrawerName.text = profile.fullName
                        drawer.tvDrawerRole.text = profile.roleLabel
                        drawer.tvDrawerCompany.text = profile.company
                        drawer.tvDrawerInitials.text = profile.fullName
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() }
                    }
                }
            }
        }
    }

    private fun closeDrawer() {
        binding.drawerLayoutRoot.closeDrawer(GravityCompat.START)
    }

    /** Las acciones del drawer solo existen como hijas de assistantHomeFragment en el nav graph. */
    private fun navigateFromDrawer(actionId: Int) {
        closeDrawer()
        if (navController.currentDestination?.id == R.id.assistantHomeFragment) {
            navController.navigate(actionId)
        }
    }

    private fun observeSession() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionViewModel.destination.collect { destination ->
                    when (destination) {
                        SessionDestination.Loading -> Unit

                        SessionDestination.Login -> {
                            navigateToLogin()
                            sessionViewModel.consumeDestination()
                        }

                        SessionDestination.Home -> {
                            navigateToHome()
                            sessionViewModel.consumeDestination()
                            lifecycleScope.launch {
                                notificationTokenSync.syncCurrentToken()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun navigateToLogin() {
        if (navController.currentDestination?.id == R.id.loginFragment) {
            return
        }

        navController.navigate(
            R.id.loginFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    true
                )
                .build()
        )
    }

    private fun navigateToHome() {
        if (navController.currentDestination?.id == R.id.assistantHomeFragment) {
            return
        }

        navController.navigate(
            R.id.assistantHomeFragment,
            null,
            NavOptions.Builder()
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    true
                )
                .build()
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() ||
                super.onSupportNavigateUp()
    }
}