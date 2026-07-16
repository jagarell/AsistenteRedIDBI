package com.upc.asistenteredidbi.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentResetPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ResetPasswordFragment : Fragment() {

    private var _binding: FragmentResetPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ResetPasswordViewModel by viewModels()

    private val email: String by lazy {
        arguments?.getString("email").orEmpty()
    }

    private val expiresInMinutes: Int by lazy {
        arguments?.getInt("expiresInMinutes") ?: 10
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResetPasswordBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvInstructions.text =
            "Ingresa el código generado para $email. El código vence en $expiresInMinutes minutos."

        setupClicks()
        observeViewModel()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnResetPassword.setOnClickListener {
            validateAndResetPassword()
        }
    }

    private fun validateAndResetPassword() {
        val code = binding.etCode.text
            ?.toString()
            .orEmpty()
            .trim()

        val password = binding.etPassword.text
            ?.toString()
            .orEmpty()

        val confirmPassword = binding.etConfirmPassword.text
            ?.toString()
            .orEmpty()

        clearErrors()

        when {
            email.isBlank() -> {
                binding.tvError.text =
                    "No se recibió el correo de recuperación"
                binding.tvError.isVisible = true
            }

            code.length != 6 -> {
                binding.tilCode.error =
                    "Ingresa el código de 6 dígitos"
            }

            password.length < 8 -> {
                binding.tilPassword.error =
                    "La contraseña debe tener al menos 8 caracteres"
            }

            confirmPassword != password -> {
                binding.tilConfirmPassword.error =
                    "Las contraseñas no coinciden"
            }

            else -> {
                viewModel.resetPassword(
                    email = email,
                    code = code,
                    newPassword = password,
                    confirmPassword = confirmPassword
                )
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progress.isVisible = state.isLoading
                    binding.btnResetPassword.isEnabled =
                        !state.isLoading

                    binding.tvError.isVisible =
                        !state.errorMessage.isNullOrBlank()

                    binding.tvError.text =
                        state.errorMessage.orEmpty()

                    if (state.passwordChanged) {
                        Toast.makeText(
                            requireContext(),
                            state.successMessage
                                ?: "Contraseña actualizada correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                        viewModel.consumePasswordChanged()

                        findNavController().navigate(
                            R.id.action_resetPasswordFragment_to_loginFragment
                        )
                    }
                }
            }
        }
    }

    private fun clearErrors() {
        binding.tilCode.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tvError.text = ""
        binding.tvError.isVisible = false
        viewModel.clearError()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}