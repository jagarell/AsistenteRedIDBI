package com.upc.asistenteredidbi.domain.model

/**
 * Roles base (ver gateway `com.upc.idbi.gateway.auth.Role`): técnico, supervisor y administrador.
 * Lo que ve cada rol en Reportes lo decide el gateway (Equipo y accesos › Roles y permisos).
 */
enum class Role {
    TECNICO,
    SUPERVISOR,
    ADMINISTRADOR;

    companion object {
        /** Convierte el texto libre que llega del backend a un rol válido. */
        fun fromString(value: String?): Role =
            values().firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
                ?: TECNICO
    }
}

fun Role.toDisplayLabel(): String = when (this) {
    Role.TECNICO -> "Técnico Senior"
    Role.SUPERVISOR -> "Supervisor"
    Role.ADMINISTRADOR -> "Administrador"
}
