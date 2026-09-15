package com.upc.asistenteredidbi.presentation.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.databinding.FragmentPerfilBinding
import com.upc.asistenteredidbi.domain.model.toDisplayLabel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PerfilFragment : Fragment() {

    private var _binding: FragmentPerfilBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PerfilViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPerfilBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.rowNombre.tvLabel.text = "Nombre"
        binding.rowCorreo.tvLabel.text = "Correo"
        binding.rowTelefono.tvLabel.text = "Teléfono"
        binding.rowCiudad.tvLabel.text = "Ciudad"
        binding.rowEmpresa.tvLabel.text = "Empresa"

        binding.btnEditProfile.setOnClickListener { viewModel.startEditing() }
        binding.btnCancelEdit.setOnClickListener { viewModel.cancelEditing() }
        binding.btnSaveProfile.setOnClickListener {
            viewModel.onDraftFullNameChange(binding.etEditNombre.text?.toString().orEmpty())
            viewModel.onDraftPhoneChange(binding.etEditTelefono.text?.toString().orEmpty())
            viewModel.onDraftCityChange(binding.etEditCiudad.text?.toString().orEmpty())
            viewModel.saveProfile()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.user?.let { user ->
                        binding.tvName.text = user.fullName
                        binding.tvRoleCompany.text = listOfNotNull(
                            user.company?.takeIf { it.isNotBlank() },
                            user.role.toDisplayLabel()
                        ).joinToString(" · ")

                        binding.tvAvatarInitials.text = user.fullName
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .joinToString("") { it.first().uppercase() }

                        binding.rowNombre.tvValue.text = user.fullName
                        binding.rowCorreo.tvValue.text = user.email
                        binding.rowTelefono.tvValue.text = user.phone?.takeIf { it.isNotBlank() } ?: "—"
                        binding.rowCiudad.tvValue.text = user.city?.takeIf { it.isNotBlank() } ?: "—"
                        binding.rowEmpresa.tvValue.text = user.company?.takeIf { it.isNotBlank() } ?: "—"
                    }

                    state.stats?.let { stats ->
                        binding.tvStatEvaluations.text = stats.totalEvaluations.toString()
                        binding.tvStatThisMonth.text = stats.evaluationsThisMonth.toString()
                        binding.tvStatProposals.text = stats.totalProposals.toString()
                    }

                    binding.cardInfoDisplay.visibility = if (state.isEditing) View.GONE else View.VISIBLE
                    binding.btnEditProfile.visibility = if (state.isEditing) View.GONE else View.VISIBLE
                    binding.cardInfoEdit.visibility = if (state.isEditing) View.VISIBLE else View.GONE

                    if (state.isEditing) {
                        if (binding.etEditNombre.text.isNullOrEmpty()) {
                            binding.etEditNombre.setText(state.draftFullName)
                        }
                        if (binding.etEditTelefono.text.isNullOrEmpty()) {
                            binding.etEditTelefono.setText(state.draftPhone)
                        }
                        if (binding.etEditCiudad.text.isNullOrEmpty()) {
                            binding.etEditCiudad.setText(state.draftCity)
                        }
                    } else {
                        binding.etEditNombre.text?.clear()
                        binding.etEditTelefono.text?.clear()
                        binding.etEditCiudad.text?.clear()
                    }
                }
            }
        }

        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
