package com.upc.asistenteredidbi.presentation.map

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.domain.model.MapImageModel
import com.upc.asistenteredidbi.domain.model.MapLinkModel
import com.upc.asistenteredidbi.domain.model.MapNodeModel
import com.upc.asistenteredidbi.domain.model.MapNodeTypes
import com.upc.asistenteredidbi.domain.model.MapTextModel
import com.upc.asistenteredidbi.domain.model.NetworkMap
import com.upc.asistenteredidbi.presentation.chat.ChatEvidenceItem
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Editor del mapa de red ("Editar topología"): arrastrar equipos, mantener presionado
 * para renombrar/cambiar tipo o color/eliminar, conectar, agregar elementos, líneas,
 * cajas de texto y fotos de la visita, deshacer y guardar.
 */
@AndroidEntryPoint
class TopologyEditorFragment : Fragment() {

    private enum class Tool { NONE, CONNECT, LINE, TEXT, IMAGE }

    private val viewModel: TopologyEditorViewModel by viewModels()

    private lateinit var canvas: MapCanvasView
    private lateinit var banner: TextView
    private lateinit var linePopup: LinearLayout
    private lateinit var panelHost: FrameLayout
    private lateinit var saveButton: TextView
    private val toolbar = HashMap<String, LinearLayout>()
    private val thumbs = HashMap<String, Bitmap?>()

    private var tool = Tool.NONE
    private var lineStyle = "solid"
    private var sourceId: String? = null

