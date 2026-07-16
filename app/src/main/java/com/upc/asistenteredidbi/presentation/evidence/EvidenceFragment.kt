package com.upc.asistenteredidbi.presentation.evidence

import android.Manifest
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentEvidenceBinding
import com.upc.asistenteredidbi.domain.repository.EvaluationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class EvidenceFragment : Fragment() {

    private var _binding: FragmentEvidenceBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: EvidenceAdapter
    private var currentItem: EvidenceItem? = null
    private var photoUri: Uri? = null

    private val viewModel: EvidenceViewModel by viewModels()

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
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && photoUri != null) {
                markCurrentAsCaptured()

                // Cuando conectes la carga real al backend:
                // viewModel.uploadPhotoForSelectedItem(photoUri!!)
            }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) markCurrentAsCaptured()
        }

    private val pickPlan =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                Toast.makeText(requireContext(), "Plano seleccionado", Toast.LENGTH_SHORT).show()
            }
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
        observeViewModel()
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
            viewModel.analyzeEvaluation()
        }

        binding.btnUploadPlan.setOnClickListener {
            pickPlan.launch("*/*")
        }

        binding.btnSelectFile.setOnClickListener {
            pickPlan.launch("*/*")
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.btnAnalyze.isEnabled = !state.isLoading

                    if (state.isLoading) {
                        binding.btnAnalyze.text = "Analizando..."
                    } else {
                        binding.btnAnalyze.text = "Analizar con IA"
                    }

                    state.analysis?.let { response ->
                        try {
                            // Reenvía el evaluationId real y todo lo que llegó
                            // del chat (resumen, topología, equipo, score) en
                            // vez de descartarlo con un id hardcodeado.
                            val bundle = Bundle().apply {
                                putString("evaluationId", viewModel.evaluationId)
                                putInt("globalScore", response.globalScore)
                                putString("proposalSummary", arguments?.getString("proposalSummary").orEmpty())
                                putString("topologyText", arguments?.getString("topologyText").orEmpty())
                                putString("equipmentJson", arguments?.getString("equipmentJson").orEmpty())
                                putString("topologyJson", arguments?.getString("topologyJson").orEmpty())
                                putInt("score", arguments?.getInt("score", -1) ?: -1)
                            }

                            viewModel.clearResult()

                            if (findNavController().currentDestination?.id == R.id.evidenceFragment) {
                                findNavController().navigate(
                                    R.id.action_evidenceFragment_to_minutaFragment,
                                    bundle
                                )
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    state.errorMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        viewModel.clearResult()
                    }
                }
            }
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

    private var currentPhotoFile: File? = null

    private fun openCamera() {
        val file = File.createTempFile(
            "evidence_",
            ".jpg",
            requireContext().cacheDir
        )

        currentPhotoFile = file

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

        binding.btnAnalyze.isEnabled = true
        binding.btnAnalyze.alpha = if (captured >= 3) 1f else 0.45f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}