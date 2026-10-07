# Documentación — Asistente Red IDBI

Todo el proyecto está en 3 repositorios (la documentación de los tres vive aquí, en el de Android):

| Repo | Qué es | GitHub |
|---|---|---|
| `AsistenteRedIDBI` | App Android (Kotlin, MVVM + Hilt) | https://github.com/jagarell/AsistenteRedIDBI |
| `AsistenteRedIDBI-API-Gateway` | Gateway Spring Boot: auth, evaluaciones, minutas, PDF, mapa, push | https://github.com/jagarell/AsistenteRedIDBI-API-Gateway |
| `AsistenteRedIDBI-API` | FastAPI: motor del chat de 76 nodos, lectura de fotos con IA, mapa | https://github.com/jagarell/AsistenteRedIDBI-API |

Producción (Railway): `https://asistenteredidbi.up.railway.app` (gateway). Cada `push` a `main` despliega solo.

## Por dónde empezar

1. [`SETUP.md`](SETUP.md) — cómo levantar los 3 repos en una máquina nueva.
2. [`CONTEXTO_PROYECTO.md`](CONTEXTO_PROYECTO.md) — qué hace cada pieza (escrito el 2026-09-14: **no incluye** lo del chat de 76 nodos, el mapa editable ni la minuta PDF nueva; para eso ver las notas de desarrollo).
3. [`notas-de-desarrollo/`](notas-de-desarrollo/) — notas de trabajo de octubre 2026, la fuente más actual:
   - [`project_estado_apk_0_0_2.md`](notas-de-desarrollo/project_estado_apk_0_0_2.md) — **estado actual**: qué está hecho, decisiones, pendientes y trampas.
   - [`project_flow_engine_76_nodos.md`](notas-de-desarrollo/project_flow_engine_76_nodos.md) — chat de 76 nodos, evidencias con IA, minuta PDF, cómo probar en local.
   - [`project_roles_push_continuar.md`](notas-de-desarrollo/project_roles_push_continuar.md) — roles Técnico/Supervisor, notificaciones push, "Continuar".
   - Notas más antiguas: rediseño Figma, checklist de evidencias, motor AS-IS/TO-BE, y dos problemas de entorno (puerto 8080, proxy del emulador).

## Arquitectura (anterior al chat de 76 nodos)

[`architecture/`](architecture/README.md): visión general, as-is, to-be, flujo de datos y despliegue.

## Otros

- [`analista-ia/`](analista-ia/00_INDICE.md) — resumen inicial del proyecto y contexto para Claude (breve; el índice menciona un `04_Backend_APIs.md` que no existe).

## Pendiente de documentar

Los repos del gateway y de FastAPI no tienen README propio (el `HELP.md` del gateway es el genérico de Spring). Las variables de entorno y cómo correrlos están en `SETUP.md` y en las notas de desarrollo.
