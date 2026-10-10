# Flujo de nodos – Asistente técnico de red con IA (minuta técnica)

Especificación del chat técnico del proyecto **Asistente Red IDBI**. **Ya está implementada** en `AsistenteRedIDBI-API/app/chat/` (motor `flow_engine.py`, flujo `app/chat/flow/flujo_asistente_red.json`). Este documento se generó desde ese JSON el 2026-10-06 y se actualizó el 2026-10-10 con los cambios de comandas, internet, puertos del router, decisión de impresión y WiFi por zona (ver §15).

**La fuente de verdad es `flujo_asistente_red.json`.** Este documento la explica.

Las pruebas del motor están en `AsistenteRedIDBI-API/tests/test_flow_engine.py` y `tests/test_rock_sample.py`.

---

## 1. Piezas (ya construidas)

1. **Motor de flujo (FastAPI, `app/chat/flow_engine.py`).**
   - Carga el JSON.
   - No guarda estado en el servidor: el cliente envía el `state` opaco en cada llamada (ver §10).
   - Devuelve el siguiente nodo que la app debe mostrar.
2. **Chat en Android (`TechnicalChatFragment`).** Renderiza TEXT, NUMBER, YES_NO, CHOICE y MULTI_SELECT. También muestra tarjetas de evidencia (galería, "Sí, es correcto / Cambiar foto", Omitir), avisos y el resumen final.
3. **Evidencias (`app/chat/evidence.py`, `checks.py`).**
   - La visión extrae los campos de `aiExtract`.
   - Se aplican los `crossCheck` y las reglas V01–V10.
   - En la etiqueta del router (E3) se ocultan usuario, contraseña, SSID, clave WLAN y QR con Pillow. Sin IA, la imagen se pixela completa.
4. **Salidas.**
   - Minuta: `minuta_doc.py`. El PDF lo genera el gateway con `MinutaPdfService`.
   - Mapa: `map_model.py` y `map_render.py`, a partir de las pistas de `topology` y `registry.printers`.

## 2. Diagrama general

```mermaid
flowchart TD
  A[A. Datos de la visita<br/>P01–P08a] --> B[B. Negocio<br/>P09 tipo · P10 pisos · P11 áreas]
  B -->|P10 > 1| BP[🔁 L_PISOS<br/>P11a piso de cada área] --> B2
  B -->|P10 = 1| B2[P12 cajas · P12b ¿comandas en caja?]
  B2 -->|sin áreas de preparación y comanda en caja| SUN[ℹ️ Recomendar Sunmi] --> C
  B2 --> C[C. Internet<br/>P13 · P14 tipo de conexión · P15–P18 + 📷 E1 E2 E3<br/>sin internet o Sunmi: P14b señal del chip + 📷 E1b<br/>sin internet: se salta P15 y E1<br/>puertos del router leídos en E2 (P18a si no se leen)]
  C -->|P12 > 0| D[🔁 L_CAJAS<br/>P22 ¿hay PC o laptop?]
  C -->|P12 = 0| E
  D -->|Sí| D1[P23 cable/WiFi → 📷 E4 ipconfig]
  D -->|No · No sé| D2{P22a ¿Ya tiene Raspberry?}
  D -->|Tablet| DT[⚠️ Raspberry obligatorio] --> D2
  D -->|Otro| DO[P22f ¿qué equipo?] --> D2
  D2 -->|Sí| D3[P22c conexión]
  D2 -->|No| D4[P22b adquirirá Raspberry/Laptop/PC] --> U1((Subflujo U))
  D1 & D3 & U1 --> E[E. Impresoras<br/>P25 áreas que imprimen · P26 cuántas]
  E -->|P26 > 0| EL[🔁 L_IMPRESORAS<br/>P27 áreas que atiende → compartida si son varias<br/>P28 ubicación · P29 marca · P30 conexión · 📷 E5<br/>área de preparación y no es de red por cable → aviso R51]
  E -->|P26 = 0| EG
  EL --> EG{¿Áreas sin impresora?}
  EG -->|Sí| EA[⚠️ Áreas sin impresora] --> EL2[🔁 por área: P32 compartir o nueva]
  EL2 -->|Compartir| E3[P32a con qué impresora]
  EL2 -->|Nueva| E4[P32b ubicación · P32c conexión] --> U2((Subflujo U))
  EG -->|No| GI
  E3 & U2 --> GI{G_IMPRESION<br/>decisión de impresión}
  GI -->|sin PC y comandas| GR[⚠️ Kit Raspberry]
  GI -->|sin PC y caja USB| GP[⚠️ Laptop o PC con controlador]
  GI -->|sin PC, sin comandas| GB[ℹ️ PC básica]
  GR & GP & GB & GI --> GPU{G_PUERTOS<br/>¿alcanzan los puertos?}
  GPU -->|faltan| GS[⚠️ Switch + puntos de red] --> US((Subflujo U<br/>switch)) --> F
  GPU -->|alcanzan| F[F. Energía<br/>P33 tomas cerca · P34 · P35 UPS · P36 extensiones]
  F -->|P33 = No| U3((Subflujo U<br/>solo energía)) --> G
  F --> G[G. Cableado<br/>P37 · P38 ¿llegan a todas las áreas?]
  G -->|No| GL[🔁 P38a por área sin punto → Subflujo U solo red] --> G2
  G -->|Sí| G2[P39 cable · P39a entre pisos · P40 gabinete · P41 · 📷 E6]
  G2 -->|P36 = Sí| G3[P43 ubicación extensión · 📷 E7]
  G2 & G3 --> H[H. WiFi y dispositivos<br/>P45 · P46 zonas sin señal → P46a repetidor o access point<br/>por zona: Subflujo U · P47 · 📷 E8 escáner IP]
  H --> I[I. Cierre<br/>📷 E9 · P50 ¿segunda visita? → P50a por qué · P51–P53]
  I --> S[P54 SUMMARY<br/>Generar minuta · Generar mapa con IA]
```

## 3. Esquema de un nodo

