package com.upc.asistenteredidbi.presentation.evidence

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.databinding.FragmentEvidenceBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class EvidenceFragment : Fragment() {

    private var _binding: FragmentEvidenceBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EvidenceViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEvidenceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        val evaluationId = arguments?.getString("evaluationId") ?: return
        //viewModel.load(evaluationId)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
