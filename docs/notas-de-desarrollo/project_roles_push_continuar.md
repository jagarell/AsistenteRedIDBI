---
name: project-roles-push-continuar
description: "Reglas de negocio pedidas 2026-10-03: roles Técnico/Supervisor, push a supervisores y técnicos, 'Continuar' retoma el chat; qué falta (Firebase)"
metadata:
  node_type: memory
  type: project
---

**Roles:** toda cuenta registrada es TECNICO (el gateway ignora `role` del request; la app ya no muestra el selector). SUPERVISOR solo por base de datos. El supervisor hace lo mismo que el técnico y además valida minutas COMPLETA (`POST /api/minutas/{id}/validar`, `@PreAuthorize`).

**Push (FCM):** `MinutaNotifier` avisa a supervisores con token cuando una minuta pasa a COMPLETA; `MinutaReminderJob` (cron diario 09:00 Lima) avisa una vez al técnico de borradores por vencer (`app.reminders.draft-ttl-days`=30 — confirmado por el usuario el 2026-10-03 —, `warn-days-before`=2: **supuesto mío**, el aviso a 2 días no lo definió el cliente). Un solo `fcmToken` por usuario (un dispositivo).
**Pendiente del usuario:** crear el proyecto Firebase → (1) `app/google-services.json` y recompilar el APK, (2) service account JSON en Railway como `FIREBASE_CREDENTIALS_JSON`. Sin eso los push no salen (el backend los omite sin romper nada).

**Continuar / Nueva Evaluación:** "Continuar · Evaluación previa" retoma el chat guardado (`ChatProgressStore.resumableEvaluationId`); solo "Nueva Evaluación" (inicio, menú lateral, FAB del Historial) borra el anterior, con diálogo de confirmación. El progreso ya no se borra al generar la minuta.

Estado general y pendientes: [[project-estado-apk-0-0-2]]. Ver [[project-flow-engine-76-nodos]] para el chat y el comparador de pantallas contra el prototipo.
