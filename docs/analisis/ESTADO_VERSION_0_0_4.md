# Estado de la versión 0.0.4 (2026-10-12)

Implementa el simulador de 2026-10-11 como especificación ejecutable.

## Hecho
- Chat con el flujo nuevo (motor Python `app/chat/sim/`, verificado contra el simulador: 200/200 escenarios + golden).
- Evidencias con revisión de IA, ✕ para eliminar lecturas, «No es correcto», edición de respuestas (`/chat/edit`).
- Análisis IA con criterios ponderados y referencias; Propuesta con estados BORRADOR → COMPLETA → VALIDADA.
- Vista previa / PDF con marca «VISTA PREVIA» o «SIN VALIDAR», zoom y paginación.
- Bandeja de notificaciones por usuario; avisos al supervisor (COMPLETA) y al técnico (VALIDADA).
- Mapa: Ubic./IP, íconos, 5 tipos de línea, rayos de energía, deshacer/rehacer, multi-selección.
- Teclado del chat con `adjustResize`.

## Pendiente / sin verificar en dispositivo
- Verificación visual de Análisis, Propuesta, vista previa PDF, notificaciones y editor de mapa.
- Push (Firebase sin configurar), validación de evidencias por dataset/CLIP (hoy es por LLM), importador de la base de conocimiento.

## Pruebas locales
- FastAPI: `VISION_MOCK=1 venv/bin/uvicorn app.main:app --port 8000`.
- Gateway: puerto 8091 con H2 (8080 y 8081 pueden estar ocupados) y `BASE_URL = http://10.0.2.2:8091/`; no commitear ese valor.

## Cambios de la 0.0.5 (2026-10-13)
- Historial, Inicio y Minutas con los estados del prototipo (En análisis, Borrador, Completa, Validada) y filtro de fecha (rápidos + rango).
- El supervisor **no edita**: solo aprueba (Validar) o rechaza con motivo. El técnico recibe el aviso, la minuta vuelve a Borrador (con «Rechazada» y el motivo), la corrige y la completa otra vez; entonces se avisa de nuevo a los supervisores.
- Gateway: `POST /api/minutas/{id}/rechazar` {reason}; `Minuta.rejectionReason/rejectedByName/rejectedAt` (sin estado nuevo: la restricción de enum de la base no se actualiza); sin rol requerido responde 403.
- Estado derivado por evaluación (`minutaStatus`): EN_CURSO si el chat no terminó, BORRADOR si terminó y no hay minuta, o el de la minuta.
