package com.upc.asistenteredidbi.domain.model

/**
 * Roles del sistema (ver gateway `com.upc.idbi.gateway.auth.Role`). Sólo hay
 * dos roles operativos, sin vista de administración: el control de acceso se
 * aplica por código, tanto en el backend como en el gating de UI de la app.
 */
enum class Role {
    TECNICO,
    SUPERVISOR;

    companion object {
        /** Convierte el texto libre que llega del backend a un rol válido. */
        fun fromString(value: String?): Role =
            values().firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
                ?: TECNICO
    }
}
