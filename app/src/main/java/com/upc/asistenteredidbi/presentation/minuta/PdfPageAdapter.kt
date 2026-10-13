package com.upc.asistenteredidbi.presentation.minuta

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.databinding.ItemPdfPageBinding
import com.upc.asistenteredidbi.presentation.common.ZoomImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Muestra las hojas reales del PDF (una por vista, con zoom). Cada hoja se renderiza al momento de verla
 * (ver PdfPageRenderer.PageSource), así un PDF de muchas páginas no llena la memoria.
 */
class PdfPageAdapter(
    private val scope: CoroutineScope,
    private val pageCount: () -> Int,
    private val load: suspend (Int) -> Bitmap,
    private val onZoomChanged: (Float) -> Unit = {}
) : RecyclerView.Adapter<PdfPageAdapter.VH>() {

    private val cache = object : android.util.LruCache<Int, Bitmap>(3) {}

    fun refresh() = notifyDataSetChanged()

    /** Vista de la hoja en pantalla (para los botones − y +). */
    var currentZoomView: ZoomImageView? = null
        private set
    private val views = mutableMapOf<Int, ZoomImageView>()

    fun zoomViewAt(position: Int): ZoomImageView? = views[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPdfPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.job?.cancel()
        views[position] = holder.binding.ivPage
        holder.binding.ivPage.onZoomChanged = onZoomChanged
        cache.get(position)?.let { holder.binding.ivPage.setImageBitmap(it); return }
        holder.binding.ivPage.setImageBitmap(null)
        holder.job = scope.launch {
            val bitmap = load(position)
            cache.put(position, bitmap)
            if (holder.bindingAdapterPosition == position) holder.binding.ivPage.setImageBitmap(bitmap)
        }
    }

    override fun onViewRecycled(holder: VH) {
        holder.job?.cancel()
        views.entries.removeAll { it.value === holder.binding.ivPage }
    }

    override fun getItemCount(): Int = pageCount()

    class VH(val binding: ItemPdfPageBinding) : RecyclerView.ViewHolder(binding.root) {
        var job: Job? = null
    }
}
