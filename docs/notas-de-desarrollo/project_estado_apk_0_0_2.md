---
name: project-estado-apk-0-0-2
description: "Estado actual (2026-10-03): APK 0.0.2 contra producción, qué está hecho, decisiones tomadas, pendientes y trampas aprendidas"
metadata:
  node_type: memory
  type: project
---

**Entregable vigente:** `AsistenteDeRed-0.0.2.apk` en la raíz del repo Android (git-ignorado vía `.git/info/exclude`), debug, sin marca IDBI visible (nombre de app «Asistente de Red», PDF sin logo), versionCode 2, apunta SOLO a `https://asistenteredidbi.up.railway.app`. Commits al 2026-10-03: Android `089040f`, gateway `c81c257`, FastAPI `dd03113`; los 3 en `main` y desplegados (Railway auto-deploy al hacer push). APK > 30 MiB: no se puede mandar por el chat, solo copiarlo de la carpeta. No hay llave de release (solo debug).

**Hecho y verificado en producción:** chat de 76 nodos + evidencias con IA; mapa editable (zoom a pantalla completa, paleta de colores, menús como el prototipo); minuta PDF (Anexo B eliminado; foto general del local E9 obligatoria: sin 'Omitir', el motor la rechaza y el gateway no genera PDF sin ella — la regla `MANDATORY_EVIDENCE` vive en `flow_engine.py` y `missingMandatory` en `minuta_doc.py`); contador "Pregunta N de ~T" (solo preguntas fijas) + "Evidencia N de M"; registro siempre Técnico; "Continuar" retoma el chat y solo "Nueva Evaluación" lo borra (con confirmación); Historial con filtro por rango de fechas (embudo), orden más nuevo primero, y deslizar un Borrador a la izquierda lo anula; Minutas/Configuración/Historial alineados al prototipo. Detalle de roles/push en [[project-roles-push-continuar]]; arquitectura y cómo comparar con el prototipo en [[project-flow-engine-76-nodos]].

**Decisiones del usuario:** el borrador vence a los 30 días; solo lo que está en estado Borrador se puede anular/eliminar (no En Análisis/Completado/Enviado). Una evaluación nueva nace en BORRADOR.
**Supuestos míos (confirmar):** aviso de vencimiento cuando faltan ≤2 días; anular es "soft" (marca `annulled`, borra los borradores de minuta ligados y el chat local), no borrado real.

**Pendiente:**
1. Firebase: `app/google-services.json` (recompilar → 0.0.3) y service account en Railway (`FIREBASE_CREDENTIALS_JSON`). Hasta entonces los push no salen.
2. Preguntar al usuario si quita del PDF las filas "Pendiente · …" (Razón social, RUC, Correo, switch) y la regla "Datos obligatorios completos: N campos pendientes" (vienen del PDF de ejemplo del cliente); hoy solo se quitó el Anexo B.
3. No probado en dispositivo: que 'Omitir' desaparezca en E9, y cómo muestra la app el error 400 al pedir el PDF de una evaluación vieja sin E9 (evaluaciones 54, 58, 59 de la cuenta de prueba lo devuelven).
4. Pantallas 01–19 del prototipo no repasadas del todo (propuesta, PDF, envío); márgenes laterales 24dp vs 16dp del prototipo; fotos del mapa solo S/M/L; comandos del mapa por reglas, no LLM.
5. Datos de prueba en producción: cuenta de prueba de producción (credenciales fuera del repo; pídelas al equipo), ~14 "Nueva Evaluación" abandonadas en En Análisis y evaluaciones 69 (anulada) y 70 (completa con E9).

**Trampas aprendidas:**
- La base de producción (Postgres) tiene una restricción con los valores de los enums existentes y `ddl-auto=update` NO la actualiza: agregar un valor nuevo a un enum (ej. `EvaluationStatus.ANULADA`) da 500. Usar una columna/marca aparte.
- Comparar con el prototipo: emulador en 720x1600 (`adb shell wm size 720x1600`) y capturas lado a lado; el emulador a veces se cae y hay que relanzarlo con `env -u HTTP_PROXY ...` ([[project-emulator-proxy-conflict]]).
- Al automatizar: la burbuja del usuario ahora dice "Sí\nToca para editar" (el texto exacto "Sí" ya no coincide) y los chips se ubican por y>1000; los scripts de /tmp (chip.py, drive_cmp.py, full_run3.py) son efímeros.
