package com.upc.asistenteredidbi.presentation.historial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import com.upc.asistenteredidbi.domain.model.EvaluationStatus
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
            findNavController().navigate(
                R.id.action_historial_to_minuta,
                Bundle().apply { putString("evaluationId", item.id) }
            )
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.fabNewEvaluation.setOnClickListener { viewModel.startNewEvaluation() }

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
                    val allItems = state.allEvaluations.map { it.toHistoryItem() }
                    adapter.submitList(state.filteredEvaluations.map { it.toHistoryItem() })
                    selectChip(state.selectedTab)

                    binding.cardTotal.tvTotalCount.text = allItems.size.toString()
                    binding.cardCompleted.tvCompletedCount.text =
                        allItems.count { it.status == HistoryStatus.COMPLETADO }.toString()
                    binding.cardPending.tvPendingCount.text =
                        allItems.count { it.status == HistoryStatus.BORRADOR || it.status == HistoryStatus.EN_ANALISIS }.toString()

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
        restaurantName = establishmentName,
        location = createdAt,
        date = "",
        status = when (status) {
            EvaluationStatus.BORRADOR -> HistoryStatus.BORRADOR
            EvaluationStatus.EN_PROGRESO -> HistoryStatus.EN_ANALISIS
            EvaluationStatus.GENERADA -> HistoryStatus.COMPLETADO
        },
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
