package com.upc.asistenteredidbi.presentation.minuta

import android.os.Bundle
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
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.databinding.FragmentMinutaProposalBinding
import com.upc.asistenteredidbi.domain.model.ChatTopology
import com.upc.asistenteredidbi.domain.model.ProposalPdfData
import com.upc.asistenteredidbi.domain.model.TechnicalEquipmentRecommendation
import com.upc.asistenteredidbi.domain.usecase.GenerateProposalPdfUseCase
import com.upc.asistenteredidbi.presentation.common.PdfFileUtils
import com.upc.asistenteredidbi.presentation.common.toHierarchicalText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MinutaProposalFragment : Fragment() {

    private var _binding: FragmentMinutaProposalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MinutaViewModel by viewModels()

    private lateinit var equipmentAdapter: ProposalEquipmentAdapter
    private lateinit var recommendationAdapter: ProposalRecommendationAdapter

    @Inject
    lateinit var moshi: Moshi

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var generateProposalPdfUseCase: GenerateProposalPdfUseCase

    private val evaluationId: Long by lazy {
        arguments?.get("evaluationId")?.toString()?.toLongOrNull() ?: 1L
    }

    private var currentEquipment: List<TechnicalEquipmentRecommendation> = emptyList()
    private var currentRecommendations: List<String> = emptyList()
    private var generatedPdfFile: File? = null

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
        renderProposalFromChat()
        observeViewModel()
        observeTechnicianName()

        viewModel.load()
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
                    putString("pdfPath", generatedPdfFile?.absolutePath.orEmpty())
                }
            )
        }

        binding.btnGeneratePdf.setOnClickListener {
            generatePdf()
        }
    }

    /** Pide al gateway el PDF real (bytes) y lo guarda/abre localmente. */
    private fun generatePdf() {
        val data = ProposalPdfData(
            establishmentName = binding.tvEstablishmentName.text.toString(),
            address = binding.tvEstablishmentAddress.text.toString(),
            technicianName = binding.tvTechnicianName.text.toString(),
            score = arguments?.getInt("score", -1)?.takeIf { it >= 0 },
            summary = arguments?.getString("proposalSummary"),
            recommendations = currentRecommendations,
            equipment = currentEquipment,
            topologyText = binding.tvTopologyDetail.text.toString()
        )

        binding.btnGeneratePdf.isEnabled = false
        binding.btnGeneratePdf.text = "Generando..."

        viewLifecycleOwner.lifecycleScope.launch {
            generateProposalPdfUseCase(data)
                .onSuccess { bytes ->
                    val file = PdfFileUtils.saveToCache(
                        requireContext(),
                        bytes,
                        "propuesta_$evaluationId.pdf"
                    )
                    generatedPdfFile = file
                    PdfFileUtils.openPdf(requireContext(), file)
                }
                .onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "No se pudo generar el PDF",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            binding.btnGeneratePdf.isEnabled = true
            binding.btnGeneratePdf.text = "Generar PDF"
        }
    }

    /** Pinta el resumen, el equipo y la topología que llegaron desde el chat. */
    private fun renderProposalFromChat() {
        val today = SimpleDateFormat("d MMM yyyy", Locale("es", "ES")).format(Date())
        binding.tvDate.text = today

        val equipmentJson = arguments?.getString("equipmentJson").orEmpty()
        val equipment = parseEquipment(equipmentJson)
        currentEquipment = equipment

        if (equipment.isNotEmpty()) {
            equipmentAdapter.submitList(equipment.map { item ->
                ProposalEquipmentItem(
                    iconRes = iconFor(item.name),
                    name = item.name,
                    description = item.description,
                    price = "",
                    quantity = "Cant: ${item.quantity}"
                )
            })
            binding.tvTotal.text = equipment.sumOf { it.quantity }.toString()
        }

        val topologyJson = arguments?.getString("topologyJson").orEmpty()
        val topology = parseTopology(topologyJson)
        val topologyText = arguments?.getString("topologyText").orEmpty()

        binding.tvTopologyDetail.text = when {
            topology != null -> topology.toHierarchicalText()
            topologyText.isNotBlank() -> topologyText
            else -> "Topología no disponible."
        }
    }

    private fun parseEquipment(json: String): List<TechnicalEquipmentRecommendation> {
        if (json.isBlank()) return emptyList()
        return try {
            val type = Types.newParameterizedType(
                List::class.java,
                TechnicalEquipmentRecommendation::class.java
            )
            moshi.adapter<List<TechnicalEquipmentRecommendation>>(type).fromJson(json).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseTopology(json: String): ChatTopology? {
        if (json.isBlank()) return null
        return try {
            moshi.adapter(ChatTopology::class.java).fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun iconFor(equipmentName: String): Int {
        val name = equipmentName.lowercase()
        return when {
            "router" in name -> R.drawable.ic_router
            "switch" in name -> R.drawable.ic_network
            "access point" in name || "wifi" in name -> R.drawable.ic_wifi
            "firewall" in name || "seguridad" in name -> R.drawable.ic_security
            "ups" in name || "energía" in name || "energia" in name -> R.drawable.ic_energy
            else -> R.drawable.ic_network
        }
    }

    private fun observeTechnicianName() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.fullNameFlow.collect { name ->
                    binding.tvTechnicianName.text = name?.takeIf { it.isNotBlank() } ?: "-"
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    state.minuta?.let { minuta ->
                        binding.tvEstablishmentName.text = minuta.establishmentName
                        binding.tvEstablishmentAddress.text =
                            minuta.establishmentAddress?.takeIf { it.isNotBlank() }
                                ?: "Propuesta de Infraestructura de Red"
                    }

                    state.analysis?.let { analysis ->
                        currentRecommendations = analysis.recommendations
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

                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.clearGeneralError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
