package com.upc.asistenteredidbi.presentation.minuta

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentMinutaBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MinutaFragment : Fragment() {

    private var _binding: FragmentMinutaBinding? = null
    private val binding get() = _binding!!

    private val evaluationId: String by lazy {
        arguments?.getString("evaluationId") ?: "demo-evaluation-001"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMinutaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnTechnicalProposal.setOnClickListener {
            findNavController().navigate(
                R.id.action_minutaFragment_to_minutaProposalFragment,
                Bundle().apply {
                    putString("evaluationId", evaluationId)
                }
            )
        }

        animateProgressBar(binding.progressGlobal, 72, 1400L)
        animateProgressBar(binding.layoutConnectivity.progressBar, 78, 1000L)
        animateProgressBar(binding.layoutAnalysisPhysical.progressBar, 62, 1000L)
        animateProgressBar(binding.layoutAnalysisEquipment.progressBar, 85, 1000L)
        animateProgressBar(binding.layoutAnalysisWifi.progressBar, 55, 1000L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun animateProgressBar(
        progressBar: ProgressBar,
        targetProgress: Int,
        duration: Long = 1200L
    ) {
        progressBar.progress = 0

        ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
        }
    }
}