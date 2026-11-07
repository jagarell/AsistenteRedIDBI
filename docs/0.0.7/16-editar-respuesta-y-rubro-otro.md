# 16 · Chat IA 0.0.15: editar sin perder respuestas, rubro «Otro», equipo de los mozos, evidencias con descripción y reportes del Asistente IA

Fuente: QA de Yoyo del 07/11/2026.
- Cambios sobre GitHub: app 0.0.14, API 0.0.13 y gateway 0.0.13.
- No hay migración SQL: la descripción se guarda en `Evidence.comment`, una columna que ya existe.

## 1. Qué se pidió y cómo queda

| # | Pedido | Cómo queda |
|---|---|---|
| 1 | Al editar una respuesta anterior se borran todas las siguientes | Solo se vuelve a hacer esa pregunta. Se quitan solo las respuestas que dependían de ella y el chat dice cuáles |
| 2 | Al editar, las opciones vuelven a lo sugerido | La pregunta vuelve con lo que ya se había respondido |
| 3 | El rubro «Otro» acorta la evaluación | Hace el mismo flujo que un restaurante o una discoteca, sin opciones marcadas de antemano |
| 4 | Los mozos no solo usan tablet | Hay más opciones (ver la sección 2) |
| 5 | Cambiar una foto por otra mejor, desde el chat o desde la lista de evidencias | Se puede cambiar cualquier foto, desde el chat o desde la lista |
| 6 | Descripción en las evidencias | Se puede agregar, editar o eliminar. Sale debajo de la foto en la minuta y la IA la usa |
| 7 | En el Asistente IA, «Reportes» respondía «No tengo acceso a reportes» y «Hazme un reporte de todos los técnicos que están por terminar su minuta» abría «Nueva minuta» | Los dos dan el reporte. Ver la sección 8 |

## 2. Cómo debe funcionar

### Editar una respuesta (en curso o terminada)

- Solo se vuelve a hacer esa pregunta. El historial del chat se conserva y la pregunta se agrega al final.
- La pregunta vuelve con lo ya respondido:
  - **Selección múltiple:** las opciones marcadas, también las escritas con «Otro». Ejemplo: Fríos, Calientes y Barra siguen marcadas. Se desmarca Barra, se agregan Barra 1 y Makis, y queda Fríos, Calientes, Barra 1 y Makis.
  - **Opciones:** un botón «✓ Mantener: …» y la opción actual marcada con ✓.
  - **Texto:** el texto escrito.
  - **Fecha:** «✓ Mantener · fecha».
- Al responder, solo se quitan:
  - las respuestas de preguntas que ya no salen (todo lo de «IMP. Barra 2» si se quitó Barra 2);
  - las respuestas cuya opción ya no existe.
- Si se agrega algo nuevo, como el área Makis, el chat pregunta lo nuevo (su impresora) y **no borra lo que ya estaba respondido después**: WiFi, fotos y resumen se conservan.
- El chat avisa:
  - «Respuesta actualizada. Tus demás respuestas se mantienen.», o
  - «… Se quitaron las que dependían de esta (si hacen falta, te las vuelvo a preguntar): …».

### Rubro «Otro (escribir)»

- Hace el mismo flujo que un restaurante o una discoteca:
  - áreas de preparación con impresora;
  - comandas en caja;
  - tomas de energía;
  - equipo de los mozos;
  - WiFi por zona;
  - fotos de cada área.
- No viene nada marcado de antemano.
- En áreas de preparación se puede elegir «Ninguna: no prepara productos».
- «Retail – Supermercado» sigue con su flujo corto.

### Equipo de los mozos (P09c)

La pregunta es «¿Los mozos toman pedidos con algún equipo? Puede ser tablet, su celular o un iMin móvil.». Las opciones son:
- Sí, con tablet
- Sí, con su celular
- Sí, con iMin móvil (toma el pedido e imprime)
- No, los mozos no toman pedidos con un equipo
- Otro (escribir)
- Aún por confirmar (sugerir iMin móvil)

Qué hace cada respuesta:
- **«No»:** no se revisa el WiFi por zona. Con cualquier otra respuesta, sí.
- **«iMin móvil»:** recomendación «iMin móvil para los mozos: debe tener buena señal WiFi en las zonas de atención».
- **«Aún por confirmar»:** recomendación «Sugerir iMin móvil: toma el pedido e imprime en el mismo equipo. Se cotiza con Desarrollo de Negocios». Además entra en «Quedan por confirmar».
- La minuta, en la tabla de Negocio, muestra la fila «Mozos toman pedidos con».

### Cambiar una foto y descripción de cada evidencia