```jsonc
{
  "id": "P22",                 // ID estable; coincide con la numeración de la hoja de preguntas
  "kind": "question",          // question | evidence | alert | router | loop | subflow_call | summary
  "block": "D",                // bloque A..I, o U para el subflujo
  "inputType": "CHOICE",       // TEXT | NUMBER | YES_NO | CHOICE | MULTI_SELECT | EVIDENCE
  "text": "Caja {i}: ¿hay una PC o laptop?",   // plantilla, ver §6
  "field": "cajas[{i}].equipo",                // dónde se guarda en el JSON de respuestas de la evaluación
  "required": true,
  "options": [{"value": "PC", "label": "PC"}, …],    // o dinámicas, ver §5
  "validation": {"min": 0, "max": 20},               // opcional: min, max, minSelected, minLength, regex
  "next": "P23"  |  {"rules": [{"if": <cond>, "goto": "A_RASPBERRY_TABLET"}], "default": "P23"},
  "effects": [ … ],            // opcional, ver §7
  "minuta": "equipos_caja",    // sección de la minuta donde va la respuesta
  "topology": { … },           // opcional: pista para el generador del mapa
  "help": "…"                  // nota para el desarrollador o el técnico (no se muestra como pregunta)
}
```

Campos propios de cada tipo:

| kind | Campos extra | Qué hace el motor |
|---|---|---|
| `question` | — | Muestra la pregunta, valida la respuesta, guarda, aplica `effects` y enruta. |
| `evidence` | `evidenceCode`, `accept`, `maxFiles`, `aiExtract`, `tagWith` | Pide fotos y las sube al endpoint multipart de evidencias. Encola la extracción con IA. |
| `alert` | `severity`, `requiresAck` | Muestra un aviso. Avanza cuando el técnico pulsa "Entendido". |
| `router` | — | Sin interfaz. Solo evalúa `next`. |
| `loop` | `over.count` o `over.list`, `body`, `exit`, `itemLabel` | Repite `body` por cada elemento. |
| `subflow_call` | `subflow`, `params`, `next` | Entra al subflujo con un contexto (`ctx`). Al volver, sigue en `next`. |
| `summary` | `actions` | Pantalla final con los botones de generación. |

Destinos especiales de `next`: `@loop.next` (siguiente vuelta o salida del loop), `@return` (volver del subflujo) y `END`.

## 4. Condiciones

```jsonc
{"eq": ["$P08", "OTRO"]}          {"ne": [a, b]}   {"gt": ["$P10", 1]}   {"lt": [a, b]}
{"in": ["$P22", ["PC","LAPTOP"]]} {"includes": ["$P36a", "ROUTER"]}       // includes: un MULTI_SELECT contiene el valor
{"notEmpty": "$derived.areasSinImpresora"}
{"includesAny": ["$P11", "$prepAreas"]}                                    // alguna de las áreas está en la lista
{"and": [c1, c2]}  {"or": [c1, c2]}
```

Cómo se resuelven las referencias:
- `$P22` busca la respuesta en el **alcance más cercano**: primero dentro del loop o subflujo actual, luego hacia afuera.
- `$ctx.x` es un parámetro del subflujo.
- `$derived.x` es un valor calculado (§8).
- `$item` es el elemento actual de un loop (por ejemplo, el área de P32c).
- `$prepAreas` es la lista de áreas de preparación (Bar, Cocina, Pizza, Brasas, Jugos, Cafetería, Parrillas, Makis, Ramen, Panadería). Define si el negocio necesita comandas.
- `$registry.printers` es un registro (§8).

YES_NO se guarda como `"SI"` o `"NO"`.

## 5. Opciones dinámicas

| Forma | Significado |
|---|---|
| `{"from": "$P11"}` | Las áreas que marcó el técnico en P11, con sus etiquetas |
| `{"from": "$P27", "plus": [{"value":"OTRA","label":"Otra"}]}` | Respuesta anterior más opciones fijas |
| `{"from": "$derived.areasSinImpresora"}` | Áreas que aún no tienen impresora |
| `{"from": "$registry.printers"}` | Impresoras registradas: existentes y nuevas creadas en vueltas anteriores |
| `{"range": {"from": 1, "to": "$P10", "label": "Piso {n}"}}` | Piso 1 … Piso N |

## 6. Plantillas de texto

Variables disponibles:

| Variable | Valor |
|---|---|
| `{i}` | Índice del loop actual, desde 1 |
| `{item.value}` / `{item.label}` | Elemento del loop sobre una lista |
| `{answer}` / `{answer.label}` | Respuesta del nodo actual, usada dentro de `effects` |
| `{P27.labels}` | Etiquetas de una respuesta anterior, unidas con " + " |
| `{ctx.equipo}` / `{ctx.area.label}` | Parámetros del subflujo |
| `{derived.areasSinImpresora.labels}` | Valor calculado, unido con comas |

## 7. Efectos

| Efecto | Qué hace |
|---|---|
| `addAction` | Agrega una línea a `registry.actions`. Esa lista prellena **P52 Acuerdos y siguientes acciones** y la sección *Siguientes acciones* de la minuta. No se repiten líneas iguales. |
| `addAlert` | Agrega una alerta técnica al resumen. Puede llevar `if`. |
| `registerPrinter` | Crea una impresora en `registry.printers`. Si viene de P27 es `existing: true`; si viene de P32c es `existing: false`. |
| `addAreaToPrinter` | Suma un área a una impresora existente (caso compartir). |
| `crossCheck` | Solo en evidencias. Compara lo que extrajo la IA con las respuestas. Si no coincide, genera una alerta. Por ejemplo: el proveedor del speedtest frente a P13, la MAC de la impresora frente al escáner E8, o el adaptador del ipconfig frente a P23. |

## 8. Registros y valores derivados

