package com.upc.asistenteredidbi.presentation.minuta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.databinding.FragmentEditProposalBinding
import com.upc.asistenteredidbi.domain.usecase.GetMinutaRecordUseCase
import com.upc.asistenteredidbi.domain.usecase.UpdateMinutaUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditProposalFragment : Fragment() {

    private var _binding: FragmentEditProposalBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var getMinutaRecordUseCase: GetMinutaRecordUseCase

    @Inject
    lateinit var updateMinutaUseCase: UpdateMinutaUseCase

    private val minutaId: Long by lazy {
        arguments?.getLong("minutaId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProposalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupClicks()
        loadCurrentData()
    }

    /** Precarga los datos reales de la minuta (si existe) para no partir en blanco. */
    private fun loadCurrentData() {
        if (minutaId <= 0) return

        viewLifecycleOwner.lifecycleScope.launch {
            getMinutaRecordUseCase(minutaId)
                .onSuccess { minuta ->
                    binding.etRestaurant.setText(minuta.clientName)
                    binding.etAddress.setText(minuta.address.orEmpty())
                    binding.etContact.setText(minuta.contactName.orEmpty())
                    binding.etPhone.setText(minuta.contactPhone.orEmpty())
                }
                .onFailure {
                    // Sin datos previos que cargar; el técnico completa desde cero.
                }
        }
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun saveChanges() {
        val clientName = binding.etRestaurant.text?.toString().orEmpty().trim()
        val address = binding.etAddress.text?.toString().orEmpty().trim()
        val contactName = binding.etContact.text?.toString().orEmpty().trim()
        val contactPhone = binding.etPhone.text?.toString().orEmpty().trim()

        if (clientName.isBlank()) {
            Toast.makeText(requireContext(), "Ingresa el nombre del establecimiento", Toast.LENGTH_SHORT).show()
            return
        }
        if (minutaId <= 0) {
            Toast.makeText(
                requireContext(),
                "No se encontró la minuta a editar (¿se creó correctamente al finalizar el chat?)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        binding.btnSave.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            updateMinutaUseCase(
                id = minutaId,
                clientName = clientName,
                address = address,
                contactName = contactName,
                contactPhone = contactPhone
            ).onSuccess {
                Toast.makeText(requireContext(), "Cambios guardados", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "No se pudieron guardar los cambios",
                    Toast.LENGTH_SHORT
                ).show()
            }

            binding.btnSave.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
