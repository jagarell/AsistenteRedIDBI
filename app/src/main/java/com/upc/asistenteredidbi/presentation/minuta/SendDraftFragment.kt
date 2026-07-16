package com.upc.asistenteredidbi.presentation.minuta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentSendDraftBinding
import com.upc.asistenteredidbi.presentation.common.PdfFileUtils
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.Locale

@AndroidEntryPoint
class SendDraftFragment : Fragment() {

    private var _binding: FragmentSendDraftBinding? = null
    private val binding get() = _binding!!

    /** El PDF real ya generado en la pantalla de propuesta (bytes del gateway). */
    private val pdfFile: File? by lazy {
        arguments?.getString("pdfPath")
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
            ?.takeIf { it.exists() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendDraftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupInitialData()
        setupClicks()
    }

    private fun setupInitialData() {
        binding.etSubject.setText("Propuesta de Infraestructura de Red")
        binding.etMessage.setText(
            "Estimado cliente,\n\nAdjunto encontrará nuestra propuesta técnica de infraestructura de red para su establecimiento."
        )

        val file = pdfFile
        if (file != null) {
            binding.tvPdfName.text = file.name
            binding.tvPdfSize.text = formatSize(file.length())
        } else {
            binding.tvPdfName.text = "Propuesta_tecnica.pdf"
            binding.tvPdfSize.text = "Genera el PDF desde la propuesta técnica"
        }
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutPdf.setOnClickListener {
            withPdfOrWarn { file -> PdfFileUtils.openPdf(requireContext(), file) }
        }

        binding.btnDownloadPdf.setOnClickListener {
            withPdfOrWarn { file ->
                val ok = PdfFileUtils.downloadToPublicDownloads(
                    requireContext(),
                    file.readBytes(),
                    file.name
                )
                Toast.makeText(
                    requireContext(),
                    if (ok) "PDF descargado en Descargas" else "No se pudo descargar el PDF",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnSendProposal.setOnClickListener {
            binding.contentForm.visibility = View.GONE
            binding.contentSuccess.visibility = View.VISIBLE
        }

        binding.btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.action_sendDraftFragment_to_historialFragment)
        }
    }

    private fun withPdfOrWarn(action: (File) -> Unit) {
        val file = pdfFile
        if (file != null) {
            action(file)
        } else {
            Toast.makeText(
                requireContext(),
                "Genera el PDF primero desde la propuesta técnica",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024f
        return if (kb < 1024f) {
            String.format(Locale.US, "%.0f KB", kb)
        } else {
            String.format(Locale.US, "%.1f MB", kb / 1024f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