- `registry.printers`: `{id, label, areas[], ubicacion, conexion, existing}`. El `label` se arma con las áreas, por ejemplo **"IMP2 – Bar + Jugos"**.
- `registry.actions`: lista de siguientes acciones.
- `registry.alerts`: alertas técnicas.
- `derived.areasSinImpresora` = áreas de P25 menos las áreas ya asignadas en `registry.printers`. **Se recalcula en cada paso.** Así, en P27 la impresora 2 no ofrece áreas que ya tomó la impresora 1.
- El loop `L_AREAS_SIN_IMP` usa `snapshot: true`: la lista se toma una sola vez al entrar al loop.

## 9. Casos especiales

| Caso | Cómo lo resuelve el flujo |
|---|---|
| **Impresora compartida (Bar + Jugos)** | En P27 el técnico marca **varias áreas** para una misma impresora. Se crea un solo nodo de impresora en el mapa, con etiqueta "Bar + Jugos", y una columna *Áreas que atiende* en la minuta. Si después queda un área sin impresora, P32 → *Compartir* → P32a permite asignarla a una impresora existente o a una nueva creada en una vuelta anterior. |
| **Sin PC ni laptop en caja** | P22 = No (o No sé) → ALERT *recomendar Raspberry* → P22a. Si ya tiene Raspberry, va a P22c (conexión). Si no tiene, va a P22b (Raspberry / Laptop / PC) y al subflujo U para registrar dónde irá el equipo. Se saltan P23 y E4. Con "No sé" se agrega la acción de confirmar el equipo con el cliente. |
| **Tablet en caja** | P22 = Tablet → ALERT *Raspberry obligatorio* (las impresoras deben ser de red) → P22a, igual que sin PC. |
| **Otro equipo en caja** | P22 = Otro → P22f (TEXT): ¿qué equipo hay en caja? Se trata como "No" y queda por confirmar con el cliente. |
| **Comanda solo en caja** | P12b = Sí y ningún área de preparación en P11 → ALERT *recomendar Sunmi con chip* (se cotiza con Desarrollo de Negocios). |
| **Sin internet** | P14 = No tiene o No sé (= sin internet) → se salta P15 (velocidad contratada) y P19 (speedtest E1); se agrega la acción de probar la señal del chip (Entel o Claro) con un celular. |
| **Impresora de preparación que no es de red por cable (R51)** | En P30 (impresora existente) o P32c (nueva): si el área es de preparación (Bar, Cocina, Pizza, Brasas, Jugos, Cafetería, Parrillas, Makis, Ramen, Panadería) y la conexión no es *Cable de red*, genera una alerta y la acción de cambiarla. Además la minuta lo muestra como regla *Impresoras de áreas de preparación de red por cable*. |
| **No hay impresoras (P26 = 0)** | Todas las áreas de P25 quedan sin impresora → ALERT → por cada área, P32. Si es *Nueva*: P32b, P32c y subflujo U, con fotos del punto de red y de la toma cercanos o del lugar donde se instalarán. |
| **No hay punto de red o toma cerca** | Subflujo U: pide dónde se instalará según el cliente (TEXT) y una 📷 foto del lugar (EU-RN / EU-EN). Genera `addAction`. |
| **Equipo sin toma de energía cerca (U5 = No)** | Agrega la acción "necesitará un punto de energía adicional o una extensión". |
| **Tomas lejos de los equipos de red (P33 = No)** | Subflujo U solo de energía, con `skipNearQuestion` porque P33 ya respondió que no hay. |
| **Áreas sin punto de red (P38 = No)** | P38a (MULTI_SELECT) → por cada área, subflujo U solo de red. |
| **Más de un piso** | L_PISOS pregunta el piso de cada área (P11a). P39a pregunta cómo pasa el cable entre pisos. El mapa agrupa los equipos por piso. |
| **Extensiones** | Si P36 = Sí: P36a (qué equipos están conectados) → más adelante P43 (ubicación) y 📷 E7. Genera la acción de pasar esos equipos a una toma directa o UPS. |
| **Segunda visita** | P50 (YES_NO). Si es Sí, P50a es un TEXT obligatorio con el motivo (mínimo 10 caracteres) y se agrega a Siguientes acciones. |

## 10. Contrato de API (implementado)

El motor real vive en `AsistenteRedIDBI-API/app/chat/flow_engine.py` (port de `motor_referencia.py`) y **no guarda estado en el servidor**: el cliente manda el `state` opaco que recibió en la respuesta anterior.

| Servicio | Endpoint | Uso |
|---|---|---|
| Gateway | `POST /api/evaluations/{id}/chat/start` | Inicia el chat (el gateway pone P06 fecha y P07 técnico) |
| Gateway | `POST /api/evaluations/{id}/chat/answer` | Responde el nodo actual (JSON) |
| Gateway | `POST /api/evaluations/{id}/chat/answer-photos` | Responde un nodo EVIDENCE (multipart `files` + `state`, hasta 3 fotos) |
| Gateway | `POST /api/evaluations/{id}/chat/amend` | Corrige datos extraídos de una evidencia o una aclaración |
| FastAPI | `POST /chat/start` · `/chat/answer` · `/chat/amend` | Lo llama solo el gateway |

Request (FastAPI): `ChatAnswerRequest { evaluationId, state, answer, photosBase64[] }`.

Respuesta: `ChatResponse { currentQuestionKey, currentQuestion, currentInputType, currentOptions, answeredQuestions, totalQuestions, progressPercent, completed, answers, proposal, state, … }`.

Reglas:
- Las respuestas se guardan con alcance, por ejemplo `P22#L_CAJAS:1`.
- Al completar, `answers["__state"]` lleva el estado completo y el gateway lo persiste en `evaluations.chat_answers_json`.
- **E9** (fotos generales) es obligatoria: `MANDATORY_EVIDENCE` en `flow_engine.py`.
- El contrato cambió respecto del chat de 23 nodos: un APK viejo no funciona con este backend. Despliega backend y APK juntos, y solo cuando el equipo lo decida.

## 11. Pistas para el mapa (`topology`)

