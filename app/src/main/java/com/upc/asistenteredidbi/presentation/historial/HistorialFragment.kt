package com.upc.asistenteredidbi.presentation.historial

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentHistorialBinding
import com.upc.asistenteredidbi.databinding.ItemEvaluationBinding
import com.upc.asistenteredidbi.domain.model.Evaluation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class HistorialFragment : Fragment() {

    private var _binding: FragmentHistorialBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistorialViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistorialBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val adapter = EvaluationAdapter { evaluation ->
            findNavController().navigate(
                R.id.action_historial_to_minuta,
                bundleOf("evaluationId" to evaluation.id)
            )
        }

        binding.rvEvaluations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvaluations.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    //adapter.submitList(state.evaluations)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/** Adapter para la lista de evaluaciones históricas. */
class EvaluationAdapter(
    private val onClick: (Evaluation) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Evaluation, EvaluationAdapter.VH>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Evaluation>() {
        override fun areItemsTheSame(a: Evaluation, b: Evaluation) = a.id == b.id
        override fun areContentsTheSame(a: Evaluation, b: Evaluation) = a == b
    }
) {
    inner class VH(val binding: ItemEvaluationBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemEvaluationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val evaluation = getItem(position)
        //holder.binding.tvLocalName.text = evaluation.localName
        holder.binding.tvDate.text = evaluation.createdAt
        //holder.binding.tvStatus.text = evaluation.status
        holder.binding.root.setOnClickListener { onClick(evaluation) }
    }
}
