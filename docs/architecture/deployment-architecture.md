# Arquitectura de despliegue

## Diagrama 4 — Despliegue actual identificado

```mermaid
flowchart LR
    subgraph DEVICE[Entorno Android]
        EMU[Android Emulator<br/>APK debug]
        STORE[DataStore local]
        EMU --> STORE
    end

    subgraph MAC[Mac de desarrollo — localhost]
        JAVA[Proceso JVM<br/>Spring Boot :8080]
        PY[Proceso Python/Uvicorn<br/>FastAPI :8000]
        PG[(PostgreSQL :5432)]
        DISK[(Directorio ./uploads)]
        ENV[Archivos .env<br/>y credenciales locales]

        JAVA -->|HTTP localhost:8000| PY
        JAVA -->|JDBC localhost:5432| PG
        JAVA -->|filesystem| DISK
        ENV --> JAVA
        ENV --> PY
    end

    INTERNET[Internet]
    EXT[OpenStreetMap / OpenAI /<br/>Flowise / SMTP / Firebase]

    EMU -->|HTTP 10.0.2.2:8080<br/>cleartext permitido| JAVA
    PY -->|HTTP(S) saliente| INTERNET
    JAVA -->|SMTP/HTTPS saliente| INTERNET
    INTERNET --> EXT
```

## Hallazgos de despliegue

| Elemento | Estado real |
|---|---|
| Android | APK ejecutado en emulador/dispositivo; build Gradle |
| Gateway | Proceso Spring Boot iniciado con `./mvnw spring-boot:run` |
| FastAPI | Uvicorn iniciado desde `venv` en puerto 8000 |
| Base de datos | PostgreSQL local en puerto 5432 |
| Evidencias | Directorio local configurable por `UPLOADS_DIR` |
| Red interna | `localhost` entre gateway, FastAPI y PostgreSQL |
| Transporte Android | HTTP cleartext a `10.0.2.2:8080` |
| Cloud/hosting | **No identificado en el repositorio** |
| Load balancer/reverse proxy | **No identificado en el repositorio** |
| TLS/certificados | **No identificado en el repositorio** |
| Docker/Compose | **No identificado en el repositorio** |
| Kubernetes | **No identificado en el repositorio** |
| CI/CD | **No identificado en el repositorio** |
| IaC | **No identificado en el repositorio** |
| Backups/DR | **No identificado en el repositorio** |
| Secret manager | **No identificado en el repositorio** |

## Restricciones observadas

- La base URL Android está compilada como `http://10.0.2.2:8080/`, específica del emulador local.
- `android:usesCleartextTraffic="true"` permite HTTP sin TLS.
- El gateway usa disco local para evidencias y expone `/uploads/**` públicamente.
- Gateway → FastAPI no tiene autenticación de servicio ni timeouts configurados en `RestTemplateConfig`.
- La instancia FastAPI acepta CORS `*`, aunque su consumidor esperado es el gateway.
- `ddl-auto=update` es útil en desarrollo, pero no sustituye migraciones versionadas en producción.

## Puertos y rutas

| Proceso | Puerto/ruta | Consumidor |
|---|---|---|
| Android emulator | Cliente | Usuario |
| Gateway | `:8080` | Android |
| FastAPI | `:8000` | Gateway |
| PostgreSQL | `:5432/asistente_red_idbi` | Gateway |
| Evidencias | `./uploads/evidence/{evaluationId}` | Gateway y endpoint público `/uploads/**` |