- `{"node": "printer", "label": "Impresora {i} – {P27.labels}", "linkType": "$P30"}` → nodo `printer`. El enlace usa CABLE_RED, WIFI o USB_BLUETOOTH según la respuesta.
- `pending: true` → equipo por adquirir o instalar. Se dibuja con borde punteado en el mapa.
- `skipIf` → no se crea el nodo cuando se cumple la condición. Por ejemplo, si P22 = No hay.
- `groupBy: "piso"` → si P10 > 1, los nodos se agrupan por el piso del área.

## 12. Tabla completa de nodos

### A. Datos de la visita

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `P01` | TEXT | ¿Cuál es el nombre del cliente o razón social? | visita.cliente |  | `P02` |
| `P02` | TEXT | ¿Cuál es el nombre del local o sede? | visita.local |  | `P03` |
| `P03` | TEXT | ¿Cuál es la dirección del local? | visita.direccion |  | `P04` |
| `P04` | TEXT | ¿Quién es el contacto en el local? (nombre y cargo) | visita.contacto |  | `P05` |
| `P05` | TEXT | ¿Cuál es el teléfono del contacto? | visita.telefono |  | `P06` |
| `P06` | TEXT | Fecha de la visita | visita.fecha |  | `P07` |
| `P07` | TEXT | Técnico responsable | visita.tecnico |  | `P08` |
| `P08` | CHOICE | ¿Cuál es el motivo de la visita? | visita.motivo | Visita de implementación / Otro | si $P08 = "OTRO" → `P08a`; si no → `P09` |
| `P08a` | TEXT | ¿Cuál es el motivo de la visita? | visita.motivoOtro |  | `P09` |

### B. Negocio

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `P09` | CHOICE | ¿Qué tipo de negocio es? | negocio.tipo | Restaurante / Fast food / Cafetería / Bar / Discoteca / Minimarket / Retail / Hospedaje / Otro | `P10` |
| `P10` | NUMBER | ¿Cuántos pisos tiene el negocio? | negocio.pisos |  | `P11` |
| `P11` | MULTI_SELECT | ¿Qué áreas tiene el local? | negocio.areas | Salón 1 / Salón 2 / Terraza / Bar / Caja / Cocina / Despacho / Pizza / Brasas / Jugos / **Cafetería / Parrillas / Makis / Ramen / Panadería** / Almacén / Oficina / Otro | si $P10 > 1 → `L_PISOS`; si no → `P12` |
| `L_PISOS` | 🔁 LOOP | itemLabel: {item.label} |  |  | repite `P11a` por cada elemento de `$P11` → al terminar `P12` |
| `P11a` | CHOICE | ¿En qué piso está el área {item.label}? | negocio.areaPiso[{item.value}] | Piso 1 … Piso `$P10` | `@loop.next` |
| `P12` | NUMBER | ¿Cuántas cajas hay? | negocio.cajas |  | `P12b` |
| `P12b` | YES_NO | ¿En caja saldrán comandas? (pregunta fija para todos los negocios) | negocio.comandaEnCaja |  | si $P11 incluye un área de preparación → `P13`; si $P12b = "SI" → `A_SUNMI`; si no → `P13` |
| `A_SUNMI` | ℹ️ ALERT | La comanda sale solo en caja. Recomienda un Sunmi con chip (imprime comprobantes, pre-cuentas y comandas). Se compra al equipo de Desarrollo de Negocios. |  |  | `P13` |

### C. Internet

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `P13` | CHOICE | ¿Quién es el proveedor de internet? | internet.proveedor | Movistar / Claro / Entel / Win / Otro | `P14` |
| `P14` | CHOICE | ¿Qué tipo de conexión tiene? | internet.tipoConexion | Fibra óptica / Cable coaxial / No sé / No tiene / Internet con chip | si sin internet (No tiene o No sé) → `P14b`; si hay Sunmi (P12b = Sí y sin áreas de preparación) → `P14b`; si no → `P15` |
| `P14b` | CHOICE | Pon el chip en un celular dentro del local. ¿Qué chip tiene señal? | internet.senalChip | Entel / Claro / Ambos / Ninguno | si hay señal → `E1b`; si no y sin internet → `P16`; si no → `P15` |
| `E1b` | 📷 EVIDENCE **E1b** | Sube la captura del speedtest hecho desde el celular con el chip. | evidencias.speedtestChip | IA extrae: bajadaMbps, subidaMbps, pingMs, proveedor | sin internet → `P16`; si no → `P15` |
| `P15` | NUMBER | ¿Cuál es la velocidad contratada? (Mbps) | internet.velocidadContratada |  | `P16` |
| `P16` | YES_NO | ¿Hay una segunda línea de internet de respaldo? | internet.respaldo |  | `P17` |
| `P17` | YES_NO | ¿Hubo caídas de internet en el último mes? | internet.caidas |  | `P18` |
| `P18` | CHOICE | ¿En qué área está el router? | internet.ubicacionRouter | dinámico: `$P11` | si sin internet → `P20`; si no → `P19` |
| `P19` | 📷 EVIDENCE **E1** | Sube la captura del speedtest (hecho desde el local). | evidencias.speedtest | IA extrae: bajadaMbps, subidaMbps, pingMs, proveedor, servidor, fechaHora | `P20` |
| `P20` | 📷 EVIDENCE **E2** | Sube una foto del router donde se vea su ubicación. | evidencias.router | IA extrae: marca, modelo, ubicacionVisual, puertosLanTotales, puertosLanOcupados, puertosLanLibres (el técnico confirma o corrige) | `G_PUERTOS_E2` |
| `G_PUERTOS_E2` | ROUTER (sin UI) |  |  |  | si la IA leyó los puertos libres → `P21`; si no → `P18a` |
| `P18a` | NUMBER | No se pudieron leer los puertos en la foto del router. ¿Cuántos puertos LAN libres tiene? | internet.puertosLibresRouter |  | `P21` |
| `P21` | 📷 EVIDENCE **E3** | Sube una foto de la etiqueta del router o switch. | evidencias.etiquetaRouter | IA extrae: marca, modelo, numeroSerie, mac | `P22_GATE` |
| `P22_GATE` | ROUTER (sin UI) |  |  |  | si $P12 = 0 → `P25`; si no → `L_CAJAS` |

