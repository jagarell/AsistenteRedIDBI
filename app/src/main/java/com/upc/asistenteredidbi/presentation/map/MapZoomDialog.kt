package com.upc.asistenteredidbi.presentation.map

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.upc.asistenteredidbi.R
import com.upc.asistenteredidbi.domain.model.MapImageModel
import com.upc.asistenteredidbi.domain.model.NetworkMap

/**
 * Mapa a pantalla completa para verlo de cerca: pellizcar para acercar, arrastrar
 * para mover y "Ver todo" para volver al encuadre inicial. Se abre al tocar la
 * vista previa del mapa (tarjeta del chat y propuesta).
 */
object MapZoomDialog {

    fun show(
        context: Context,
        map: NetworkMap,
        imageProvider: (MapImageModel) -> Bitmap? = { null }
    ) {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val canvas = MapCanvasView(context).apply {
            mode = MapCanvasView.Mode.VIEW
            this.map = map
            this.imageProvider = imageProvider
        }

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }
        root.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1976D2"))
            setPadding(dp(8), dp(34), dp(16), dp(12))
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_close)
                setColorFilter(Color.WHITE)
                setPadding(dp(12), dp(12), dp(12), dp(12))
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                setOnClickListener { dialog.dismiss() }
            })
            addView(TextView(context).apply {
                text = "Topología de red"
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(context).apply {
                text = "Ver todo"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#1565C0"))
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 18f * density
                }
                setPadding(dp(14), dp(7), dp(14), dp(7))
                setOnClickListener { canvas.resetView() }
            })
        })
        root.addView(TextView(context).apply {
            text = "Pellizca para acercar o alejar · arrastra para mover"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#6B7280"))
            setBackgroundColor(Color.parseColor("#F5F7FB"))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        })
        root.addView(
            FrameLayout(context).apply {
                setBackgroundColor(Color.parseColor("#FAFAFA"))
                addView(canvas, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        dialog.setContentView(root)
        dialog.show()
    }
}
