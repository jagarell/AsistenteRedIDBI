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
import com.upc.asistenteredidbi.presentation.common.PdfPageRenderer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class PdfPreviewFragment : Fragment() {

    private var _binding: FragmentPdfPreviewBinding? = null
    private val binding get() = _binding!!

    private lateinit var pageAdapter: PdfPageAdapter
    private var pageCount = 0

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

        pageAdapter = PdfPageAdapter()
        binding.pager.adapter = pageAdapter
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.tvPageIndicator.text = "${position + 1} / $pageCount"
            }
        })

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

        viewLifecycleOwner.lifecycleScope.launch {
            val widthPx = resources.displayMetrics.widthPixels - (32 * resources.displayMetrics.density).toInt()
            runCatching { PdfPageRenderer.renderPages(file, widthPx) }
                .onSuccess { bitmaps ->
                    binding.progress.isVisible = false
                    if (bitmaps.isEmpty()) {
                        binding.tvEmpty.isVisible = true
                    } else {
                        pageCount = bitmaps.size
                        pageAdapter.submitPages(bitmaps)
                        binding.tvPageIndicator.text = "1 / $pageCount"
                    }
                }
                .onFailure {
                    binding.progress.isVisible = false
                    binding.tvEmpty.isVisible = true
                }
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
        _binding = null
        super.onDestroyView()
    }
}