### D. Cajas (loop por caja)

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `L_CAJAS` | 🔁 LOOP | itemLabel: Caja {i} |  |  | repite `P22` por cada elemento de `$P12` → al terminar `P25` |
| `P22` | CHOICE | Caja {i}: ¿hay una PC o laptop? | cajas[{i}].equipo | Sí / No / Tablet / No sé / Otro | Sí → `P23`; Tablet → `A_RASPBERRY_TABLET`; Otro → `P22f`; No y No sé → `P22a` (la recomendación se decide al final del bloque E, en `G_IMPRESION`) |
| `P22f` | TEXT | Caja {i}: ¿qué equipo hay en caja? | cajas[{i}].equipoOtro |  | `P22a` |
| `A_RASPBERRY_TABLET` | ⚠️ ALERT | Caja {i}: la caja es una tablet. Es obligatorio un kit Raspberry para el controlador de impresiones y las impresoras deben ser de red. Cotízalo con Desarrollo de Negocios. |  |  | `P22a` |
| `P22a` | YES_NO | ¿El cliente ya cuenta con un Raspberry? | cajas[{i}].tieneRaspberry |  | si $P22a = "SI" → `P22c`; si no → `P22b` |
| `P22b` | CHOICE | ¿Qué adquirirá el cliente para el sistema de impresiones? | cajas[{i}].adquirira | Raspberry / Laptop / PC | `U_CAJA` |
| `P22c` | CHOICE | ¿Cómo se conecta el Raspberry a la red? | cajas[{i}].raspberryConexion | Cable de red / WiFi | `@loop.next` |
| `U_CAJA` | ↪ SUBFLOW U | params: area="CAJA", equipo="{P22b.label} Caja {i}", needsNetwork=true, needsPower=true |  |  | `@loop.next` |
| `P23` | CHOICE | Caja {i}: ¿la computadora va por cable o por WiFi? | cajas[{i}].conexion | Cable de red / WiFi | `P24` |
| `P24` | 📷 EVIDENCE **E4** | Caja {i}: sube la captura de ipconfig. | evidencias.ipconfig[{i}] | IA extrae: ipv4, mascara, puertaEnlace, adaptador, mac | `@loop.next` |

### E. Impresoras

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `P25` | MULTI_SELECT | ¿Qué áreas necesitan imprimir comandas o tickets? | impresion.areas | dinámico: `$P11` | `P26` |
| `P26` | NUMBER | ¿Cuántas impresoras hay actualmente? | impresion.cantidad |  | si $P26 = 0 → `P32_GATE`; si no → `L_IMPRESORAS` |
| `L_IMPRESORAS` | 🔁 LOOP | itemLabel: Impresora {i} |  |  | repite `P27` por cada elemento de `$P26` → al terminar `P32_GATE` |
| `P27` | MULTI_SELECT | Impresora {i}: ¿qué áreas imprimen en esta impresora? | impresoras[{i}].areas | dinámico: `$derived.areasSinImpresora` | `P28` |
| `P28` | CHOICE | Impresora {i}: ¿en qué área está físicamente? | impresoras[{i}].ubicacion | dinámico: `$P27` + Otra | `P29` |
| `P29` | CHOICE | Impresora {i}: ¿cuál es la marca? | impresoras[{i}].marca | Epson / Bixolon / Star / Otro | `P30` |
| `P30` | CHOICE | Impresora {i}: ¿cómo se conecta? | impresoras[{i}].conexion | Cable de red / USB / WiFi / Bluetooth | `P31` (si es de un área de preparación y no es de red por cable: alerta R51) |
| `P31` | 📷 EVIDENCE **E5** | Impresora {i}: sube el ticket de autotest o la etiqueta. | evidencias.impresora[{i}] | IA extrae: marca, modelo, numeroSerie, ip, mac | `@loop.next` |
| `P32_GATE` | ROUTER (sin UI) |  |  |  | si $derived.areasSinImpresora no vacío → `A_SIN_IMPRESORA`; si no → `G_IMPRESION` |
| `A_SIN_IMPRESORA` | ⚠️ ALERT | Estas áreas no tienen impresora asignada: {derived.areasSinImpresora.labels}. |  |  | `L_AREAS_SIN_IMP` |
| `L_AREAS_SIN_IMP` | 🔁 LOOP | itemLabel: {item.label} |  |  | repite `P32` por cada elemento de `$derived.areasSinImpresora` → al terminar `G_IMPRESION` |
| `P32` | CHOICE | {item.label}: ¿cómo se resolverá la impresión? | impresionPendiente[{item.value}].solucion | Compartirá una impresora existente / Se instalará una impresora nueva | si $P32 = "COMPARTIR" → `P32a`; si no → `P32b` |
| `P32a` | CHOICE | {item.label}: ¿con qué impresora compartirá? | impresionPendiente[{item.value}].impresora | dinámico: `$registry.printers` | `@loop.next` |
| `P32b` | CHOICE | {item.label}: ¿en qué área se colocará la impresora nueva? | impresionPendiente[{item.value}].ubicacion | dinámico: `$P11` | `P32c` |
| `P32c` | CHOICE | ¿Cómo se conectará la impresora nueva? | impresionPendiente[{item.value}].conexion | Cable de red / USB / WiFi | `U_IMPRESORA` (si el área es de preparación y no es cable de red: alerta R51) |
| `U_IMPRESORA` | ↪ SUBFLOW U | params: area="$P32b", equipo="Impresora nueva ({item.label})", needsNetwork={"eq": ["$P32c", "CABLE_RED"]}, needsPower=true |  |  | `@loop.next` |
| `G_IMPRESION` | ROUTER (sin UI) |  |  |  | sin PC y sin comandas (P12b = No y sin áreas de preparación) → `A_IMP_PC_BASICA`; sin PC y la impresora de caja es USB → `A_IMP_PC_CONTROLADOR`; sin PC y con comandas → `A_IMP_RASPBERRY`; si no → `G_PUERTOS` |
| `A_IMP_RASPBERRY` | ⚠️ ALERT | No hay PC ni laptop en caja y las impresoras son de red. Recomienda un kit Raspberry para el controlador de impresiones. Cotízalo con Desarrollo de Negocios. |  |  | `G_PUERTOS` |
| `A_IMP_PC_CONTROLADOR` | ⚠️ ALERT | La impresora de caja es USB y solo funciona con laptop o PC, no con Raspberry. Recomienda una laptop o PC para el controlador de impresiones. |  |  | `G_PUERTOS` |
| `A_IMP_PC_BASICA` | ℹ️ ALERT | No hay áreas de preparación ni comanda. Recomienda una laptop o PC básica con USB para pre-cuentas y comprobantes; sin controlador. |  |  | `G_PUERTOS` |
| `G_PUERTOS` | ROUTER (sin UI) |  |  |  | si faltan puertos ($derived.faltanPuertos > 0) → `A_FALTAN_PUERTOS`; si no → `P33` |
| `A_FALTAN_PUERTOS` | ⚠️ ALERT | El router tiene {derived.puertosLibresRouter} puertos libres y hay {derived.equiposCableados} equipos por cable. Recomienda un switch de {derived.tamanoSwitch} puertos e implementar {derived.puntosRedFaltantes} puntos de red. |  |  | `U_SWITCH` |
| `U_SWITCH` | ↪ SUBFLOW U | params: area="$P18", equipo="Switch nuevo", needsNetwork=false, needsPower=true |  |  | `P33` |

