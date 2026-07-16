package com.upc.asistenteredidbi.presentation.minutas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.ItemMinutaRecordBinding
import com.upc.asistenteredidbi.domain.model.MinutaRecord
import com.upc.asistenteredidbi.domain.model.MinutaRecordStatus

/**
 * Lista minutas de todos los técnicos (incluidos los borradores). Gating de
 * UI: "Marcar completa" se ofrece siempre que la minuta no esté validada;
 * "Validar" sólo se muestra si [isSupervisor] es true Y la minuta está
 * COMPLETA — el backend refuerza la misma regla.
 */
class MinutaRecordAdapter(
    private val onComplete: (MinutaRecord) -> Unit,
    private val onValidate: (MinutaRecord) -> Unit
) : RecyclerView.Adapter<MinutaRecordAdapter.VH>() {

    private val items = mutableListOf<MinutaRecord>()
    private var isSupervisor = false

    fun submitList(newItems: List<MinutaRecord>, isSupervisor: Boolean) {
        items.clear()
        items.addAll(newItems)
        this.isSupervisor = isSupervisor
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMinutaRecordBinding.inflate(
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

    inner class VH(private val binding: ItemMinutaRecordBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MinutaRecord) {
            val context = binding.root.context

            binding.tvClientName.text = item.clientName
            binding.tvAddress.text = item.address?.takeIf { it.isNotBlank() } ?: "Sin dirección registrada"
            binding.tvTechnician.text = "Técnico: ${item.technicianName ?: "Sin asignar"}"

            when (item.status) {
                MinutaRecordStatus.BORRADOR -> {
                    binding.tvStatus.text = "Borrador"
                    binding.tvStatus.setTextColor(context.getColor(R.color.orange_status))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_draft)
                }
                MinutaRecordStatus.COMPLETA -> {
                    binding.tvStatus.text = "Completa"
                    binding.tvStatus.setTextColor(context.getColor(R.color.purple_status))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_analysis)
                }
                MinutaRecordStatus.VALIDADA -> {
                    binding.tvStatus.text = "Validada"
                    binding.tvStatus.setTextColor(context.getColor(R.color.success_green))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_completed)
                }
            }

            binding.tvValidatedBy.isVisible = item.status == MinutaRecordStatus.VALIDADA
            binding.tvValidatedBy.text = "Validada por: ${item.validatedByName ?: "-"}"

            // "Completar": disponible para cualquier técnico/supervisor mientras
            // esté en borrador (cualquiera puede continuar la minuta de otro).
            binding.btnComplete.isVisible = item.status == MinutaRecordStatus.BORRADOR
            binding.btnComplete.setOnClickListener { onComplete(item) }

            // "Validar": sólo visible para SUPERVISOR y sólo si está COMPLETA.
            binding.btnValidate.isVisible =
                isSupervisor && item.status == MinutaRecordStatus.COMPLETA
            binding.btnValidate.setOnClickListener { onValidate(item) }
        }
    }
}
