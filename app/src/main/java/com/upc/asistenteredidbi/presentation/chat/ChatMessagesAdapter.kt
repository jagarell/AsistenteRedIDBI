package com.upc.asistenteredidbi.presentation.chat

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.Outline
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.domain.model.ChatNodePrompt
import java.io.File

/** Un dato leído por IA (ej. "Bajada" -> "633.33 Mbps"). No se usa kotlin.Pair
 *  acá: ChatMessage (y por lo tanto este tipo) se serializa con Moshi en
 *  ChatProgressStore, y Moshi no trae un adapter para kotlin.Pair — sin esto
 *  falla el guardado de CUALQUIER mensaje del chat (crash "Platform class
 *  kotlin.Pair ... requires explicit JsonAdapter"). */
data class ChatResultField(val label: String, val value: String)

/** Fila de una lista (ej. un dispositivo del escáner de IP). `status`: "ok", "warn" o "". */
data class ChatListRow(val tag: String, val label: String, val status: String = "")

/** Miniatura del resumen de evidencias. `status`: "ok", "warn" o "missing". */
data class ChatThumb(val label: String, val path: String? = null, val status: String = "ok")

/**
 * Tarjeta del bot dentro del chat (todo con tipos simples para que Moshi la
 * guarde en el snapshot). `kind`:
 *  - EVIDENCE_PROMPT: pide una evidencia (título + texto + pista),
 *  - RESULT: "Esto leí en la captura" (tiles, filas, lista, avisos, nota),
 *  - ALERT: aviso del asistente,
 *  - SUMMARY: resumen de evidencias de la visita.
 */
data class ChatCard(
    val kind: String,
    val title: String,
    val badge: String? = null,
    val text: String? = null,
    val tiles: List<ChatResultField> = emptyList(),
    val rows: List<ChatResultField> = emptyList(),
    val list: List<ChatListRow> = emptyList(),
    val thumbs: List<ChatThumb> = emptyList(),
    val warnings: List<String> = emptyList(),
    val note: String? = null,
    val footer: String? = null,
    /** Botones dentro de la tarjeta (SUMMARY): cada uno notifica su `id` al fragment. */
    val actions: List<ChatAction> = emptyList()
)

/** Botón de una tarjeta. `style`: "primary" (relleno azul), "outline" (borde azul) o "warn" (borde ámbar). */
data class ChatAction(val label: String, val id: String, val style: String = "outline")

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    /** Id estable (índice de creación) para que el DiffUtil identifique cada
     * mensaje por sí mismo en vez de por igualdad de contenido — dos "Sí"
     * seguidos, por ejemplo, no deben confundirse entre sí. */
    val id: Long = 0L,
    /** Nodo que plantea este mensaje del bot: permite reabrirlo si el técnico
     * edita la respuesta que le siguió (el `state` previo está guardado en
     * archivo bajo este mismo id, ver ChatLocalFiles). */
    val prompt: ChatNodePrompt? = null,
    /** Puede editarse si es la respuesta a una pregunta reabrible (no aplica
     * a mensajes del bot ni al mensaje final de cierre). */
    val isEditable: Boolean = false,
    val card: ChatCard? = null,
    /** Fotos que subió el técnico (rutas locales ya comprimidas); `text` lleva el pie. */
    val imagePaths: List<String> = emptyList()
)

