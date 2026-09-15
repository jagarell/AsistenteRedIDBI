package com.upc.asistenteredidbi.presentation.configuration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentConfiguracionBinding
import com.upc.asistenteredidbi.presentation.main.SessionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ConfiguracionFragment : Fragment() {

    private var _binding: FragmentConfiguracionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ConfiguracionViewModel by viewModels()
    private val sessionViewModel: SessionViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConfiguracionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        setupStaticRows()
        setupSwitches()

        binding.rowLogout.setOnClickListener {
            sessionViewModel.logout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.user?.let { user ->
                        binding.rowMiPerfil.tvRowSubtitle.visibility = View.VISIBLE
                        binding.rowMiPerfil.tvRowSubtitle.text = user.fullName
                        binding.rowEmpresa.tvRowSubtitle.visibility = View.VISIBLE
                        binding.rowEmpresa.tvRowSubtitle.text = user.company?.takeIf { it.isNotBlank() } ?: "—"
                        binding.rowCorreo.tvRowSubtitle.visibility = View.VISIBLE
                        binding.rowCorreo.tvRowSubtitle.text = user.email
                    }

                    binding.switchTwoFactor.isChecked = state.twoFactorAuth
                    binding.switchPush.isChecked = state.pushNotifications
                    binding.switchEmail.isChecked = state.emailNotifications
                    binding.switchSystemAlerts.isChecked = state.systemAlerts
                }
            }
        }

        viewModel.load()
    }

    private fun setupStaticRows() {
        binding.rowMiPerfil.apply {
            ivRowIcon.setImageResource(R.drawable.ic_account_circle)
            tvRowTitle.text = "Mi Perfil"
            root.setOnClickListener { findNavController().navigate(R.id.action_config_to_perfil) }
        }
        binding.rowEmpresa.apply {
            ivRowIcon.setImageResource(R.drawable.ic_building)
            tvRowTitle.text = "Empresa"
        }
        binding.rowCorreo.apply {
            ivRowIcon.setImageResource(R.drawable.ic_email)
            tvRowTitle.text = "Correo Electrónico"
        }
        binding.rowCambiarPassword.apply {
            ivRowIcon.setImageResource(R.drawable.ic_lock_outline)
            tvRowTitle.text = "Cambiar Contraseña"
            root.setOnClickListener { showComingSoon() }
        }
        binding.rowDispositivos.apply {
            ivRowIcon.setImageResource(R.drawable.ic_devices)
            tvRowTitle.text = "Dispositivos Activos"
            root.setOnClickListener { showComingSoon() }
        }
        binding.rowIdioma.apply {
            ivRowIcon.setImageResource(R.drawable.ic_language)
            tvRowTitle.text = "Idioma"
            tvRowSubtitle.visibility = View.VISIBLE
            tvRowSubtitle.text = "Español"
            root.setOnClickListener { showComingSoon() }
        }
        binding.rowVersion.apply {
            ivRowIcon.setImageResource(R.drawable.ic_info)
            tvRowTitle.text = "Versión"
            tvRowSubtitle.visibility = View.VISIBLE
            tvRowSubtitle.text = runCatching {
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
            }.getOrNull() ?: "—"
            ivChevron.visibility = View.INVISIBLE
        }
        binding.rowSoporte.apply {
            ivRowIcon.setImageResource(R.drawable.ic_help)
            tvRowTitle.text = "Soporte Técnico"
            root.setOnClickListener { showComingSoon() }
        }
    }

    private fun setupSwitches() {
        binding.switchTwoFactor.setOnCheckedChangeListener { _, checked -> viewModel.setTwoFactorAuth(checked) }
        binding.switchPush.setOnCheckedChangeListener { _, checked -> viewModel.setPushNotifications(checked) }
        binding.switchEmail.setOnCheckedChangeListener { _, checked -> viewModel.setEmailNotifications(checked) }
        binding.switchSystemAlerts.setOnCheckedChangeListener { _, checked -> viewModel.setSystemAlerts(checked) }
    }

    private fun showComingSoon() {
        Toast.makeText(requireContext(), "Próximamente", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
