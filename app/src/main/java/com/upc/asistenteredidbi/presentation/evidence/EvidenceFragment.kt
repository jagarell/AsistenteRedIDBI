package com.upc.asistenteredidbi.presentation.evidence

import android.Manifest
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.databinding.FragmentEvidenceBinding
import com.upc.asistenteredidbi.domain.model.EquipmentTypeCatalog
import com.upc.asistenteredidbi.domain.model.EvidenceAreaItem
import com.upc.asistenteredidbi.domain.model.EvidenceChecklist
import com.upc.asistenteredidbi.domain.model.EvidenceEquipmentItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

private const val PLAN_TOPOLOGY_AREA_NAME = "Plano y Topología"

/** Nombre del área/equipo fijo reservado para plano + topología — ver
 * `build_evidence_checklist` en idbi-fastapi. */
private fun EvidenceChecklist.planTopologyAreaId(): Long? =
    areas.firstOrNull { it.name == PLAN_TOPOLOGY_AREA_NAME }?.id

@AndroidEntryPoint
class EvidenceFragment : Fragment() {

    private var _binding: FragmentEvidenceBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: EvidenceAdapter

    private val viewModel: EvidenceViewModel by viewModels()

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }

    private var photoUri: Uri? = null
    private var currentPhotoFile: File? = null

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && photoUri != null) {
                viewModel.uploadPhotoForSelectedItem(photoUri!!)
            }
        }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) viewModel.uploadPhotoForSelectedItem(uri)
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
        observeViewModel()
        viewModel.loadChecklist()
    }

    private fun setupRecycler() {
        adapter = EvidenceAdapter { item -> onItemTapped(item) }
        binding.rvEvidence.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvEvidence.adapter = adapter
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnMoreEvidence.setOnClickListener {
            showAddItemDialog()
        }

        binding.btnAnalyze.setOnClickListener {
            val checklist = viewModel.uiState.value.checklist
            if (checklist?.selectionLocked == true) {
                viewModel.analyzeEvaluation()
            } else {
                viewModel.lockSelection()
            }
        }

        binding.btnUploadPlan.setOnClickListener { openPlanTopologyArea() }
        binding.btnSelectFile.setOnClickListener { openPlanTopologyArea() }
        binding.btnUploadTopology.setOnClickListener { openPlanTopologyArea() }
        binding.btnSelectTopologyFile.setOnClickListener { openPlanTopologyArea() }
    }

    private fun openPlanTopologyArea() {
        val areaId = viewModel.uiState.value.checklist?.planTopologyAreaId()
        if (areaId == null) {
            Toast.makeText(requireContext(), "Todavía se está preparando el checklist.", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.openAreaDetail(areaId)
        showImageOptions()
    }

    /** Fase A: agregar un área o un equipo fuera de lo ya sembrado del chat. */
    private fun showAddItemDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("¿Qué quieres agregar?")
            .setItems(arrayOf("Área del local", "Equipo")) { _, which ->
                if (which == 0) showAddAreaDialog() else showAddEquipmentDialog()
            }
            .show()
    }

    private fun showAddAreaDialog() {
        val input = EditText(requireContext()).apply { hint = "Ej: Terraza, Almacén..." }
        AlertDialog.Builder(requireContext())
            .setTitle("Nueva área")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                viewModel.openAddDialog(isArea = true)
                viewModel.onAddDialogTextChange(input.text.toString())
                viewModel.confirmAddDialog()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showAddEquipmentDialog() {
        val input = EditText(requireContext()).apply { hint = "Ej: Impresora de cocina" }
        val spinner = Spinner(requireContext()).apply {
            adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                EquipmentTypeCatalog.TYPES.map { it.second }
            )
        }
        val container = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(spinner)
            addView(input)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Nuevo equipo")
            .setView(container)
            .setPositiveButton("Agregar") { _, _ ->
                val type = EquipmentTypeCatalog.TYPES[spinner.selectedItemPosition].first
                viewModel.openAddDialog(isArea = false)
                viewModel.onAddDialogEquipmentTypeChange(type)
                viewModel.onAddDialogTextChange(input.text.toString())
                viewModel.confirmAddDialog()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun onItemTapped(item: EvidenceItem) {
        val checklist = viewModel.uiState.value.checklist ?: return
        val (kind, rawId) = item.id.split(":", limit = 2)
        val id = rawId.toLongOrNull() ?: return

        if (checklist.selectionLocked) {
            if (kind == "area") viewModel.openAreaDetail(id) else viewModel.openEquipmentDetail(id)

            val hasPhotos = if (kind == "area") {
                checklist.areas.firstOrNull { it.id == id }?.photos?.isNotEmpty() == true
            } else {
                checklist.equipment.firstOrNull { it.id == id }?.photos?.isNotEmpty() == true
            }

            if (hasPhotos) {
                showImageOptions()
            } else {
                showImageOptionsOrRemove(kind, id)
            }
        } else {
            val name = if (kind == "area") {
                checklist.areas.firstOrNull { it.id == id }?.name
            } else {
                checklist.equipment.firstOrNull { it.id == id }?.label
            } ?: "este ítem"

            AlertDialog.Builder(requireContext())
                .setTitle("¿Quitar $name?")
                .setMessage("Todavía estás en la selección (Fase A) — puedes quitarlo o agregarlo de nuevo después.")
                .setPositiveButton("Quitar") { _, _ ->
                    if (kind == "area") viewModel.deleteArea(id) else viewModel.deleteEquipment(id)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderChecklist(state)

                    binding.btnAnalyze.isEnabled = !state.isLoading && !state.isAnalyzing
                    binding.btnAnalyze.text = when {
                        state.isAnalyzing -> "Analizando..."
                        state.checklist?.selectionLocked != true -> "Confirmar selección"
                        else -> "Analizar con IA"
                    }
                    if (state.checklist?.selectionLocked == true) {
                        binding.btnAnalyze.alpha = if (state.canAnalyze) 1f else 0.45f
                        binding.btnAnalyze.isEnabled = state.canAnalyze && !state.isAnalyzing
                        val missingNames = missingItemNames(state.checklist)
                        binding.tvMissingHint.isVisible = !state.canAnalyze && missingNames.isNotEmpty()
                        binding.tvMissingHint.text = "Faltan fotos: ${missingNames.joinToString(", ")}"
                    } else {
                        binding.btnAnalyze.alpha = 1f
                        binding.tvMissingHint.isVisible = false
                    }
                    binding.btnMoreEvidence.isVisible = state.checklist?.selectionLocked != true

                    state.analysis?.let { response ->
                        try {
                            val bundle = Bundle().apply {
                                putString("evaluationId", viewModel.evaluationId)
                                putInt("globalScore", response.globalScore)
                                putString("proposalSummary", arguments?.getString("proposalSummary").orEmpty())
                                putString("topologyText", arguments?.getString("topologyText").orEmpty())
                                putString("equipmentJson", arguments?.getString("equipmentJson").orEmpty())
                                putString("topologyJson", arguments?.getString("topologyJson").orEmpty())
                                putInt("score", arguments?.getInt("score", -1) ?: -1)
                                putLong("minutaId", arguments?.getLong("minutaId", -1L) ?: -1L)
                            }

                            viewModel.clearResult()

                            if (findNavController().currentDestination?.id == R.id.evidenceFragment) {
                                findNavController().navigate(R.id.action_evidenceFragment_to_minutaFragment, bundle)
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

    private fun renderChecklist(state: EvidenceUiState) {
        val checklist = state.checklist ?: return
        val items = mutableListOf<EvidenceItem>()

        checklist.areas.forEach { area -> items += area.toDisplayItem(checklist.selectionLocked) }
        checklist.equipment.forEach { equipment -> items += equipment.toDisplayItem(checklist.selectionLocked) }

        adapter.submitList(items)

        val captured = items.count { it.captured }
        val total = items.size
        val percent = if (total > 0) ((captured.toFloat() / total) * 100).toInt() else 0

        binding.tvCounter.text = "$captured de $total fotos capturadas"
        binding.tvPercent.text = "$percent%"
        binding.progressEvidence.progress = percent

        // "Plano del Local" y "Topología de Red" son dos cajas de subida en la
        // UI, pero en el checklist del backend son UN SOLO ítem ("Plano y
        // Topología", ver build_evidence_checklist en idbi-fastapi) — una
        // foto en cualquiera de las dos cuenta para el mismo requisito. Se
        // muestra el mismo contador combinado en ambas tarjetas para que no
        // parezca que subir a "topología" está sumando fotos a "plano".
        val planAreaId = checklist.planTopologyAreaId()
        val planArea = checklist.areas.firstOrNull { it.id == planAreaId }
        val planTopologyCount = planArea?.photos?.size ?: 0
        val hasPlanTopologyPhotos = planTopologyCount > 0
        val planTopologyText = "✓ Plano y Topología: $planTopologyCount foto(s) en total (mismo requisito en ambas tarjetas)"
        binding.tvPlanStatus.isVisible = hasPlanTopologyPhotos
        binding.tvPlanStatus.text = planTopologyText
        binding.tvTopologyStatus.isVisible = hasPlanTopologyPhotos
        binding.tvTopologyStatus.text = planTopologyText
    }

    /** Nombres de los ítems del checklist que todavía no tienen ninguna foto
     *  — para explicar por qué "Analizar con IA" sigue bloqueado en vez de
     *  dejar solo un botón gris sin motivo aparente. */
    private fun missingItemNames(checklist: EvidenceChecklist): List<String> {
        val missingAreas = checklist.areas.filter { it.photos.isEmpty() }.map { it.name }
        val missingEquipment = checklist.equipment.filter { it.photos.isEmpty() }.map { it.label }
        return missingAreas + missingEquipment
    }

    private fun EvidenceAreaItem.toDisplayItem(locked: Boolean): EvidenceItem = EvidenceItem(
        id = "area:$id",
        title = name,
        subtitle = when {
            photos.isNotEmpty() -> "${photos.size} foto(s)"
            locked -> "Toca para agregar foto"
            else -> "Toca para quitar"
        },
        iconRes = R.drawable.ic_people,
        captured = photos.isNotEmpty()
    )

    private fun EvidenceEquipmentItem.toDisplayItem(locked: Boolean): EvidenceItem {
        val specsText = extractedSpecs?.values?.filterNotNull()?.joinToString(" ")?.takeIf { it.isNotBlank() }
        return EvidenceItem(
            id = "equipment:$id",
            title = label,
            subtitle = when {
                specsText != null -> specsText
                photos.isNotEmpty() -> "${photos.size} foto(s)"
                locked -> "Toca para agregar foto"
                else -> "Toca para quitar"
            },
            iconRes = iconForEquipmentType(equipmentType),
            captured = photos.isNotEmpty()
        )
    }

    private fun iconForEquipmentType(type: String): Int = when (type) {
        "router" -> R.drawable.ic_router
        "switch", "access_point" -> R.drawable.ic_network
        "camera" -> R.drawable.ic_camera
        "pos", "computer", "printer" -> R.drawable.ic_monitor
        else -> R.drawable.ic_server
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

    /** Igual que [showImageOptions], pero para un ítem ya bloqueado (Fase B)
     *  que todavía no tiene ninguna foto: agrega la opción de quitarlo si en
     *  el local real no aplica (ej. "Rack / Router" cuando no existe un
     *  rack), en vez de dejar al técnico bloqueado para siempre en
     *  "Analizar con IA" por un ítem que nunca va a poder fotografiar. */
    private fun showImageOptionsOrRemove(kind: String, id: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Agregar evidencia")
            .setItems(arrayOf("Tomar foto", "Seleccionar de galería", "No aplica — quitar")) { _, which ->
                when (which) {
                    0 -> requestCameraPermission.launch(Manifest.permission.CAMERA)
                    1 -> pickImage.launch("image/*")
                    2 -> confirmRemoveNonApplicable(kind, id)
                }
            }
            .show()
    }

    private fun confirmRemoveNonApplicable(kind: String, id: Long) {
        val checklist = viewModel.uiState.value.checklist ?: return
        val name = if (kind == "area") {
            checklist.areas.firstOrNull { it.id == id }?.name
        } else {
            checklist.equipment.firstOrNull { it.id == id }?.label
        } ?: "este ítem"

        AlertDialog.Builder(requireContext())
            .setTitle("¿Quitar $name?")
            .setMessage("Se quitará del checklist porque no aplica en este local. Como la selección ya está confirmada, esto no se puede deshacer.")
            .setPositiveButton("Quitar") { _, _ ->
                if (kind == "area") viewModel.deleteArea(id) else viewModel.deleteEquipment(id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun openCamera() {
        val file = File.createTempFile("evidence_", ".jpg", requireContext().cacheDir)
        currentPhotoFile = file
        photoUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", file
        )
        takePicture.launch(photoUri)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
