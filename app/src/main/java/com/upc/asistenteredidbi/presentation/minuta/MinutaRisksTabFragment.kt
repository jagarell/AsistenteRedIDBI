package com.upc.asistenteredidbi.presentation.minuta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.databinding.FragmentMinutaRisksBinding
import com.upc.asistenteredidbi.databinding.ItemRiskBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class MinutaRisksTabFragment : Fragment() {

    private var _binding: FragmentMinutaRisksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MinutaViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMinutaRisksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val evaluationId = arguments?.getString(MinutaAnalysisTabFragment.ARG_EVAL_ID) ?: return
        viewModel.load()

        //val adapter = RiskAdapter()
        binding.rvRisks.layoutManager = LinearLayoutManager(requireContext())
        //binding.rvRisks.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    //adapter.submitList(state.minuta?.risks ?: emptyList())
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        fun newInstance(evaluationId: String) = MinutaRisksTabFragment().apply {
            arguments = Bundle().also { it.putString(MinutaAnalysisTabFragment.ARG_EVAL_ID, evaluationId) }
        }
    }
}

/*class RiskAdapter : androidx.recyclerview.widget.ListAdapter<RiskItem, RiskAdapter.VH>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<RiskItem>() {
        override fun areItemsTheSame(a: RiskItem, b: RiskItem) = a.id == b.id
        override fun areContentsTheSame(a: RiskItem, b: RiskItem) = a == b
    }
) {
    inner class VH(val binding: ItemRiskBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRiskBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.binding.tvRiskTitle.text = item.category
        holder.binding.tvRiskDescription.text = item.description
        holder.binding.tvRiskLevel.text = item.level
        val (bgColor, textColor) = when (item.level.lowercase()) {
            "alto" -> Pair(R.color.risk_alto, android.R.color.white)
            "medio" -> Pair(R.color.risk_medio, android.R.color.white)
            else -> Pair(R.color.risk_bajo, android.R.color.white)
        }
        holder.binding.tvRiskLevel.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, bgColor))
        holder.binding.tvRiskLevel.setTextColor(ContextCompat.getColor(holder.itemView.context, textColor))
        holder.binding.viewRiskColor.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, bgColor))
    }
}*/
