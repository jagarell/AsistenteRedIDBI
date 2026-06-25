package com.upc.asistenteredidbi.presentation.historial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentHistorialBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HistorialFragment : Fragment() {

    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: HistoryAdapter

    private val allItems = listOf(
        HistoryItem("1", "Restaurante El Rincón", "CDMX · Insurgentes", "20 Jun 2026", HistoryStatus.COMPLETADO, 72),
        HistoryItem("2", "Café Central Gourmet", "CDMX · Polanco", "18 Jun 2026", HistoryStatus.BORRADOR, 58),
        HistoryItem("3", "Pizza Palace Express", "CDMX · Coyoacán", "10 Jun 2026", HistoryStatus.ENVIADO, 85),
        HistoryItem("4", "Sushi Bar Zen", "CDMX · Santa Fe", "5 Jun 2026", HistoryStatus.COMPLETADO, 91),
        HistoryItem("5", "Taco Tradicional MX", "CDMX · Xochimilco", "28 May 2026", HistoryStatus.EN_ANALISIS, 44)
    )

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
        setupCounters()
        showAll()
    }

    private fun setupRecycler() {
        adapter = HistoryAdapter { item ->
            when (item.status) {
                HistoryStatus.EN_ANALISIS -> {
                    findNavController().navigate(
                        R.id.action_historial_to_minuta,
                        Bundle().apply { putString("evaluationId", item.id) }
                    )
                }
                else -> {
                    findNavController().navigate(
                        R.id.action_historial_to_minuta,
                        Bundle().apply { putString("evaluationId", item.id) }
                    )
                }
            }
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.fabNewEvaluation.setOnClickListener {
            findNavController().navigate(
                R.id.action_historial_to_chat,
                Bundle().apply { putString("evaluationId", "new") }
            )
        }

        binding.chipAll.setOnClickListener { showAll() }
        binding.chipCompleted.setOnClickListener {
            filterBy(HistoryStatus.COMPLETADO)
        }
        binding.chipDrafts.setOnClickListener {
            filterBy(HistoryStatus.BORRADOR)
        }
        binding.chipSent.setOnClickListener {
            filterBy(HistoryStatus.ENVIADO)
        }
        binding.chipAnalysis.setOnClickListener {
            filterBy(HistoryStatus.EN_ANALISIS)
        }
    }

    private fun setupCounters() {
        binding.cardTotal.tvTotalCount.text = allItems.size.toString()
        binding.cardCompleted.tvCompletedCount.text = allItems.count { it.status == HistoryStatus.COMPLETADO }.toString()
        binding.cardPending.tvPendingCount.text = allItems.count { it.status == HistoryStatus.BORRADOR || it.status == HistoryStatus.EN_ANALISIS }.toString()
    }

    private fun showAll() {
        selectChip("all")
        adapter.submitList(allItems)
    }

    private fun filterBy(status: HistoryStatus) {
        selectChip(status.name)
        adapter.submitList(allItems.filter { it.status == status })
    }

    private fun selectChip(selected: String) {
        binding.chipAll.isChecked = selected == "all"
        binding.chipCompleted.isChecked = selected == HistoryStatus.COMPLETADO.name
        binding.chipDrafts.isChecked = selected == HistoryStatus.BORRADOR.name
        binding.chipSent.isChecked = selected == HistoryStatus.ENVIADO.name
        binding.chipAnalysis.isChecked = selected == HistoryStatus.EN_ANALISIS.name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}