- **Chat IA:** al tocar la foto de una evidencia salen «Cambiar foto» y «Agregar descripción» (o «Editar descripción»).
- **Lista de evidencias** (durante el chat y al final):
  - cada evidencia tiene «Cambiar foto» (o «Tomar foto» si falta) y «Agregar descripción» o «Editar descripción»;
  - la descripción se ve debajo de la evidencia;
  - en «Editar» se puede eliminar.
- **Al subir una foto:** el enlace «✎ Agregar descripción (opcional)» deja escribir qué muestra la foto antes de subirla, y la descripción viaja con la foto.
- **Cambiar foto:** se vuelve a pedir solo esa evidencia (como al editar una respuesta). La foto nueva reemplaza a la anterior, y la descripción y las demás respuestas se mantienen.
- **Minuta:** en el anexo, debajo de la foto, sale «Descripción: …».
- **IA:**
  - al leer la foto, la IA recibe la descripción como contexto («El técnico describe la foto así: …»), aunque igual la verifica con la imagen;
  - el Asistente IA la tiene en la minuta, para sus observaciones y recomendaciones;
  - en la Parte 7, la descripción servirá para saber si un equipo es del local, está por implementar o es de otra ubicación (por ejemplo, la laptop del técnico).

## 3. API (`AsistenteRedIDBI-API`)

| Archivo | Cambio |
|---|---|
| `app/chat/sim/engine.py` | Ver el detalle debajo |
| `app/chat/sim/service.py` | `_prev_answer()`: con `edit` activo, el prompt trae `editing`, `currentLabel`, `selected` (y lo escrito con Otro en `options`), `currentValue` (`"OTRO:texto"` u `"OTRO:600"`), `defaultValue` o `value`. Aviso `EDIT_OK`. `evidence_note()`. `descripcion` al subir fotos. `evidenceNotes` en cada respuesta |
| `app/chat/sim/flow.py` | Rubro «Otro» completo (`X.simple = rub == "RETAIL"`, `X.otroRub`, P11e con «Ninguna»). P09c con `MOZ_OPTS`, `PC["P09c"]="PC"` y «Aún por confirmar (sugerir iMin móvil)» |
| `app/chat/sim/rules.py` | `R_IMIN_MOVIL`. El ancho de banda de los mozos cuenta salvo que P09c sea «No» |
| `app/chat/sim/minuta.py` | Fila «Mozos toman pedidos con» y «Descripción: …» debajo de cada foto del anexo |
| `app/chat/schemas.py` | `ChatAnswerRequest.descripcion`, `ChatEvidenceNoteRequest` y `ChatResponse.evidenceNotes` |
| `app/chat/router.py` | `POST /chat/evidence-note` |
| `tests/` | `test_editar_respuesta_015.py` (10 pruebas) y `test_mozos_y_descripciones_015.py` (7). `test_sim_engine._answer_text` manda `NS52` para «Por confirmar». `golden/sim_golden.json` se regeneró con `tools/sim_oracle` sobre el simulador actualizado |

**Detalle de `engine.py`:**
- `reopen()`: guarda la respuesta anterior en `state["edit"]`. En una foto de lista (`EV#…`) reabre la lista. Conserva las descripciones.
- `prune()`: decide qué respuestas siguen valiendo con `complete_nodes()`, que es el flujo completo con lo que falta respondido con lo sugerido. Así lo nuevo no esconde lo que viene después.
- Descripción: entradas `{"t": "desc"}` en `order`, `set_desc()` y `evidence_notes()`. En `A[id]["desc"]`.
- `upload(..., descripcion)` pasa la descripción a la IA.
- `norm_pc` de P09c.

Resultado: **191 pruebas pasan.** Con la Parte 6 encima también pasan.

## 4. Gateway (`AsistenteRedIDBI-API-Gateway`)

| Archivo | Cambio |
|---|---|
| `ChatAnswerRequest` | `descripcion` |
| `ChatResponse` | `evidenceNotes` |
| `ChatController` | `answer-photos` con la parte `descripcion` y `POST /api/evaluations/{id}/chat/evidence-note` (body `{state, nodeId, descripcion}`) |
| `ChatService` | `evidenceNote()` y `syncEvidenceComments()`: copian la descripción a `Evidence.comment` de las fotos guardadas de esa evidencia |
| `ChatEditRequest` | Comentario: la app usa siempre `reopen` |

El reemplazo de la foto ya existía: `storeEvidence` borra las fotos anteriores del mismo código y alcance. No se pudo compilar aquí (no hay Maven).

## 5. App Android (`AsistenteRedIDBI`)

