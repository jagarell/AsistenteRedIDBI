package com.upc.asistenteredidbi.presentation.minuta

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.graphics.Color
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.data.session.SessionManager
import com.upc.asistenteredidbi.databinding.FragmentMinutaProposalBinding
import com.upc.asistenteredidbi.domain.model.ChatTopology
import com.upc.asistenteredidbi.presentation.common.TopologyGraphView
import com.upc.asistenteredidbi.domain.model.ProposalPdfData
import com.upc.asistenteredidbi.domain.model.TechnicalEquipmentRecommendation
import com.upc.asistenteredidbi.domain.usecase.GenerateMinutaPdfUseCase
import com.upc.asistenteredidbi.domain.usecase.GetMinutaInfoUseCase
import com.upc.asistenteredidbi.domain.usecase.MinutaEditionsUseCase
import com.upc.asistenteredidbi.domain.usecase.ValidateMinutaUseCase
import com.upc.asistenteredidbi.domain.usecase.GenerateProposalPdfUseCase
import com.upc.asistenteredidbi.presentation.common.PdfFileUtils
import com.upc.asistenteredidbi.presentation.common.toExplainedText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MinutaProposalFragment : Fragment() {

    private var _binding: FragmentMinutaProposalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MinutaViewModel by viewModels()

    private lateinit var equipmentAdapter: ProposalEquipmentAdapter
    private lateinit var recommendationAdapter: ProposalRecommendationAdapter
    private lateinit var asIsAdapter: ProposalRecommendationAdapter

    @Inject
    lateinit var moshi: Moshi

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var generateProposalPdfUseCase: GenerateProposalPdfUseCase

    @Inject
    lateinit var generateMinutaPdfUseCase: GenerateMinutaPdfUseCase

    @Inject
    lateinit var getMapUseCase: com.upc.asistenteredidbi.domain.usecase.GetMapUseCase

    @Inject
    lateinit var chatLocalFiles: com.upc.asistenteredidbi.data.session.ChatLocalFiles

    /** Mismo evaluationId que ya resuelve el ViewModel desde SavedStateHandle — se
     *  reutiliza aquí en vez de re-parsear `arguments` con un fallback propio. */
    private val evaluationId: String get() = viewModel.evaluationId

    private val minutaId: Long by lazy {
        arguments?.getLong("minutaId", -1L) ?: -1L
    }

    @Inject
    lateinit var getMinutaInfoUseCase: GetMinutaInfoUseCase

    @Inject
    lateinit var validateMinutaUseCase: ValidateMinutaUseCase

    @Inject
    lateinit var editionsUseCase: MinutaEditionsUseCase

    /** BORRADOR | COMPLETA | VALIDADA, tal como está en el gateway. */
    private var minutaStatus = "BORRADOR"
    private var validatedBy: String? = null
    private var isSupervisor = false
    private var recordId = -1L
    private var editions: MutableMap<String, Any?> = mutableMapOf()
    private var analysis: com.upc.asistenteredidbi.data.remote.dto.AnalysisResponseDto? = null

    private var currentEquipment: List<TechnicalEquipmentRecommendation> = emptyList()
    private var currentRecommendations: List<String> = emptyList()
    private var generatedPdfFile: File? = null

    /** El PDF generado es la minuta técnica (flujo nuevo) y no la propuesta del flujo anterior. */
    private var generatedPdfIsMinuta = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMinutaProposalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecyclerViews()
        setupClicks()
        renderProposalFromChat()
        observeViewModel()
        observeTechnicianName()

        viewModel.load()
        viewModel.loadAnalysis()
        addEditMapButton()
        showSavedMapIfAny()
        observeRole()
        refreshStatus()
        loadEditions()
    }

    private fun observeRole() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.isSupervisorFlow.collect { supervisor ->
                    isSupervisor = supervisor
                    renderStatus()
                }
            }
        }
    }

    /** Estado de la minuta (borrador, completa, validada) y quién la validó. */
    private fun refreshStatus() {
        val id = evaluationId.toLongOrNull() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            getMinutaInfoUseCase(id).onSuccess { record ->
                if (record != null) {
                    recordId = record.id
                    minutaStatus = record.status.name
                    validatedBy = record.validatedByName
                }
                renderStatus()
            }
        }
    }

    private fun loadEditions() {
        val id = evaluationId.toLongOrNull() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            editionsUseCase.load(id).onSuccess {
                editions = it.toMutableMap()
                applyEditions()
            }
        }
    }

    /** Lo editado (equipos, AS-IS, TO-BE) reemplaza lo generado por la IA. */
    @Suppress("UNCHECKED_CAST")
    private fun applyEditions() {
        val ed = (editions["ed"] as? Map<String, Any?>) ?: return
        (ed["eqs"] as? List<Map<String, Any?>>)?.let { list ->
            equipmentAdapter.submitList(list.mapIndexed { i, m ->
                ProposalEquipmentItem(iconRes = iconFor(m["n"].toString()), name = m["n"].toString(), description = "", price = "",
                    quantity = "Cant: ${(m["c"] as? Number)?.toInt() ?: 1}")
            })
            binding.tvTotal.text = list.sumOf { (it["c"] as? Number)?.toInt() ?: 1 }.toString()
        }
        (ed["asis"] as? List<String>)?.let { list ->
            asIsAdapter.submitList(list.mapIndexed { i, t -> ProposalRecommendationItem(i + 1, t) })
        }
        (ed["tobe"] as? List<Map<String, Any?>>)?.let { list ->
            recommendationAdapter.submitList(list.mapIndexed { i, m -> ProposalRecommendationItem(i + 1, "${m["t"]}. ${m["d"]}") })
        }
    }

    private fun canEdit(): Boolean = when (minutaStatus) {
        "VALIDADA" -> false
        "COMPLETA" -> isSupervisor
        else -> true
    }

    private fun renderStatus() {
        if (_binding == null) return
        val (label, bg, fg) = when (minutaStatus) {
            "VALIDADA" -> Triple("Validada", "#E3F2FD", "#1565C0")
            "COMPLETA" -> Triple("Completa", "#E8F5E9", "#2E7D32")
            else -> Triple("Borrador", "#FFF3E0", "#EF6C00")
        }
        binding.tvStatusPill.text = label
        binding.tvStatusPill.setTextColor(Color.parseColor(fg))
        binding.tvStatusPill.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.parseColor(bg)); cornerRadius = 16 * resources.displayMetrics.density
        }
        binding.tvValidatedBy.text = validatedBy ?: "—"
        val generated = generatedPdfFile != null
        if (isSupervisor) {
            binding.btnGeneratePdf.text = if (minutaStatus == "VALIDADA") "Minuta validada" else "Validar minuta"
            binding.btnGeneratePdf.isEnabled = minutaStatus == "COMPLETA"
        } else {
            binding.btnGeneratePdf.text = if (generated || minutaStatus != "BORRADOR") "Volver a generar" else "Generar PDF"
            binding.btnGeneratePdf.isEnabled = true
        }
        binding.btnEdit.isEnabled = canEdit()
        binding.btnEdit.alpha = if (canEdit()) 1f else 0.4f
        val note = if ((generated || minutaStatus != "BORRADOR") && minutaStatus != "VALIDADA")
            "Aún no está validada: se comparte por WhatsApp o correo como no verificada, con la marca de agua SIN VALIDAR." else ""
        binding.tvShareNote.text = note
        binding.tvShareNote.isVisible = note.isNotEmpty()
    }

    /** Botón "Editar mapa" bajo la topología: abre el editor (si aún no hay mapa, lo genera). */
    private fun addEditMapButton() {
        val card = binding.tvTopologyDetail.parent as android.view.ViewGroup
        val density = resources.displayMetrics.density
        val button = android.widget.TextView(requireContext()).apply {
            text = "✎ Editar mapa"
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(android.graphics.Color.parseColor("#1565C0"))
            gravity = android.view.Gravity.CENTER
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.WHITE)
                cornerRadius = 22 * density
                setStroke((1.5f * density).toInt(), android.graphics.Color.parseColor("#1565C0"))
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, (44 * density).toInt()
            ).apply { topMargin = (14 * density).toInt() }
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_minutaProposalFragment_to_topologyEditor,
                    Bundle().apply { putString("evaluationId", evaluationId) }
                )
            }
        }
        card.addView(button)
    }

    /** Si el técnico generó o editó el mapa de red, la propuesta muestra ese mapa en vez del automático. */
    private fun showSavedMapIfAny() {
        val id = evaluationId.toLongOrNull() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val map = getMapUseCase(id).getOrNull()?.takeIf { it.nodes.isNotEmpty() } ?: return@launch
            val evidences = chatLocalFiles.loadEvidenceIndex(id)
            val provider: (com.upc.asistenteredidbi.domain.model.MapImageModel) -> android.graphics.Bitmap? = { image ->
                evidences.firstOrNull { it.code == image.evidenceCode && it.scope == image.scope }
                    ?.paths?.firstOrNull()
                    ?.let { com.upc.asistenteredidbi.presentation.map.MapCanvasView.decodeThumb(it) }
            }
            val card = binding.topologyGraphView.parent as android.view.ViewGroup
            binding.topologyGraphView.isVisible = false
            val index = card.indexOfChild(binding.topologyGraphView)
            val preview = com.upc.asistenteredidbi.presentation.map.MapCanvasView(requireContext()).apply {
                staticPreview = true
                this.map = map
                imageProvider = provider
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    (240 * resources.displayMetrics.density).toInt()
                ).apply { topMargin = (16 * resources.displayMetrics.density).toInt() }
                setOnClickListener { openMapZoomDialog(map, provider) }
            }
            card.addView(preview, index)
            binding.tvTopologyLegend.isVisible = true
            binding.tvTopologyDetail.text = map.summary()
        }
    }

    private fun openMapZoomDialog(
        map: com.upc.asistenteredidbi.domain.model.NetworkMap,
        provider: (com.upc.asistenteredidbi.domain.model.MapImageModel) -> android.graphics.Bitmap?
    ) = com.upc.asistenteredidbi.presentation.map.MapZoomDialog.show(requireContext(), map, provider)

    private fun setupRecyclerViews() {
        equipmentAdapter = ProposalEquipmentAdapter()
        recommendationAdapter = ProposalRecommendationAdapter()
        asIsAdapter = ProposalRecommendationAdapter()

        binding.rvEquipment.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = equipmentAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvRecommendations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recommendationAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvAsIsFindings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = asIsAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Ver: vista previa como quedará el PDF (marca VISTA PREVIA); no cambia el estado.
        binding.btnView.setOnClickListener { openPdf(preview = true) }

        binding.btnEdit.setOnClickListener { showEditMenu() }

        binding.btnSaveDraft.setOnClickListener {
            Toast.makeText(requireContext(), "Borrador guardado", Toast.LENGTH_SHORT).show()
        }

        binding.btnGeneratePdf.setOnClickListener {
            if (isSupervisor) validate() else openPdf(preview = false)
        }

        binding.btnWhatsapp.setOnClickListener { sharePdf("com.whatsapp") }
        binding.btnEmail.setOnClickListener { sendByEmail() }
        binding.btnMore.setOnClickListener { showMoreMenu() }
    }

    /** El supervisor valida una minuta COMPLETA: el PDF ya sale sin la marca SIN VALIDAR y se avisa al técnico. */
    private fun validate() {
        if (recordId <= 0 || minutaStatus != "COMPLETA") return
        binding.btnGeneratePdf.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            validateMinutaUseCase(recordId)
                .onSuccess { record ->
                    minutaStatus = record.status.name
                    validatedBy = record.validatedByName
                    generatedPdfFile = null
                    Toast.makeText(requireContext(), "Minuta validada", Toast.LENGTH_SHORT).show()
                }
                .onFailure { Toast.makeText(requireContext(), it.message ?: "No se pudo validar", Toast.LENGTH_SHORT).show() }
            renderStatus()
        }
    }

    private fun sharePdf(targetPackage: String? = null) {
        val file = generatedPdfFile
        if (file == null || !file.exists()) {
            Toast.makeText(requireContext(), "Primero genera el PDF", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        val name = binding.tvEstablishmentName.text.toString()
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Minuta técnica de red — $name")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            targetPackage?.let { setPackage(it) }
        }
        runCatching { startActivity(if (targetPackage == null) android.content.Intent.createChooser(intent, "Compartir a") else intent) }
            .onFailure { Toast.makeText(requireContext(), "No se encontró una app para compartir", Toast.LENGTH_SHORT).show() }
    }

    private fun sendByEmail() {
        val file = generatedPdfFile
        if (file == null || !file.exists()) {
            Toast.makeText(requireContext(), "Primero genera el PDF", Toast.LENGTH_SHORT).show()
            return
        }
        val dataJson = moshi.adapter(ProposalPdfData::class.java).toJson(buildProposalPdfData())
        findNavController().navigate(
            R.id.action_minutaProposalFragment_to_sendDraftFragment,
            Bundle().apply {
                putString("evaluationId", evaluationId)
                putString("pdfPath", file.absolutePath)
                putString("proposalDataJson", dataJson)
                putBoolean("isMinutaPdf", generatedPdfIsMinuta)
            }
        )
    }

    /** ⋮ : mapa, edición, lecturas, cambios por chat, observaciones y la hoja de compartir de Android. */
    private fun showMoreMenu() {
        val items = listOf("Ver mapa de red", "Editar propuesta técnica", "Corregir una lectura de evidencia",
            "Pedir un cambio en el chat", "Observaciones", "Compartir a…")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> findNavController().navigate(
                        R.id.action_minutaProposalFragment_to_topologyEditor,
                        Bundle().apply { putString("evaluationId", evaluationId) }
                    )
                    1 -> if (canEdit()) showProposalEditor() else cannotEdit()
                    2 -> if (canEdit()) showReadingPicker() else cannotEdit()
                    3 -> openChat(null)
                    4 -> if (canEdit()) showObservations() else cannotEdit()
                    5 -> sharePdf(null)
                }
            }.show()
    }

    private fun cannotEdit() =
        Toast.makeText(requireContext(), if (minutaStatus == "VALIDADA") "La minuta ya está validada" else "La minuta está completa: solo el supervisor la edita", Toast.LENGTH_SHORT).show()

    /** Lápiz: editar la propuesta, corregir una lectura o pedir un cambio en el chat. */
    private fun showEditMenu() {
        if (!canEdit()) return cannotEdit()
        val items = arrayOf("Editar propuesta técnica", "Corregir una lectura de evidencia", "Pedir un cambio en el chat")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Editar")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showProposalEditor()
                    1 -> showReadingPicker()
                    2 -> openChat(null)
                }
            }.show()
    }

    /** Abre el chat de la evaluación (terminada) para corregir una respuesta o una lectura. */
    private fun openChat(reopenNodeId: String?) {
        findNavController().navigate(
            R.id.technicalChatFragment,
            Bundle().apply {
                putString("evaluationId", evaluationId)
                reopenNodeId?.let { putString("reopenNodeId", it) }
            }
        )
    }

    private fun showReadingPicker() {
        val id = evaluationId.toLongOrNull() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val items = chatLocalFiles.loadEvidenceIndex(id).filter { it.status != "missing" && it.paths.isNotEmpty() || it.status == "missing" }
            if (items.isEmpty()) {
                Toast.makeText(requireContext(), "Todavía no hay evidencias en el chat", Toast.LENGTH_SHORT).show()
                return@launch
            }
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("¿Qué lectura quieres corregir?")
                .setItems(items.map { it.title }.toTypedArray()) { _, which ->
                    val item = items[which]
                    openChat(if (item.scope.isBlank()) item.code else "${item.code}#${item.scope}")
                }.show()
        }
    }

    private fun showObservations() {
        @Suppress("UNCHECKED_CAST")
        val current = (editions["obs"] as? Map<String, Any?>).orEmpty()
        ObservationsDialog.show(requireContext(), current) { obs ->
            editions["obs"] = obs
            saveEditions("Observaciones guardadas en la minuta")
        }
    }

    private fun saveEditions(message: String) {
        val id = evaluationId.toLongOrNull() ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            editionsUseCase.save(id, editions)
                .onSuccess { Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show(); generatedPdfFile = null; renderStatus() }
                .onFailure { Toast.makeText(requireContext(), it.message ?: "No se pudo guardar", Toast.LENGTH_SHORT).show() }
        }
    }

    /** Editar la propuesta por apartado: datos del local, equipos, AS-IS, TO-BE, observaciones y topología. */
    private fun showProposalEditor() {
        val sections = arrayOf("Datos del local", "Topología (editor del mapa)", "Equipamiento recomendado",
            "Diagnóstico actual (AS-IS)", "Propuesta recomendada (TO-BE)", "Observaciones", "Volver a la propuesta generada por la IA")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Editar propuesta técnica")
            .setItems(sections) { _, which ->
                when (which) {
                    0 -> editMainData()
                    1 -> findNavController().navigate(
                        R.id.action_minutaProposalFragment_to_topologyEditor,
                        Bundle().apply { putString("evaluationId", evaluationId) }
                    )
                    2 -> editEquipment()
                    3 -> editAsIs()
                    4 -> editToBe()
                    5 -> showObservations()
                    6 -> {
                        editions.remove("ed")
                        saveEditions("Se volvió a la propuesta generada por la IA")
                        viewModel.loadAnalysis()
                    }
                }
            }.show()
    }

    private fun editMainData() {
        @Suppress("UNCHECKED_CAST")
        val cur = (editions["datos"] as? Map<String, Any?>).orEmpty()
        val fields = listOf("razon" to "Razón social", "ruc" to "RUC (11 dígitos)", "nombre" to "Nombre del local", "dir" to "Dirección",
            "encargado" to "Encargado del local", "celular" to "Celular del encargado (9 dígitos)", "correo" to "Correo del encargado")
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val form = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding((24 * dp).toInt(), (8 * dp).toInt(), (24 * dp).toInt(), 0) }
        val inputs = fields.map { (k, label) ->
            form.addView(android.widget.TextView(ctx).apply { text = label; textSize = 12f; setTextColor(Color.parseColor("#6B7280")); setPadding(0, (10 * dp).toInt(), 0, 0) })
            android.widget.EditText(ctx).apply {
                setText(cur[k]?.toString().orEmpty())
                hint = when (k) { "nombre" -> binding.tvEstablishmentName.text.toString(); "dir" -> "Se toma del chat si lo dejas vacío"; else -> "Se toma del chat si lo dejas vacío" }
                if (k == "ruc" || k == "celular") inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }.also { form.addView(it) }
        }
        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("Datos del local")
            .setView(android.widget.ScrollView(ctx).apply { addView(form) })
            .setPositiveButton("Guardar") { _, _ ->
                val ruc = inputs[1].text.toString().trim()
                val cel = inputs[5].text.toString().trim()
                if (ruc.isNotEmpty() && !Regex("(10|15|17|20)\\d{9}").matches(ruc)) { Toast.makeText(ctx, "El RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20", Toast.LENGTH_LONG).show(); return@setPositiveButton }
                if (cel.isNotEmpty() && !Regex("9\\d{8}").matches(cel)) { Toast.makeText(ctx, "El celular debe tener 9 dígitos y empezar con 9", Toast.LENGTH_LONG).show(); return@setPositiveButton }
                editions["datos"] = fields.mapIndexed { i, (k, _) -> k to inputs[i].text.toString().trim() }.filter { it.second.isNotEmpty() }.toMap()
                saveEditions("Datos guardados. Se ven en el PDF.")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Editor de listas: cada elemento con título (y detalle opcional); se puede quitar o agregar. */
    private fun editList(title: String, initial: List<Pair<String, String>>, withDetail: Boolean, onSave: (List<Pair<String, String>>) -> Unit) {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val rows = initial.toMutableList()
        val box = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding((20 * dp).toInt(), (8 * dp).toInt(), (20 * dp).toInt(), 0) }
        val titleInputs = mutableListOf<android.widget.EditText>()
        val detailInputs = mutableListOf<android.widget.EditText>()
        lateinit var render: () -> Unit
        render = {
            box.removeAllViews(); titleInputs.clear(); detailInputs.clear()
            rows.forEachIndexed { i, (t, d) ->
                val row = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(0, (8 * dp).toInt(), 0, 0) }
                val top = android.widget.LinearLayout(ctx).apply { orientation = android.widget.LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
                val ti = android.widget.EditText(ctx).apply { setText(t); hint = "Texto"; textSize = 14f }
                titleInputs += ti
                top.addView(ti, android.widget.LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                top.addView(android.widget.TextView(ctx).apply {
                    text = "✕"; textSize = 16f; setTextColor(Color.parseColor("#D32F2F")); setPadding((12 * dp).toInt(), 0, (4 * dp).toInt(), 0)
                    setOnClickListener {
                        rows.clear(); rows.addAll(titleInputs.indices.map { j -> titleInputs[j].text.toString() to (detailInputs.getOrNull(j)?.text?.toString() ?: "") })
                        rows.removeAt(i); render()
                    }
                })
                row.addView(top)
                if (withDetail) {
                    val di = android.widget.EditText(ctx).apply { setText(d); hint = "Detalle"; textSize = 13f }
                    detailInputs += di; row.addView(di)
                }
                box.addView(row)
            }
            box.addView(android.widget.TextView(ctx).apply {
                text = "+ Agregar"; setTextColor(Color.parseColor("#1565C0")); textSize = 15f; typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, (14 * dp).toInt(), 0, (6 * dp).toInt())
                setOnClickListener {
                    rows.clear(); rows.addAll(titleInputs.indices.map { j -> titleInputs[j].text.toString() to (detailInputs.getOrNull(j)?.text?.toString() ?: "") })
                    rows.add("" to ""); render()
                }
            })
        }
        render()
        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(title)
            .setView(android.widget.ScrollView(ctx).apply { addView(box) })
            .setPositiveButton("Guardar") { _, _ ->
                onSave(titleInputs.indices.map { j -> titleInputs[j].text.toString().trim() to (detailInputs.getOrNull(j)?.text?.toString()?.trim() ?: "") }.filter { it.first.isNotEmpty() })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @Suppress("UNCHECKED_CAST")
    private fun editedPart(): MutableMap<String, Any?> = ((editions["ed"] as? Map<String, Any?>) ?: emptyMap()).toMutableMap()

    private fun editEquipment() {
        val ed = editedPart()
        val current = (ed["eqs"] as? List<Map<String, Any?>>)?.map { it["n"].toString() to ((it["c"] as? Number)?.toInt() ?: 1).toString() }
            ?: currentEquipment.map { it.name to it.quantity.toString() }
        editList("Equipamiento recomendado (nombre y cantidad)", current, withDetail = true) { rows ->
            ed["eqs"] = rows.map { mapOf("n" to it.first, "c" to (it.second.toIntOrNull() ?: 1)) }
            editions["ed"] = ed; saveEditions("Equipamiento guardado"); applyEditions()
        }
    }

    private fun editAsIs() {
        val ed = editedPart()
        val current = (ed["asis"] as? List<String>) ?: analysis?.asIsFindings.orEmpty()
        editList("Diagnóstico actual (AS-IS)", current.map { it to "" }, withDetail = false) { rows ->
            ed["asis"] = rows.map { it.first }; editions["ed"] = ed; saveEditions("Diagnóstico guardado"); applyEditions()
        }
    }

    private fun editToBe() {
        val ed = editedPart()
        val current = (ed["tobe"] as? List<Map<String, Any?>>)?.map { it["t"].toString() to it["d"].toString() }
            ?: analysis?.recommendationItems.orEmpty().filter { it.type != "AVISO" }.map { it.title to it.detail }
        editList("Propuesta recomendada (TO-BE)", current, withDetail = true) { rows ->
            ed["tobe"] = rows.map { mapOf("t" to it.first, "d" to it.second) }; editions["ed"] = ed; saveEditions("Propuesta guardada"); applyEditions()
        }
    }

    /** Los datos reales en pantalla, listos para generar/enviar el PDF. */
    private fun buildProposalPdfData(): ProposalPdfData = ProposalPdfData(
        establishmentName = binding.tvEstablishmentName.text.toString(),
        address = binding.tvEstablishmentAddress.text.toString(),
        technicianName = binding.tvTechnicianName.text.toString(),
        score = arguments?.getInt("score", -1)?.takeIf { it >= 0 },
        summary = arguments?.getString("proposalSummary"),
        recommendations = currentRecommendations,
        equipment = currentEquipment,
        topologyText = binding.tvTopologyDetail.text.toString()
    )

    /**
     * Pide al gateway la minuta técnica de la evaluación y la abre. `preview` = vista previa (marca VISTA PREVIA,
     * no cambia el estado); sin él, al generarse bien la minuta pasa a COMPLETA y se avisa a los supervisores.
     * Las evaluaciones hechas con el flujo anterior no tienen datos para la minuta: ahí se genera el PDF de siempre.
     */
    private fun openPdf(preview: Boolean) {
        val data = buildProposalPdfData()
        val button = if (preview) binding.btnView else binding.btnGeneratePdf
        binding.btnGeneratePdf.isEnabled = false
        binding.btnView.isEnabled = false
        val previousText = binding.btnGeneratePdf.text
        if (!preview) binding.btnGeneratePdf.text = "Generando..."

        viewLifecycleOwner.lifecycleScope.launch {
            val id = evaluationId.toLongOrNull()
            val minutaResult = id?.let { generateMinutaPdfUseCase(it, preview) }
            val minuta = minutaResult?.getOrNull()
            if (minuta == null && minutaResult != null && !isLegacy(minutaResult.exceptionOrNull())) {
                // La minuta nueva no se pudo generar por un motivo que el técnico puede resolver (falta una foto obligatoria...).
                Toast.makeText(requireContext(), minutaResult.exceptionOrNull()?.message ?: "No se pudo generar el PDF", Toast.LENGTH_LONG).show()
                binding.btnGeneratePdf.text = previousText
                binding.btnView.isEnabled = true
                renderStatus()
                return@launch
            }
            val isMinuta = minuta != null
            val result = if (minuta != null) Result.success(minuta) else generateProposalPdfUseCase(data)
            result
                .onSuccess { bytes ->
                    val file = PdfFileUtils.saveToCache(
                        requireContext(), bytes,
                        (if (isMinuta) "minuta_" else "propuesta_") + (if (preview) "vista_" else "") + "$evaluationId.pdf"
                    )
                    if (!preview) {
                        generatedPdfFile = file
                        generatedPdfIsMinuta = isMinuta
                        refreshStatus()
                    }
                    findNavController().navigate(
                        R.id.action_minutaProposalFragment_to_pdfPreview,
                        Bundle().apply {
                            putString("pdfPath", file.absolutePath)
                            putString("establishmentName", binding.tvEstablishmentName.text.toString())
                            putBoolean("preview", preview)
                        }
                    )
                }
                .onFailure { error ->
                    Toast.makeText(requireContext(), error.message ?: "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                }
            binding.btnView.isEnabled = true
            renderStatus()
        }
    }

    /** El gateway responde así cuando la evaluación se hizo con el flujo anterior (sin datos para la minuta). */
    private fun isLegacy(error: Throwable?): Boolean {
        val m = error?.message.orEmpty() + error?.cause?.message.orEmpty()
        return m.contains("flujo anterior") || m.contains("aún no se completó")
    }

    /** Pinta el resumen, el equipo y la topología que llegaron desde el chat. */
    private fun renderProposalFromChat() {
        val today = SimpleDateFormat("d MMM yyyy", Locale("es", "ES")).format(Date())
        binding.tvDate.text = today

        val equipmentJson = arguments?.getString("equipmentJson").orEmpty()
        val equipment = parseEquipment(equipmentJson)
        currentEquipment = equipment

        if (equipment.isNotEmpty()) {
            equipmentAdapter.submitList(equipment.map { item ->
                ProposalEquipmentItem(
                    iconRes = iconFor(item.name),
                    name = item.name,
                    description = item.description,
                    price = item.unitPrice?.let { "$${"%.0f".format(it)}" }.orEmpty(),
                    quantity = "Cant: ${item.quantity}"
                )
            })
            binding.tvTotal.text = equipment.sumOf { it.quantity }.toString()
        }

        val topologyJson = arguments?.getString("topologyJson").orEmpty()
        val topology = parseTopology(topologyJson)
        val topologyText = arguments?.getString("topologyText").orEmpty()

        binding.tvTopologyDetail.text = when {
            topology != null -> topology.toExplainedText()
            topologyText.isNotBlank() -> topologyText
            else -> "Topología no disponible."
        }

        val hasGraph = topology != null && topology.nodes.isNotEmpty()
        binding.topologyGraphView.isVisible = hasGraph
        binding.tvTopologyLegend.isVisible = hasGraph
        if (hasGraph) {
            binding.topologyGraphView.setTopology(topology)
            binding.topologyGraphView.setOnClickListener {
                openTopologyZoomDialog(topology)
            }
        }
    }

    /** Pantalla completa con pellizcar-para-zoom y arrastre — la vista chica
     * embebida no soporta gestos para no competir con el scroll de la
     * pantalla de Propuesta Técnica. */
    private fun openTopologyZoomDialog(topology: ChatTopology) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_topology_zoom)

        dialog.findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TopologyGraphView>(R.id.topologyGraphViewZoom).apply {
            zoomEnabled = true
            setTopology(topology)
        }

        dialog.show()
    }

    private fun parseEquipment(json: String): List<TechnicalEquipmentRecommendation> {
        if (json.isBlank()) return emptyList()
        return try {
            val type = Types.newParameterizedType(
                List::class.java,
                TechnicalEquipmentRecommendation::class.java
            )
            moshi.adapter<List<TechnicalEquipmentRecommendation>>(type).fromJson(json).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseTopology(json: String): ChatTopology? {
        if (json.isBlank()) return null
        return try {
            moshi.adapter(ChatTopology::class.java).fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    private fun iconFor(equipmentName: String): Int {
        val name = equipmentName.lowercase()
        return when {
            "router" in name -> R.drawable.ic_router
            "switch" in name -> R.drawable.ic_network
            "access point" in name || "wifi" in name -> R.drawable.ic_wifi
            "firewall" in name || "seguridad" in name -> R.drawable.ic_security
            "ups" in name || "energía" in name || "energia" in name -> R.drawable.ic_energy
            else -> R.drawable.ic_network
        }
    }

    private fun observeTechnicianName() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sessionManager.fullNameFlow.collect { name ->
                    binding.tvTechnicianName.text = name?.takeIf { it.isNotBlank() } ?: "-"
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    state.minuta?.let { minuta ->
                        binding.tvEstablishmentName.text = minuta.establishmentName
                        val type = minuta.establishmentType?.takeIf { it.isNotBlank() }
                        val address = minuta.establishmentAddress?.takeIf { it.isNotBlank() }
                        binding.tvEstablishmentAddress.text = when {
                            type != null && address != null -> "$type · $address"
                            type != null -> type
                            address != null -> address
                            else -> "Propuesta de Infraestructura de Red"
                        }
                    }

                    state.analysis?.let { analysis ->
                        this@MinutaProposalFragment.analysis = analysis
                        if (currentEquipment.isEmpty()) {
                            val counts = linkedMapOf<String, Int>()
                            analysis.recommendationItems.mapNotNull { it.equipment }.forEach { counts[it] = (counts[it] ?: 0) + 1 }
                            if (counts.isNotEmpty()) {
                                currentEquipment = counts.map { (n, c) -> TechnicalEquipmentRecommendation(n, "", c) }
                                equipmentAdapter.submitList(counts.entries.map { (n, c) ->
                                    ProposalEquipmentItem(iconRes = iconFor(n), name = n, description = "", price = "", quantity = "Cant: $c")
                                })
                                binding.tvTotal.text = counts.values.sum().toString()
                            }
                        }
                        currentRecommendations = analysis.recommendations
                        recommendationAdapter.submitList(
                            if (analysis.recommendationItems.isNotEmpty())
                                analysis.recommendationItems.filter { it.type != "AVISO" }.mapIndexed { index, r ->
                                    ProposalRecommendationItem(index + 1, "${r.title}. ${r.detail}")
                                }
                            else analysis.recommendations.mapIndexed { index, text ->
                                ProposalRecommendationItem(index + 1, text)
                            }
                        )
                        asIsAdapter.submitList(
                            analysis.asIsFindings.mapIndexed { index, text ->
                                ProposalRecommendationItem(index + 1, text)
                            }
                        )
                        applyEditions()
                    }

                    state.analysisErrorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.clearAnalysisError()
                    }

                    state.errorMessage?.let { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        viewModel.clearGeneralError()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
