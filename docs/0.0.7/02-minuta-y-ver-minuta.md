# Ver minuta y observaciones resueltas

## 1. Ver minuta (Propuesta PDF)
La pantalla se mantiene como está. Debajo del PDF se agregan tres acciones para técnico y administrador:

| Botón | Acción |
|---|---|
| **Editar** | Abre el menú de edición actual (datos del local, topología, equipamiento, diagnóstico, propuesta, observaciones) |
| **Compartir · Sin validar** | Abre «Compartir»: el PDF lleva la marca de agua SIN VALIDAR mientras no esté validado |
| **Solicitar validación** | Pendiente → Completa y aviso al supervisor. Si ya está Completa: «Solicitada». Si está Validada: «Validada» (deshabilitado) |

- Si la minuta la redactó o actualizó el Asistente IA, arriba del PDF sale el aviso «Redactada / Actualizada por el Asistente IA · vN · fecha».
- Al volver desde Ver minuta se regresa al Asistente IA.

## 2. Observaciones resueltas en la visita (Análisis IA)
Tarjeta nueva **«Observaciones de la visita»** antes del Resumen IA:
- Lista las observaciones, cada una con el botón **Se resolvió**.
- Al marcarla:
  - si era un equipo por WiFi (PC con el sistema de impresiones o Raspberry), la respuesta cambia a **cable de red**;
  - la observación deja de contar y el **estado final se recalcula** (APTO si no quedan otras);
  - el Resumen IA agrega: «Durante la visita se había observado «Caja 2: PC con Wrapper por WiFi» (se conectó por cable de red), y se resolvió en la visita».
- Lo resuelto se lista con ✓ y el botón **Deshacer**.
- Si la minuta estaba Completa o Validada, vuelve a Pendiente.
- El supervisor solo ve la lista (sin botones).

## 3. Criterios de aceptación
**CA-01 Solicitar validación**
- Dada una minuta Pendiente sin evidencias obligatorias faltantes, cuando el técnico toca «Solicitar validación», entonces la minuta pasa a Completa y el supervisor recibe la notificación «Minuta completa: [local]».
- Dada una minuta con evidencias obligatorias «Subir después», entonces se muestra la lista de faltantes y la minuta no cambia de estado.

**CA-02 Compartir sin validar**
- Dada una minuta no validada, cuando se comparte, entonces el archivo dice «No verificada · marca de agua SIN VALIDAR».

**CA-03 Resuelto en la visita**
- Dada una minuta OBSERVADA solo por «Caja 2: PC con Wrapper por WiFi», cuando el técnico toca «Se resolvió», entonces el estado final es APTO, la regla «PC de Caja 2 por cable» cumple y el resumen menciona que se resolvió en la visita.
- Cuando toca «Deshacer», entonces vuelve a OBSERVADO y la respuesta vuelve a WiFi.
