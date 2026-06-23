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
import androidx.recyclerview.widget.LinearLayoutManager
import com.upc.asistenteredidbi.databinding.FragmentMinutaEquipmentBinding
import com.upc.asistenteredidbi.databinding.ItemEquipmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.getValue

@AndroidEntryPoint
class MinutaEquipmentTabFragment : Fragment() {

    private var _binding: FragmentMinutaEquipmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MinutaViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMinutaEquipmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val evaluationId = arguments?.getString(MinutaAnalysisTabFragment.ARG_EVAL_ID) ?: return
        viewModel.load()

        /*val adapter = EquipmentAdapter()
        binding.rvEquipment.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEquipment.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progress.isVisible = state.isLoading
                    adapter.submitList(state.minuta?.equipmentItems ?: emptyList())
                }
            }
        }*/
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    companion object {
        fun newInstance(evaluationId: String) = MinutaEquipmentTabFragment().apply {
            arguments = Bundle().also { it.putString(MinutaAnalysisTabFragment.ARG_EVAL_ID, evaluationId) }
        }
    }
}
/*
class EquipmentAdapter : androidx.recyclerview.widget.ListAdapter<EquipmentItem, EquipmentAdapter.VH>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<EquipmentItem>() {
        override fun areItemsTheSame(a: EquipmentItem, b: EquipmentItem) = a.id == b.id
        override fun areContentsTheSame(a: EquipmentItem, b: EquipmentItem) = a == b
    }
) {
    inner class VH(val binding: ItemEquipmentBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemEquipmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        /*holder.binding.tvEquipmentName.text = item.name
        holder.binding.tvEquipmentDetail.text = item.category
        holder.binding.tvEquipmentQty.text = "×${item.quantity}"*/
    }
}*/
