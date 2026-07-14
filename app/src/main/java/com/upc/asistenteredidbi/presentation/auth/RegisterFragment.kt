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
import com.google.android.gms.internal.mlkit_common.zzrq
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeViewModel()

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        binding.btnRegister.setOnClickListener {
            validateRegister()
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun validateRegister() {
        val name = binding.etName.text?.toString().orEmpty().trim()
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val phone = binding.etPhone.text?.toString().orEmpty().trim()
        val company = binding.etCompany.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty()

        clearErrors()

        when {
            name.isEmpty() ->
                binding.tilName.error = "Ingresa tu nombre completo"

            email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                binding.tilEmail.error = "Ingresa un correo válido"

            phone.isEmpty() ->
                binding.tilPhone.error = "Ingresa tu teléfono"

            company.isEmpty() ->
                binding.tilCompany.error = "Ingresa tu empresa"

            password.length < 8 ->
                binding.tilPassword.error = "Mínimo 8 caracteres"

            confirmPassword != password ->
                binding.tilConfirmPassword.error = "Las contraseñas no coinciden"

            else -> {
                viewModel.register(
                    fullName = name,
                    email = email,
                    phone = phone,
                    company = company,
                    city = "Lima",
                    password = password,
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
                    binding.btnRegister.isEnabled = !state.isLoading

                    binding.tvError.isVisible = state.errorMessage != null
                    binding.tvError.text = state.errorMessage.orEmpty()

                    if (state.isRegistered) {
                        Toast.makeText(
                            requireContext(),
                            state.successMessage ?: "Usuario registrado correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        viewModel.consumeRegistration()

                        findNavController().navigate(
                            R.id.action_register_to_login
                        )
                    }
                }
            }
        }
    }

    private fun clearErrors() {
        binding.tilName.error = null
        binding.tilEmail.error = null
        binding.tilPhone.error = null
        binding.tilCompany.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tvError.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}