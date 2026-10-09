# Arquitectura de Asistente Red IDBI

Documentación reconstruida desde el código de los tres repositorios.

- **Primera versión:** 17 de agosto de 2026 (chat de 20 nodos, solo local).
- **Actualizada:** 6 de octubre de 2026, contra los commits desplegados (Android `089040f`, gateway `c81c257`, FastAPI `dd03113`).

## Alcance analizado

| Repo en GitHub | Carpeta local habitual | Qué es |
|---|---|---|
| `AsistenteRedIDBI` | — | Aplicación Android nativa (APK 0.0.2) |
| `AsistenteRedIDBI-API-Gateway` | `idbi-api-gateway` | API y backend de negocio Spring Boot |
| `AsistenteRedIDBI-API` | `idbi-fastapi` | Servicio Python/FastAPI: chat de 76 nodos, visión, minuta y mapa |

Esta documentación distingue entre componentes activos, código legado o no usado y capacidades recomendadas. Que un elemento aparezca en el modelo recomendado no significa que ya exista.

## Documentos

- [Arquitectura general](architecture-overview.md)
- [Arquitectura técnica detallada](architecture-detailed.md)
- [Flujo de datos](data-flow.md)
- [Arquitectura de despliegue](deployment-architecture.md)
- [Arquitectura actual (AS-IS)](architecture-as-is.md)
- [Arquitectura recomendada (TO-BE)](architecture-to-be.md)

Relacionados:
- [`../flujo/FLUJO_NODOS.md`](../flujo/FLUJO_NODOS.md): detalle del chat de 76 nodos.
- [`../base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md`](../base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md): base de conocimiento de minutas manuales (propuesta, solo local).

## Resumen ejecutivo

El sistema es una app Android respaldada por un backend modular monolítico en Spring Boot (el "gateway") y un servicio Python especializado.

- **Android** solo consume el gateway, por REST/JSON y JWT.
- **El gateway** controla autenticación (JWT + refresh token con rotación), usuarios y roles, evaluaciones, chat, evidencias, minutas, PDF, mapa, correo y notificaciones. Es el único componente que persiste el negocio en PostgreSQL.
- **FastAPI** ejecuta:
  - el chat técnico de **76 nodos** (loops por caja, impresora y área; subflujo de ubicación; 13 evidencias con IA);
  - la lectura de fotos con OpenAI Vision;
  - las reglas de validación V01–V10 y el motor AS-IS/TO-BE;
  - el documento de la minuta y el mapa de red.

**Despliegue.** Desde octubre de 2026 el sistema corre en **Railway**. Cada servicio usa su propio Dockerfile, con HTTPS en `asistenteredidbi.up.railway.app`. Railway **despliega automáticamente cada push a `main`**. En local se sigue usando el emulador, procesos Java/Python y Postgres o H2 (ver [despliegue](deployment-architecture.md)).

**Lo que no se identificó en los repositorios:** CI/CD con pruebas, infraestructura como código, colas, WebSockets, caché distribuida, observabilidad centralizada ni ambientes separados (staging/producción).

> ⛔ Mientras no exista un ambiente de staging, **no se hace push a `main` sin aprobación**: el mismo push que sube el código lo publica en producción.

## Leyenda

- Flecha continua: dependencia o flujo implementado.
- Flecha discontinua: integración opcional o que depende de credenciales.
- `No identificado`: no hay evidencia suficiente en el código o la configuración.
- `Legado/no usado`: código presente, pero fuera del flujo activo.
- ✅ Resuelto desde agosto · 🔴 Crítico · 🟠 Mejora recomendada.
