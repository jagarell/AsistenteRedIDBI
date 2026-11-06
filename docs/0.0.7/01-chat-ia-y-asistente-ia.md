# Chat IA y Asistente IA

## 1. Renombre: Chat IA
El chat guiado de la evaluación (pregunta por pregunta) pasa a llamarse **Chat IA**.

| Lugar | Antes | Ahora |
|---|---|---|
| Cabecera del chat | Asistente IA | Chat IA |
| Botón de Inicio | Iniciar con asistente IA | Iniciar con el Chat IA |
| Menú lateral | Chat técnico | Chat IA |

La lógica del chat no cambia, salvo la pregunta nueva del sistema de impresiones (ver API `02-reglas-estado-final.md`) y la tarjeta GPS de la dirección (ver `03-gps-direccion.md`).

## 2. Módulo nuevo: Asistente IA
**Acceso:** tarjeta en Inicio (debajo de «Nueva Evaluación») y opción en el menú lateral, para técnico, supervisor y administrador.

### 2.1 Pantalla
- Cabecera: «Asistente IA» · subtítulo «En línea».
- **Sin botones de opciones.** Solo un mensaje de bienvenida que explica por escrito en qué ayuda, según el rol:
  - **Técnico:** redactar una minuta nueva (fotos + texto o audio, incluidas las conexiones de red), actualizar una minuta, consultar una minuta, pendientes del día, minutas por validar, reportes y dudas de la visita.
  - **Supervisor:** consultar cualquier minuta, minutas por validar, pendientes del día, reportes de técnicos por periodo y reportes programados.
  - El saludo usa el nombre real de la cuenta; no saluda con «Técnico» ni «Supervisor».
- Barra inferior: 📎 adjuntar evidencias, 🎤 audio (se transcribe y se revisa antes de enviar), caja de texto y enviar.
- Botón para reiniciar la conversación. Cada rol tiene su propia conversación.

### 2.2 Qué entiende (intención → respuesta)
| El usuario escribe o dicta | Respuesta |
|---|---|
| Contexto de una visita o «Redacta una minuta…» | Tarjeta **Borrador de minuta** |
| «Actualiza la minuta de [cliente] del [fecha]: …» | Tarjeta **Actualizar minuta** |
| Pregunta sobre un cliente («¿en qué fecha se visitó…?», «¿qué observaciones tuvo…?») | Tarjeta **Base de datos** |
| «Minutas por validar», «pendientes de hoy», «reporte de octubre…», «cada fin de mes…» | Tarjetas de gestión |
| Cualquier otra pregunta o análisis | Respuesta de **ChatGPT** (burbuja con etiqueta «IA») |

La intención la decide la API (`POST /asistente/mensaje`, ver API `01-asistente-ia-chatgpt.md`). La app solo dibuja la tarjeta que llega en la respuesta.

### 2.3 Borrador de minuta
Muestra lo que la IA entendió: local, razón social, RUC, dirección, encargado, celular, correo, rubro, estado, áreas, internet, **ubicación GPS**, **conexiones de red**, contenido (observaciones, recomendaciones y pasos), evidencias recibidas y evidencias que faltan.
- Si faltan las **conexiones de red**, el asistente las pide: «cuéntame por audio o texto cómo están conectados el router, el switch, los repetidores, las PC y las impresoras».
- Botones: **Agregar más** y **Redactar minuta**. Al redactar se muestra el progreso (leyendo evidencias, extrayendo datos, armando la topología, aplicando reglas, redactando) y se abre **Ver minuta**.
- La minuta nueva queda en estado **Pendiente**.

### 2.4 Actualizar minuta
Muestra cada cambio antes de aplicarlo: datos (antes → después), observación con fecha, recomendaciones, pasos del cliente o del técnico, evidencias nuevas, **cambios de topología** y observaciones **resueltas en la visita**.
- Casilla **«Fue una nueva visita al local»**: marcada, la minuta toma la fecha de la actualización; desmarcada, conserva la fecha original. La IA la propone marcada si el texto habla de trabajos en el local.
- Si la minuta estaba Completa o Validada, avisa que vuelve a **Pendiente**.
- Botones: **Cancelar** y **Aplicar y ver minuta**. Al aplicar sube la versión (v1 → v2) y abre **Ver minuta**.

### 2.5 Respuestas de ChatGPT
- Burbuja con etiqueta «IA», texto en streaming y botón **Detener**.
- Si ChatGPT no está disponible, la API responde con las reglas de la app y la burbuja lo indica.
- Las guías paso a paso (selftest, ipconfig, AnyDesk…) solo salen cuando la pregunta es de cómo hacer algo.

## 3. Criterios de aceptación
**CA-01 Bienvenida sin opciones**
- Dado un técnico con sesión iniciada, cuando abre el Asistente IA, entonces ve un solo mensaje con la lista escrita de lo que puede hacer y ningún botón de opción.

**CA-02 Borrador pide conexiones**
- Dado un borrador sin conexiones de red, cuando el técnico envía el contexto, entonces el asistente pide por mensaje cómo están conectados los equipos y la fila «Conexiones de red» muestra «Sin dato».

**CA-03 Conexiones en la topología**
- Dado el texto «La impresora de cocina está conectada por cable al switch», cuando se redacta la minuta, entonces la topología muestra un enlace de cable del switch a «IMP. Cocina».
- Dado el texto «estaba por WiFi y se dejó por cable», cuando se interpreta la conexión, entonces queda como cable (el último medio mencionado).

**CA-04 Actualización con nueva visita**
- Dada una minuta del 29/10/2026, cuando se aplica una actualización con la casilla de nueva visita marcada el 02/11/2026, entonces la fecha de evaluación de la minuta es 02/11/2026 y el historial dice «Nueva visita».
- Dada la casilla desmarcada, entonces la fecha sigue siendo 29/10/2026 y el historial dice «se mantiene la fecha de la visita».

**CA-05 Pregunta abierta**
- Dado un técnico, cuando pregunta «si tengo un iMin, ¿necesito impresora?», entonces recibe una respuesta de ChatGPT basada en la regla del equipo de caja, y no la guía del selftest.
