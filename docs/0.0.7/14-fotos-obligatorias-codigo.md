# 0.0.7 · Parte 5: fotos obligatorias en el Chat IA (código listo para subir)

Esta parte lleva al código lo que la Parte 4 (md 13, secciones 2.2, 2.3 y 2.4) solo describía. Corrige el bloqueo que se vio en la app
real el 06/11/2026: una foto obligatoria rechazada por la IA no dejaba seguir.

Va **sobre la 0.0.7** (Partes 1, 2 y 3 ya aplicadas). Orden de despliegue: **API → Gateway → App**.
La web usa el mismo Chat IA. El simulador publicado ya tiene el mismo comportamiento y sirve de referencia.

## Qué hace (app y web, en cualquier estado del local)

1. **Guardar como correcta.**
   - Cuándo aparece: la IA dice «no corresponde» o «no es legible» en cualquier foto del chat, sea obligatoria, opcional o de la lista del local.
   - Qué muestra: una hoja con lo que vio la IA, una **nota opcional** y los atajos:
     - siempre: «La foto es correcta», «Aún no hay puntos», «El cliente no permite»;
     - además, en obra o por inaugurar: «Local en obra».
   - Qué pasa al guardar: se usa la misma foto y el chat avanza. En la minuta (anexo) **solo se muestra la nota** que se escribió («Nota: …»). Si no se escribió nota, la foto se ve como cualquier otra. La minuta no dice «guardada por el técnico».
2. **Subir después** (solo fotos obligatorias de la lista: frontis, lugares de preparación, caja y otros equipos del local).
   - El chat sigue y el encabezado muestra «N obligatorias por completar».
   - Al terminar aparece **«Subir fotos pendientes (N)»**, con una fila por foto: «Tomar foto», «Subir foto» y, si la IA la observa, «Guardar como correcta».
   - La minuta **no se genera** mientras falte alguna. «Ver propuesta» abre la lista de pendientes y el PDF responde «No se puede generar la minuta: falta …».
3. **La IA rechaza menos fotos buenas.**
   - Recibe qué es el lugar («un lugar de preparación… llamado «Calientes»») y el estado del local.
   - Solo rechaza lo que claramente no es un lugar del local (captura de pantalla, documento o selfie). Un nombre de área desconocido ya no es motivo de rechazo.

## Archivos

| Repositorio | Archivo | Cambio |
|---|---|---|
| API | `app/chat/sim/engine.py` | `place_desc()` (lugar + estado del local); `_item`/`item_node`; `@LATER`; `pending_later()`; `upload(…, mode="forzar", nota, node_id)`; el conteo de evidencias no cuenta las pendientes |
| API | `app/chat/sim/service.py` | `canForce`, `canLater`, `enObra` en la evidencia; mensajes con «Guardar como correcta»; subir por `nodeId`; `pendingMandatory` en la respuesta |
| API | `app/chat/sim/vision.py` | lectura `place`, que solo rechaza lo que claramente no es un lugar del local |
| API | `app/chat/sim/minuta.py` | anexo: muestra solo la nota («Nota: …»), si se escribió |
| API | `app/chat/router.py` | `minuta-document` devuelve `missingMandatory` si quedan pendientes (el gateway ya bloquea el PDF con eso) |
| API | `app/chat/schemas.py` | `ChatAnswerRequest.nota`, `.nodeId`; `ChatResponse.pendingMandatory` |
| API | `tests/test_fotos_obligatorias_007.py` | 11 pruebas nuevas |
| Gateway | `chat/ChatAnswerRequest.java`, `ChatResponse.java` | campos `nota`, `nodeId`, `pendingMandatory` |
| Gateway | `chat/ChatController.java`, `ChatService.java` | `answer-photos` acepta `nota` y `nodeId`; la foto se guarda igual (`storeEvidence`) |
| Android | `data/remote/ChatApiService.kt`, `ChatRepository*.kt`, `TechnicalChatUseCases.kt` | `nota` y `nodeId` en `answer-photos` |
| Android | `data/remote/dto/TechnicalChatDto.kt`, `domain/model/TechnicalChatModels.kt`, `data/mapper/TechnicalChatMappers.kt` | `canForce`, `canLater`, `enObra`, `ChatPendingPhoto`, `pendingMandatory` |
| Android | `presentation/chat/TechnicalChatViewModel.kt` | `forceEvidence(nota)`, `laterEvidence()`, `sendPendingPhoto(nodeId, uris, mode, nota)`; aviso al terminar con pendientes |
| Android | `presentation/chat/TechnicalChatFragment.kt` | botón verde «Guardar como correcta» + hoja con nota opcional; «Subir después»; contador; «Subir fotos pendientes (N)»; «Ver propuesta» no avanza con pendientes |

## Probado

- **API.** 158 pruebas pasan: las 147 de antes más 11 nuevas. Las nuevas cubren:
  - «Guardar como correcta» en los 4 estados del local;
  - que solo se pueda usar si la IA observó la foto;
  - «Subir después» hasta subir todas;
  - pendiente rechazada por la IA y luego guardada como correcta;
  - minuta bloqueada con pendientes;
  - en el anexo solo aparece la nota, sin «guardada por el técnico»;
  - descripción del lugar para la IA.
- **Simulador (app y web).** «Guardar como correcta» en una foto del local (Cocina) en operación y en obra, con los atajos correctos; «Subir después»; 300 recorridos completos del chat sin bloqueos.
- **Gateway y Android:** **no se pudieron compilar aquí** (sin Maven ni Android SDK). Revisados a mano.

## Cómo probar en la app (con API y gateway desplegados)

1. Nueva evaluación, en cualquier estado del local, hasta «Fotos del local».
2. En «Calientes» sube una foto que la IA rechace (por ejemplo, una pared):
   - aparece «Guardar como correcta»;
   - escribe una nota o déjala vacía → «Guardar y continuar» → el chat avanza.
3. En otra foto obligatoria toca «Subir después»:
   - el encabezado dice «1 obligatoria por completar»;
   - al terminar, «Subir fotos pendientes (1)» → súbela y recién entonces se genera la minuta.
4. En el PDF, debajo de la foto de Calientes aparece «Nota: …» si se escribió. No aparece «guardada por el técnico».
5. Regresión: `pytest`, `./mvnw test`, `./gradlew assembleDebug`.
