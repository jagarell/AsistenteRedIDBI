package com.upc.asistenteredidbi.presentation.historial

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val onItemClick: (HistoryItem) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val items = mutableListOf<HistoryItem>()

    fun submitList(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemHistoryBinding.inflate(
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

    inner class VH(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            binding.tvTitle.text = item.restaurantName
            binding.tvSubtitle.text = "${item.location} · ${item.date}"
            binding.tvStatus.text = item.status.label
            binding.progressHistory.progress = item.progress
            binding.tvProgress.text = "${item.progress}%"

            when (item.status) {
                HistoryStatus.COMPLETADO -> {
                    binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.success_green))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_completed)
                    binding.progressHistory.progressTintList =
                        binding.root.context.getColorStateList(R.color.primary_blue)
                }

                HistoryStatus.BORRADOR -> {
                    binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.orange_status))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_draft)
                    binding.progressHistory.progressTintList =
                        binding.root.context.getColorStateList(R.color.orange_status)
                }

                HistoryStatus.ENVIADO -> {
                    binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.primary_blue))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_sent)
                    binding.progressHistory.progressTintList =
                        binding.root.context.getColorStateList(R.color.success_green)
                }

                HistoryStatus.EN_ANALISIS -> {
                    binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.purple_status))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_analysis)
                    binding.progressHistory.progressTintList =
                        binding.root.context.getColorStateList(R.color.orange_status)
                }
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}