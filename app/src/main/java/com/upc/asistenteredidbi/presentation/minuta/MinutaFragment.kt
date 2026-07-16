package com.upc.asistenteredidbi.presentation.minuta

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.data.remote.dto.AnalysisItemDto
import com.upc.asistenteredidbi.data.remote.dto.AnalysisResponseDto
import com.upc.asistenteredidbi.databinding.FragmentMinutaBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MinutaFragment : Fragment() {

    private var _binding: FragmentMinutaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MinutaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMinutaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupClicks()
        observeViewModel()
        viewModel.loadAnalysis()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnTechnicalProposal.setOnClickListener {
            findNavController().navigate(
                R.id.action_minutaFragment_to_minutaProposalFragment,
                Bundle().apply {
                    // Reenvía el evaluationId real y todo lo recibido del chat
                    // (resumen, topología, equipo, score); antes se perdía.
                    putString("evaluationId", viewModel.evaluationId)
                    putString("proposalSummary", arguments?.getString("proposalSummary").orEmpty())
                    putString("topologyText", arguments?.getString("topologyText").orEmpty())
                    putString("equipmentJson", arguments?.getString("equipmentJson").orEmpty())
                    putString("topologyJson", arguments?.getString("topologyJson").orEmpty())
                    putInt("score", arguments?.getInt("score", -1) ?: -1)
                    putLong("minutaId", arguments?.getLong("minutaId", -1L) ?: -1L)
                }
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.isAnalyzing) {
                        binding.tvAiSummary.text = "Analizando infraestructura con IA..."
                    }

                    state.analysis?.let { renderAnalysis(it) }

                    state.analysisErrorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.clearAnalysisError()
                    }
                }
            }
        }
    }

    private fun renderAnalysis(analysis: AnalysisResponseDto) {
        binding.progressGlobal.progress = analysis.globalScore
        binding.tvGlobalScorePercent.text = "${analysis.globalScore}%"
        binding.tvAreasEvaluated.text = "${analysis.evaluatedAreas}\nÁreas\nevaluadas"
        binding.tvAttentionRequired.text = "${analysis.attentionRequired}\nRequieren\natención"
        binding.tvRecommendationsCount.text = "${analysis.recommendations.size}\nRecomendaciones"
        binding.tvAiSummary.text = analysis.summary

        binding.layoutConnectivity.let {
            bindRow(it.tvStatus, it.tvPercent, it.tvDescription, it.progressBar,
                analysis.results.find { r -> r.title.contains("Conectividad", ignoreCase = true) })
        }
        binding.layoutAnalysisPhysical.let {
            bindRow(it.tvStatus, it.tvPercent, it.tvDescription, it.progressBar,
                analysis.results.find { r -> r.title.contains("Física", ignoreCase = true) || r.title.contains("Fisica", ignoreCase = true) })
        }
        binding.layoutAnalysisEquipment.let {
            bindRow(it.tvStatus, it.tvPercent, it.tvDescription, it.progressBar,
                analysis.results.find { r -> r.title.contains("Equipamiento", ignoreCase = true) })
        }
        binding.layoutAnalysisWifi.let {
            bindRow(it.tvStatus, it.tvPercent, it.tvDescription, it.progressBar,
                analysis.results.find { r -> r.title.contains("WiFi", ignoreCase = true) })
        }
    }

    private fun bindRow(
        tvStatus: TextView,
        tvPercent: TextView,
        tvDescription: TextView,
        progressBar: ProgressBar,
        item: AnalysisItemDto?
    ) {
        if (item == null) return

        val color = colorFor(item.color)

        tvStatus.text = item.status
        tvStatus.setTextColor(color)
        tvPercent.text = "${item.score}%"
        tvPercent.setTextColor(color)
        tvDescription.text = item.description

        progressBar.progress = item.score
        progressBar.progressTintList = ColorStateList.valueOf(color)
    }

    private fun colorFor(colorName: String): Int = when (colorName.lowercase()) {
        "green" -> Color.parseColor("#2E7D32")
        "orange" -> Color.parseColor("#F57C00")
        "red" -> Color.parseColor("#D32F2F")
        else -> Color.parseColor("#1565C0")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
