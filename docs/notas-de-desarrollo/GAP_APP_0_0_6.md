# Brecha app actual (0.0.5) vs. prototipo Simulador 0.0.6

Fuente: https://claude.ai/artifact/TbPUrzm9RLY5MaAbzviMx4 (leído 2026-11-05). El prototipo web (13d9AyAHWE4SWa7oU2hYPs) solo mostraba "Design / Loading…", no se pudo leer.

| Cambio 0.0.6 | Ya en la app | Falta | Repos |
|---|---|---|---|
| Nombre "Perfil y configuración" | Hecho (drawer y título) | — | app |
| Modo noche | Tema DayNight vacío; colores fijos en layouts | Paleta night + switch en la cuenta | app |
| Estados Borrador / Pendiente / Completa / En revisión / Validada | BORRADOR, COMPLETA, VALIDADA (+ motivo de rechazo) | Estados EN_CURSO y EN_REVISION, renombres, mismos nombres en web | gateway + app |
| Rol Administrador (panel, reportes, equipo, preguntas y reglas, plan) | Solo TECNICO / SUPERVISOR | Rol, endpoints y pantallas | gateway + app |
| Foto dudosa «Guardar como correcta» | No | Opción en el chat tras "no corresponde" | fastapi + app |
| Obligatorias «Subir después» + pedido al generar la minuta | Saltar solo en no obligatorias | Estado "later" y bloqueo al generar PDF | fastapi + gateway + app |
| Chat de Ayuda (no va a la minuta) | No | Base de ayuda y hoja en el chat | app |
| «Ver topología de red» | Mapa solo en flujo de minuta | Acceso directo | app |
