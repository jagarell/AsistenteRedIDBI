package com.upc.asistenteredidbi.presentation.minuta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.databinding.FragmentEditProposalBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EditProposalFragment : Fragment() {

    private var _binding: FragmentEditProposalBinding? = null
    private val binding get() = _binding!!

    private val evaluationId: String by lazy {
        arguments?.getString("evaluationId") ?: "demo-evaluation-001"
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
        setupInitialData()
        setupClicks()
    }

    private fun setupInitialData() {
        binding.etRestaurant.setText("Restaurante El Rincón")
        binding.etAddress.setText("Av. Insurgentes Sur 1234, CDMX")
        binding.etContact.setText("Roberto García")
        binding.etPhone.setText("+52 55 1234 5678")
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSave.setOnClickListener {
            Toast.makeText(requireContext(), "Cambios guardados", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}