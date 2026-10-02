package com.upc.asistenteredidbi.presentation.chat

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.domain.model.TechnicalChatInputType

/** Un campo leído por IA de la captura (ej. "Bajada" -> "633.33 Mbps"). No
 *  se usa kotlin.Pair acá: ChatMessage (y por lo tanto este tipo) se
 *  serializa con Moshi en ChatProgressStore, y Moshi no trae un adapter
 *  para kotlin.Pair — sin esto, falla el guardado de CUALQUIER mensaje del
 *  chat, no solo los que tienen foto (ver crash: "Platform class kotlin.Pair
 *  ... requires explicit JsonAdapter"). */
data class ChatResultField(val label: String, val value: String)

/** Tarjeta "Esto leí en la captura" que sigue a una respuesta tipo PHOTO
 *  (ej. Mbps/ping/ISP leídos de un speedtest) — ver ChatMessagesAdapter. */
data class ChatPhotoResult(
    val fields: List<ChatResultField>,
    val warning: String? = null
)

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    /** Id estable (índice de creación) para que el DiffUtil identifique cada
     * mensaje por sí mismo en vez de por igualdad de contenido — dos "Sí"
     * seguidos, por ejemplo, no deben confundirse entre sí. */
    val id: Long = 0L,
    /** Metadata de la pregunta (solo para mensajes del bot): permite reabrir
     * ese paso si el técnico edita la respuesta que le siguió. */
    val stepIndex: Int? = null,
    val inputType: TechnicalChatInputType? = null,
    val options: List<String> = emptyList(),
    val unit: String? = null,
    val answersSnapshot: Map<String, String> = emptyMap(),
    /** Puede editarse si es la respuesta a una pregunta reabrible (no aplica
     * a mensajes del bot ni al mensaje final de cierre). */
    val isEditable: Boolean = false,
    /** Si viene seteado, este mensaje del bot se renderiza como una tarjeta
     *  de campos leídos por IA en vez de (o además de) texto plano. */
    val photoResult: ChatPhotoResult? = null
)

class ChatMessagesAdapter(
    private val onEditClick: (ChatMessage) -> Unit
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
        val bubble: TextView,
        val editCaption: TextView,
        val photoCard: LinearLayout
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
            layoutParams = LinearLayout.LayoutParams(32.dp(context), 32.dp(context)).apply {
                marginEnd = 8.dp(context)
                topMargin = 2.dp(context)
            }
            setBackgroundResource(R.drawable.bg_home_icon_circle)
        }
        val avatarIcon = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(18.dp(context), 18.dp(context)).apply {
                gravity = Gravity.CENTER
            }
            setImageResource(R.drawable.ic_robot)
            setColorFilter(Color.WHITE)
        }
        avatar.addView(avatarIcon)

        val bubble = TextView(context).apply {
            maxWidth = 260.dp(context)
            setPadding(18.dp(context), 14.dp(context), 18.dp(context), 14.dp(context))
            textSize = 17f
            setLineSpacing(4f, 1.0f)
        }

        row.addView(avatar)
        row.addView(bubble)
        root.addView(row)

        // Tarjeta "Esto leí en la captura" — se arma/limpia en onBindViewHolder
        // según item.photoResult; alineada bajo la burbuja (deja el ancho del
        // avatar libre), oculta por defecto.
        val photoCard = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 40.dp(context)
                topMargin = 6.dp(context)
            }
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_chat_bot)
            setPadding(16.dp(context), 12.dp(context), 16.dp(context), 12.dp(context))
            visibility = ViewGroup.GONE
        }
        root.addView(photoCard)

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

        return VH(root, row, avatar, bubble, editCaption, photoCard)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.bubble.text = item.text
        // Un mensaje "solo tarjeta" (ej. resultado de foto) no lleva texto —
        // sin esto quedaba una burbuja blanca vacía arriba de la tarjeta.
        holder.bubble.visibility =
            if (item.text.isBlank() && item.photoResult != null) ViewGroup.GONE else ViewGroup.VISIBLE

        if (item.isFromUser) {
            holder.row.gravity = Gravity.END
            holder.avatar.visibility = ViewGroup.GONE
            holder.bubble.setBackgroundResource(R.drawable.bg_chat_user)
            holder.bubble.setTextColor(0xFFFFFFFF.toInt())

            holder.editCaption.visibility = if (item.isEditable) ViewGroup.VISIBLE else ViewGroup.GONE
            (holder.editCaption.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
            holder.editCaption.setOnClickListener { onEditClick(item) }
        } else {
            holder.row.gravity = Gravity.START
            holder.avatar.visibility = ViewGroup.VISIBLE
            holder.bubble.setBackgroundResource(R.drawable.bg_chat_bot)
            holder.bubble.setTextColor(0xFF374151.toInt())
            holder.editCaption.visibility = ViewGroup.GONE
        }

        bindPhotoCard(holder.photoCard, item.photoResult)
    }

    private fun bindPhotoCard(card: LinearLayout, result: ChatPhotoResult?) {
        card.removeAllViews()
        if (result == null) {
            card.visibility = ViewGroup.GONE
            return
        }
        card.visibility = ViewGroup.VISIBLE
        val context = card.context

        card.addView(TextView(context).apply {
            text = "📊 Esto leí en la captura"
            textSize = 14f
            setTextColor(0xFF1F2937.toInt())
            setPadding(0, 0, 0, 6.dp(context))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })

        result.fields.forEach { (label, value) ->
            card.addView(TextView(context).apply {
                text = "$label: $value"
                textSize = 14f
                setTextColor(0xFF374151.toInt())
                setPadding(0, 2.dp(context), 0, 2.dp(context))
            })
        }

        if (!result.warning.isNullOrBlank()) {
            card.addView(TextView(context).apply {
                text = "⚠️ ${result.warning}"
                textSize = 13f
                setTextColor(0xFF8A6D00.toInt())
                setBackgroundColor(0xFFFFF4CC.toInt())
                setPadding(10.dp(context), 8.dp(context), 10.dp(context), 8.dp(context))
                setLineSpacing(3f, 1.0f)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 8.dp(context) }
            })
        }
    }

    private fun Int.dp(context: android.content.Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}
