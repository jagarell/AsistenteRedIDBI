package com.upc.asistenteredidbi.presentation.evidence

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView que se mide a sí mismo con un alto prácticamente ilimitado,
 * sin importar el heightMeasureSpec que le pase el padre.
 *
 * rvEvidence vive dentro del ScrollView de toda la pantalla de Evidencias
 * con layout_height="wrap_content" para que la pantalla completa scrollee
 * como un solo bloque. En ese contexto, GridLayoutManager a veces recibe
 * un heightMeasureSpec acotado (AT_MOST, no UNSPECIFIED) del ScrollView y
 * solo mide/dibuja la primera fila — el resto de los ítems (equipos del
 * checklist) quedan en el adapter pero nunca se les llama
 * onCreateViewHolder, así que no aparecen en pantalla aunque el contador de
 * fotos ya los cuente. Forzar la medición acá evita depender de que el
 * padre le pase el spec correcto.
 */
class FullHeightRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val expandedHeightSpec = MeasureSpec.makeMeasureSpec(
            Int.MAX_VALUE shr 2,
            MeasureSpec.AT_MOST
        )
        super.onMeasure(widthSpec, expandedHeightSpec)
    }
}
