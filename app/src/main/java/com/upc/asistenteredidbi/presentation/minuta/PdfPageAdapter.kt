package com.upc.asistenteredidbi.presentation.minuta

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.databinding.ItemPdfPageBinding

/** Muestra las páginas reales del PDF ya renderizadas a bitmap (ver [PdfPageRenderer]) — nunca contenido inventado. */
class PdfPageAdapter : RecyclerView.Adapter<PdfPageAdapter.VH>() {

    private val pages = mutableListOf<Bitmap>()

    fun submitPages(newPages: List<Bitmap>) {
        pages.clear()
        pages.addAll(newPages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPdfPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.binding.ivPage.setImageBitmap(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    class VH(val binding: ItemPdfPageBinding) : RecyclerView.ViewHolder(binding.root)
}
