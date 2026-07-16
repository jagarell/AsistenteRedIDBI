package com.upc.asistenteredidbi.presentation.minuta

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.squareup.moshi.Moshi
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentSendDraftBinding
import com.upc.asistenteredidbi.domain.model.ProposalPdfData
import com.upc.asistenteredidbi.domain.usecase.SendProposalUseCase
import com.upc.asistenteredidbi.presentation.common.PdfFileUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SendDraftFragment : Fragment() {

    private var _binding: FragmentSendDraftBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var moshi: Moshi

    @Inject
    lateinit var sendProposalUseCase: SendProposalUseCase

    /** El PDF real ya generado en la pantalla de propuesta (bytes del gateway). */
    private val pdfFile: File? by lazy {
        arguments?.getString("pdfPath")
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
            ?.takeIf { it.exists() }
    }

    /** Los mismos datos con los que se generó (o se generaría) el PDF. */
    private val proposalData: ProposalPdfData? by lazy {
        arguments?.getString("proposalDataJson")
            ?.takeIf { it.isNotBlank() }
            ?.let {
                try {
                    moshi.adapter(ProposalPdfData::class.java).fromJson(it)
                } catch (_: Exception) {
                    null
                }
            }
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
        val establishment = proposalData?.establishmentName?.takeIf { it.isNotBlank() }

        binding.etSubject.setText(
            if (establishment != null) {
                "Propuesta de Infraestructura de Red - $establishment"
            } else {
                "Propuesta de Infraestructura de Red"
            }
        )
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
            sendProposal()
        }

        binding.btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.action_sendDraftFragment_to_historialFragment)
        }
    }

    private fun sendProposal() {
        val to = binding.etTo.text?.toString().orEmpty().trim()
        val cc = binding.etCc.text?.toString().orEmpty().trim()
        val subject = binding.etSubject.text?.toString().orEmpty().trim()
        val message = binding.etMessage.text?.toString().orEmpty().trim()
        val data = proposalData

        if (to.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(to).matches()) {
            Toast.makeText(requireContext(), "Ingresa un correo válido en \"Para\"", Toast.LENGTH_SHORT).show()
            return
        }
        if (data == null) {
            Toast.makeText(
                requireContext(),
                "No hay datos de la propuesta; vuelve a la pantalla anterior",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.btnSendProposal.isEnabled = false
        binding.btnSendProposal.text = "Enviando..."

        viewLifecycleOwner.lifecycleScope.launch {
            sendProposalUseCase(
                to = to,
                cc = cc.ifBlank { null },
                subject = subject,
                message = message,
                data = data
            ).onSuccess {
                binding.contentForm.visibility = View.GONE
                binding.contentSuccess.visibility = View.VISIBLE
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "No se pudo enviar la propuesta",
                    Toast.LENGTH_LONG
                ).show()
            }

            binding.btnSendProposal.isEnabled = true
            binding.btnSendProposal.text = "Enviar Propuesta"
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
