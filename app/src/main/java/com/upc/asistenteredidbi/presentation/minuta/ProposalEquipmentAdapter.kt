package com.upc.asistenteredidbi.presentation.minuta

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.databinding.ItemProposalEquipmentBinding

class ProposalEquipmentAdapter : RecyclerView.Adapter<ProposalEquipmentAdapter.VH>() {

    private val items = mutableListOf<ProposalEquipmentItem>()

    fun submitList(newItems: List<ProposalEquipmentItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemProposalEquipmentBinding.inflate(
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

    class VH(private val binding: ItemProposalEquipmentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProposalEquipmentItem) {
            binding.ivIcon.setImageResource(item.iconRes)
            binding.tvName.text = item.name
            binding.tvDescription.text = item.description
            binding.tvPrice.text = item.price
            binding.tvQuantity.text = item.quantity
        }
    }
}