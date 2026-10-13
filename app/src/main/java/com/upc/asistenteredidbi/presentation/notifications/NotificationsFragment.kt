package com.upc.asistenteredidbi.presentation.notifications

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.data.remote.NotificationApiService
import com.upc.asistenteredidbi.data.remote.dto.NotificationItemDto
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Bandeja de notificaciones: minutas por validar (supervisor), validadas y borradores por vencer (técnico). */
@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    @Inject
    lateinit var api: NotificationApiService

    private lateinit var list: LinearLayout
    private lateinit var empty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F5F7FB")) }
        root.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1976D2"))
            setPadding((16 * dp).toInt(), (40 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
            addView(TextView(ctx).apply {
                text = "←"; textSize = 26f; setTextColor(Color.WHITE); setPadding(0, 0, (16 * dp).toInt(), 0)
                setOnClickListener { findNavController().navigateUp() }
            })
            addView(TextView(ctx).apply { text = "Notificaciones"; textSize = 22f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) })
        })
        list = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (24 * dp).toInt()) }
        empty = TextView(ctx).apply {
            text = "No tienes notificaciones."; textSize = 15f; setTextColor(Color.parseColor("#6B7280")); gravity = Gravity.CENTER
            setPadding(0, (48 * dp).toInt(), 0, 0)
        }
        list.addView(empty)
        root.addView(ScrollView(ctx).apply { addView(list) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { api.listNotifications() }
                .onSuccess { response ->
                    render(response.items)
                    runCatching { api.markAllRead() }
                }
                .onFailure { Toast.makeText(requireContext(), "No se pudieron cargar las notificaciones", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun render(items: List<NotificationItemDto>) {
        list.removeAllViews()
        if (items.isEmpty()) { list.addView(empty); return }
        val ctx = requireContext()
        val dp = resources.displayMetrics.density
        items.forEach { n ->
            list.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply {
                    setColor(if (n.read) Color.WHITE else Color.parseColor("#EAF2FD")); cornerRadius = 16 * dp
                    setStroke((1 * dp).toInt(), Color.parseColor("#D6E4FA"))
                }
                setPadding((16 * dp).toInt(), (12 * dp).toInt(), (16 * dp).toInt(), (12 * dp).toInt())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = (10 * dp).toInt() }
                addView(TextView(ctx).apply { text = n.title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#1F2937")) })
                addView(TextView(ctx).apply { text = n.body; textSize = 14f; setTextColor(Color.parseColor("#374151")); setPadding(0, (4 * dp).toInt(), 0, 0) })
                n.createdAt?.take(16)?.replace('T', ' ')?.let {
                    addView(TextView(ctx).apply { text = it; textSize = 12f; setTextColor(Color.parseColor("#9CA3AF")); setPadding(0, (6 * dp).toInt(), 0, 0) })
                }
                n.evaluationId?.let { evaluationId ->
                    setOnClickListener {
                        findNavController().navigate(
                            R.id.minutaProposalFragment,
                            bundleOf("evaluationId" to evaluationId.toString(), "minutaId" to (n.minutaId ?: -1L))
                        )
                    }
                }
            })
        }
    }
}
