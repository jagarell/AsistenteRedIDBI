package com.upc.asistenteredidbi.presentation.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialState()
        setupClicks()
        observeViewModel()
    }

    private fun setupInitialState() {
        binding.tvError.isVisible = false
        binding.progress.isVisible = false
    }

    private fun setupClicks() {
        binding.btnLogin.setOnClickListener {
            validateAndLogin()
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(
                R.id.action_login_to_register
            )
        }

        binding.tvForgot.setOnClickListener {
            findNavController().navigate(
                R.id.action_login_to_forgot
            )
        }
    }

    private fun validateAndLogin() {
        val email = binding.etEmail.text
            ?.toString()
            .orEmpty()
            .trim()

        val password = binding.etPassword.text
            ?.toString()
            .orEmpty()

        clearErrors()

        when {
            email.isBlank() -> {
                binding.tilEmail.error = "Ingresa tu correo"
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Ingresa un correo válido"
            }

            password.isBlank() -> {
                binding.tilPassword.error = "Ingresa tu contraseña"
            }

            else -> {
                viewModel.onEmailChange(email)
                viewModel.onPasswordChange(password)
                viewModel.login()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progress.isVisible = state.isLoading
                    binding.btnLogin.isEnabled = !state.isLoading
                    binding.etEmail.isEnabled = !state.isLoading
                    binding.etPassword.isEnabled = !state.isLoading

                    binding.tvError.isVisible =
                        !state.errorMessage.isNullOrBlank()

                    binding.tvError.text =
                        state.errorMessage.orEmpty()

                    if (state.loginSuccess) {
                        viewModel.consumeLoginSuccess()

                        findNavController().navigate(
                            R.id.action_login_to_home
                        )
                    }
                }
            }
        }
    }

    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tvError.text = ""
        binding.tvError.isVisible = false
        viewModel.clearError()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}