### F. Energía

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `P33` | YES_NO | ¿Hay tomas de energía cerca de los equipos de red? | energia.tomasCerca |  | si $P33 = "NO" → `U_ENERGIA`; si no → `P34` |
| `U_ENERGIA` | ↪ SUBFLOW U | params: area="$P18", equipo="Router / equipos de red", needsNetwork=false, needsPower=true, skipNearQuestion=true |  |  | `P34` |
| `P34` | YES_NO | ¿Los equipos comparten una misma toma? | energia.tomaCompartida |  | `P35` |
| `P35` | YES_NO | ¿Se cuenta con energía de respaldo, como un UPS? | energia.ups |  | `P36` |
| `P36` | YES_NO | ¿Hay extensiones donde va conectado algún equipo de red o impresora? | energia.extensiones |  | si $P36 = "SI" → `P36a`; si no → `P37` |
| `P36a` | MULTI_SELECT | ¿Qué equipos están conectados a la extensión? | energia.extensionEquipos | Router / Switch / Access point / Impresora / PC / Raspberry | `P37` |

### G. Cableado

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `P37` | NUMBER | ¿Cuántos puntos de red hay? | cableado.puntos |  | `P38` |
| `P38` | YES_NO | ¿Los puntos de red llegan a todas las áreas que se necesitan? | cableado.cobertura |  | si $P38 = "NO" → `P38a`; si no → `P39` |
| `P38a` | MULTI_SELECT | ¿Qué áreas no tienen punto de red? | cableado.areasSinPunto | dinámico: `$P11` | `L_AREAS_SIN_PUNTO` |
| `L_AREAS_SIN_PUNTO` | 🔁 LOOP | itemLabel: {item.label} |  |  | repite `U_RED` por cada elemento de `$P38a` → al terminar `P39` |
| `U_RED` | ↪ SUBFLOW U | params: area="{item.value}", equipo="Punto de red – {item.label}", needsNetwork=true, needsPower=false, skipNearQuestion=true |  |  | `@loop.next` |
| `P39` | CHOICE | ¿Qué cable de red usan? | cableado.tipoCable | Interior Cat5e / Interior Cat6 / Cable exterior / No sé | si $P10 > 1 → `P39a`; si no → `P40` |
| `P39a` | CHOICE | ¿Cómo pasa el cable de red entre pisos? | cableado.entrePisos | Por ducto o canaleta / Expuesto por pared o escalera / No pasa, el otro piso va por WiFi / No sé | `P40` |
| `P40` | CHOICE | ¿Cómo está el gabinete? | cableado.gabinete | No hay / Ordenado / Ordenado y rotulado / Desordenado o con cables | `P41` |
| `P41` | YES_NO | ¿Algún equipo está expuesto a calor o grasa? | cableado.calorGrasa |  | `P42` |
| `P42` | 📷 EVIDENCE **E6** | Sube fotos de los puntos de red y las tomas. | evidencias.puntosTomas | IA extrae: estadoCableado, rotulado, observaciones | si $P36 = "SI" → `P43`; si no → `P45` |
| `P43` | CHOICE | ¿Dónde está la extensión de energía? | energia.extensionUbicacion | dinámico: `$P11` | `P44` |
| `P44` | 📷 EVIDENCE **E7** | Sube una foto de la extensión de energía. | evidencias.extension | IA extrae: equiposConectados, estado, riesgo | `P45` |

### H. Red inalámbrica y dispositivos

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `P45` | YES_NO | ¿Hay access points o repetidores? | wifi.accessPoints |  | `P46` |
| `P46` | MULTI_SELECT | ¿Hay zonas sin señal WiFi? | wifi.zonasSinSenal | dinámico: `$P11` + Ninguna | si hay zonas sin señal → `P46a`; si no → `P47` |
| `P46a` | CHOICE | Sugerencia: {derived.sugerenciaWifi}. ¿Qué se instalará en las zonas sin señal? | wifi.solucion | Repetidor / Access point / Ambos | `L_ZONAS_SIN_SENAL` |
| `L_ZONAS_SIN_SENAL` | 🔁 LOOP | itemLabel: {item.label} |  |  | repite `U_WIFI` por cada zona de `$derived.zonasSinSenal` → al terminar `P47` |
| `U_WIFI` | ↪ SUBFLOW U | params: area="{item.value}", equipo="Repetidor/AP – {item.label}", needsNetwork=(solo la 1.ª zona), needsPower=true |  |  | `@loop.next` |
| `P47` | YES_NO | ¿Hay cámaras de seguridad conectadas a la red? | red.camaras |  | `P48` |
| `P48` | 📷 EVIDENCE **E8** | Sube la captura del escáner de IP. | evidencias.escanerIp | IA extrae: dispositivos[ip, mac, fabricante, nombre] | `P49` |

