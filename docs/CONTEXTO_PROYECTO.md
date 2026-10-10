# Contexto del proyecto — Asistente Red IDBI ("Analista IA")

Última actualización: 2026-10-06. Resume el estado real del proyecto para retomarlo en otra sesión sin redescubrir todo.

La versión anterior de este documento (2026-09-14) describía el chat de 23 nodos. Ese chat **ya no existe**: hoy corre el flujo de 76 nodos.

> ⛔ **Producción no se toca.** Cada push a `main` despliega solo en Railway. La versión en uso es el APK 0.0.3 contra `https://asistenteredidbi.up.railway.app`. Todo trabajo nuevo va en una rama local y se prueba con Postgres local o H2 (ver "Cómo probar sin tocar producción").

## Arquitectura (3 repos)

| Repo (GitHub) | Carpeta local habitual | Qué hace |
|---|---|---|
| `AsistenteRedIDBI` | este repo | App Android nativa: Kotlin, MVVM + Hilt + Retrofit/Moshi + Navigation. `applicationId` `com.upc.asistenteredidbi`. Aquí vive toda la documentación (`docs/`) |
| `AsistenteRedIDBI-API-Gateway` | `~/IdeaProjects/idbi-api-gateway` | Spring Boot (puerto 8080), Postgres `asistente_red_idbi`. **Única fuente de verdad de negocio**: auth/JWT, evaluaciones, minutas, perfil, push, evidencias, PDF de propuesta y de minuta, mapa |
| `AsistenteRedIDBI-API` | `~/IdeaProjects/idbi-fastapi` | FastAPI (puerto 8000). Motor del chat de 76 nodos (`app/chat/`), lectura de fotos con IA (OpenAI Vision), documento de minuta y mapa. No tiene base propia |

- El Android habla siempre con el gateway: `10.0.3.2:8080` desde el emulador, o Railway en producción.
- Solo el gateway le habla a FastAPI (`FASTAPI_BASE_URL`).
- Endpoints: [`analista-ia/04_Backend_APIs.md`](analista-ia/04_Backend_APIs.md).

## Estado actual (APK 0.0.3, 2026-10-10)

Commits desplegados al 2026-10-10: gateway `a254db8`, FastAPI `6a0acec` y Android APK 0.0.3 (código 3).

**Hecho y verificado en producción:**
- **Chat técnico de 76 nodos** ([`flujo/FLUJO_NODOS.md`](flujo/FLUJO_NODOS.md)).
  - Incluye loops por caja, impresora y área; el subflujo U de ubicación; avisos (Raspberry, áreas sin impresora); 13 evidencias con IA y el resumen final.
  - El motor no guarda estado en el servidor: el cliente envía el `state` opaco.
  - Al completarse, el gateway guarda las respuestas en `evaluations.chat_answers_json`.
- **Evidencias con IA.**
  - Se extraen los datos de cada foto y se cruzan entre sí: proveedor del speedtest frente a P13, MAC de la impresora frente al escáner, adaptador del ipconfig.
  - En E3 se ocultan las credenciales.
  - E9 (fotos generales) es **obligatoria**: sin ella no se genera el PDF.
- **Minuta PDF** (Thymeleaf + OpenHTMLtoPDF, `MinutaPdfService`). Tiene validación técnica automática (reglas V01–V10), recomendaciones, mapa y anexo A. El anexo B se eliminó.
- **Mapa editable.** Zoom a pantalla completa, paleta de colores, menú Agregar (elemento, línea recta o discontinua, caja de texto, imagen) y comandos del mapa (por reglas, no LLM).
- **Contador.** Muestra "Pregunta N de ~T" (solo preguntas fijas) y "Evidencia N de M".
- **Historial.** Filtro por rango de fechas y orden del más nuevo al más antiguo. Deslizar un Borrador a la izquierda lo anula (anulación soft, con una marca aparte).
- **"Continuar" y "Nueva Evaluación".** "Continuar" retoma el chat. Solo "Nueva Evaluación" lo borra, y pide confirmación.
- **Base.** Auth (JWT), perfil, home con datos reales, minutas (crear, completar, validar), roles Técnico/Supervisor e infraestructura de push FCM.
- **Motor AS-IS/TO-BE** (`app/chat/proposal.py`). Funciona por reglas, sin OpenAI, con umbrales de industria: TIA/EIA-568 a 100 m, cobertura de AP, demanda frente a plan contratado, presupuesto PoE y redundancia. `app/analysis.py` lo reutiliza: hay una sola fuente de verdad.

