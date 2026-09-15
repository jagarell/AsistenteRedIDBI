# Contexto del proyecto — Asistente Red IDBI ("Analista IA")

Última actualización: 2026-09-14. Este doc resume el estado real del proyecto
para retomarlo en otra sesión sin tener que re-descubrir todo de cero.

## Arquitectura (3 repos)

- **`AsistenteRedIDBI`** (este repo) — app Android nativa (Kotlin, MVVM +
  Hilt + Retrofit/Moshi + Navigation Component). `applicationId`
  `com.upc.asistenteredidbi`.
- **`idbi-api-gateway`** (`~/IdeaProjects/idbi-api-gateway`) — backend Spring
  Boot (puerto 8080), Postgres (`asistente_red_idbi`, usuario `idbi_user`).
  Es la ÚNICA fuente de verdad de negocio: auth/JWT, evaluaciones, minutas,
  perfil, notificaciones push, PDF/envío de propuesta.
- **`idbi-fastapi`** (`~/IdeaProjects/idbi-fastapi`) — microservicio Python
  FastAPI (puerto 8000). Solo dos responsabilidades: el motor de chat
  técnico de 23 nodos (`app/chat/`) y el análisis de fotos por visión
  (`/analyze-photo`, OpenAI Vision). El gateway le hace de proxy.

El Android habla siempre con el gateway (`10.0.2.2:8080` desde el emulador);
el gateway es el único que le habla a FastAPI (`FASTAPI_BASE_URL`).

## Cómo levantar todo para seguir trabajando

```bash
# 1) Postgres ya corre como servicio de Postgres.app, no hace falta nada.

# 2) Gateway — el .env con credenciales YA EXISTE en el repo (gitignored),
#    no hay que regenerarlo salvo que se haya perdido.
cd ~/IdeaProjects/idbi-api-gateway
set -a; source .env; set +a
./mvnw spring-boot:run

# 3) FastAPI
cd ~/IdeaProjects/idbi-fastapi
./venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
```

**Importante**: cada vez que el gateway se reinicia, si en algún momento se
regenera `JWT_SECRET` en `.env`, todas las sesiones guardadas en la app
(tokens viejos) quedan inválidas (403 silencioso). Si algo que antes andaba
empieza a devolver 403 en todo, lo primero a probar es cerrar sesión y
volver a entrar.

## Decisiones de arquitectura tomadas

- **El mismatch `/api/v1/...` vs rutas reales del gateway**: el Android
  tenía toda una capa (`EvaluationApiService`, partes de `ProfileApiService`)
  apuntando a rutas `/api/v1/...` en snake_case que nunca existieron en
  ningún backend (parecen diseñadas para un tercer servicio Python que se
  abandonó — quedan tablas huérfanas en Postgres como evidencia: `locals`,
  `technical_evaluations`, `network_devices`, etc., sin código que las
  use). Se decidió **no** construir ese backend paralelo: se repuntó Android
  a las rutas reales del gateway donde hacía falta (perfil). Gran parte de
  ese código roto (`AssistantHomeViewModel` original, `HistorialViewModel`,
  `ListEvaluationsUseCase`) resultó ser código muerto — ninguna pantalla real
  lo llamaba — así que no bloqueaba nada, solo estaba mal.
- **Nodos del chat sin UI real** (fotos, coordenadas GPS): en vez de
  construir UI para algo que no la tenía, se cambiaron de estrategia:
  - "Adjunta fotos" → nodo de confirmación Sí/No (la carga de fotos real
    vive en Evidencias Técnicas).
  - "Coordenadas GPS" → se resuelve solo (`auto=True` en `nodes.py`),
    geocodificando nombre+dirección vía Nominatim/OpenStreetMap (gratis, sin
    API key), sin preguntarle nada al técnico.

## Estado de features (real vs. pendiente)

