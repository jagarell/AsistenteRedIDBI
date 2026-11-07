package com.upc.asistenteredidbi.presentation.gestion

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.upc.asistenteredidbi.data.remote.Api007Service
import com.upc.asistenteredidbi.presentation.reportes.Ui007
import com.upc.asistenteredidbi.presentation.reportes.Ui007.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Equipo y accesos › Roles y permisos (QA G9): qué puede hacer cada rol. Los permisos se guardan en la cuenta de la empresa y valen igual
 * en la web. Las casillas bloqueadas de cada rol base no se pueden quitar (el servidor también lo exige).
 */
object RolesPermisos {

    private val GRUPOS = listOf(
        "Minutas" to listOf("m_own" to "Ver sus propias minutas", "m_drafts" to "Ver borradores", "m_all" to "Ver todas las minutas (General)", "m_validated" to "Ver las validadas"),
        "Acciones" to listOf("a_create" to "Crear minutas", "a_validate" to "Validar", "a_reject" to "Devolver a revisión", "a_annul" to "Anular", "a_map" to "Editar el mapa de red", "a_codes" to "Entregar códigos de contraseña"),
        "Módulos" to listOf("mod_panel" to "Panel", "mod_notif" to "Notificaciones", "mod_rep" to "Reportes", "mod_equipo" to "Equipo y accesos", "mod_preg" to "Preguntas y reglas", "mod_plan" to "Plan"),
        "Reportes por técnico" to listOf("r_tec_all" to "Por técnico (todos)", "r_tec_mine" to "Mis técnicos (los asignados)"),
        "Avisos" to listOf("n_own" to "Avisos de sus minutas", "n_team" to "Avisos de todo el equipo")
    )
    private val BLOQUEADAS = mapOf(
        "sup" to setOf("a_validate", "a_reject", "n_own", "mod_notif"),
        "tec" to setOf("m_own", "a_create", "n_own")
    )
    private val EXCLUSIVOS = mapOf("r_tec_all" to "r_tec_mine", "r_tec_mine" to "r_tec_all")

    fun pintar(ctx: Context, cont: LinearLayout, scope: CoroutineScope, api: Api007Service, recargar: () -> Unit) {
        cont.removeAllViews()
        cont.addView(Ui007.text(ctx, "Cargando roles…", 14f, Ui007.GREY))
        scope.launch {
            val roles = runCatching { api.roles() }.getOrNull()
            cont.removeAllViews()
            if (roles == null) { cont.addView(Ui007.text(ctx, "No se pudieron cargar los roles.", 14f, Ui007.GREY)); return@launch }
            cont.addView(Ui007.text(ctx, "Define qué puede hacer cada rol. Los cambios valen en la app y en la web.", 12.5f, Ui007.GREY).apply { setPadding(0, dp(ctx, 8), 0, 0) })
            roles.forEach { r -> cont.addView(tarjeta(ctx, scope, api, r)) }
            cont.addView(Ui007.button(ctx, "+ Crear un rol", false) { nuevo(ctx, scope, api, recargar) })
        }
    }

    private fun tarjeta(ctx: Context, scope: CoroutineScope, api: Api007Service, r: Map<String, Any?>): LinearLayout {
        val id = r["id"].toString()
        val actuales = r["perms"].toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        val fijo = id == "admin"
        val bloq = BLOQUEADAS[id].orEmpty()
        val c = Ui007.card(ctx)
        c.addView(Ui007.text(ctx, r["name"].toString(), 16f, Ui007.INK, true))
        (r["description"] as? String)?.takeIf { it.isNotBlank() }?.let { c.addView(Ui007.text(ctx, it, 12.5f, Ui007.GREY).apply { setPadding(0, dp(ctx, 2), 0, dp(ctx, 4)) }) }
        if (fijo) c.addView(Ui007.text(ctx, "El administrador siempre tiene todos los permisos.", 12f, Ui007.BLUE))
        val interruptores = mutableMapOf<String, SwitchCompat>()
        var interno = false
        GRUPOS.forEach { (grupo, items) ->
            c.addView(Ui007.text(ctx, grupo.uppercase(), 11f, Ui007.GREY, true).apply { setPadding(0, dp(ctx, 10), 0, dp(ctx, 2)) })
            items.forEach { (clave, texto) ->
                val sw = SwitchCompat(ctx).apply { isChecked = clave in actuales; isEnabled = !fijo && clave !in bloq }
                interruptores[clave] = sw
                val fila = Ui007.row(ctx).apply { setPadding(0, dp(ctx, 4), 0, dp(ctx, 4)) }
                fila.addView(Ui007.text(ctx, texto, 13.5f, Ui007.INK), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                fila.addView(sw)
                c.addView(fila)
                sw.setOnCheckedChangeListener { _, on ->
                    if (interno) return@setOnCheckedChangeListener
                    if (on) { actuales.add(clave); EXCLUSIVOS[clave]?.let { otro -> actuales.remove(otro); interno = true; interruptores[otro]?.isChecked = false; interno = false } }
                    else actuales.remove(clave)
                    scope.launch {
                        runCatching { api.rolActualizar(id, mapOf("name" to r["name"], "perms" to actuales.toList())) }
                            .onFailure { Toast.makeText(ctx, "No se pudo guardar el cambio.", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
        }
        return c
    }

    private fun nuevo(ctx: Context, scope: CoroutineScope, api: Api007Service, recargar: () -> Unit) {
        val et = EditText(ctx).apply { hint = "Nombre del rol (por ejemplo, Solo consulta)"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES }
        val caja = LinearLayout(ctx).apply { setPadding(dp(ctx, 20), dp(ctx, 8), dp(ctx, 20), 0); addView(et, LinearLayout.LayoutParams(-1, -2)) }
        AlertDialog.Builder(ctx).setTitle("Crear un rol").setView(caja)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = et.text.toString().trim()
                if (nombre.isEmpty()) return@setPositiveButton
                scope.launch {
                    runCatching { api.rolCrear(mapOf("name" to nombre, "perms" to listOf("m_own"))) }
                        .onSuccess { recargar() }
                        .onFailure { Toast.makeText(ctx, "No se pudo crear el rol.", Toast.LENGTH_SHORT).show() }
                }
            }.show()
    }
}
