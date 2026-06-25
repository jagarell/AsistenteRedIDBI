package com.upc.asistenteredidbi.presentation.minuta

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentSendDraftBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SendDraftFragment : Fragment() {

    private var _binding: FragmentSendDraftBinding? = null
    private val binding get() = _binding!!

    private val pdfName = "Propuesta_ElRincon_v1.0.pdf"
    private val pdfSizeMb = 2.4
    private val pdfUrl = "https://example.com/Propuesta_ElRincon_v1.0.pdf"

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
        binding.etTo.setText("roberto.garcia@elrincon.com")
        binding.etCc.setText("tecnico@technet.com")
        binding.etSubject.setText("Propuesta de Infraestructura de Red - Restaurante El Rincón")
        binding.etMessage.setText(
            "Estimado cliente,\n\nAdjunto encontrará nuestra propuesta técnica de infraestructura de red para su establecimiento."
        )

        binding.tvPdfName.text = pdfName
        binding.tvPdfSize.text = "${pdfSizeMb} MB"
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutPdf.setOnClickListener {
            downloadPdf(pdfUrl, pdfName)
        }

        binding.btnSendProposal.setOnClickListener {
            binding.contentForm.visibility = View.GONE
            binding.contentSuccess.visibility = View.VISIBLE
        }

        binding.btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.action_sendDraftFragment_to_historialFragment)
        }
    }

    private fun downloadPdf(url: String, fileName: String) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(fileName)
            .setDescription("Descargando propuesta técnica")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)

        Toast.makeText(requireContext(), "Descargando PDF...", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}