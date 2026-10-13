package com.upc.asistenteredidbi.presentation.minuta

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.upc.asistenteredidbi.databinding.FragmentPdfPreviewBinding
import com.upc.asistenteredidbi.presentation.common.PdfFileUtils
import com.upc.asistenteredidbi.presentation.common.PdfPageSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class PdfPreviewFragment : Fragment() {

    private var _binding: FragmentPdfPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var pageAdapter: PdfPageAdapter
    private var pageCount = 0
    private var source: PdfPageSource? = null

    /** Vista previa antes de generar el PDF: no se descarga ni se comparte. */
    private val isPreview: Boolean by lazy { arguments?.getBoolean("preview", false) ?: false }

    private val pdfFile: File? by lazy {
        arguments?.getString("pdfPath")?.takeIf { it.isNotBlank() }?.let { File(it) }
    }

    private val establishmentName: String by lazy {
        arguments?.getString("establishmentName").orEmpty()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPdfPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTitle.text = if (isPreview) "Vista previa" else "Propuesta PDF"
        if (isPreview) {
            binding.btnDownload.isVisible = false
            binding.btnWhatsapp.isVisible = false
            binding.btnEmail.isVisible = false
            binding.tvPreviewNote.isVisible = true
        }
        pageAdapter = PdfPageAdapter(
            scope = viewLifecycleOwner.lifecycleScope,
            pageCount = { pageCount },
            load = { index -> source!!.render(index) },
            onZoomChanged = { z -> _binding?.tvZoom?.text = "${(z * 100).toInt()} %" }
        )
        binding.pager.adapter = pageAdapter
        binding.pager.offscreenPageLimit = 1
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tvPageIndicator.text = "Página ${position + 1} de $pageCount"
                binding.tvZoom.text = "100 %"
            }
        })
        binding.btnPrev.setOnClickListener { binding.pager.setCurrentItem(maxOf(0, binding.pager.currentItem - 1), true) }
        binding.btnNext.setOnClickListener { binding.pager.setCurrentItem(minOf(pageCount - 1, binding.pager.currentItem + 1), true) }
        binding.btnZoomIn.setOnClickListener { pageAdapter.zoomViewAt(binding.pager.currentItem)?.zoomBy(1.5f) }
        binding.btnZoomOut.setOnClickListener { pageAdapter.zoomViewAt(binding.pager.currentItem)?.zoomBy(1f / 1.5f) }

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnDownload.setOnClickListener { downloadPdf() }
        binding.btnWhatsapp.setOnClickListener { sharePdf(targetPackage = "com.whatsapp") }
        binding.btnEmail.setOnClickListener { sharePdf(asEmail = true) }

        loadPages()
    }

    private fun loadPages() {
        val file = pdfFile
        if (file == null || !file.exists()) {
            binding.progress.isVisible = false
            binding.tvEmpty.isVisible = true
            return
        }
        // Una hoja a la vez, a doble resolución para que el zoom se lea nítido.
        val widthPx = (resources.displayMetrics.widthPixels * 2).coerceAtMost(2400)
        runCatching { PdfPageSource(file, widthPx) }
            .onSuccess { src ->
                source = src
                pageCount = src.pageCount
                binding.progress.isVisible = false
                if (pageCount == 0) {
                    binding.tvEmpty.isVisible = true
                } else {
                    binding.tvPageIndicator.text = "Página 1 de $pageCount"
                    pageAdapter.refresh()
                }
            }
            .onFailure {
                binding.progress.isVisible = false
                binding.tvEmpty.isVisible = true
            }
    }

    private fun downloadPdf() {
        val file = pdfFile ?: return
        val ok = PdfFileUtils.downloadToPublicDownloads(requireContext(), file.readBytes(), file.name)
        Toast.makeText(
            requireContext(),
            if (ok) "PDF guardado en Descargas" else "No se pudo guardar el PDF",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun sharePdf(targetPackage: String? = null, asEmail: Boolean = false) {
        val file = pdfFile ?: return
        val uri: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (asEmail) "message/rfc822" else "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Propuesta técnica de infraestructura — $establishmentName")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            targetPackage?.let { setPackage(it) }
        }

        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(requireContext(), "No se encontró una app para compartir", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        binding.pager.adapter = null
        source?.close()
        source = null
        _binding = null
        super.onDestroyView()
    }
}