**Decisiones del equipo:**
- El borrador vence a los 30 días.
- Solo un Borrador se puede anular.
- Una evaluación nueva nace en BORRADOR.

## En curso: base de conocimiento de minutas manuales

Las minutas hechas a mano (PDF) se cargan con el Excel [`base-conocimiento/Base_Conocimiento_Minutas.xlsx`](base-conocimiento/Base_Conocimiento_Minutas.xlsx) en tablas `kb_*`. El objetivo es que las recomendaciones se basen en casos reales.

- Ya hay 18 minutas cargadas (2023–2026: Sicilia, Don Oscar, Barrio Pesquero, Rock & Burgers, Dorcher, Malala, Hotel La Confianza, 7 sedes Rikoton y 3 de Gelato Alore), con 52 reglas, 13 acciones estándar y la tabla de decisión de impresión IDPos.
- **Reglas de negocio IDPos** (sección 4b de `base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md`):
  - **Comandas:** se deducen de las áreas de preparación (bar, cocina, jugos, cafetería, parrillas, makis, ramen, panadería…).
  - **Controlador de impresiones:** va en la PC o laptop de caja. Si no hay ninguna, en un Raspberry, solo con impresoras de red; una USB solo funciona con laptop o PC.
  - **Tablet en caja (o solo tablets):** Raspberry obligatorio.
  - **Impresoras de áreas de preparación:** siempre de red por cable.
  - **Comanda solo en caja:** Sunmi con chip.
  - **Sin fibra ni cable coaxial** (P14 = Inalámbrica o "No sé", que se toma como sin internet): probar la señal del chip (Entel o Claro) con un celular y, si hay comandas, router con chip.
  - **Compras:** el Raspberry y el Sunmi se cotizan con el equipo de Desarrollo de Negocios.
  - **Puertos y energía:** los puertos libres se leen de la foto del router (E2). Si los equipos por cable los superan, se recomienda switch y cuántos puntos de red implementar. Cada equipo de red necesita toma cerca; si no hay, punto de energía adicional o extensión.
  - **WiFi:** el access point o el primer repetidor van con red y energía; el segundo repetidor, solo con energía. Todo el local debe tener WiFi para que los meseros comanden.
  - **Aviso de viabilidad:** si no se siguen las recomendaciones, no es viable la implementación del punto de venta. La minuta debe mostrarlo.
  - **Estado (2026-10-09):**
    - **Implementado y desplegado el 2026-10-10 (APK 0.0.3):** P22 con Sí/No/Tablet/No sé/Otro (+ P22f); la pregunta fija P12b "¿En caja saldrán comandas?" con aviso de Sunmi; P14 con Fibra óptica/Cable coaxial/No sé/No tiene/Internet con chip; la prueba de chip (P14b + E1b); los puertos del router leídos en la foto E2; la decisión de impresión (Raspberry, laptop o PC); el switch y los puntos de red; el WiFi por zona; la regla R51 (impresoras de preparación de red por cable) y el aviso de viabilidad en la minuta.
    - **Sigue propuesto:** las tablas `kb_*`, el importador del Excel y las recomendaciones desde la base de conocimiento.
- Diseño, importador y prompt para Claude Code: [`base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md`](base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md).
- **Todo esto se hace solo en local.**

## Pendientes

1. **Firebase.** Falta `app/google-services.json` (requiere recompilar a 0.0.3) y la service account en Railway (`FIREBASE_CREDENTIALS_JSON`). Hasta entonces no salen los push.
2. **Filas "Pendiente · …" del PDF** (Razón social, RUC, Correo, switch) y la regla "Datos obligatorios completos". Hay que decidir si se quitan.
3. **Pruebas en dispositivo pendientes:**
   - Que "Omitir" desaparezca en E9.
   - Cómo muestra la app el error 400 al pedir el PDF de una evaluación vieja sin E9.
