package com.upc.asistenteredidbi

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.upc.asistenteredidbi.databinding.ActivityMainBinding
import com.upc.asistenteredidbi.presentation.main.SessionDestination
import com.upc.asistenteredidbi.presentation.main.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        observeSession()

        if (savedInstanceState == null) {
            sessionViewModel.checkSession()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
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