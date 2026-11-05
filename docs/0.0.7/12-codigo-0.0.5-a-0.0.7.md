# Código de la app Android: de 0.0.5 a 0.0.7

Base: `jagarell/AsistenteRedIDBI`, rama `main` (0.0.5, versionCode 5). Esta entrega deja la app en **0.0.7 (versionCode 7)**.
Depende de la **API 0.0.7** (Parte 1) y del **gateway 0.0.7** (Parte 2) desplegados: las pantallas nuevas llaman a sus rutas.

> El Chat IA (evaluación de implementación) es dirigido por el servidor: las preguntas nuevas (router, puertos del switch,
> contraseñas en la tarjeta del router, iMin con o sin impresora, etc.) llegan desde el catálogo de la API 0.0.7 en
> `node.fields`, así que **no requieren cambios en la app**.

## Archivos nuevos

| Archivo | Qué hace |
|---|---|
| `data/remote/Api007Service.kt` | Rutas Retrofit 0.0.7: perfil de Reportes, Reportes con filtros, minuta, versiones, plan/aplicar cambios, antecedentes del cliente, evaluar/crear minuta de soporte, mensaje al Asistente IA, «¿Qué aprendiste hoy?», aplicar aprendizaje y fichas. DTOs de Reportes. |
| `data/repository/Asistente007Repository.kt` | Envuelve esas rutas. `Filtros` (fecha, técnico, supervisor, rubro, estado, criticidad, esfuerzo, plazo) y `count()` para el chip «Limpiar (n)». |
| `presentation/reportes/ReportesViewModel.kt` | Pestañas según el rol (las decide el gateway con Roles y permisos), filtros por pestaña, datos base para las opciones de cada filtro, minuta seleccionada y su historial de versiones. |
| `presentation/reportes/ReportesFragment.kt` | Pantalla **Reportes** (reemplaza Historial): pestañas General / Por técnico o Mis técnicos / Mis minutas, filtros arriba (selección múltiple y rango de fechas), 4 indicadores, lista con estado, criticidad y plazo, trazabilidad (versiones) y «Ver minuta». Conserva la posición al cambiar de pestaña. |
| `presentation/reportes/DocBlocksView.kt` | Dibuja el documento de la minuta de soporte (bloques que arma la API) en Reportes y en el Asistente IA. |
| `presentation/reportes/Ui007.kt` | Piezas de interfaz comunes (tarjetas, chips, pastillas, colores por estado/criticidad). Las pantallas nuevas se arman en código para no tocar los layouts existentes. |
| `presentation/asistente/AsistenteViewModel.kt` | **Asistente IA**: «Nueva minuta» (Implementación o Soporte / incidencia); flujo de soporte (cliente → antecedentes, motivo, alcance, evidencias, lo realizado por punto, reglas S1–S4 corregibles, estado final con sugerencia); actualizar una minuta (plan → aplicar = versión nueva + historial + validación del supervisor; los cambios de criterio se rechazan); «¿Qué aprendiste hoy?» solo administrador (Procede / Solo las reglas / Guardar y generar ficha PDF); preguntas libres (ChatGPT en la API). |
| `presentation/asistente/AsistenteFragment.kt` | Conversación con opciones tocables (solo en el último mensaje), vista previa de la minuta de soporte y apertura de la ficha PDF (se guarda también en Descargas). |

## Archivos cambiados

| Archivo | Cambio |
|---|---|
| `app/build.gradle.kts` | `versionCode 7`, `versionName "0.0.7"`. |
| `domain/model/Role.kt` | Rol `ADMINISTRADOR` («Administrador»). |
| `data/session/SessionManager.kt` | `isSupervisor` también es verdadero para el administrador (valida minutas). |
| `domain/di/AuthModule.kt` | Provee `Api007Service`. |
| `res/navigation/nav_graph.xml` | `historialFragment` ahora abre `ReportesFragment` («Reportes»; se mantiene el id para no romper acciones). Destino nuevo `asistenteFragment` y acción `action_home_to_asistente`. |
| `res/layout/view_nav_drawer.xml` + `MainActivity.kt` | Menú lateral: «Historial» → «Reportes»; entrada nueva «Asistente IA». |
| `res/layout/fragment_assistant_home.xml`, `fragment_send_draft.xml` | Textos «Reportes» / «Ver Reportes». |
| `presentation/home/AssistantHomeFragment.kt` | Si en el Asistente IA eligen «Nueva minuta › Implementación», el inicio abre la evaluación nueva con el flujo de siempre. |

`presentation/historial/*` queda en el repositorio (ya no lo abre la navegación); se puede borrar en una limpieza posterior.

## Pendiente (no incluido en esta entrega)

- Audio en el Asistente IA (hoy es texto), fotos adjuntas desde el Asistente IA (hoy se nombran; la lectura real la hace la visión en la API).
- Panel del supervisor/administrador, Equipo › Roles y permisos, Plazos y Preguntas y reglas siguen en la **web**; en la app Reportes muestra el aviso de plazos.
- Modo sin conexión, bitácora, copias de seguridad y reportes programados (requieren módulos del gateway que no están en la Parte 2).

## Cómo probar

1. `./gradlew assembleDebug` (no se pudo compilar donde se generó: no hay Android SDK ni acceso a Maven). El código nuevo se revisó
   con el compilador de Kotlin 2.0 contra firmas de las librerías, sin errores.
2. Con API y gateway 0.0.7: entrar como técnico → Reportes (solo «Mis minutas»); como supervisor → General, Mis técnicos, Mis minutas;
   como administrador → General, Por técnico y en el Asistente IA «¿Qué aprendiste hoy?».
3. Asistente IA › Nueva minuta › Soporte / incidencia: completar el flujo y verificar que la minuta aparece en Reportes como Pendiente,
   con «Ver minuta» mostrando el documento.
4. Asistente IA › Actualizar una minuta: escribir «el router quedó en el rack y la clave del WiFi es …» → Aplicar → la minuta sube de versión
   y su trazabilidad muestra el cambio.
