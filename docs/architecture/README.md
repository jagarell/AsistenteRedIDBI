# Arquitectura de Asistente Red IDBI

Documentación reconstruida desde la implementación existente en los tres repositorios del sistema, revisada el 17 de agosto de 2026.

## Alcance analizado

- `AsistenteRedIDBI`: aplicación Android nativa.
- `idbi-api-gateway`: API y backend de negocio Spring Boot.
- `idbi-fastapi`: servicio especializado Python/FastAPI.

Esta documentación distingue entre componentes activos, código legado/no usado y capacidades recomendadas. Un elemento del modelo recomendado no implica que ya exista.

## Documentos

- [Arquitectura general](architecture-overview.md)
- [Arquitectura técnica detallada](architecture-detailed.md)
- [Flujo de datos](data-flow.md)
- [Arquitectura de despliegue](deployment-architecture.md)
- [Arquitectura actual (AS-IS)](architecture-as-is.md)
- [Arquitectura recomendada (TO-BE)](architecture-to-be.md)

## Resumen ejecutivo

El sistema implementado es una solución móvil Android respaldada por un backend modular monolítico Spring Boot y un servicio Python especializado. Android consume únicamente el gateway mediante REST/JSON y JWT. El gateway controla autenticación, autorización, usuarios, evaluaciones, minutas, evidencias, PDF, correo y notificaciones, y es el único componente que persiste el negocio en PostgreSQL. FastAPI ejecuta el chat técnico de 20 nodos, análisis determinista, topología, geocodificación y, opcionalmente, OpenAI o Flowise.

La ejecución identificada es local: Android Emulator, procesos Java/Python, PostgreSQL y almacenamiento en disco. No se identificaron en los repositorios Docker, Kubernetes, infraestructura como código, balanceador, despliegue cloud, CI/CD, cola de mensajes, WebSockets, caché distribuida ni plataforma centralizada de observabilidad.

## Leyenda

- Flecha continua: dependencia o flujo implementado.
- Flecha discontinua: integración opcional o dependiente de credenciales.
- `No identificado`: no existe evidencia suficiente en código o configuración.
- `Legado/no usado`: código presente, pero fuera del flujo activo identificado.

