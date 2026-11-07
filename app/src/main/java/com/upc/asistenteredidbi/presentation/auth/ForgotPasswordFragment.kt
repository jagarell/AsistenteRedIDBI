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
                solicitarAprobacion(email)
            }
        }
    }

    @javax.inject.Inject lateinit var api007: com.upc.asistenteredidbi.data.remote.Api007Service

    /** Flujo tipo Google: el administrador ve un número de 2 dígitos y el técnico elige ese número entre 3 (doc 0.0.7 §36). */
    private fun solicitarAprobacion(email: String) {
        binding.progress.isVisible = true
        binding.btnSend.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val r = runCatching { api007.recuperacionSolicitar(mapOf("email" to email)) }.getOrNull()
            if (_binding == null) return@launch
            binding.progress.isVisible = false
            binding.btnSend.isEnabled = true
            if (r == null) { binding.tvError.isVisible = true; binding.tvError.text = "No se pudo enviar la solicitud. Revisa tu conexión."; return@launch }
            val id = (r["solicitudId"] as? Number)?.toLong() ?: -1L
            val opciones = (r["opciones"] as? List<*>)?.map { it.toString() }.orEmpty()
            elegirNumero(id, opciones)
        }
    }

    private fun elegirNumero(id: Long, opciones: List<String>) {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(ctx)
        val col = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (24 * dp).toInt()) }
        col.addView(android.widget.TextView(ctx).apply { text = "Elige el código de tu administrador"; textSize = 19f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(0xFF1F2937.toInt()) })
        col.addView(android.widget.TextView(ctx).apply { text = "Tu administrador recibió una notificación con un número de 2 dígitos. Pídeselo en persona o por llamada y elígelo aquí. Vence en 10 minutos."; textSize = 14f; setTextColor(0xFF6B7280.toInt()); setPadding(0, (6 * dp).toInt(), 0, (14 * dp).toInt()) })
        val fila = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER }
        opciones.forEach { n ->
            fila.addView(android.widget.TextView(ctx).apply {
                text = n; textSize = 30f; typeface = android.graphics.Typeface.DEFAULT_BOLD; gravity = android.view.Gravity.CENTER; setTextColor(0xFF1565C0.toInt())
                background = android.graphics.drawable.GradientDrawable().apply { setColor(0xFFE8F1FD.toInt()); cornerRadius = 20 * dp }
                layoutParams = android.widget.LinearLayout.LayoutParams(0, (84 * dp).toInt(), 1f).apply { marginStart = (6 * dp).toInt(); marginEnd = (6 * dp).toInt() }
                setOnClickListener { sheet.dismiss(); responder(id, n) }
            })
        }
        col.addView(fila)
        col.addView(android.widget.TextView(ctx).apply { text = "Cancelar"; textSize = 15f; typeface = android.graphics.Typeface.DEFAULT_BOLD; gravity = android.view.Gravity.CENTER; setTextColor(0xFF1565C0.toInt()); setPadding(0, (18 * dp).toInt(), 0, 0); setOnClickListener { sheet.dismiss() } })
        sheet.setContentView(col)
        sheet.show()
    }

    private fun responder(id: Long, numero: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val r = runCatching { api007.recuperacionRespuesta(id, mapOf("numero" to numero)) }.getOrNull()
            if (_binding == null) return@launch
            if (r?.get("resultado") == "aprobada") {
                findNavController().navigate(
                    R.id.action_forgotPasswordFragment_to_resetPasswordFragment,
                    Bundle().apply { putString("email", r["email"].toString()); putString("code", r["codigo"].toString()); putInt("expiresInMinutes", 10) }
                )
            } else {
                androidx.appcompat.app.AlertDialog.Builder(requireContext()).setTitle("Solicitud rechazada")
                    .setMessage(if (r == null) "No se pudo comprobar el código. Revisa tu conexión y pide la solicitud de nuevo." else "El número no coincide o venció. Pide la solicitud de nuevo.")
                    .setPositiveButton("Entendido", null).show()
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