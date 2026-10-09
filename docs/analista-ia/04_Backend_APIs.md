# Backend: APIs

Endpoints reales a 2026-10-06, sacados del código de los controladores.

- El Android solo llama al **gateway**.
- FastAPI solo lo llama el gateway.
- Todo requiere JWT salvo `/api/auth/*` y `/`.

## Gateway (Spring Boot, `AsistenteRedIDBI-API-Gateway`)

| Recurso | Método y ruta | Uso |
|---|---|---|
| Salud | `GET /` | Health check |
| Auth | `POST /api/auth/register` · `/login` · `/refresh` · `/logout` · `/forgot-password` · `/reset-password` | Registro, sesión y recuperación |
| Perfil | `GET /api/profile/me` | Nombre, empresa y stats del home |
| Push | `PUT /api/notifications/device-token` | Registra el token FCM |
| Evaluaciones | `GET /api/evaluations` · `GET /{id}` · `POST` · `PUT /{id}` · `DELETE /{id}` | CRUD de evaluaciones |
| | `POST /api/evaluations/{id}/analyze` | Análisis de la evaluación |
| | `POST /api/evaluations/{id}/anular` | Anula un Borrador (soft) |
| Historial | `GET /api/history` · `GET /api/history/{id}` | Historial con filtros |
| Chat | `POST /api/evaluations/{id}/chat/start` · `/answer` · `/amend` | Chat de 76 nodos (proxy a FastAPI) |
| | `POST /api/evaluations/{id}/chat/answer-photos` (multipart) | Nodo EVIDENCE: `files` (1 a 3) + `state` |
| Mapa | `GET` · `PUT /api/evaluations/{id}/map` · `POST .../map/generate` · `POST .../map/command` | Mapa editable de la red |
| Análisis | `POST` · `GET /api/evaluations/{id}/analysis` | AS-IS/TO-BE y desglose por áreas |
| Minuta PDF | `GET /api/evaluations/{id}/minuta/pdf` · `POST .../minuta/send` | PDF de la minuta técnica y envío por correo |
| Minutas | `GET /api/minutas` · `GET /{id}` · `POST` · `PUT /{id}` · `POST /{id}/completar` · `POST /{id}/validar` | Ciclo de vida de la minuta |
| Propuesta PDF | `POST /api/proposals/pdf` · `POST /api/proposals/send` | PDF de la propuesta y envío |
| Checklist de evidencias | `/api/v1/evaluations/{id}/evidence/...` (checklist, areas, equipment, lock, photos) y `GET .../minuta` | Checklist dinámico anterior al chat de 76 nodos. Tras el chat, la app va directo a la minuta |
| Propuesta (legado) | `GET` · `PUT /api/evaluations/{id}/proposal` · `POST .../send` | **Código muerto**: hardcodeado, nada lo llama |

## FastAPI (`AsistenteRedIDBI-API`)

| Método y ruta | Uso |
|---|---|
| `GET /` · `GET /health` | Salud |
| `POST /chat/start` · `/chat/answer` · `/chat/amend` | Motor del chat (`app/chat/flow_engine.py`) |
| `POST /chat/minuta-document` | Arma el documento de la minuta (`minuta_doc.py`) |
| `POST /chat/map/generate` · `/chat/map/command` | Mapa de red y comandos |
| `GET /chat/nodes` | Nodos del flujo |
| `POST /analyze` | Análisis AS-IS/TO-BE (`analysis.py` → `proposal.py`) |
| `POST /analyze-photo` | Visión: descripción, marca y modelo |
| `POST /evidence-checklist/seed` | Siembra del checklist de evidencias |

## Propuesto (solo en local, no desplegado)

Tablas `kb_*` e importador del Excel de minutas manuales. Es un script local, no un endpoint. Ver [`../base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md`](../base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md).
