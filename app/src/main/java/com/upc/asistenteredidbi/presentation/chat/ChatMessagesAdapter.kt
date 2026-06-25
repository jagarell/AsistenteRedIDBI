package com.upc.asistenteredidbi.presentation.chat

import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.upc.asistenteredidbi.R

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)

class ChatMessagesAdapter : ListAdapter<ChatMessage, ChatMessagesAdapter.VH>(
    object : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
            oldItem == newItem
    }
) {

    inner class VH(
        val container: LinearLayout,
        val bubble: TextView
    ) : RecyclerView.ViewHolder(container)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val context = parent.context

        val container = LinearLayout(context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 6.dp(context), 0, 6.dp(context))
        }

        val bubble = TextView(context).apply {
            maxWidth = 270.dp(context)
            setPadding(
                18.dp(context),
                14.dp(context),
                18.dp(context),
                14.dp(context)
            )
            textSize = 17f
            setLineSpacing(4f, 1.0f)
        }

        container.addView(bubble)
        return VH(container, bubble)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)

        holder.bubble.text = item.text

        if (item.isFromUser) {
            holder.container.gravity = Gravity.END
            holder.bubble.setBackgroundResource(R.drawable.bg_chat_user)
            holder.bubble.setTextColor(0xFFFFFFFF.toInt())
        } else {
            holder.container.gravity = Gravity.START
            holder.bubble.setBackgroundResource(R.drawable.bg_chat_bot)
            holder.bubble.setTextColor(0xFF374151.toInt())
        }
    }

    private fun Int.dp(context: android.content.Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}