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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SendDraftFragment : Fragment() {

    private var _binding: FragmentSendDraftBinding? = null
    private val binding get() = _binding!!

    // El backend todavía no genera el PDF real (ExportService pendiente);
    // se muestra un nombre de archivo genérico en vez de uno con datos
    // ficticios de un cliente.
    private val pdfName = "Propuesta_tecnica.pdf"

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

        binding.tvPdfName.text = pdfName
        binding.tvPdfSize.text = "Pendiente de generar"
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.layoutPdf.setOnClickListener {
            // El backend todavía no genera un PDF real para descargar.
            Toast.makeText(
                requireContext(),
                "La generación de PDF aún no está disponible.",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnSendProposal.setOnClickListener {
            binding.contentForm.visibility = View.GONE
            binding.contentSuccess.visibility = View.VISIBLE
        }

        binding.btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.action_sendDraftFragment_to_historialFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}