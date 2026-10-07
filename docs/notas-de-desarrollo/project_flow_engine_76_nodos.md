---
name: project-flow-engine-76-nodos
description: "Nuevo motor de chat de 76 nodos (loops/subflujo U/evidencias IA) + minuta PDF HTML→PDF; arquitectura, trampas y cómo se probó (2026-10-02)"
metadata:
  node_type: memory
  type: project
---

El chat técnico dejó de ser una lista plana de 22 nodos: ahora ejecuta `flujo_asistente_red.json` (spec del cliente, 76 nodos, loops por caja/impresora/área, subflujo U, avisos, 13 evidencias con IA, resumen final).

**Arquitectura (3 repos):**
- FastAPI `app/chat/flow_engine.py` = port reanudable de `motor_referencia.py`. Sigue SIN estado de servidor: el cliente manda `state` (JSON opaco) + `answer`; recibe el siguiente nodo en `node`. Respuestas con alcance `P22#L_CAJAS:1`. Evidencias en `app/chat/evidence.py` (E3 redacta credenciales con Pillow; sin IA se pixela completa), reglas/crossChecks en `checks.py`, documento de minuta en `minuta_doc.py`, mapa PNG en `map_render.py` (fuentes DejaVu incluidas: la fuente por defecto de Pillow no trae tildes).
- `legacy_adapter.py` deriva las claves viejas (`establishment_name`, `wifi_zones`, `pos_count`…) para que proposal/topology/checklist/analysis sigan igual; `answers["__state"]` lleva el estado completo al completarse (el gateway lo persiste en `Evaluation.chatAnswersJson`).
- Gateway: `/chat/answer` (JSON) y `/chat/answer-photos` (multipart `files`+`state`, hasta 3 fotos); guarda fotos en `Evidence` (columnas nuevas evidence_code/chat_scope/...); minuta PDF con Thymeleaf + OpenHTMLtoPDF en `MinutaPdfService` (`GET /api/evaluations/{id}/minuta/pdf`, `POST .../minuta/send`).
- Android: chat reescrito (tarjetas de evidencia/resultado/aviso/resumen, galería, "Sí, es correcto/Cambiar foto", Omitir). Tras completar va directo a `minutaFragment` (se salta el checklist de evidencias viejo, redundante).

**Why:** el cliente pidió preguntas e interfaz idénticas al prototipo y la minuta PDF de Rock & Burgers.
**How to apply:** el contrato HTTP cambió (APK viejo NO funciona con el backend nuevo): desplegar backend + APK juntos.

**Cómo se probó en local (sin tocar producción):** gateway con H2: `JWT_SECRET=... DB_URL=jdbc:h2:mem:devdb;MODE=PostgreSQL DB_USERNAME=sa DB_PASSWORD=x SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver JPA_DDL_AUTO=create-drop SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect ./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true`; FastAPI con IA simulada (sin OPENAI_API_KEY local) envolviendo `engine.analyze_evidence` con los valores de `tests/test_rock_sample.py`; APK apuntando temporalmente a `http://10.0.2.2:8080/` (BASE_URL en AuthModule.kt — REVERTIR antes de commitear; el manifest ya permite cleartext).

**Comparar contra el prototipo (2026-10-02):** el prototipo (`claude.ai/artifact/SypJGogTsemziqaiBUwXyE`) son 38 capturas webp (720x1600) + hotspots; se bajan con la herramienta Artifact (`read` con `paths`, `out_dir`) y se convierten con `sips -s format png`. Para comparar 1:1: `adb shell wm size 720x1600` (mismo tamaño) y capturas del emulador lado a lado. La app trae las pantallas 01–19 casi idénticas; las diferencias eran sobre todo del chat/mapa (nuevas 20–38).

**Trampas:**
- Moshi: no usar `kotlin.Pair` en modelos que viajan por ChatProgressStore (ver también [[feedback-dead-code-layers]]).
- Emulador: este shell volvió a traer HTTP_PROXY (Proxyman) → lanzarlo con `env -u HTTP_PROXY -u HTTPS_PROXY ...` ([[project-emulator-proxy-conflict]]). El /data del AVD se llena: `INSTALL_FAILED_INSUFFICIENT_STORAGE` → desinstalar y reinstalar.
- Automatización UI con uiautomator: el texto de un botón a veces no es clickable (su padre sí); preferir matches clickable y luego caer a no clickable; el "Done" del photo picker se toca por coordenadas. El HorizontalScrollView de chips conserva su scroll entre preguntas (se resetea en renderQuickReplies).
