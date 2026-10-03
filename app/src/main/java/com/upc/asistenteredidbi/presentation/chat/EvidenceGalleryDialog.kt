package com.upc.asistenteredidbi.presentation.chat

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.domain.model.TechnicalChatInputType
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

/**
 * Galería de evidencias de la visita (pantalla "Evidencias · se adjuntan a la
 * minuta" del prototipo): lista agrupada por sección, con filtros "Todas / Con
 * datos IA / Por resolver" y el resumen de lo que leyó la IA de cada foto.
 */
@AndroidEntryPoint
class EvidenceGalleryDialog : DialogFragment() {

    private val viewModel: TechnicalChatViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private enum class Filter { ALL, AI, UNRESOLVED }

    private var filter = Filter.ALL

    companion object {
        private const val ARG_UNRESOLVED = "unresolvedOnly"

        fun newInstance(unresolvedOnly: Boolean) = EvidenceGalleryDialog().apply {
            arguments = Bundle().apply { putBoolean(ARG_UNRESOLVED, unresolvedOnly) }
        }
    }
    private lateinit var listContainer: LinearLayout
    private lateinit var filterRow: LinearLayout

    override fun getTheme(): Int = android.R.style.Theme_Material_Light_NoActionBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        if (arguments?.getBoolean(ARG_UNRESOLVED) == true) filter = Filter.UNRESOLVED
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F7FB"))
        }

        root.addView(header(context))

        filterRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(4))
        }
        root.addView(filterRow)

        listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(16))
        }
        root.addView(
            ScrollView(context).apply { addView(listContainer) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        val atSummary = viewModel.uiState.value.node?.inputType == TechnicalChatInputType.SUMMARY
        root.addView(TextView(context).apply {
            text = if (atSummary) "Listo, generar diagnóstico" else "Listo"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded("#1565C0", 28f, context)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
                setMargins(dp(16), dp(8), dp(16), dp(20))
            }
            setOnClickListener {
                dismiss()
                if (atSummary) viewModel.sendAnswer("GENERAR_MINUTA", "Continuar al diagnóstico")
            }
        })

        render()
        return root
    }

    private fun header(context: android.content.Context): View = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Color.parseColor("#1976D2"))
        setPadding(dp(8), dp(34), dp(16), dp(14))
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_arrow_back)
            setColorFilter(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            setOnClickListener { dismiss() }
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = "Evidencias"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            })
            addView(TextView(context).apply {
                val client = viewModel.uiState.value.messages.firstOrNull { it.isFromUser }?.text.orEmpty()
                text = (if (client.isNotBlank()) "$client · " else "") + "se adjuntan a la minuta"
                textSize = 13f
                setTextColor(Color.parseColor("#D9EAFE"))
            })
        })
    }

    private fun render() {
        val context = requireContext()
        val all = viewModel.uiState.value.evidences
        val withAi = all.filter { it.status != "missing" && it.summary.isNotBlank() }
        val unresolved = all.filter { it.status != "ok" }

        filterRow.removeAllViews()
        listOf(
            Triple(Filter.ALL, "Todas ${all.size}", "#1565C0"),
            Triple(Filter.AI, "Con datos IA ${withAi.size}", "#1565C0"),
            Triple(Filter.UNRESOLVED, "Por resolver ${unresolved.size}", "#B45309")
        ).forEach { (f, label, color) ->
            filterRow.addView(TextView(context).apply {
                text = label
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                val selected = filter == f
                setTextColor(if (selected) Color.WHITE else Color.parseColor(color))
                background = if (selected) rounded(color, 18f, context)
                else rounded("#FFFFFF", 18f, context, Color.parseColor(color))
                setPadding(dp(14), dp(7), dp(14), dp(7))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }
                setOnClickListener {
                    filter = f
                    render()
                }
            })
        }

        val shown = when (filter) {
            Filter.ALL -> all
            Filter.AI -> withAi
            Filter.UNRESOLVED -> unresolved
        }

        listContainer.removeAllViews()
        if (shown.isEmpty()) {
            listContainer.addView(TextView(context).apply {
                text = "Todavía no hay evidencias en esta categoría."
                setTextColor(Color.parseColor("#6B7280"))
                setPadding(0, dp(24), 0, 0)
            })
            return
        }
        shown.groupBy { it.section }.forEach { (section, items) ->
            listContainer.addView(TextView(context).apply {
                text = section.uppercase()
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#6B7280"))
                setPadding(dp(2), dp(14), 0, dp(6))
            })
            items.forEach { listContainer.addView(item(context, it)) }
        }
    }

    private fun item(context: android.content.Context, e: ChatEvidenceItem): View {
        val missing = e.status == "missing"
        val warn = e.status == "warn" || missing
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = if (warn) rounded("#FFF8EA", 14f, context, Color.parseColor("#F0C675"))
            else rounded("#FFFFFF", 14f, context)
            setPadding(dp(10), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }

            val thumb = FrameLayout(context).apply {
                background = if (missing) {
                    GradientDrawable().apply {
                        setColor(Color.parseColor("#FFF7EC"))
                        cornerRadius = 10f * context.resources.displayMetrics.density
                        setStroke(dp(1) + 1, Color.parseColor("#F0A63B"), dp(5).toFloat(), dp(3).toFloat())
                    }
                } else rounded("#263238", 10f, context)
                layoutParams = LinearLayout.LayoutParams(dp(56), dp(56)).apply { marginEnd = dp(12) }
                clipToOutline = true
            }
            if (missing) {
                thumb.addView(TextView(context).apply {
                    text = "+"
                    textSize = 22f
                    setTextColor(Color.parseColor("#B45309"))
                    gravity = Gravity.CENTER
                }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            e.paths.firstOrNull()?.let { path ->
                val image = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
                Glide.with(context).load(File(path)).centerCrop().into(image)
                thumb.addView(image, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            addView(thumb)

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(context).apply {
                    text = e.title
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#1F2937"))
                })
                addView(TextView(context).apply {
                    text = if (missing) "Falta esta evidencia: la minuta la marca como pendiente."
                    else if (e.summary.isNotBlank()) highlighted("✦ ${e.summary}") else "Foto adjunta"
                    textSize = 12f
                    setTextColor(Color.parseColor(if (missing) "#B45309" else "#4B5563"))
                })
                addView(TextView(context).apply {
                    text = "${e.code} · ${e.section}"
                    textSize = 11f
                    setTextColor(Color.parseColor("#9CA3AF"))
                })
            })

            if (warn) {
                addView(TextView(context).apply {
                    text = "Repetir"
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor("#B45309"))
                    gravity = Gravity.CENTER
                    background = rounded("#FFFFFF", 16f, context, Color.parseColor("#E0A040"))
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply { marginStart = dp(8) }
                    setOnClickListener { confirmRetake(e) }
                })
            }
        }
    }

    /** Los datos entre * van en azul y negrita; el resto, en gris. */
    private fun highlighted(text: String): CharSequence {
        val builder = android.text.SpannableStringBuilder()
        text.split("*").forEachIndexed { index, part ->
            if (part.isEmpty()) return@forEachIndexed
            val start = builder.length
            builder.append(part)
            if (index % 2 == 1) {
                builder.setSpan(android.text.style.ForegroundColorSpan(Color.parseColor("#1565C0")), start, builder.length, 0)
                builder.setSpan(android.text.style.StyleSpan(Typeface.BOLD), start, builder.length, 0)
            }
        }
        return builder
    }

    private fun confirmRetake(e: ChatEvidenceItem) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Repetir ${e.title}")
            .setMessage("Vuelves a esa pregunta para subir la evidencia. Las respuestas que diste después se descartan.")
            .setPositiveButton("Repetir") { _, _ ->
                viewModel.retakeEvidence(e.messageId)
                dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    private fun rounded(color: String, radiusDp: Float, context: android.content.Context, stroke: Int? = null) =
        GradientDrawable().apply {
            setColor(Color.parseColor(color))
            cornerRadius = radiusDp * context.resources.displayMetrics.density
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
