package com.upc.asistenteredidbi.presentation.auth

import android.os.Bundle
import android.util.Patterns
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
import com.upc.asistenteredidbi.databinding.FragmentForgotPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForgotPasswordViewModel by viewModels()

    private var submittedEmail: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupClicks()
        observeViewModel()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSend.setOnClickListener {
            requestRecoveryCode()
        }
    }

    private fun requestRecoveryCode() {
        val email = binding.etEmail.text
            ?.toString()
            .orEmpty()
            .trim()

        clearErrors()

        when {
            email.isBlank() -> {
                binding.tilEmail.error = "Ingresa tu correo"
            }

            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Ingresa un correo válido"
            }

            else -> {
                submittedEmail = email
                viewModel.requestCode(email)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progress.isVisible = state.isLoading
                    binding.btnSend.isEnabled = !state.isLoading
                    binding.etEmail.isEnabled = !state.isLoading

                    binding.tvError.isVisible =
                        !state.errorMessage.isNullOrBlank()

                    binding.tvError.text =
                        state.errorMessage.orEmpty()

                    if (state.codeSent) {
                        // El código de recuperación se envía por correo; nunca
                        // se muestra en la app ni se registra en logs.
                        Toast.makeText(
                            requireContext(),
                            "Si el correo existe, te enviamos un código de recuperación.",
                            Toast.LENGTH_LONG
                        ).show()

                        viewModel.consumeCodeSent()

                        findNavController().navigate(
                            R.id.action_forgotPasswordFragment_to_resetPasswordFragment,
                            Bundle().apply {
                                putString("email", submittedEmail)
                                putInt(
                                    "expiresInMinutes",
                                    state.expiresInMinutes
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun clearErrors() {
        binding.tilEmail.error = null
        binding.tvError.text = ""
        binding.tvError.isVisible = false
        viewModel.clearError()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}