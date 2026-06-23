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
import com.upc.asistenteredidbi.databinding.FragmentMinutaProposalBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class MinutaProposalTabFragment : Fragment() {

    private var _binding: FragmentMinutaProposalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MinutaViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMinutaProposalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val evaluationId = arguments?.getString(MinutaAnalysisTabFragment.ARG_EVAL_ID) ?: return
        viewModel.load()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    //binding.tvSummary.isVisible = state.minuta?.proposalSummary != null
                    //binding.tvSummary.text = state.minuta?.proposalSummary
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        fun newInstance(evaluationId: String) = MinutaProposalTabFragment().apply {
            arguments = Bundle().also { it.putString(MinutaAnalysisTabFragment.ARG_EVAL_ID, evaluationId) }
        }
    }
}
