package com.upc.asistenteredidbi.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.ItemHomeRecentBinding

class HomeRecentAdapter(
    private val onItemClick: (HomeRecentItem) -> Unit
) : RecyclerView.Adapter<HomeRecentAdapter.HomeRecentViewHolder>() {

    private val items = mutableListOf<HomeRecentItem>()

    fun submitList(newItems: List<HomeRecentItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeRecentViewHolder {
        val binding = ItemHomeRecentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HomeRecentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeRecentViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HomeRecentViewHolder(
        private val binding: ItemHomeRecentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: HomeRecentItem) {
            binding.tvTitle.text = item.title
            binding.tvDate.text = item.date
            binding.tvStatus.text = item.status

            when (item.statusType) {
                StatusType.COMPLETADO -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_completed)
                    binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.status_completed_text))
                }

                StatusType.BORRADOR -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_draft)
                    binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.status_draft_text))
                }

                StatusType.ENVIADO -> {
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_sent)
                    binding.tvStatus.setTextColor(binding.root.context.getColor(R.color.status_sent_text))
                }
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}