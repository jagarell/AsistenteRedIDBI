package com.upc.asistenteredidbi.presentation.evidence

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.ItemEvidenceBinding

class EvidenceAdapter(
    private val onTakePhoto: (EvidenceItem) -> Unit
) : RecyclerView.Adapter<EvidenceAdapter.VH>() {

    private val items = mutableListOf<EvidenceItem>()

    fun submitList(newItems: List<EvidenceItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemEvidenceBinding.inflate(
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

    inner class VH(private val binding: ItemEvidenceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EvidenceItem) {
            binding.tvTitle.text = item.title
            binding.tvSubtitle.text = item.subtitle
            binding.ivIcon.setImageResource(item.iconRes)

            if (item.captured) {
                binding.root.setBackgroundResource(R.drawable.bg_evidence_card_done)

                binding.ivCheck.visibility = android.view.View.VISIBLE

                binding.ivAction.setImageResource(R.drawable.ic_check_small)
                binding.ivAction.setColorFilter(binding.root.context.getColor(R.color.success_green))

                binding.tvAction.text = "Capturada"
                binding.tvAction.setTextColor(binding.root.context.getColor(R.color.success_green))
            } else {
                binding.root.setBackgroundResource(R.drawable.bg_evidence_card)

                binding.ivCheck.visibility = android.view.View.GONE

                binding.ivAction.setImageResource(R.drawable.ic_camera)
                binding.ivAction.setColorFilter(binding.root.context.getColor(R.color.primary_blue))

                binding.tvAction.text = "Tomar foto"
                binding.tvAction.setTextColor(binding.root.context.getColor(R.color.primary_blue))
            }

            binding.layoutAction.setOnClickListener { onTakePhoto(item) }
            binding.root.setOnClickListener { onTakePhoto(item) }
        }
    }
}