# Contexto para Claude

Este proyecto utiliza:

-   Arquitectura Clean
-   MVVM
-   Kotlin + XML
-   FastAPI
-   Spring Gateway
-   PostgreSQL

Claude debe mantener:

-   separación por capas
-   SOLID
-   Clean Code
-   patrones consistentes
-   documentación
-   pruebas cuando sea posible

Nunca romper la arquitectura existente.

## Producción: prohibido tocarla sin aprobación

-   **No hacer push a `main`** en ninguno de los 3 repos: Railway despliega `main` automáticamente.
-   No abrir ni mergear PRs a `main`, no cambiar variables de Railway, no conectarse a la base de producción y no generar un APK nuevo, salvo que el equipo lo pida explícitamente.
-   Trabajar en ramas locales y probar con Postgres local o H2. Ver `../CONTEXTO_PROYECTO.md`, sección "Cómo probar sin tocar producción".
-   Mostrar el diff y los tests antes de cualquier commit.
-   Nunca escribir credenciales (AnyDesk, claves WiFi, contraseñas de router) en código, documentos, minutas ni en la base de conocimiento.
