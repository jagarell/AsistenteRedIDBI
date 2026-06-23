package com.upc.asistenteredidbi.presentation.minuta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.upc.asistenteredidbi.databinding.FragmentMinutaAnalysisBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class MinutaAnalysisTabFragment : Fragment() {

    private var _binding: FragmentMinutaAnalysisBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MinutaViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMinutaAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val evaluationId = arguments?.getString(ARG_EVAL_ID) ?: return
        viewModel.load()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    binding.cardAnalysis.isVisible = state.minuta != null
                    state.minuta?.let { minuta ->
                        //binding.tvAnalysisResult.text = minuta.aiAnalysis ?: "Sin análisis disponible"
                    }
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        const val ARG_EVAL_ID = "evaluationId"
        fun newInstance(evaluationId: String) = MinutaAnalysisTabFragment().apply {
            arguments = Bundle().also { it.putString(ARG_EVAL_ID, evaluationId) }
        }
    }
}
