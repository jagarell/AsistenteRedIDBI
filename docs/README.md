# Documentación — Asistente Red IDBI

Todo el proyecto está en 3 repositorios. La documentación de los tres vive aquí, en el de Android.

| Repo | Qué es | GitHub |
|---|---|---|
| `AsistenteRedIDBI` | App Android (Kotlin, MVVM + Hilt) | https://github.com/jagarell/AsistenteRedIDBI |
| `AsistenteRedIDBI-API-Gateway` | Gateway Spring Boot: auth, evaluaciones, minutas, PDF, mapa, push. Dueño de la base Postgres | https://github.com/jagarell/AsistenteRedIDBI-API-Gateway |
| `AsistenteRedIDBI-API` | FastAPI: motor del chat de 76 nodos, lectura de fotos con IA, mapa | https://github.com/jagarell/AsistenteRedIDBI-API |

Producción (Railway): `https://asistenteredidbi.up.railway.app` (gateway).

> 🌿 **Rama de trabajo actual:** `feature/flujo-comandas-internet` (FastAPI y esta documentación; local, sin push). Trae los cambios del flujo del 2026-10-09: comanda en caja (P12b), P14 y P22 con opciones nuevas, y la regla de impresoras de preparación de red. Ver [`flujo/FLUJO_NODOS.md`](flujo/FLUJO_NODOS.md), sección 15.

> ⛔ **Cada `push` a `main` despliega solo en producción.** El trabajo nuevo va en ramas locales y se prueba con Postgres local o H2. No se hace push ni merge a `main` sin aprobación del equipo. Versión en uso: APK 0.0.2.

## Por dónde empezar

1. [`SETUP.md`](SETUP.md): cómo levantar los 3 repos en una máquina nueva.
2. [`CONTEXTO_PROYECTO.md`](CONTEXTO_PROYECTO.md): estado actual del proyecto, qué hace cada pieza y trampas conocidas (actualizado al 2026-10-06).
3. [`flujo/FLUJO_NODOS.md`](flujo/FLUJO_NODOS.md): el chat técnico de 76 nodos. Incluye diagrama, todos los nodos, evidencias, casos especiales y contrato de API. Se genera desde `flujo_asistente_red.json`.
4. [`base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md`](base-conocimiento/BASE_CONOCIMIENTO_MINUTAS.md) **(nuevo)**: cómo cargar las minutas manuales en PDF a la base, con el Excel [`Base_Conocimiento_Minutas.xlsx`](base-conocimiento/Base_Conocimiento_Minutas.xlsx), para nutrir las recomendaciones.
5. [`notas-de-desarrollo/`](notas-de-desarrollo/): notas de trabajo de octubre de 2026.
   - [`project_estado_apk_0_0_2.md`](notas-de-desarrollo/project_estado_apk_0_0_2.md): **estado actual**. Qué está hecho, decisiones, pendientes y trampas.
   - [`project_flow_engine_76_nodos.md`](notas-de-desarrollo/project_flow_engine_76_nodos.md): chat de 76 nodos, evidencias con IA, minuta PDF y cómo probar en local sin tocar producción.
   - [`project_roles_push_continuar.md`](notas-de-desarrollo/project_roles_push_continuar.md): roles Técnico/Supervisor, notificaciones push y "Continuar".
   - Notas más antiguas: rediseño Figma, checklist de evidencias, motor AS-IS/TO-BE, y dos problemas de entorno (puerto 8080 y proxy del emulador).

## Arquitectura

[`architecture/`](architecture/README.md): visión general, componentes, flujo de datos, despliegue (producción en Railway y local), AS-IS y TO-BE. Actualizada al 2026-10-06 con el chat de 76 nodos; marca con ✅ lo que se resolvió desde la revisión de agosto.

## Otros

- [`analista-ia/`](analista-ia/00_INDICE.md): resumen del proyecto, endpoints ([`04_Backend_APIs.md`](analista-ia/04_Backend_APIs.md)), roadmap y contexto para Claude.

## Pendiente de documentar

Los repos del gateway y de FastAPI no tienen README propio (el `HELP.md` del gateway es el genérico de Spring). Las variables de entorno y cómo correrlos están en `SETUP.md` y en las notas de desarrollo.