class ChatMessagesAdapter(
    private val onEditClick: (ChatMessage) -> Unit,
    private val onCardAction: (String) -> Unit = {}
) : ListAdapter<ChatMessage, ChatMessagesAdapter.VH>(
    object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
            oldItem == newItem
    }
) {

    inner class VH(
        val root: LinearLayout,
        val row: LinearLayout,
        val avatar: FrameLayout,
        val content: LinearLayout,
        val caption: TextView,
        val bubble: TextView,
        val photoBubble: LinearLayout,
        val card: LinearLayout,
        val editCaption: TextView
    ) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val context = parent.context

        val root = LinearLayout(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(0, 6.dp(context), 0, 6.dp(context))
        }

        val row = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val avatar = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(28.dp(context), 28.dp(context)).apply {
                marginEnd = 8.dp(context)
                topMargin = 2.dp(context)
            }
            setBackgroundResource(R.drawable.bg_home_icon_circle)
        }
        val avatarIcon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(16.dp(context), 16.dp(context)).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(R.drawable.ic_robot)
            setColorFilter(Color.WHITE)
        }
        avatar.addView(avatarIcon)

        val content = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        // Dónde va el técnico dentro de lo que se repite ("Caja 1 de 2").
        val caption = TextView(context).apply {
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#2F6FCB"))
            setPadding(6.dp(context), 0, 0, 3.dp(context))
            visibility = View.GONE
        }

        val bubble = TextView(context).apply {
            maxWidth = 250.dp(context)
            setPadding(16.dp(context), 11.dp(context), 16.dp(context), 11.dp(context))
            textSize = 15f
            setLineSpacing(3f, 1.0f)
        }

        // Foto subida por el técnico: burbuja azul con la miniatura y el pie "archivo ✓ Subida".
        val photoBubble = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#2F6FCB"), 18f, context)
            setPadding(6.dp(context), 6.dp(context), 6.dp(context), 6.dp(context))
            visibility = View.GONE
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_chat_bot)
            setPadding(16.dp(context), 14.dp(context), 16.dp(context), 14.dp(context))
            visibility = View.GONE
        }

        content.addView(caption)
        content.addView(bubble)
        content.addView(photoBubble)
        content.addView(card)
        row.addView(avatar)
        row.addView(content)
        root.addView(row)

        val editCaption = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4.dp(context)
            }
            text = "✎ Editar respuesta"
            textSize = 13f
            setTextColor(0xFF1565C0.toInt())
            setPadding(4.dp(context), 2.dp(context), 4.dp(context), 2.dp(context))
        }
        root.addView(editCaption)

        return VH(root, row, avatar, content, caption, bubble, photoBubble, card, editCaption)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val context = holder.root.context
        val hasPhotos = item.imagePaths.isNotEmpty()
        val hasCard = item.card != null

        val contextLabel = item.prompt?.context.orEmpty()
        holder.caption.text = contextLabel.uppercase()
        holder.caption.visibility =
            if (!item.isFromUser && contextLabel.isNotBlank()) View.VISIBLE else View.GONE

        holder.bubble.text = item.text
        // Un mensaje "solo tarjeta" o "solo foto" no lleva burbuja de texto.
        holder.bubble.visibility =
            if (item.text.isBlank() || hasPhotos || hasCard) View.GONE else View.VISIBLE

        if (item.isFromUser) {
            holder.row.gravity = Gravity.END
            holder.avatar.visibility = View.GONE
            holder.bubble.setBackgroundResource(R.drawable.bg_chat_user)
            holder.bubble.setTextColor(0xFFFFFFFF.toInt())

            holder.editCaption.visibility = if (item.isEditable) View.VISIBLE else View.GONE
            (holder.editCaption.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
            holder.editCaption.setOnClickListener { onEditClick(item) }
        } else {
            holder.row.gravity = Gravity.START
            holder.avatar.visibility = View.VISIBLE
            holder.bubble.setBackgroundResource(R.drawable.bg_chat_bot)
            holder.bubble.setTextColor(0xFF374151.toInt())
            holder.editCaption.visibility = View.GONE
        }

        bindPhotoBubble(holder.photoBubble, item, context)
        bindCard(holder.card, item.card, context)
    }

    // ---- foto subida -------------------------------------------------------
    private fun bindPhotoBubble(box: LinearLayout, item: ChatMessage, context: Context) {
        box.removeAllViews()
        if (item.imagePaths.isEmpty()) {
            box.visibility = View.GONE
            return
        }
        box.visibility = View.VISIBLE
        item.imagePaths.take(3).forEach { path ->
            val image = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(220.dp(context), 120.dp(context)).apply {
                    bottomMargin = 4.dp(context)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipRounded(this, 12f)
            }
            Glide.with(context).load(File(path)).centerCrop().into(image)
            box.addView(image)
        }
        box.addView(TextView(context).apply {
            text = "${item.text}   ✓ Subida"
            textSize = 12f
            setTextColor(Color.WHITE)
            gravity = Gravity.END
            setPadding(4.dp(context), 0, 4.dp(context), 2.dp(context))
        })
    }

    // ---- tarjetas del bot ------------------------------------------------------
    private fun bindCard(card: LinearLayout, data: ChatCard?, context: Context) {
        card.removeAllViews()
        if (data == null) {
            card.visibility = View.GONE
            return
        }
        card.visibility = View.VISIBLE
        // Ancho máximo: lo que queda al lado del avatar (pantallas angostas incluidas).
        val available = context.resources.displayMetrics.widthPixels - 80.dp(context)
        card.maxWidth(minOf(280.dp(context), available))

        when (data.kind) {
            "ALERT" -> {
                card.background = rounded(Color.parseColor("#FFF4E5"), 16f, context, Color.parseColor("#F5C26B"))
                card.addView(title(context, "⚠ ${data.title}", Color.parseColor("#B45309")))
                data.text?.let { card.addView(body(context, it, Color.parseColor("#7A4A0A"))) }
                return
            }
            "EVIDENCE_PROMPT" -> {
                card.setBackgroundResource(R.drawable.bg_chat_bot)
                card.addView(title(context, data.title, Color.parseColor("#1F2937")))
                data.text?.let { card.addView(body(context, it, Color.parseColor("#374151"))) }
                data.note?.let { card.addView(small(context, it, Color.parseColor("#6B7280"), top = 6)) }
                return
            }
        }

        card.setBackgroundResource(R.drawable.bg_chat_bot)
        card.addView(headerRow(context, data.title, data.badge, data.kind == "SUMMARY"))

        if (data.tiles.isNotEmpty()) card.addView(tiles(context, data.tiles))
        data.rows.forEach { card.addView(keyValueRow(context, it)) }
        data.list.forEach { card.addView(listRow(context, it)) }
        if (data.thumbs.isNotEmpty()) card.addView(thumbGrid(context, data.thumbs))

        data.warnings.forEach { card.addView(box(context, "⚠ $it", "#FFF4E5", "#B45309")) }
        data.note?.let { card.addView(box(context, it, "#E8F1FD", "#1F4E9E")) }
        data.footer?.let { card.addView(small(context, it, Color.parseColor("#6B7280"), top = 6)) }

        if (data.actions.isNotEmpty()) {
            card.addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 10.dp(context) }
                data.actions.forEachIndexed { index, action ->
                    val color = Color.parseColor(if (action.style == "warn") "#B45309" else "#2F6FCB")
                    val filled = action.style == "primary"
                    addView(TextView(context).apply {
                        text = action.label
                        textSize = 13f
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(if (filled) Color.WHITE else color)
                        gravity = Gravity.CENTER
                        background = if (filled) rounded(color, 20f, context)
                        else rounded(Color.WHITE, 20f, context, color)
                        layoutParams = LinearLayout.LayoutParams(0, 40.dp(context), 1f).apply {
                            if (index > 0) marginStart = 8.dp(context)
                        }
                        setOnClickListener { onCardAction(action.id) }
                    })
                }
            })
        }
    }

    private fun headerRow(context: Context, title: String, badge: String?, warnBadge: Boolean): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 8.dp(context))
            addView(TextView(context).apply {
                text = title
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F2937"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (badge != null) {
                addView(TextView(context).apply {
                    text = badge
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor(if (warnBadge) "#B45309" else "#1565C0"))
                    background = rounded(
                        Color.parseColor(if (warnBadge) "#FFF1DC" else "#E3F0FF"), 12f, context
                    )
                    setPadding(8.dp(context), 3.dp(context), 8.dp(context), 3.dp(context))
                })
            }
        }

    private fun tiles(context: Context, fields: List<ChatResultField>): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 6.dp(context) }
            fields.forEachIndexed { index, field ->
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    background = rounded(Color.parseColor("#E8F1FD"), 10f, context)
                    setPadding(4.dp(context), 8.dp(context), 4.dp(context), 8.dp(context))
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (index > 0) marginStart = 6.dp(context)
                    }
                    addView(TextView(context).apply {
                        text = field.value
                        textSize = 17f
                        typeface = Typeface.DEFAULT_BOLD
                        gravity = Gravity.CENTER
                        setTextColor(Color.parseColor("#1F5FBF"))
                    })
                    addView(TextView(context).apply {
                        text = field.label
                        textSize = 11f
                        gravity = Gravity.CENTER
                        setTextColor(Color.parseColor("#6B7280"))
                    })
                })
            }
        }

    private fun keyValueRow(context: Context, field: ChatResultField): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 2.dp(context), 0, 2.dp(context))
            addView(TextView(context).apply {
                text = field.label
                textSize = 12f
                setTextColor(Color.parseColor("#6B7280"))
                layoutParams = LinearLayout.LayoutParams(78.dp(context), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(context).apply {
                text = field.value
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1F5FBF"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }

    private fun listRow(context: Context, row: ChatListRow): View =
        LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 3.dp(context), 0, 3.dp(context))
            addView(TextView(context).apply {
                text = row.tag
                textSize = 12f
                setTextColor(Color.parseColor("#6B7280"))
                layoutParams = LinearLayout.LayoutParams(40.dp(context), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            addView(TextView(context).apply {
                text = row.label
                textSize = 13f
                setTextColor(Color.parseColor("#1F2937"))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            if (row.status.isNotEmpty()) {
                addView(TextView(context).apply {
                    text = if (row.status == "ok") "✓" else "⚑"
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.parseColor(if (row.status == "ok") "#2E7D32" else "#E65100"))
                })
            }
        }

    private fun thumbGrid(context: Context, thumbs: List<ChatThumb>): View {
        val grid = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 6.dp(context) }
        }
        thumbs.chunked(3).forEach { rowThumbs ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 6.dp(context) }
            }
            rowThumbs.forEachIndexed { index, thumb ->
                row.addView(thumbCell(context, thumb).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 82.dp(context), 1f).apply {
                        if (index > 0) marginStart = 6.dp(context)
                    }
                })
            }
            // Relleno para que la última fila no estire sus celdas.
            repeat(3 - rowThumbs.size) {
                row.addView(View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 82.dp(context), 1f).apply { marginStart = 6.dp(context) }
                })
            }
            grid.addView(row)
        }
        return grid
    }

    private fun thumbCell(context: Context, thumb: ChatThumb): View {
        val missing = thumb.status == "missing"
        val cell = FrameLayout(context).apply {
            background = if (missing) {
                dashed(Color.parseColor("#FFF7EC"), Color.parseColor("#F0A63B"), 12f, context)
            } else {
                rounded(Color.parseColor("#263238"), 12f, context)
            }
            clipRounded(this, 12f)
        }
        if (!missing && thumb.path != null) {
            val image = ImageView(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            Glide.with(context).load(File(thumb.path)).centerCrop().into(image)
            cell.addView(image)
        }
        cell.addView(TextView(context).apply {
            text = if (missing) "+\n${thumb.label}\nFalta" else thumb.label
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (missing) Color.parseColor("#B45309") else Color.WHITE)
            gravity = if (missing) Gravity.CENTER else Gravity.START
            if (!missing) setShadowLayer(4f, 0f, 1f, Color.BLACK)
            setPadding(6.dp(context), 0, 6.dp(context), 4.dp(context))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                if (missing) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
                if (missing) Gravity.CENTER else Gravity.BOTTOM
            )
        })
        if (!missing) {
            cell.addView(TextView(context).apply {
                text = if (thumb.status == "ok") "✓" else "!"
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(if (thumb.status == "ok") "#2E9E57" else "#F0A63B"))
                }
                layoutParams = FrameLayout.LayoutParams(16.dp(context), 16.dp(context), Gravity.TOP or Gravity.END).apply {
                    topMargin = 4.dp(context)
                    marginEnd = 4.dp(context)
                }
            })
        }
        return cell
    }

    // ---- helpers de texto/cajas --------------------------------------------------
    private fun title(context: Context, text: String, color: Int) = TextView(context).apply {
        this.text = text
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color)
    }

    private fun body(context: Context, text: String, color: Int) = TextView(context).apply {
        this.text = text
        textSize = 15f
        setTextColor(color)
        setLineSpacing(3f, 1f)
    }

    private fun small(context: Context, text: String, color: Int, top: Int = 0) = TextView(context).apply {
        this.text = text
        textSize = 12f
        setTextColor(color)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = top.dp(context) }
    }

    private fun box(context: Context, text: String, bg: String, fg: String) = TextView(context).apply {
        this.text = text
        textSize = 12f
        setTextColor(Color.parseColor(fg))
        setLineSpacing(2f, 1f)
        background = rounded(Color.parseColor(bg), 10f, context)
        setPadding(10.dp(context), 8.dp(context), 10.dp(context), 8.dp(context))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8.dp(context) }
    }

    private fun LinearLayout.maxWidth(px: Int) {
        // LinearLayout no tiene maxWidth: se limita el ancho del contenedor fijándolo si hace falta.
        layoutParams = (layoutParams ?: LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )).also { it.width = px }
    }

    private fun rounded(color: Int, radiusDp: Float, context: Context, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp * context.resources.displayMetrics.density
            if (stroke != null) setStroke((1 * context.resources.displayMetrics.density).toInt(), stroke)
        }

    private fun dashed(color: Int, stroke: Int, radiusDp: Float, context: Context): GradientDrawable {
        val d = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radiusDp * d
            setStroke((1.5f * d).toInt(), stroke, 6 * d, 4 * d)
        }
    }

    private fun clipRounded(view: View, radiusDp: Float) {
        val radius = radiusDp * view.resources.displayMetrics.density
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, radius)
            }
        }
        view.clipToOutline = true
    }

    private fun Int.dp(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}