### I. Cierre

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `P49` | 📷 EVIDENCE **E9** | Sube fotos generales del local. | evidencias.generales | IA extrae: descripcion | `P50` |
| `P50` | YES_NO | ¿Considerar agendar una segunda visita? | cierre.segundaVisita |  | si $P50 = "SI" → `P50a`; si no → `P51` |
| `P50a` | TEXT | ¿Por qué se debe agendar una segunda visita? | cierre.segundaVisitaMotivo |  | `P51` |
| `P51` | TEXT | ¿Quiénes asistieron a la visita? | cierre.asistentes |  | `P52` |
| `P52` | TEXT | Acuerdos y siguientes acciones | cierre.acuerdos |  | `P53` |
| `P53` | TEXT | Observaciones adicionales | cierre.observaciones |  | `P54` |
| `P54` | SUMMARY | Resumen de respuestas y evidencias agrupadas por área. Confirmar para generar minuta y mapa. |  |  | `END` |

### U. Subflujo U – Ubicación de equipo o punto nuevo

Parámetros: `area`, `equipo`, `needsNetwork`, `needsPower`, `skipNearQuestion`.

| ID | Tipo | Texto | Campo | Opciones / extracción | Siguiente |
|---|---|---|---|---|---|
| `U0` | ROUTER (sin UI) |  |  |  | si $ctx.needsNetwork = true y $ctx.skipNearQuestion = true → `U3`; si $ctx.needsNetwork = true → `U1`; si no → `U5_GATE` |
| `U1` | YES_NO | {ctx.equipo}: ¿hay un punto de red cerca de donde irá el equipo? | ubicaciones[{ctx.key}].redCerca |  | si $U1 = "SI" → `U2`; si no → `U3` |
| `U2` | 📷 EVIDENCE **EU-R** | Sube una foto del punto de red cercano ({ctx.area.label}). | ubicaciones[{ctx.key}].fotoPuntoRed | IA extrae: estado, rotulado | `U5_GATE` |
| `U3` | TEXT | {ctx.equipo}: ¿dónde se instalará el punto de red en {ctx.area.label}, según el cliente? | ubicaciones[{ctx.key}].ubicacionPuntoRedNuevo |  | `U4` |
| `U4` | 📷 EVIDENCE **EU-RN** | Sube una foto del lugar donde se instalará el punto de red ({ctx.area.label}). | ubicaciones[{ctx.key}].fotoLugarPuntoRed | IA extrae: descripcion | `U5_GATE` |
| `U5_GATE` | ROUTER (sin UI) |  |  |  | si $ctx.needsPower = true y $ctx.skipNearQuestion = true → `U7`; si $ctx.needsPower = true → `U5`; si no → `@return` |
| `U5` | YES_NO | {ctx.equipo}: ¿hay una toma de energía cerca de donde irá el equipo? | ubicaciones[{ctx.key}].energiaCerca |  | si $U5 = "SI" → `U6`; si no → `U7` |
| `U6` | 📷 EVIDENCE **EU-E** | Sube una foto de la toma cercana ({ctx.area.label}). | ubicaciones[{ctx.key}].fotoToma | IA extrae: estado, tipoToma | `@return` |
| `U7` | TEXT | {ctx.equipo}: ¿dónde se instalará la toma de energía en {ctx.area.label}, según el cliente? | ubicaciones[{ctx.key}].ubicacionTomaNueva |  | `U8` |
| `U8` | 📷 EVIDENCE **EU-EN** | Sube una foto del lugar donde se instalará la toma ({ctx.area.label}). | ubicaciones[{ctx.key}].fotoLugarToma | IA extrae: descripcion | `@return` |

## 13. Evidencias

| Código | Nodo | Qué se pide | La IA extrae |
|---|---|---|---|
| **E1** | `P19` | Sube la captura del speedtest (hecho desde el local). | bajadaMbps, subidaMbps, pingMs, proveedor, servidor, fechaHora |
| **E1b** | `E1b` | Sube la captura del speedtest hecho desde el celular con el chip. (Solo sin internet o con Sunmi.) | bajadaMbps, subidaMbps, pingMs, proveedor |
| **E2** | `P20` | Sube una foto del router donde se vea su ubicación. | marca, modelo, ubicacionVisual, puertosLanTotales, puertosLanOcupados, puertosLanLibres |
| **E3** | `P21` | Sube una foto de la etiqueta del router o switch. | marca, modelo, numeroSerie, mac |
| **E4** | `P24` | Caja {i}: sube la captura de ipconfig. | ipv4, mascara, puertaEnlace, adaptador, mac |
| **E5** | `P31` | Impresora {i}: sube el ticket de autotest o la etiqueta. | marca, modelo, numeroSerie, ip, mac |
| **E6** | `P42` | Sube fotos de los puntos de red y las tomas. | estadoCableado, rotulado, observaciones |
| **E7** | `P44` | Sube una foto de la extensión de energía. | equiposConectados, estado, riesgo |
| **E8** | `P48` | Sube la captura del escáner de IP. | dispositivos[ip, mac, fabricante, nombre] |
| **E9** | `P49` | Sube fotos generales del local. | descripcion |
| **EU-R** | `U2` | Sube una foto del punto de red cercano ({ctx.area.label}). | estado, rotulado |
| **EU-RN** | `U4` | Sube una foto del lugar donde se instalará el punto de red ({ctx.area.label}). | descripcion |
| **EU-E** | `U6` | Sube una foto de la toma cercana ({ctx.area.label}). | estado, tipoToma |
| **EU-EN** | `U8` | Sube una foto del lugar donde se instalará la toma ({ctx.area.label}). | descripcion |