4. **Pantallas 01–19 del prototipo** (propuesta, PDF, envío): no se repasaron del todo. Además, los márgenes son de 24 dp y el prototipo usa 16 dp.
5. **Credenciales externas pendientes:**
   - Correo: `RESEND_API_KEY` + `MAIL_ENABLED=true`. El correo sale por la API HTTPS de Resend, no por SMTP, porque Railway Hobby bloquea SMTP.
   - RUC (APIs Peru): `RUC_VALIDATION_*`, apagado por diseño.
   - En local no hay `OPENAI_API_KEY`: la IA se simula (ver abajo).
6. **Base de conocimiento.** Crear las tablas `kb_*` y el importador en local, y después conectar el motor de recomendaciones a esas tablas.

## Cómo levantar todo en local

```bash
# Gateway: el .env con credenciales existe en la máquina de desarrollo (gitignored)
cd ~/IdeaProjects/idbi-api-gateway
set -a; source .env; set +a
./mvnw spring-boot:run

# FastAPI
cd ~/IdeaProjects/idbi-fastapi
./venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000
```

### Cómo probar sin tocar producción

- **Gateway con H2:**
  ```
  JWT_SECRET=... DB_URL=jdbc:h2:mem:devdb;MODE=PostgreSQL DB_USERNAME=sa DB_PASSWORD=x SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.h2.Driver JPA_DDL_AUTO=create-drop SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.H2Dialect ./mvnw spring-boot:run -Dspring-boot.run.useTestClasspath=true
  ```
- **FastAPI sin `OPENAI_API_KEY`:** simular la IA con los valores de `tests/test_rock_sample.py`.
- **APK contra local:** cambiar `BASE_URL` en `AuthModule.kt` a `http://10.0.3.2:8080/` solo en local. **Revertir antes de cualquier commit.**

## Trampas conocidas

- **Enums en la base de producción.** La base tiene restricciones con los valores de los enums, y `ddl-auto=update` no las actualiza. Agregar un valor nuevo a un enum da 500: hay que usar una columna o marca aparte.
- **Tablas nuevas en producción.** Por `ddl-auto=update`, cualquier entidad nueva que llegue a `main` crea tablas en producción. Es otra razón para no subir las tablas `kb_*` sin aprobación.
- **Dueño de las tablas.** En local, las tablas deben ser propiedad de `idbi_user`. Si aparece `must be owner of table`:
  ```
  REASSIGN OWNED BY "<usuario_mac>" TO idbi_user;
  ```
- **JWT.** Si se regenera `JWT_SECRET`, los tokens guardados en la app dejan de servir (403 silencioso). Cierra sesión y vuelve a entrar.
- **Puerto 8080.** Puede estar ocupado por otro proyecto (Voya). Revisa con `lsof -i :8080 -sTCP:LISTEN` que el proceso sea `IdbiApiGatewayApplication`.
- **Emulador.** Lánzalo con `env -u HTTP_PROXY -u HTTPS_PROXY …` (ver `notas-de-desarrollo/project_emulator_proxy_conflict.md`).
- **Moshi.** No uses `kotlin.Pair` en modelos que pasan por `ChatProgressStore`.
- **Credenciales.** Nunca incluyas credenciales (AnyDesk, claves WiFi, contraseñas de router) en minutas, documentos ni en la base de conocimiento.
- **Tablas huérfanas.** Quedan tablas de un backend abandonado: `locals`, `technical_evaluations`, `network_devices`. Ningún código las usa.

## Código muerto conocido

- Android: no hay código muerto confirmado en el historial. `HistorialViewModel`, `EvaluationSummaryItem` y `EvaluationFilters` **sí se usan** (el Historial los usa para el filtro por fecha y anular borradores).
- Gateway: `ProposalController` (`/api/evaluations/{id}/proposal`). Está hardcodeado y nada lo llama.