**Reales y funcionando de punta a punta:**
- Auth (registro/login, JWT).
- Chat técnico de 23 nodos → genera propuesta + topología + score reales.
- **Motor de propuesta AS-IS/TO-BE, ahora UNIFICADO** (2026-09-08/09 y
  2026-09-14, sin OpenAI): el chat de 23 nodos separa un diagnóstico
  **AS-IS** (`asIsFindings`) de la propuesta **TO-BE** (`recommendations`),
  razonando con umbrales reales de ingeniería (límite de cobre TIA/EIA-568 a
  100m, cobertura de AP según m²/material de pared, demanda de ancho de banda
  vs. plan contratado, presupuesto PoE, redundancia por cantidad de POS) —
  constantes documentadas y ajustables al inicio de
  `idbi-fastapi/app/chat/proposal.py` y `app/chat/topology.py` (valores
  estándar de industria, no el catálogo real de IDBI). **Desde el
  2026-09-14 ya no hay dos motores separados**: `app/analysis.py` (el que
  alimenta `/analyze` y la tarjeta "Recomendaciones IA" de la pantalla
  principal de propuesta) llama internamente a
  `chat.proposal.generate_proposal()` para el score/AS-IS/TO-BE, y solo
  agrega el desglose por 4 áreas (Conectividad/Infraestructura/Equipamiento/
  WiFi) que esa pantalla necesita. Una sola fuente de verdad de punta a
  punta, verificada por curl.
- **Respuestas del chat persistidas server-side** (2026-09-14): al
  completarse el chat, el gateway guarda el `Map<String,String>` de
  respuestas en `evaluations.chat_answers_json`
  (`ChatService.persistAnswers`). Esto arregló un bug real: la pantalla
  principal de propuesta llamaba `/analysis` sin respuestas en cada carga
  (`MinutaViewModel.loadAnalysis()`), pisando en silencio el análisis real ya
  calculado en Evidencias con uno genérico — ahora `AnalysisService` cae a
  las respuestas persistidas en vez de a un mapa vacío.
- **Identificación de equipos por foto** (2026-09-14): al subir una foto de
  un equipo (router/switch/pos/printer/camera/computer/access_point),
  `app/vision.py` le pide a OpenAI Vision marca/modelo en JSON estructurado
  (antes solo devolvía texto libre). Si detecta algo, queda guardado en
  `evidence_photos.detected_brand/model` (gateway) y se contrasta contra lo
  autorreportado en el chat dentro de `_compute_as_is()` — ej. "el técnico
  reportó X pero la foto muestra Y, verificar". Requiere `OPENAI_API_KEY`
  real para probarse de punta a punta (pendiente, ver sección de
  credenciales); la lógica de merge y contraste se verificó simulando el
  resultado de visión directo en Postgres.
- Minutas (crear/completar/validar).
- Análisis IA (pantalla post-Evidencias): calculado de verdad a partir de
  las respuestas del chat, no hardcodeado — motor unificado (ver arriba).
- Topología: íconos por tipo de dispositivo, se centra sola, tap para
  pantalla completa con pinch-zoom.