Las evidencias EU-* se repiten por cada ubicación. Cada una se guarda con `area` y `equipo` (`tagWith`). En el resumen final se agrupan por área.

## 14. Criterios de aceptación (tests)

Escenarios que deben seguir pasando:

1. **Rock & Burgers.** Áreas: Caja, Cocina, Bar, Jugos. Hay 2 impresoras: IMP1 para Caja e IMP2 compartida por Bar y Jugos. Cocina queda sin impresora, así que se instala una nueva por cable, sin punto de red cerca y con toma cerca. Sin UPS. Una impresora conectada a una extensión. Se agenda segunda visita.
   - Esperado: impresoras `IMP1 – Caja`, `IMP2 – Bar + Jugos`, `IMP3 – Cocina`.
   - Evidencias: E1, E2, E3, E4, E5×2, EU-RN, EU-E, E6, E7, E8, E9.
   - 6 acciones: impresora nueva, punto de red, UPS, extensión, segunda visita y controlador de impresiones en la PC de caja.
2. **Sin PC en caja, 0 impresoras, 2 pisos.** Caja = No; el cliente adquirirá un Raspberry. Bar tendrá impresora nueva por WiFi y Jugos la compartirá. No hay tomas ni puntos de red cerca.
   - Esperado: alerta Raspberry, alerta de áreas sin impresora, `IMP1 – Bar + Jugos`.
   - Para la impresora WiFi no se pregunta por punto de red.
   - Se pregunta el piso de cada área (P11a) y P39a.
3. **Sin cajas (P12 = 0).** Se salta todo el bloque D (P12b se pregunta igual).
4. **Comandas, internet y equipo de caja (2026-10-09).** Tablet y Otro en P22; Sunmi con comanda solo en caja; sin internet salta P15 y E1; impresora de preparación por USB o WiFi genera la alerta R51 y la regla en la minuta. Pruebas al final de `tests/test_flow_engine.py`.
5. **Rock & Burgers tiene ahora 6 acciones**: las 5 de antes más *instalar el controlador de impresiones en la PC de caja* (hay áreas de preparación).

Además:
- Validación estática: todos los `goto` existen y todos los nodos son alcanzables desde `P01`.
- Ningún CHOICE o MULTI_SELECT queda sin opciones.

## 15. Cambios de octubre de 2026 (comandas, internet, puertos, impresión y WiFi)

| Cambio | Dónde |
|---|---|
| **P22** pasa a *Sí / No / Tablet / No sé / Otro*. Con Otro se pregunta qué equipo es (P22f). Tablet exige Raspberry. No sé y Otro se tratan como "No" y quedan por confirmar. | `P22`, `P22f`, `A_RASPBERRY_TABLET` |
| **Comanda en caja** pasa a ser pregunta fija para todos los negocios (**P12b**). Sin áreas de preparación y con comanda en caja: Sunmi. | `P12b`, `A_SUNMI` |
| **Regla R51:** si un área de preparación necesita impresora, tiene que ser de red por cable. Genera alerta y acción, y una regla en la minuta. | efectos de `P30` y `P32c` |
| **P14** pasa a *Fibra óptica / Cable coaxial / No sé / No tiene / Internet con chip*. Sin internet se salta P15 y E1. | `P14`, `P18` |
| **Prueba de la señal del chip:** si no hay internet, o si va un Sunmi, el técnico prueba el chip con un celular (P14b) y sube el speedtest con chip (E1b). Sin señal: alerta de internet satelital y segunda visita. | `P14b`, `E1b` |
| **Puertos del router:** la IA cuenta los puertos LAN en la foto E2 y el técnico los confirma; si no se pueden leer, P18a. | `P20`, `G_PUERTOS_E2`, `P18a` |
| **Decisión de impresión** al final del bloque E: Raspberry, laptop o PC con controlador, o PC básica. Se quitó la alerta de Raspberry que salía en cada caja sin PC. | `G_IMPRESION`, `A_IMP_*` |
| **Switch y puntos de red:** si hay más equipos por cable que puertos libres, recomienda un switch (5, 8, 16 o 24 puertos) y cuántos puntos de red implementar. | `G_PUERTOS`, `A_FALTAN_PUERTOS`, `U_SWITCH` |
| **WiFi por zona:** repetidor o access point (el primero con red y energía; los siguientes solo energía). | `P46a`, `L_ZONAS_SIN_SENAL`, `U_WIFI` |
| **Aviso de viabilidad** en la minuta (resumen ejecutivo y siguientes acciones) cuando hay recomendaciones. | `minuta_doc.py`, plantilla del PDF |
| **Controlador de impresiones:** si hay comandas y hay PC o laptop en caja, se agrega la acción de instalarlo. | efecto de `P23` |
| **Áreas de preparación nuevas** en P11: Cafetería, Parrillas, Makis, Ramen, Panadería. | `areas`, `prepAreas` |
| El motor suma `includesAny`, `$item`, `$prepAreas`, `$loop.index` y los valores derivados `necesitaComandas`, `sinInternet`, `sinPcEnCaja`, `conexionImpresoraCaja`, `puertosLibresRouter`, `equiposCableados`, `faltanPuertos`, `tamanoSwitch`, `puntosRedFaltantes`, `zonasSinSenal` y `sugerenciaWifi`. | `flow_engine.py` |

Valores guardados: P22 ahora es `SI/NO/TABLET/NO_SE/OTRO` (antes `PC/LAPTOP/NO_HAY`) y P14 es `FIBRA/COAXIAL/NO_SE/NO_TIENE/CHIP` (antes `FIBRA/COBRE/INALAMBRICA/NO_SE`). Las evaluaciones guardadas con los valores anteriores se siguen leyendo bien (minuta, mapa y reglas).

Sigue sin hacerse: las tablas `kb_*` y el importador del Excel de minutas manuales (ver `../base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md`). El motor de recomendaciones todavía usa las reglas fijas del código, no la base de conocimiento.
