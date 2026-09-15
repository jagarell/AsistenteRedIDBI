# Flujo de datos

## Diagrama 3 — Flujo principal: evaluación técnica completa

```mermaid
sequenceDiagram
    autonumber
    actor U as Técnico
    participant UI as Android UI
    participant VM as ViewModel / Repository
    participant DS as DataStore
    participant GW as Spring Boot Gateway
    participant DB as PostgreSQL
    participant PY as FastAPI
    participant OSM as Nominatim
    participant AI as OpenAI/Flowise opcional

    U->>UI: Inicia sesión
    UI->>VM: Credenciales
    VM->>GW: POST /api/auth/login (HTTP JSON)
    GW->>DB: Buscar usuario y hash (JPA/JDBC)
    DB-->>GW: Usuario y rol
    GW->>GW: BCrypt + emitir JWT HS256
    GW-->>VM: JWT, expiración, usuario y rol
    VM->>DS: Guardar sesión
    VM-->>UI: Abrir Home

    U->>UI: Inicia evaluación
    UI->>VM: Datos iniciales
    VM->>DS: Leer JWT
    VM->>GW: POST /api/evaluations (Bearer JWT)
    GW->>DB: INSERT evaluación
    DB-->>GW: ID de evaluación
    GW-->>UI: Evaluación creada

    UI->>GW: POST .../chat/start (JWT)
    GW->>PY: POST /chat/start (JSON)
    PY-->>GW: Primera pregunta
    GW-->>UI: Nodo inicial

    loop Hasta completar 20 nodos
        U->>UI: Responde pregunta
        UI->>GW: POST .../chat/answer (JWT, paso y respuestas)
        GW->>PY: POST /chat/answer (JSON síncrono)
        opt Nodo automático de ubicación
            PY->>OSM: HTTPS GET de nombre + dirección
            OSM-->>PY: Latitud y longitud
        end
        PY-->>GW: Siguiente nodo y progreso
        GW-->>UI: Pregunta siguiente
    end

    opt Motor de propuesta configurado como OpenAI o Flowise
        PY->>AI: HTTPS/REST con respuestas técnicas
        AI-->>PY: Resumen enriquecido
    end
    PY->>PY: Reglas, score y topología
    PY-->>GW: Propuesta completa
    GW-->>UI: Resultado técnico

    UI->>GW: POST evidencia multipart (JWT)
    GW->>GW: Guardar archivo en uploads/
    GW->>DB: INSERT metadata de evidencia
    DB-->>GW: ID de evidencia
    GW-->>UI: URL y metadata

    U->>UI: Solicita análisis de foto
    UI->>GW: POST .../evidence/{id}/analyze
    GW->>GW: Leer archivo desde disco
    GW->>PY: POST /analyze-photo (imagen Base64)
    PY->>AI: OpenAI Vision, si existe API key
    AI-->>PY: Descripción
    PY-->>GW: Resultado del análisis
    GW->>DB: UPDATE analysis_result
    GW-->>UI: Evidencia analizada

    U->>UI: Completa minuta
    UI->>GW: POST/PUT /api/minutas (JWT)
    GW->>DB: INSERT/UPDATE minuta
    DB-->>GW: Minuta persistida
    GW-->>UI: Estado de minuta
```

## Explicación paso a paso

1. Android recoge las credenciales y las envía como JSON al gateway.
2. El gateway normaliza el correo, verifica el hash BCrypt y genera un JWT HS256 que contiene identidad y rol.
3. Android guarda JWT, expiración, ID, nombre y rol en DataStore Preferences. El interceptor OkHttp agrega el token a llamadas posteriores.
4. Al crear una evaluación, el gateway valida el JWT y persiste el registro mediante JPA/Hibernate.
5. Para el chat, el gateway actúa como proxy síncrono. Envía a FastAPI el identificador, el paso actual y el mapa acumulado de respuestas.
6. FastAPI conserva el estado del chat en el payload recibido; no se identificó persistencia propia ni sesión server-side del chat.
7. En el nodo automático de ubicación, FastAPI consulta Nominatim y añade coordenadas al mapa de respuestas.
8. Al concluir los 20 nodos, FastAPI calcula reglas, score, equipos, recomendaciones y topología. Puede reemplazar únicamente el resumen mediante OpenAI o Flowise y vuelve a reglas si la integración falla.
9. El gateway devuelve el resultado a Android. En el flujo revisado, no se observó que `ChatService` persista por sí mismo todas las respuestas o la propuesta; la minuta posterior almacena resumen/contenido/topología como texto JSON.
10. Las fotografías se envían como `multipart/form-data` al gateway. El binario va a disco local y sus metadatos a PostgreSQL.
11. Para analizar una imagen, el gateway lee el archivo, lo codifica en Base64 y lo envía síncronamente a FastAPI; FastAPI usa OpenAI si existe una clave.
12. La minuta se crea y actualiza en PostgreSQL. La validación está protegida por rol `SUPERVISOR`.

## Flujo de PDF y correo

```mermaid
sequenceDiagram
    actor U as Usuario
    participant A as Android
    participant G as Gateway
    participant P as PDFBox
    participant S as SMTP

    U->>A: Generar propuesta PDF
    A->>G: POST /api/proposals/pdf + JWT + contenido
    G->>P: Construir PDF en memoria
    P-->>G: byte[] application/pdf
    G-->>A: Stream del PDF
    A-->>U: Visualizar/compartir archivo

    opt Envío por correo solicitado
        A->>G: POST /api/proposals/send + JWT
        G->>P: Generar PDF
        P-->>G: byte[]
        G->>S: SMTP STARTTLS + adjunto
        S-->>G: Resultado del envío
        G-->>A: Confirmación o error explícito
    end
```

El contenido del PDF llega desde Android en el request; el controlador de PDF no consulta directamente PostgreSQL en la implementación observada.

## Flujo de notificaciones

```mermaid
sequenceDiagram
    participant A as Android
    participant F as Firebase Cloud Messaging
    participant G as Gateway
    participant DB as PostgreSQL

    A->>F: Solicitar token FCM
    F-->>A: Token de dispositivo
    A->>G: PUT /api/notifications/device-token + JWT
    G->>DB: Guardar token en usuario
    Note over G,F: Un servicio de negocio puede solicitar el envío
    G->>F: Firebase Admin SDK / HTTPS
    F-->>A: Push asíncrono
```

No se identificó una cola: el envío desde el gateway hacia Firebase es directo.

## Sincronía y persistencia

| Flujo | Tipo | Persistencia |
|---|---|---|
| Android → Gateway | Síncrono HTTP REST | DataStore solo guarda sesión local |
| Gateway → FastAPI | Síncrono HTTP REST | FastAPI no persiste en el flujo activo |
| Gateway → PostgreSQL | Síncrono transaccional vía JPA | Persistencia permanente |
| Gateway → disco | Síncrono Java NIO | Archivo permanente solo en el host actual |
| FastAPI → Nominatim/OpenAI/Flowise | Síncrono HTTP(S) | No identificada |
| Gateway → SMTP | Síncrono durante la petición | No identificada |
| Gateway → FCM | Solicitud síncrona; entrega push asíncrona | Token en usuario |

