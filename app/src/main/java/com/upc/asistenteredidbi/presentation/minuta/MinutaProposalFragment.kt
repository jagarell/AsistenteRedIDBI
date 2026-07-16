package com.upc.asistenteredidbi.presentation.minuta

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentMinutaProposalBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MinutaProposalFragment : Fragment() {

    private var _binding: FragmentMinutaProposalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MinutaViewModel by viewModels()

    private lateinit var equipmentAdapter: ProposalEquipmentAdapter
    private lateinit var recommendationAdapter: ProposalRecommendationAdapter

    private val evaluationId: Long by lazy {
        arguments?.get("evaluationId")?.toString()?.toLongOrNull() ?: 1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMinutaProposalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerViews()
        setupClicks()
        loadMockEquipment()
        observeViewModel()

        viewModel.loadAnalysis()
    }

    private fun setupRecyclerViews() {
        equipmentAdapter = ProposalEquipmentAdapter()
        recommendationAdapter = ProposalRecommendationAdapter()

        binding.rvEquipment.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = equipmentAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEdit.setOnClickListener {
            findNavController().navigate(
                R.id.action_minutaProposalFragment_to_editProposalFragment,
                Bundle().apply {
                    putString("evaluationId", evaluationId.toString())
                }
            )
        }

        binding.btnSaveDraft.setOnClickListener {
            findNavController().navigate(
                R.id.action_minutaProposalFragment_to_sendDraftFragment,
                Bundle().apply {
                    putString("evaluationId", evaluationId.toString())
                }
            )
        }

        binding.btnGeneratePdf.setOnClickListener {
            Toast.makeText(requireContext(), "Generando PDF...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    if (state.isAnalyzing) {
                        Toast.makeText(requireContext(), "Cargando recomendaciones IA...", Toast.LENGTH_SHORT).show()
                    }

                    state.analysis?.let { analysis ->
                        recommendationAdapter.submitList(
                            analysis.recommendations.mapIndexed { index, text ->
                                ProposalRecommendationItem(index + 1, text)
                            }
                        )
                    }

                    state.analysisErrorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.clearAnalysisError()
                    }
                }
            }
        }
    }

    private fun loadMockEquipment() {
        equipmentAdapter.submitList(
            listOf(
                ProposalEquipmentItem(
                    R.drawable.ic_router,
                    "Cisco ISR 1100 Series Router",
                    "Router principal con firewall integrado",
                    "$450",
                    "Cant: 1"
                ),
                ProposalEquipmentItem(
                    R.drawable.ic_network,
                    "Cisco Catalyst 2960-X Switch",
                    "Switch administrable 24 puertos PoE",
                    "$760",
                    "Cant: 2"
                ),
                ProposalEquipmentItem(
                    R.drawable.ic_wifi,
                    "Ubiquiti UniFi AP AC PRO",
                    "Punto de acceso WiFi 6, cobertura 200m²",
                    "$1,000",
                    "Cant: 4"
                ),
                ProposalEquipmentItem(
                    R.drawable.ic_security,
                    "Fortinet FortiGate 60F",
                    "UTM/Firewall empresarial",
                    "$620",
                    "Cant: 1"
                ),
                ProposalEquipmentItem(
                    R.drawable.ic_energy,
                    "UPS APC Smart-UPS 1500VA",
                    "Respaldo de energía 15 min",
                    "$320",
                    "Cant: 1"
                )
            )
        )
    }

    private fun setTopologyFromBase64(base64: String) {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.imgTopology.setImageBitmap(bitmap)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}