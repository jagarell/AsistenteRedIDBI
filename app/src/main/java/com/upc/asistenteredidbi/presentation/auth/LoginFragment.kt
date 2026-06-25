package com.upc.asistenteredidbi.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val demoEmail = "arellano0217@gmail.com"
    private val demoPassword = "1234"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvError.isVisible = false
        binding.progress.isVisible = false

        binding.btnLogin.setOnClickListener {
            //validateLogin()
            findNavController().navigate(R.id.action_login_to_home)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.tvForgot.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot)
        }
    }

    private fun validateLogin() {
        val email = binding.etEmail.text?.toString().orEmpty().trim()
        val password = binding.etPassword.text?.toString().orEmpty().trim()

        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tvError.isVisible = false

        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Ingresa tu correo"
            }

            password.isEmpty() -> {
                binding.tilPassword.error = "Ingresa tu contraseña"
            }

            email == demoEmail && password == demoPassword -> {
                findNavController().navigate(R.id.action_login_to_home)
            }

            else -> {
                binding.tvError.text = "Correo o contraseña incorrectos"
                binding.tvError.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}