| Archivo | Cambio |
|---|---|
| `TechnicalChatViewModel` | `reopenNode` siempre con `"reopen"` y sin recortar el historial. `evidenceNotes`, `pendingDescription`, `setPendingDescription()`, `saveEvidenceNote()`, `evidenceNodeId()` y `evidenceNote()`. La descripción viaja con `sendPhotos` y `sendPendingPhoto` |
| `TechnicalChatFragment` | «✓ Mantener» en opciones y fecha. `showEvidenceOptions()` al tocar una foto. «✎ Agregar descripción (opcional)» en el paso de foto |
| `ChatMessagesAdapter` | `onPhotoClick` |
| `EvidenceGalleryDialog` | En cada evidencia: descripción, «Cambiar foto» y «Agregar / Editar descripción». Se vuelve a pintar al guardar |
| `EvidenceNoteDialog` (nuevo) | Diálogo de la descripción, con Guardar, Eliminar y Cancelar |
| DTO, mappers, modelos, `ChatApiService`, repositorio y casos de uso | `editing`, `currentValue`, `currentLabel`, `evidenceNotes`, `descripcion` y `evidenceNote()` |
| `docs/simulador/simulador.html` | Mismo comportamiento (bloques «0.0.15 · …»), P09c y rubro «Otro» |

No se pudo compilar aquí (no hay Android SDK).

## 6. Web

Cuando se construya, debe usar los mismos endpoints:
- `edit` con `reopen` y lo marcado (`selected`, `currentValue`, `defaultValue`);
- `answer-photos` con `descripcion`;
- `evidence-note`.

En la lista de evidencias debe mostrar lo mismo que la app.

## 7. Referencia: estado del local

| Estado | Qué cambia en el Chat IA |
|---|---|
| **En operación** | Flujo completo |
| **En obra** | Internet: «¿Ya tiene internet contratado? ¿Qué tipo tendrá?». Sin respaldo ni caídas. Router: «¿Dónde irá?» con foto, sin speedtest, escáner ni ipconfig. «¿El cliente ya tiene la impresora?». Tomas «instaladas o por instalar». Sin AnyDesk, sin prueba de chip (si ya hay internet), sin mozos ni WiFi por zona. Fotos «Dónde irá…» |
| **Por inaugurar** | Como en operación (speedtest y WiFi). «¿Qué proveedor tiene contratado?». Sin respaldo ni caídas. Impresora «Nueva» por defecto |
| **Ya cuenta con sistema anterior** | Como en operación, sin respaldo ni caídas. Sunmi o iMin: ¿tiene impresora integrada? |

«Por implementar» es el estado de un equipo, no del local (Parte 7, C3).

**Propuesta pendiente de aprobar** (Parte 7):
- En obra, todos los equipos empiezan como «Por implementar» y pasan a «Existente» si la foto o su descripción lo muestran.
- Speedtest opcional en obra si el router ya está instalado.

## 8. Asistente IA: pedir reportes (QA del 07/11, captura de la app)

**Qué pasaba**

| Mensaje | Lo que hacía | Causa |
|---|---|---|
| «Reportes» | Respondía ChatGPT: «No tengo acceso a reportes específicos» | `REPORTE` buscaba `\breporte\b`. En plural («reportes») no coincide, así que el mensaje iba a la respuesta libre |
| «Hazme un reporte de todos los técnicos que están por terminar su minuta» | Abría «Nueva minuta · ¿Qué minuta vas a redactar?» | `NUEVA` («haz… minuta») se revisaba antes que `REPORTE` |

**Cambio en el gateway (`AsistenteChatService`)**
- `REPORTE` ahora acepta `reportes?|informes?`.
- Nuevo `POR_TERMINAR`: «por terminar», «sin terminar», «falta(n) terminar», «no ha(n) terminado», «en curso».
- Si el mensaje pide datos (reporte, por validar o por terminar), nunca abre «Nueva minuta».
- «Por terminar» va al reporte con estado Pendiente y busca en **todas las fechas**: una minuta de un mes anterior sigue pendiente. El reporte dice «todas las fechas».

**Cambio en la API (`app/asistente/router.py`)**
- `PIDE_DATOS`: la respuesta libre tampoco ofrece «Nueva minuta» para esos pedidos.
- Prueba: `test_asistente_pedir_reporte_no_abre_nueva_minuta`.

**Simulador:** el mismo arreglo. «Por terminar» filtra las minutas en Borrador y Pendiente.

**Comprobado aquí**

Ruteo del gateway (las mismas expresiones, ejecutadas con Java):

| Mensaje | Resultado |
|---|---|
| «Reportes» | Reporte |
| «Hazme un reporte de todos los técnicos que están por terminar su minuta» | Reporte |
| «Qué técnicos no han terminado su minuta» | Reporte |
| «Redacta una minuta de Restobar Larco» | Nueva minuta |
| «Minutas por validar» | Por validar |
| «Actualiza la minuta de AMAT…» | Actualizar |
