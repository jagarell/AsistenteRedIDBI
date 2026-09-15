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
    val answersSnapshot: Map<String, String> = emptyMap(),
    /** Puede editarse si es la respuesta a una pregunta reabrible (no aplica
     * a mensajes del bot ni al mensaje final de cierre). */
    val isEditable: Boolean = false
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

        return VH(root, row, avatar, bubble, editCaption)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.bubble.text = item.text

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
    }

    private fun Int.dp(context: android.content.Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}
