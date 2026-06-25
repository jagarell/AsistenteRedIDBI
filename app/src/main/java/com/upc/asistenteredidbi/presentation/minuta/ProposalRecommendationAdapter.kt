package com.upc.asistenteredidbi.presentation.minuta

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.databinding.ItemProposalRecommendationBinding

class ProposalRecommendationAdapter :
    RecyclerView.Adapter<ProposalRecommendationAdapter.VH>() {

    private val items = mutableListOf<ProposalRecommendationItem>()

    fun submitList(newItems: List<ProposalRecommendationItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProposalRecommendationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(private val binding: ItemProposalRecommendationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProposalRecommendationItem) {
            binding.tvNumber.text = item.number.toString()
            binding.tvText.text = item.text
        }
    }
}