- **Checklist dinámico de Evidencias Técnicas, reconstruido de cero**
  (2026-09-14): reemplaza el viejo flujo fijo de 7 categorías. Al entrar a
  Evidencias, el backend siembra automáticamente áreas (una por zona WiFi
  del chat + "Rack / Router" + "Plano y Topología" fijas) y equipos (uno por
  tipo presente según las cantidades reales del chat) — ver
  `app/chat/checklist.py` (FastAPI) y `EvidenceChecklistService` (gateway,
  paquete `evidence.checklist`, 11 endpoints en
  `/api/v1/evaluations/{id}/evidence/...` + `/minuta`). Fase A (selección
  libre: agregar/quitar áreas o equipos a mano) → "Confirmar selección" →
  Fase B (cada ítem necesita ≥1 foto, multi-foto permitido, botón "Analizar
  con IA" se habilita solo cuando todos tienen foto). Verificado de punta a
  punta por curl: sembrado real, agregar área custom, bloquear, subir foto,
  `GET minuta` con las 21 respuestas reales + tabla de equipos con conteo de
  fotos. El flujo viejo (`EvidenceUploadApiService`/`EvidenceController` del
  gateway) se borró — ya no existen dos pipelines de evidencia compitiendo.
- Generación de PDF de la propuesta (Apache PDFBox) y envío por correo con
  el PDF adjunto — el código es real; el envío falla explícito si
  `MAIL_ENABLED=false` (default) en vez de simular éxito.
- Home: nombre/empresa/stats reales (`GET /api/profile/me`), lista de
  "Recientes" con minutas reales.
- Perfil: nombre/correo reales, "Cerrar sesión" funcional.
- Notificaciones push (FCM): infraestructura completa en Android + gateway.

**Pendiente — requiere que el usuario provea credenciales externas:**
- **Firebase (push)**: crear proyecto en Firebase Console → descargar
  `app/google-services.json` (Android) y una clave de cuenta de servicio →
  `FIREBASE_CREDENTIALS_PATH` en el `.env` del gateway.
- **SMTP (envío de correo real)**: `MAIL_HOST`/`MAIL_USERNAME`/
  `MAIL_PASSWORD` + `MAIL_ENABLED=true` en el `.env` del gateway.
- **OpenAI (visión de fotos + identificación de marca/modelo)**:
  `OPENAI_API_KEY` en el `.env` de `idbi-fastapi` para que el análisis de
  fotos en Evidencias deje de decir "no configurado" y para que la
  extracción de marca/modelo (2026-09-14) funcione de verdad.
- **Validación de RUC/negocio (SUNAT vía APIs Peru)**: `RUC_VALIDATION_API_KEY`
  + `RUC_VALIDATION_ENABLED=true` en el `.env` del gateway
  (`RucValidationService`, 2026-09-14). Apagado por defecto: solo corre
  validación de formato local (nombre no vacío/no solo números). El usuario
  aún no tiene cuenta en apis.net.pe — queda preparado, no simula éxito.

**Pendiente de verificación manual (2026-09-14)**: todo lo de hoy se probó
por curl/psql contra los backends reales y los 3 repos compilan, pero
**nadie recorrió a mano en el emulador** la nueva UI del checklist dinámico
de Evidencias (diálogos de agregar área/equipo, captura Fase B) ni los fixes
de interacción del chat (texto que se limpiaba solo, feedback visual en
botones Sí/No/opción). Antes de dar por cerrado hay que abrir la app, crear
una evaluación, completar el chat, y probar Evidencias de punta a punta a
mano. El plan completo con el detalle de las 6 piezas de esta sesión queda
en `~/.claude/plans/spicy-imagining-falcon.md` si hace falta repasar el
diseño.

**Código muerto conocido (no bloquea nada, pero confunde si se lee)**:
`HistorialViewModel`/`EvaluationSummaryItem`/`EvaluationFilters` (Android) y
`ProposalController` (gateway, `/api/evaluations/{id}/proposal`, sigue
100% hardcodeado — nada lo llama). `HistorialFragment` real usa datos mock
propios, no ese ViewModel.

## Gotchas de infraestructura local

- Las tablas de Postgres deben ser propiedad de `idbi_user` (no del usuario
  de macOS) para que `ddl-auto: update` de Hibernate pueda migrar el
  esquema. Si un día vuelve el error `must be owner of table`, correr:
  `REASSIGN OWNED BY "<tu_usuario_mac>" TO idbi_user;` conectado a la base
  `asistente_red_idbi`.
- **Puerto 8080 puede estar ocupado por otro proyecto (Voya)**: el usuario
  tiene otro proyecto Android Studio, `~/AndroidStudioProjects/Voya/backend`
  (Spring Boot), que también levanta en el puerto 8080 por defecto
  (`pe.voya.backend.VoyaBackendApplication`). Si quedó corriendo de una
  sesión anterior, la app le pega a Voya sin darse cuenta y **todos** los
  endpoints — incluso los públicos como `/api/auth/login` — devuelven
  `403 Forbidden` sin cuerpo, pareciendo un bug de seguridad del gateway
  cuando en realidad `SecurityConfig.java` está bien. Antes de debuggear
  cualquier "no tengo permisos"/403 inesperado, correr
  `lsof -i :8080 -sTCP:LISTEN` y confirmar que el proceso es
  `IdbiApiGatewayApplication`, no `VoyaBackendApplication`; si no, matarlo y
  relevantar el gateway real.
