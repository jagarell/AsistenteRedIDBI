package com.upc.asistenteredidbi.presentation.minuta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.upc.asistenteredidbi.databinding.FragmentMinutaBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MinutaFragment : Fragment() {

    private var _binding: FragmentMinutaBinding? = null
    private val binding get() = _binding!!

    // evaluationId llega como argumento del NavGraph
    private val evaluationId: String by lazy {
        arguments?.getString("evaluationId") ?: ""
    }

    private val tabs = listOf("Análisis IA", "Equipamiento", "Riesgos", "Propuesta")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMinutaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.viewPager.adapter = MinutaPagerAdapter(this, evaluationId)

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = tabs[pos]
        }.attach()

        binding.fabPdf.setOnClickListener {
            // Acción: generar PDF (puede navegar a PdfPreview en Sprint 2 o mostrar snackbar aquí)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class MinutaPagerAdapter(fragment: Fragment, private val evaluationId: String) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = 4
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> MinutaAnalysisTabFragment.newInstance(evaluationId)
        1 -> MinutaEquipmentTabFragment.newInstance(evaluationId)
        2 -> MinutaRisksTabFragment.newInstance(evaluationId)
        3 -> MinutaProposalTabFragment.newInstance(evaluationId)
        else -> MinutaAnalysisTabFragment.newInstance(evaluationId)
    }
}
