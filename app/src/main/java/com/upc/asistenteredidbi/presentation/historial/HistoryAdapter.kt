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

    fun itemAt(position: Int): HistoryItem? = items.getOrNull(position)

    inner class VH(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HistoryItem) {
            binding.tvTitle.text = item.establishmentName
            binding.tvSubtitle.text = listOf(item.location, item.date)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            com.upc.asistenteredidbi.presentation.common.StagePill.apply(binding.tvStatus, item.stage)
            binding.progressHistory.progress = item.progress
            binding.tvProgress.text = "${item.progress}%"
            // La barra va siempre en azul, como en el prototipo; el estado lo da la etiqueta.
            binding.progressHistory.progressTintList = binding.root.context.getColorStateList(R.color.primary_blue)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }
}