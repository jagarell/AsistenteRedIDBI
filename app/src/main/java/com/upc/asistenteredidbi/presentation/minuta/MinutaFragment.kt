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
import android.app.AlertDialog
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
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
import com.upc.asistenteredidbi.domain.usecase.MinutaEditionsUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MinutaFragment : Fragment() {

    private var _binding: FragmentMinutaBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MinutaViewModel by viewModels()

    @Inject
    lateinit var editionsUseCase: MinutaEditionsUseCase

    /** Observaciones del técnico ya guardadas (para no volver a mostrar el aviso). */
    private var observations: MutableMap<String, Any?> = mutableMapOf()
    private var lastAnalysis: AnalysisResponseDto? = null

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
        viewLifecycleOwner.lifecycleScope.launch {
            val id = viewModel.evaluationId.toLongOrNull() ?: return@launch
            editionsUseCase.load(id).onSuccess { editions ->
                @Suppress("UNCHECKED_CAST")
                observations = ((editions["obs"] as? Map<String, Any?>) ?: emptyMap()).toMutableMap()
                lastAnalysis?.let { renderExtras(it) }
            }
        }
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

        lastAnalysis = analysis
        renderExtras(analysis)
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
        val score = item.score

        tvStatus.text = item.status
        tvStatus.setTextColor(color)
        tvPercent.text = if (score == null) "—" else "$score%"
        tvPercent.setTextColor(color)
        tvDescription.text = item.description

        progressBar.progress = score ?: 0
        progressBar.progressTintList = ColorStateList.valueOf(color)
        addCriteria(tvDescription, item, color)
    }

    /** "Ver criterios": cada criterio con su peso y cuánto cumple (suma ponderada). */
    private fun addCriteria(description: TextView, item: AnalysisItemDto, color: Int) {
        val parent = description.parent as? LinearLayout ?: return
        parent.findViewWithTag<View>("criteria")?.let { parent.removeView(it) }
        if (item.criteria.isEmpty()) return
        val dp = resources.displayMetrics.density
        val box = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; tag = "criteria" }
        val list = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val toggle = TextView(requireContext()).apply {
            text = "Ver criterios ▾"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1565C0"))
            setPadding(0, (10 * dp).toInt(), 0, (4 * dp).toInt())
            setOnClickListener {
                val open = list.visibility == View.GONE
                list.visibility = if (open) View.VISIBLE else View.GONE
                text = if (open) "Ocultar criterios ▴" else "Ver criterios ▾"
            }
        }
        list.addView(TextView(requireContext()).apply {
            text = "Puntaje = Σ peso × cumplimiento ÷ Σ pesos que aplican. Pesos por criticidad; se validan con AHP."
            textSize = 12f
            setTextColor(Color.parseColor("#6B7280"))
            setPadding(0, 0, 0, (6 * dp).toInt())
        })
        item.criteria.forEach { c ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(0, (6 * dp).toInt(), 0, 0) }
            val head = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            head.addView(TextView(requireContext()).apply {
                text = c.name; textSize = 13f; setTextColor(Color.parseColor("#1F2937"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            head.addView(TextView(requireContext()).apply {
                val got = c.compliance?.let { Math.round(it * c.weight * 10) / 10.0 }
                text = if (got == null) "N/A" else "${if (got == got.toLong().toDouble()) got.toLong().toString() else got.toString()}/${c.weight}"
                textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(color)
            })
            row.addView(head)
            row.addView(ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100; progress = ((c.compliance ?: 0.0) * 100).toInt()
                progressTintList = ColorStateList.valueOf(color)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (6 * dp).toInt())
            })
            row.addView(TextView(requireContext()).apply { text = c.detail; textSize = 12f; setTextColor(Color.parseColor("#6B7280")) })
            list.addView(row)
        }
        box.addView(toggle)
        box.addView(list)
        parent.addView(box, parent.indexOfChild(description) + 1)
    }

    // ---- observaciones y referencias ---------------------------------------------------
    private fun renderExtras(analysis: AnalysisResponseDto) {
        val container = binding.containerExtras
        container.removeAllViews()
        val dp = resources.displayMetrics.density
        val hasObs = observations.values.any { v -> v != null && v != "" && v != emptyList<Any>() }
        if (!hasObs) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(Color.parseColor("#FFF7E6"))
                setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = (18 * dp).toInt() }
            }
            row.addView(TextView(requireContext()).apply {
                text = "✎  Antes de cerrar, agrega tus observaciones."
                textSize = 14f; setTextColor(Color.parseColor("#B45309"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(TextView(requireContext()).apply {
                text = "Agregar"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#1565C0"))
                setOnClickListener { showObservationsDialog() }
            })
            container.addView(row)
        }
        analysis.references?.let { refs ->
            if (refs.similar.isEmpty()) return@let
            val box = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_home_card)
                elevation = 5 * dp
                setPadding((20 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt(), (16 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = (18 * dp).toInt() }
            }
            val body = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
            val toggle = TextView(requireContext()).apply {
                text = "Referencias ▾"; textSize = 17f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#1F2937"))
                setOnClickListener {
                    val open = body.visibility == View.GONE
                    body.visibility = if (open) View.VISIBLE else View.GONE
                    text = if (open) "Referencias ▴" else "Referencias ▾"
                }
            }
            fun head(t: String) = TextView(requireContext()).apply {
                text = t; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#1565C0"))
                setPadding(0, (12 * dp).toInt(), 0, (4 * dp).toInt())
            }
            fun line(t: String, sub: String? = null) = TextView(requireContext()).apply {
                text = if (sub.isNullOrBlank()) t else "$t\n$sub"; textSize = 13f; setTextColor(Color.parseColor("#374151"))
                setPadding(0, (3 * dp).toInt(), 0, (3 * dp).toInt())
            }
            body.addView(line("Solo para el técnico: no aparece en la minuta ni en el PDF."))
            body.addView(head("Casos más parecidos"))
            refs.similar.forEach { body.addView(line("${it.name} · ${it.pct}%", "${it.rubro} · ${it.why}")) }
            if (refs.recommendations.isNotEmpty()) {
                body.addView(head("Tus recomendaciones en minutas anteriores"))
                refs.recommendations.forEach { body.addView(line(it.t, if (it.n > 0) "visto en ${it.n} minuta${if (it.n > 1) "s" else ""}" else "aún no vista en minutas")) }
            }
            if (refs.suggested.isNotEmpty()) {
                body.addView(head("También se recomendó en casos parecidos"))
                refs.suggested.forEach { sg ->
                    body.addView(line(sg.t, "${sg.d}\nEn ${sg.n} de 5 casos parecidos. Agrégala solo si lo viste en la visita."))
                    body.addView(TextView(requireContext()).apply {
                        text = "Agregar a la propuesta"; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#1565C0"))
                        setOnClickListener { addSuggestionToProposal(sg.t, sg.d, this) }
                    })
                }
            }
            box.addView(toggle)
            box.addView(body)
            container.addView(box)
        }
    }

    /** Lo agregado entra a la propuesta como texto propio (no cita minutas). */
    private fun addSuggestionToProposal(title: String, detail: String, button: TextView) {
        val id = viewModel.evaluationId.toLongOrNull() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val editions = editionsUseCase.load(id).getOrNull().orEmpty().toMutableMap()
            @Suppress("UNCHECKED_CAST")
            val ed = ((editions["ed"] as? Map<String, Any?>) ?: emptyMap()).toMutableMap()
            @Suppress("UNCHECKED_CAST")
            val tobe = ((ed["tobe"] as? List<Map<String, Any?>>) ?: lastAnalysis?.recommendationItems.orEmpty().filter { it.type != "AVISO" }.map { mapOf("t" to it.title, "d" to it.detail) }).toMutableList()
            if (tobe.none { it["t"] == title }) tobe.add(mapOf("t" to title, "d" to detail))
            ed["tobe"] = tobe
            editions["ed"] = ed
            editionsUseCase.save(id, editions).onSuccess {
                button.text = "Agregada"
                button.setTextColor(Color.parseColor("#2E7D32"))
                button.isClickable = false
            }.onFailure { Toast.makeText(requireContext(), it.message ?: "No se pudo agregar", Toast.LENGTH_SHORT).show() }
        }
    }

    /** Observaciones: se guardan en la minuta y salen en el PDF. */
    private fun showObservationsDialog() {
        ObservationsDialog.show(requireContext(), observations) { obs ->
            val id = viewModel.evaluationId.toLongOrNull() ?: return@show
            viewLifecycleOwner.lifecycleScope.launch {
                val editions = editionsUseCase.load(id).getOrNull().orEmpty().toMutableMap()
                editions["obs"] = obs
                editionsUseCase.save(id, editions)
                    .onSuccess {
                        observations = obs.toMutableMap()
                        lastAnalysis?.let { renderExtras(it) }
                        Toast.makeText(requireContext(), "Observaciones guardadas en la minuta", Toast.LENGTH_SHORT).show()
                    }
                    .onFailure { Toast.makeText(requireContext(), it.message ?: "No se pudieron guardar", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun colorFor(colorName: String): Int = when (colorName.lowercase()) {
        "green" -> Color.parseColor("#2E7D32")
        "orange" -> Color.parseColor("#F57C00")
        "red" -> Color.parseColor("#D32F2F")
        "gray" -> Color.parseColor("#90A4AE")
        else -> Color.parseColor("#1565C0")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
