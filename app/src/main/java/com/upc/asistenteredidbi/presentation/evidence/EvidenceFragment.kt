package com.upc.asistenteredidbi.presentation.evidence

import android.Manifest
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentEvidenceBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class EvidenceFragment : Fragment() {

    private var _binding: FragmentEvidenceBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: EvidenceAdapter
    private var currentItem: EvidenceItem? = null
    private var photoUri: Uri? = null

    private val items = mutableListOf(
        EvidenceItem("router", "Router/Modem", "Equipo principal", R.drawable.ic_router),
        EvidenceItem("switch", "Switch", "Conmutador de red", R.drawable.ic_network),
        EvidenceItem("pos", "POS/Caja", "Terminal punto de venta", R.drawable.ic_monitor),
        EvidenceItem("kitchen", "Área Cocina", "Ambiente cocina", R.drawable.ic_kitchen),
        EvidenceItem("hall", "Salón/Comedor", "Área de clientes", R.drawable.ic_people),
        EvidenceItem("servers", "Servidores", "Rack/gabinete", R.drawable.ic_server)
    )

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) markCurrentAsCaptured()
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) markCurrentAsCaptured()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvidenceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecycler()
        setupClicks()
        updateProgress()
    }

    private fun setupRecycler() {
        adapter = EvidenceAdapter { item ->
            currentItem = item
            showImageOptions()
        }

        binding.rvEvidence.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvEvidence.adapter = adapter
        adapter.submitList(items)
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnMoreEvidence.setOnClickListener {
            Toast.makeText(requireContext(), "Agregar evidencia adicional", Toast.LENGTH_SHORT).show()
        }

        binding.btnAnalyze.setOnClickListener {
            /*val captured = items.count { it.captured }

            if (captured < 3) {
                Toast.makeText(
                    requireContext(),
                    "Captura al menos 3 evidencias",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                findNavController().navigate(
                    R.id.action_evidenceFragment_to_minutaFragment,
                    Bundle().apply {
                        putString("evaluationId", "demo-evaluation-001")
                    }
                )
            }*/

            findNavController().navigate(
                R.id.action_evidenceFragment_to_minutaFragment,
                Bundle().apply {
                    putString("evaluationId", "demo-evaluation-001")
                }
            )
        }

        binding.btnUploadPlan.setOnClickListener {
            pickPlan.launch("*/*")
        }

        binding.btnSelectFile.setOnClickListener {
            pickPlan.launch("*/*")
        }
    }

    private val pickPlan =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                Toast.makeText(requireContext(), "Plano seleccionado", Toast.LENGTH_SHORT).show()
            }
        }

    private fun showImageOptions() {
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar evidencia")
            .setItems(arrayOf("Tomar foto", "Seleccionar de galería")) { _, which ->
                when (which) {
                    0 -> requestCameraPermission.launch(Manifest.permission.CAMERA)
                    1 -> pickImage.launch("image/*")
                }
            }
            .show()
    }

    private fun openCamera() {
        val file = File.createTempFile("evidence_", ".jpg", requireContext().cacheDir)
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )
        takePicture.launch(photoUri)
    }

    private fun markCurrentAsCaptured() {
        val selected = currentItem ?: return
        val index = items.indexOfFirst { it.id == selected.id }
        if (index != -1) {
            items[index] = items[index].copy(captured = true)
            adapter.submitList(items.toList())
            updateProgress()
        }
    }

    private fun updateProgress() {
        val captured = items.count { it.captured }
        val total = items.size
        val percent = ((captured.toFloat() / total) * 100).toInt()

        binding.tvCounter.text = "$captured de $total fotos capturadas"
        binding.tvPercent.text = "$percent%"
        binding.progressEvidence.progress = percent

        //binding.btnAnalyze.isEnabled = captured >= 3
        binding.btnAnalyze.isEnabled = true
        binding.btnAnalyze.alpha = if (captured >= 3) 1f else 0.45f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}