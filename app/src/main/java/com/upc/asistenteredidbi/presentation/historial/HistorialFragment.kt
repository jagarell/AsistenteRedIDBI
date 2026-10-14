package com.upc.asistenteredidbi.presentation.historial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentHistorialBinding
import com.upc.asistenteredidbi.domain.model.EvaluationStage
import com.upc.asistenteredidbi.domain.model.EvaluationSummaryItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HistorialFragment : Fragment() {

    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistorialViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecycler()
        setupClicks()
        observeViewModel()
        viewModel.loadHistorial()
    }

    private fun setupRecycler() {
        adapter = HistoryAdapter { item ->
            // Igual que el prototipo: con propuesta se abre el análisis (y de ahí la minuta); si el chat no terminó, se retoma.
            if (item.stage.isFinished) {
                findNavController().navigate(
                    R.id.action_historial_to_minuta,
                    Bundle().apply { putString("evaluationId", item.id) }
                )
            } else {
                findNavController().navigate(
                    R.id.action_historial_to_chat,
                    Bundle().apply { putString("evaluationId", item.id) }
                )
            }
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
        attachAnnulSwipe()
    }

    /**
     * Deslizar a la izquierda anula la evaluación, pero solo si está en borrador
     * (lo completado, enviado o en análisis no se puede anular).
     */
    private fun attachAnnulSwipe() {
        val density = resources.displayMetrics.density
        val red = android.graphics.Paint().apply { color = android.graphics.Color.parseColor("#C62828") }
        val label = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 15f * density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        val callback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
            0, androidx.recyclerview.widget.ItemTouchHelper.LEFT
        ) {
            override fun getSwipeDirs(rv: androidx.recyclerview.widget.RecyclerView, vh: androidx.recyclerview.widget.RecyclerView.ViewHolder): Int =
                if (adapter.itemAt(vh.bindingAdapterPosition)?.stage == EvaluationStage.BORRADOR) super.getSwipeDirs(rv, vh) else 0

            override fun onMove(
                rv: androidx.recyclerview.widget.RecyclerView,
                vh: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(vh: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = vh.bindingAdapterPosition
                val item = adapter.itemAt(position) ?: return
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Anular evaluación")
                    .setMessage("¿Anular «${item.establishmentName}»? Es un borrador y se descarta junto con su chat; no se puede deshacer.")
                    .setPositiveButton("Anular") { _, _ -> viewModel.annul(item.id) }
                    .setNegativeButton("Cancelar") { _, _ -> adapter.notifyItemChanged(position) }
                    .setOnCancelListener { adapter.notifyItemChanged(position) }
                    .show()
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                rv: androidx.recyclerview.widget.RecyclerView,
                vh: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isActive: Boolean
            ) {
                val v = vh.itemView
                if (dX < 0) {
                    val radius = 24f * density
                    c.drawRoundRect(v.right + dX, v.top.toFloat() + 4 * density, v.right.toFloat(), v.bottom.toFloat() - 4 * density, radius, radius, red)
                    c.drawText("Anular", v.right - 24f * density, v.top + v.height / 2f + 5 * density, label)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        }
        androidx.recyclerview.widget.ItemTouchHelper(callback).attachToRecyclerView(binding.rvHistory)
    }

    /** El embudo abre la hoja "Filtrar por fecha" del prototipo (rápidos + rango Desde/Hasta). */
    private fun showDateFilter() {
        DateFilterSheet.show(
            requireContext(), viewModel.uiState.value,
            onQuick = { viewModel.setQuickDate(it) },
            onRange = { from, to -> viewModel.setDateRange(from, to) }
        )
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.fabNewEvaluation.setOnClickListener {
            viewModel.requestNewEvaluation {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Empezar una evaluación nueva")
                    .setMessage("Tienes una evaluación previa guardada. Si empiezas una nueva, el chat anterior se borra.")
                    .setPositiveButton("Empezar nueva") { _, _ -> viewModel.discardPreviousAndStartNew() }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        binding.btnFilter.setOnClickListener { showDateFilter() }
        binding.tvDateFilter.setOnClickListener { viewModel.clearDateRange() }

        binding.etSearch.addTextChangedListener { text ->
            viewModel.onSearchQueryChange(text?.toString().orEmpty())
        }

        binding.chipAll.setOnClickListener { viewModel.onTabSelected(HistorialTab.TODOS) }
        binding.chipCompleted.setOnClickListener { viewModel.onTabSelected(HistorialTab.COMPLETADOS) }
        binding.chipDrafts.setOnClickListener { viewModel.onTabSelected(HistorialTab.BORRADORES) }
        binding.chipAnalysis.setOnClickListener { viewModel.onTabSelected(HistorialTab.EN_ANALISIS) }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Los contadores siguen el filtro de fecha (no el de estado).
                    val allItems = state.inDateRange.map { it.toHistoryItem() }
                    val filtering = state.dateFrom != null || state.dateTo != null
                    binding.tvDateFilter.isVisible = filtering
                    if (filtering) {
                        val fmt: (String) -> String = { com.upc.asistenteredidbi.presentation.common.formatShortDate(it) }
                        binding.tvDateFilter.text = when (state.dateQuick) {
                            DateQuick.HOY, DateQuick.SIETE, DateQuick.MES -> "Fecha: ${state.dateQuick.label}  ✕"
                            else -> "Fecha: ${state.dateFrom?.let(fmt) ?: "…"} – ${state.dateTo?.let(fmt) ?: "…"}  ✕"
                        }
                    }
                    binding.btnFilter.setColorFilter(
                        if (filtering) android.graphics.Color.parseColor("#FFD54F") else android.graphics.Color.WHITE
                    )
                    adapter.submitList(state.filteredEvaluations.map { it.toHistoryItem() })
                    selectChip(state.selectedTab)

                    binding.cardTotal.tvTotalCount.text = allItems.size.toString()
                    binding.cardCompleted.tvCompletedCount.text =
                        allItems.count { it.stage == EvaluationStage.COMPLETA || it.stage == EvaluationStage.VALIDADA }.toString()
                    binding.cardPending.tvPendingCount.text =
                        allItems.count { it.stage == EvaluationStage.BORRADOR || it.stage == EvaluationStage.EN_CURSO }.toString()

                    binding.fabNewEvaluation.isEnabled = !state.isStartingEvaluation

                    state.newEvaluationId?.let { evaluationId ->
                        viewModel.consumeNewEvaluationId()
                        findNavController().navigate(
                            R.id.action_historial_to_chat,
                            Bundle().apply { putString("evaluationId", evaluationId.toString()) }
                        )
                    }

                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun EvaluationSummaryItem.toHistoryItem(): HistoryItem = HistoryItem(
        id = id,
        establishmentName = establishmentName,
        location = address.orEmpty(),
        date = com.upc.asistenteredidbi.presentation.common.formatShortDate(createdAt),
        stage = stage,
        minutaId = minutaId,
        rejected = isRejected,
        progress = overallScore?.toInt() ?: 0
    )

    private fun selectChip(selected: HistorialTab) {
        binding.chipAll.isChecked = selected == HistorialTab.TODOS
        binding.chipCompleted.isChecked = selected == HistorialTab.COMPLETADOS
        binding.chipDrafts.isChecked = selected == HistorialTab.BORRADORES
        binding.chipAnalysis.isChecked = selected == HistorialTab.EN_ANALISIS
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