    private val blue = Color.parseColor("#1565C0")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        // ---- barra superior ------------------------------------------------------------------
        root.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(34), dp(16), dp(10))
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_close)
                setColorFilter(Color.parseColor("#1F2937"))
                setPadding(dp(10), dp(10), dp(10), dp(10))
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
                setOnClickListener { confirmExit() }
            })
            addView(TextView(context).apply {
                text = "Editar topología"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F2937"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(6) }
            })
            saveButton = TextView(context).apply {
                text = "Guardar"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = rounded(blue, 18f)
                setPadding(dp(18), dp(8), dp(18), dp(8))
                setOnClickListener { viewModel.save() }
            }
            addView(saveButton)
        })

        // ---- banner de modo ---------------------------------------------------------------------
        banner = TextView(context).apply {
            textSize = 11.5f
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(7), dp(12), dp(7))
        }
        root.addView(banner)

        // ---- lienzo + selector de línea -------------------------------------------------------
        val canvasHost = FrameLayout(context).apply { setBackgroundColor(Color.parseColor("#F7F8FB")) }
        canvas = MapCanvasView(context).apply {
            mode = MapCanvasView.Mode.EDIT
            imageProvider = { image -> thumbFor(image) }
            listener = object : MapCanvasView.Listener {
                override fun onBeforeChange() = viewModel.pushUndo(canvas.map)
                override fun onMapChanged(map: NetworkMap) = viewModel.setMap(map)
                override fun onSelectionChanged(selection: MapCanvasView.Selection?) = refreshChrome()
                override fun onNodeLongPress(node: MapNodeModel) = showNodeMenu(node)
                override fun onConnectSourcePicked(sourceId: String) {
                    this@TopologyEditorFragment.sourceId = sourceId
                    refreshBanner()
                }
                override fun onConnect(sourceId: String, targetId: String) = finishLink(sourceId, targetId)
            }
        }
        canvasHost.addView(canvas, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        linePopup = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.WHITE, 12f, Color.parseColor("#D8DEE9"))
            elevation = 6f * resources.displayMetrics.density
            setPadding(dp(6), dp(6), dp(6), dp(6))
            visibility = View.GONE
        }
        canvasHost.addView(linePopup, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(10), dp(10), 0, 0)
        })
        root.addView(canvasHost, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        // ---- panel inferior (texto / imagen) y barra de herramientas ----------------------------
        panelHost = FrameLayout(context).apply { visibility = View.GONE }
        root.addView(panelHost, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(View(context).apply { setBackgroundColor(Color.parseColor("#E5E9F0")) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1))
        root.addView(LinearLayout(context).apply {
            setPadding(dp(8), dp(6), dp(8), dp(20))
            listOf(
                Triple("add", "Agregar", R.drawable.ic_add),
                Triple("connect", "Conectar", R.drawable.ic_link),
                Triple("delete", "Borrar", R.drawable.ic_delete),
                Triple("undo", "Deshacer", R.drawable.ic_undo)
            ).forEach { (id, label, icon) ->
                val item = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(0, dp(6), 0, dp(6))
                    addView(ImageView(context).apply {
                        setImageResource(icon)
                        layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
                    })
                    addView(TextView(context).apply { text = label; textSize = 12f; gravity = Gravity.CENTER_HORIZONTAL })
                    setOnClickListener { onToolbar(id) }
                }
                toolbar[id] = item
                addView(item, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
        })

        refreshChrome()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    if (canvas.map != s.map) canvas.map = s.map
                    saveButton.alpha = if (s.isSaving || s.isLoading) 0.5f else 1f
                    saveButton.text = if (s.isSaving) "Guardando…" else "Guardar"
                    toolbar["undo"]?.alpha = if (s.canUndo) 1f else 0.4f
                    s.errorMessage?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                    if (s.saved) {
                        setFragmentResult("map_saved", bundleOf())
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Barra de herramientas
    // ---------------------------------------------------------------------------------------------
    private fun onToolbar(id: String) {
        when (id) {
            "add" -> showAddMenu()
            "connect" -> if (tool == Tool.CONNECT) endTool() else startTool(Tool.CONNECT)
            "delete" -> deleteSelected()
            "undo" -> {
                endTool()
                viewModel.undo()
            }
        }
    }

    private fun startTool(newTool: Tool) {
        endTool(keepPanel = false)
        tool = newTool
        sourceId = null
        canvas.mode = if (newTool == Tool.CONNECT || newTool == Tool.LINE) MapCanvasView.Mode.CONNECT else MapCanvasView.Mode.EDIT
        refreshChrome()
    }

    private fun endTool(keepPanel: Boolean = false) {
        tool = Tool.NONE
        sourceId = null
        canvas.mode = MapCanvasView.Mode.EDIT
        if (!keepPanel) {
            panelHost.removeAllViews()
            panelHost.visibility = View.GONE
        }
        refreshChrome()
    }

    private fun refreshChrome() {
        refreshBanner()
        val active = when (tool) {
            Tool.CONNECT -> "connect"
            Tool.LINE, Tool.TEXT, Tool.IMAGE -> "add"
            else -> null
        }
        toolbar.forEach { (id, item) ->
            val selected = id == active
            item.background = if (selected) rounded(Color.parseColor("#E3EEFC"), 12f) else null
            for (i in 0 until item.childCount) {
                val child = item.getChildAt(i)
                val color = if (selected) blue else Color.parseColor("#4B5563")
                if (child is ImageView) child.setColorFilter(color) else if (child is TextView) child.setTextColor(color)
            }
        }
        linePopup.visibility = if (tool == Tool.LINE) View.VISIBLE else View.GONE
        if (tool == Tool.LINE) buildLinePopup()
    }

    private fun refreshBanner() {
        val map = viewModel.state.value.map
        val source = sourceId?.let { id -> map.nodes.firstOrNull { it.id == id }?.label }
        val (text, bg, fg) = when (tool) {
            Tool.CONNECT -> Triple(
                if (source != null) "Modo conectar · origen: $source · ahora toca el destino" else "Modo conectar · toca el origen",
                "#E0F2F1", "#00695C"
            )
            Tool.LINE -> Triple(
                (if (lineStyle == "solid") "Línea recta" else "Línea discontinua") +
                    (if (source != null) " · origen: $source · ahora toca el destino" else " · toca el origen"),
                if (lineStyle == "solid") "#ECEFF3" else "#F0E6F8", if (lineStyle == "solid") "#4B5563" else "#6A1B9A"
            )
            Tool.TEXT -> Triple("Caja de texto · arrástrala a su lugar", "#FFF3E0", "#B26A00")
            Tool.IMAGE -> Triple("Imagen · arrastra para mover · elige el tamaño abajo", "#E8F5E9", "#2E7D32")
            Tool.NONE -> Triple("Arrastra los equipos · mantén presionado para más opciones", "#F3F5F9", "#6B7280")
        }
        banner.text = text
        banner.setBackgroundColor(Color.parseColor(bg))
        banner.setTextColor(Color.parseColor(fg))
    }

    private fun buildLinePopup() {
        linePopup.removeAllViews()
        listOf("solid" to "—  Recta", "dashed" to "···  Discontinua").forEach { (style, label) ->
            linePopup.addView(TextView(requireContext()).apply {
                text = label
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                val selected = lineStyle == style
                setTextColor(if (selected) Color.WHITE else Color.parseColor("#374151"))
                background = if (selected) rounded(blue, 14f) else null
                setPadding(dp(12), dp(6), dp(12), dp(6))
                setOnClickListener {
                    lineStyle = style
                    refreshChrome()
                }
            })
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Menú "Agregar al mapa"
    // ---------------------------------------------------------------------------------------------
    private fun showAddMenu() {
        val context = requireContext()
        val dialog = BottomSheetDialog(context)
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(28))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(dp(28).toFloat(), dp(28).toFloat(), dp(28).toFloat(), dp(28).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        box.addView(View(context).apply {
            background = rounded(Color.parseColor("#E0E3EA"), 3f)
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(5)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(6) }
        })
        box.addView(TextView(context).apply {
            text = "Agregar al mapa"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1F2937"))
            setPadding(0, dp(12), 0, dp(12))
        })
        data class Row(val title: String, val subtitle: String, val icon: Int, val tint: String, val bg: String, val action: () -> Unit)
        listOf(
            Row("Elemento de red", "Router, switch, AP, PC, impresora…", R.drawable.ic_network, "#1565C0", "#E3EEFC") { showElementSheet() },
            Row("Línea recta", "Conexión por cable entre dos elementos", R.drawable.ic_remove, "#546E7A", "#ECEFF1") { lineStyle = "solid"; startTool(Tool.LINE) },
            Row("Línea discontinua", "Enlace WiFi o inalámbrico", R.drawable.ic_more_horiz, "#7E57C2", "#EDE7F6") { lineStyle = "dashed"; startTool(Tool.LINE) },
            Row("Caja de texto", "Nota o etiqueta sobre el mapa", R.drawable.ic_text_fields, "#EF8F00", "#FFF3E0") { addTextBox() },
            Row("Imagen", "Foto del equipo, gabinete o plano", R.drawable.ic_image, "#2E7D32", "#E8F5E9") { pickImage() }
        ).forEach { row ->
            box.addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(10), 0, dp(10))
                addView(ImageView(context).apply {
                    setImageResource(row.icon)
                    setColorFilter(Color.parseColor(row.tint))
                    background = rounded(Color.parseColor(row.bg), 14f)
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    layoutParams = LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginEnd = dp(16) }
                })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(TextView(context).apply { text = row.title; textSize = 16f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#1F2937")) })
                    addView(TextView(context).apply { text = row.subtitle; textSize = 13f; setTextColor(Color.parseColor("#6B7280")) })
                })
                setOnClickListener {
                    dialog.dismiss()
                    row.action()
                }
            })
        }
        dialog.setContentView(box)
        dialog.show()
        // La hoja de Material tiñe el fondo; el prototipo lo lleva blanco.
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(Color.TRANSPARENT)
    }

    /** Hoja "Elemento de red": tipo, nombre y a qué se conecta. */
    private fun showElementSheet() {
        val context = requireContext()
        val dialog = BottomSheetDialog(context)
        val map = viewModel.state.value.map
        var chosen = "switch"
        val tiles = HashMap<String, LinearLayout>()

        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(24))
        }
        box.addView(TextView(context).apply {
            text = "Elemento de red"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1F2937"))
            setPadding(0, dp(12), 0, dp(10))
        })
        val grid = GridLayout(context).apply { columnCount = 3 }
        fun refreshTiles() = tiles.forEach { (type, tile) ->
            tile.background = if (type == chosen) rounded(Color.parseColor("#E3EEFC"), 12f, blue) else rounded(Color.WHITE, 12f, Color.parseColor("#E1E6EE"))
        }
        MapNodeTypes.all.forEach { (type, name) ->
            val tile = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, dp(8))
                addView(ImageView(context).apply {
                    setImageResource(iconFor(type))
                    setColorFilter(Color.WHITE)
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(MapNodeTypes.colorHex(type))) }
                    setPadding(dp(8), dp(8), dp(8), dp(8))
                    layoutParams = LinearLayout.LayoutParams(dp(38), dp(38))
                })
                addView(TextView(context).apply {
                    text = name; textSize = 11f; setTextColor(Color.parseColor("#374151")); setPadding(0, dp(4), 0, 0)
                    gravity = Gravity.CENTER
                })
                setOnClickListener { chosen = type; refreshTiles() }
            }
            tiles[type] = tile
            grid.addView(tile, GridLayout.LayoutParams().apply {
                width = 0; height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
        refreshTiles()
        box.addView(grid)

        val nameInput = EditText(context).apply {
            hint = "Nombre"
            setText("Switch")
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            background = rounded(Color.WHITE, 12f, blue)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        box.addView(nameInput, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) })
        val targets = listOf("— Sin conexión —") + map.nodes.map { it.label }
        val spinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, targets)
            setSelection(map.nodes.indexOfFirst { it.type == "router" }.let { if (it >= 0) it + 1 else 0 })
        }
        box.addView(TextView(context).apply { text = "Conectar a"; textSize = 11f; setTextColor(Color.parseColor("#6B7280")); setPadding(dp(4), dp(10), 0, 0) })
        box.addView(spinner)
        box.addView(TextView(context).apply {
            text = "Agregar al mapa"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded(blue, 24f)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(14) }
            setOnClickListener {
                val name = nameInput.text?.toString().orEmpty().trim().ifBlank { MapNodeTypes.label(chosen) }
                val target = spinner.selectedItemPosition.takeIf { it > 0 }?.let { map.nodes[it - 1] }
                addNode(chosen, name, target)
                dialog.dismiss()
            }
        })
        dialog.setContentView(android.widget.ScrollView(context).apply { addView(box) })
        dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    // ---------------------------------------------------------------------------------------------
    // Operaciones sobre el mapa (cada una guarda un paso de "Deshacer")
    // ---------------------------------------------------------------------------------------------
    private fun current(): NetworkMap = viewModel.state.value.map

    private fun mutate(change: (NetworkMap) -> NetworkMap) {
        viewModel.pushUndo(current())
        viewModel.setMap(change(current()))
    }

    private fun newId(prefix: String) = prefix + UUID.randomUUID().toString().take(6)

    private fun addNode(type: String, name: String, target: MapNodeModel?) {
        val map = current()
        val (x, y) = freeSpot(map, target)
        val node = MapNodeModel(id = newId("n"), label = name, type = type, x = x, y = y)
        mutate { m ->
            var next = m.copy(nodes = m.nodes + node)
            if (target != null) next = next.copy(links = next.links + autoLink(target, node))
            next
        }
        canvas.select(MapCanvasView.Selection("node", node.id))
    }

    private fun autoLink(a: MapNodeModel, b: MapNodeModel): MapLinkModel {
        val wireless = setOf("access_point", "repeater")
        val style = if (a.type in wireless || b.type in wireless || b.type == "pos" && a.type != "switch") "dashed" else "solid"
        return MapLinkModel(id = newId("l"), source = a.id, target = b.id, style = style)
    }

    private fun freeSpot(map: NetworkMap, near: MapNodeModel?): Pair<Float, Float> {
        var x = ((near?.x ?: 0.5f) + 0.2f).coerceAtMost(0.9f)
        var y = ((near?.y ?: 0.4f) + 0.2f).coerceAtMost(0.9f)
        var guard = 0
        while (map.nodes.any { kotlin.math.abs(it.x - x) < 0.14f && kotlin.math.abs(it.y - y) < 0.1f } && guard++ < 20) {
            x = if (x > 0.5f) x - 0.16f else x + 0.16f
            x = x.coerceIn(0.08f, 0.92f)
            y = (y + 0.07f).coerceAtMost(0.92f)
        }
        return x to y
    }

    private fun finishLink(source: String, target: String) {
        val map = current()
        val a = map.nodes.firstOrNull { it.id == source } ?: return
        val b = map.nodes.firstOrNull { it.id == target } ?: return
        val style = when (tool) {
            Tool.LINE -> lineStyle
            else -> autoLink(a, b).style
        }
        if (map.links.none { (it.source == source && it.target == target) || (it.source == target && it.target == source) }) {
            mutate { it.copy(links = it.links + MapLinkModel(id = newId("l"), source = source, target = target, style = style)) }
        }
        endTool()
    }

    private fun deleteSelected() {
        val sel = canvas.selection
        if (sel == null) {
            Toast.makeText(requireContext(), "Toca un elemento para seleccionarlo y luego Borrar", Toast.LENGTH_SHORT).show()
            return
        }
        mutate { m ->
            when (sel.kind) {
                "node" -> m.copy(nodes = m.nodes.filterNot { it.id == sel.id }, links = m.links.filterNot { sel.id in listOf(it.source, it.target) })
                "link" -> m.copy(links = m.links.filterNot { it.id == sel.id })
                "text" -> m.copy(texts = m.texts.filterNot { it.id == sel.id })
                else -> m.copy(images = m.images.filterNot { it.id == sel.id })
            }
        }
        canvas.select(null)
        endTool()
    }

    // ---- menú de un equipo (mantener presionado) ------------------------------------------------------
    private fun showNodeMenu(node: MapNodeModel) {
        val context = requireContext()
        val map = current()
        val link = map.links.firstOrNull { it.source == node.id || it.target == node.id }
        val peer = link?.let { l -> map.nodes.firstOrNull { it.id == (if (l.source == node.id) l.target else l.source) } }
        val way = when (link?.style) { "dashed" -> "WiFi"; "dotted" -> "USB"; else -> "Cable" }
        val subtitle = MapNodeTypes.label(node.type) + (peer?.let { " · $way al ${it.label}" } ?: "")

        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(8))
        }
        box.addView(TextView(context).apply { text = node.label; textSize = 18f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#1F2937")) })
        box.addView(TextView(context).apply { text = subtitle; textSize = 12f; setTextColor(Color.parseColor("#6B7280")); setPadding(0, 0, 0, dp(8)) })
        lateinit var dialog: AlertDialog
        fun row(text: String, icon: Int, color: String, action: () -> Unit) = box.addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
            addView(ImageView(context).apply {
                setImageResource(icon); setColorFilter(Color.parseColor(color))
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(18) }
            })
            addView(TextView(context).apply { this.text = text; textSize = 15f; setTextColor(Color.parseColor(color)) })
            setOnClickListener { dialog.dismiss(); action() }
        })
        row("Renombrar", R.drawable.ic_edit, "#1F2937") { promptRename(node) }
        row("Cambiar tipo", R.drawable.ic_devices, "#1F2937") { chooseType(node) }
        row("Cambiar color", R.drawable.ic_palette, "#1F2937") { chooseColor(node) }
        row("Eliminar", R.drawable.ic_delete, "#C62828") {
            canvas.select(MapCanvasView.Selection("node", node.id))
            deleteSelected()
        }
        dialog = AlertDialog.Builder(context).setView(box).create()
        dialog.show()
        roundDialog(dialog)
    }

    /** Diálogo de tarjeta redondeada (28dp) con 35dp de margen lateral, como en el prototipo. */
    private fun roundDialog(dialog: AlertDialog) {
        dialog.window?.apply {
            setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(28).toFloat()
            })
            setLayout(resources.displayMetrics.widthPixels - dp(70), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun promptRename(node: MapNodeModel) {
        val input = EditText(requireContext()).apply { setText(node.label); setSelection(node.label.length) }
        AlertDialog.Builder(requireContext()).setTitle("Renombrar").setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val name = input.text?.toString().orEmpty().trim()
                if (name.isNotBlank()) mutate { m -> m.copy(nodes = m.nodes.map { if (it.id == node.id) it.copy(label = name) else it }) }
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun chooseType(node: MapNodeModel) {
        val types = MapNodeTypes.all
        AlertDialog.Builder(requireContext()).setTitle("Cambiar tipo")
            .setItems(types.map { it.second }.toTypedArray()) { _, i ->
                mutate { m -> m.copy(nodes = m.nodes.map { if (it.id == node.id) it.copy(type = types[i].first, color = null) else it }) }
            }.show()
    }

    private fun chooseColor(node: MapNodeModel) {
        val context = requireContext()
        val colors = listOf("#1565C0", "#2E7D32", "#6A1B9A", "#00838F", "#EF6C00", "#C62828", "#546E7A", "#263238")
        lateinit var dialog: AlertDialog
        val box = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(18))
        }
        box.addView(TextView(context).apply {
            text = "Cambiar color"; textSize = 18f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1F2937")); setPadding(0, 0, 0, dp(14))
        })
        colors.chunked(4).forEach { rowColors ->
            box.addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                rowColors.forEach { hex ->
                    addView(View(context).apply {
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor(hex))
                            if (node.color.equals(hex, ignoreCase = true)) setStroke(dp(3), Color.parseColor("#1F2937"))
                        }
                        layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply { setMargins(dp(8), dp(6), dp(8), dp(6)) }
                        setOnClickListener {
                            dialog.dismiss()
                            mutate { m -> m.copy(nodes = m.nodes.map { if (it.id == node.id) it.copy(color = hex) else it }) }
                        }
                    })
                }
            })
        }
        dialog = AlertDialog.Builder(context).setView(box).create()
        dialog.show()
        roundDialog(dialog)
    }

    // ---------------------------------------------------------------------------------------------
    // Caja de texto
    // ---------------------------------------------------------------------------------------------
    private fun addTextBox() {
        val box = MapTextModel(id = newId("t"), text = "Nueva nota", x = 0.5f, y = 0.55f)
        mutate { it.copy(texts = it.texts + box) }
        startTool(Tool.TEXT)
        canvas.select(MapCanvasView.Selection("text", box.id))
        showTextPanel(box.id)
    }

    private fun updateText(id: String, change: (MapTextModel) -> MapTextModel) {
        viewModel.setMap(current().copy(texts = current().texts.map { if (it.id == id) change(it) else it }))
    }

    private fun showTextPanel(id: String) {
        val context = requireContext()
        val initial = current().texts.first { it.id == id }
        val input = EditText(context).apply {
            setText(initial.text)
            setSelection(initial.text.length)
            background = rounded(Color.WHITE, 12f, blue)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) = Unit
                override fun afterTextChanged(s: Editable?) = updateText(id) { it.copy(text = s?.toString().orEmpty()) }
            })
        }
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, 0) }
        fun pill(text: String, onClick: () -> Unit) = TextView(context).apply {
            this.text = text; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(blue); gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 16f, blue)
            setPadding(dp(12), dp(6), dp(12), dp(6))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(8) }
        }
        row.addView(pill("A−") { updateText(id) { it.copy(size = MapCanvasView.clampSize(it.size, -1)) } })
        row.addView(pill("A+") { updateText(id) { it.copy(size = MapCanvasView.clampSize(it.size, 1)) } })
        listOf("#FFF4CC", "#E3F2FD", "#FDE7E7").forEach { hex ->
            row.addView(View(context).apply {
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(hex)); setStroke(dp(1), Color.parseColor("#C9CFDA")) }
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply { marginEnd = dp(8) }
                setOnClickListener { updateText(id) { it.copy(color = hex) } }
            })
        }
        row.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        row.addView(TextView(context).apply {
            text = "Listo"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = rounded(blue, 18f); setPadding(dp(22), dp(8), dp(22), dp(8))
            setOnClickListener { endTool() }
        })
        setPanel(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(10))
            addView(input); addView(row)
        })
    }

    // ---------------------------------------------------------------------------------------------
    // Imagen
    // ---------------------------------------------------------------------------------------------
    private fun photoEvidences(): List<ChatEvidenceItem> =
        viewModel.state.value.evidences.filter { it.status != "missing" && it.paths.isNotEmpty() }

    private fun pickImage(replacing: String? = null) {
        val items = photoEvidences()
        if (items.isEmpty()) {
            Toast.makeText(requireContext(), "Todavía no hay fotos de evidencias para agregar", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext()).setTitle("Evidencias de la visita")
            .setItems(items.map { "${it.title} · ${it.code}" }.toTypedArray()) { _, i ->
                val e = items[i]
                if (replacing != null) {
                    mutate { m -> m.copy(images = m.images.map { if (it.id == replacing) it.copy(evidenceCode = e.code, scope = e.scope, label = e.title) else it }) }
                } else {
                    val image = MapImageModel(id = newId("i"), evidenceCode = e.code, scope = e.scope, label = e.title, x = 0.8f, y = 0.18f)
                    mutate { it.copy(images = it.images + image) }
                    startTool(Tool.IMAGE)
                    canvas.select(MapCanvasView.Selection("image", image.id))
                    showImagePanel(image.id)
                }
            }.show()
    }

    private fun showImagePanel(id: String) {
        val context = requireContext()
        val info = TextView(context).apply {
            text = "Origen: Evidencias de la visita · toca aquí para reemplazar la foto"
            textSize = 12f
            setTextColor(Color.parseColor("#4B5563"))
            setOnClickListener { pickImage(replacing = id) }
        }
        val row = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(10), 0, 0) }
        val sizes = HashMap<String, TextView>()
        fun refreshSizes() {
            val now = current().images.firstOrNull { it.id == id }?.size ?: "M"
            sizes.forEach { (s, v) ->
                v.background = if (s == now) rounded(Color.parseColor("#E3EEFC"), 16f, blue) else rounded(Color.WHITE, 16f, Color.parseColor("#C9CFDA"))
                v.setTextColor(if (s == now) blue else Color.parseColor("#4B5563"))
            }
        }
        listOf("S", "M", "L").forEach { s ->
            val v = TextView(context).apply {
                text = s; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(8) }
                setOnClickListener {
                    viewModel.setMap(current().copy(images = current().images.map { if (it.id == id) it.copy(size = s) else it }))
                    refreshSizes()
                }
            }
            sizes[s] = v
            row.addView(v)
        }
        row.addView(TextView(context).apply {
            text = "Eliminar"; textSize = 12f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#C62828")); gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 16f, Color.parseColor("#E0A8A8")); setPadding(dp(14), dp(7), dp(14), dp(7))
            setOnClickListener { canvas.select(MapCanvasView.Selection("image", id)); deleteSelected() }
        })
        row.addView(View(context), LinearLayout.LayoutParams(0, 1, 1f))
        row.addView(TextView(context).apply {
            text = "Listo"; textSize = 14f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); gravity = Gravity.CENTER
            background = rounded(blue, 18f); setPadding(dp(22), dp(8), dp(22), dp(8))
            setOnClickListener { endTool() }
        })
        setPanel(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(10))
            addView(info); addView(row)
        })
        refreshSizes()
    }

    private fun setPanel(content: View) {
        panelHost.removeAllViews()
        panelHost.addView(content)
        panelHost.visibility = View.VISIBLE
    }

    private fun thumbFor(image: MapImageModel): Bitmap? {
        val path = viewModel.state.value.evidences
            .firstOrNull { it.code == image.evidenceCode && it.scope == image.scope }?.paths?.firstOrNull() ?: return null
        return thumbs.getOrPut(path) { MapCanvasView.decodeThumb(path) }
    }

    // ---------------------------------------------------------------------------------------------
    private fun confirmExit() {
        if (!viewModel.state.value.canUndo) {
            findNavController().navigateUp()
            return
        }
        AlertDialog.Builder(requireContext()).setTitle("¿Salir sin guardar?")
            .setMessage("Los cambios que hiciste en el mapa se perderán.")
            .setPositiveButton("Salir") { _, _ -> findNavController().navigateUp() }
            .setNegativeButton("Seguir editando", null).show()
    }

    private fun iconFor(type: String) = when (type) {
        "router" -> R.drawable.ic_router
        "switch" -> R.drawable.ic_server
        "access_point", "repeater" -> R.drawable.ic_wifi
        "computer", "pos" -> R.drawable.ic_monitor
        "printer" -> R.drawable.ic_document
        "camera" -> R.drawable.ic_camera
        else -> R.drawable.ic_devices
    }

    private fun rounded(color: Int, radiusDp: Float, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusDp * resources.displayMetrics.density
        if (stroke != null) setStroke(dp(1) + 1, stroke)
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
