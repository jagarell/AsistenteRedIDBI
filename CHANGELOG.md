# Changelog

## [Sin publicar]
### Corregido
- **Editar topología**: el lienzo ya no se corta ni se queda sin moverse a los lados. Es más ancho cuando hay muchos equipos en una fila (para que los nombres no se pisen), se puede desplazar libremente y pellizcar para acercar hasta 4 veces y alejar hasta 25 %.
- **Puntitos de cuadrícula** en el editor: marcan el área de trabajo y los equipos se pegan a ellos al soltarlos, para que queden alineados y no se amontonen.
- Botón **Ajustar** en la barra del editor: hace que quepa todo el mapa en la pantalla.
- Los nombres largos de los equipos se recortan con «…» en lugar de pisar al vecino.

## [0.0.17] - 2026-11-08
### Agregado
- **Ubicación GPS del local con Google Maps** (vuelve la opción de las entregas anteriores): en «¿Cuál es la dirección del local?» aparece **📍 Usar GPS**. Muestra coordenadas, precisión, dirección, distrito/departamento y si el lugar coincide con el nombre del local; **Usar esta dirección** la responde y **Ver en Google Maps** abre el mapa. La minuta lleva «Distrito / Departamento» y «Ubicación GPS» como enlace al mapa.
### Corregido
- **Supervisor**: el rol Supervisor conserva siempre «Validar minutas» y «Devolver minutas» (y el Técnico «Crear minutas») aunque la base de la empresa viniera de una versión anterior.

## [0.0.16] - 2026-11-07
### Agregado
- **Parte 6 · actualizar la minuta desde el Asistente IA** (caso AMAT ROOFTOP): impresoras, equipos, fotos, tomas, router, recomendaciones y pasos; lo actualizado llega al PDF.
- **Chat IA**: editar una respuesta anterior sin perder las demás; rubro «Otro» con el flujo completo; equipo de los mozos (tablet, celular, iMin móvil, otro o por confirmar); cambiar una foto y agregarle una descripción; «Reportes» y «reporte de los técnicos por terminar» en el Asistente IA.
- **Chat IA · Internet**: «Internet satelital» y «Otro (escribir)»; si el cliente va a contratar, ahora se anota el tipo de conexión y el proveedor (Starlink, otro).
- **Ubicar el puerto del switch**: la app envía de verdad los pings a la IP de la impresora (20 s, 2 s sí y 2 s no) mientras la pregunta está abierta.
- **Dictar la respuesta** por voz en el Chat IA.
- **Ver minuta** abre también las minutas redactadas con el Asistente IA (sin evaluación) desde Minutas, Notificaciones, Panel y Reportes; «Ver topología» baja hasta el mapa.
- **Cambiar contraseña** con la sesión iniciada (pide la actual).
- **Mi Cuenta**: Notificaciones antes que Seguridad; «Recibir notificaciones» (apaga o enciende todo) y «Avisarme cuando…» guardados en la cuenta; la hora de la copia de seguridad y «Incluir fotos»; Ayuda abre el Asistente IA.
- **Equipo y accesos**: pestañas Personas y Roles y permisos (editar lo que puede hacer cada rol y crear roles) y filtros con cantidad.
- **Panel**: tarjetas con borde de color y subtítulos reales («N con análisis IA listo», «−N % por minuta»), bloque de avance por técnico con la meta diaria, supervisores con horas promedio de espera (meta 24 h), «Ver todas» y «Ver toda la actividad».
- **Análisis IA**: «Observaciones de la visita» con «Se resolvió» y el Estado final (APTO, OBSERVADO o NO APTO).
- **Propuesta PDF**: Ajustar, Editar, Bitácora, Solicitar validación y Compartir.
### Cambiado
- El menú lateral solo se abre en Inicio; una notificación tocada en Iniciar sesión se abre al entrar.
- Se quitaron los interruptores que no hacían nada (2 factores, push, correo y alertas), «Dispositivos activos», «Idioma», «Soporte» de Iniciar sesión y los botones «Próximamente» del Plan.
- Sin datos de ejemplo visibles en Inicio (24/18/12) ni en Análisis IA (72 %).
### Pendiente
- Parte 7 restante: estado de cada equipo, una sola lista de impresoras, punto de red y energía por equipo, equipos que no son del local y notas largas en Observaciones.
- Web, Verificación en 2 pasos y Sesiones abiertas.

## [0.0.15] - 2026-11-07
### Agregado
- **Notificaciones push (Firebase)**: proyecto `asistente-red-idbi` creado y probado de punta a punta (el gateway envía y el celular recibe «Minuta rechazada»). Falta cargar la credencial del servidor en Railway.
- **Modo sin conexión**: las consultas (Inicio, Reportes, Notificaciones, Panel…) se guardan en el celular y se ven sin señal con la barra «Sin conexión»; el monitor de red detecta bien el corte. Lo guardado se borra al cerrar sesión.
- **Borradores (evaluaciones sin terminar)** en Reportes con su avance («31 % · sin minuta todavía»), ficha de solo lectura y «Pedir al técnico que la termine»; cuentan en **Pendientes** de Inicio.
- **Lo que falta · por prioridad** al filtrar Reportes por un técnico: primero lo que falta validar (menor % primero), luego rechazadas, pendientes y sin terminar.
- **Topología en la minuta: Incluida / No se pudo mapear** (Editar): si no se pudo mapear no sale en el PDF y el supervisor lo ve en la notificación de validación como tarea pendiente del cliente.
- **Avisarme cuando…** en Notificaciones, según el rol; se respeta al enviar el push.
- **Asistente IA**: «¿Qué me falta?» / «¿cómo voy?» muestra **Tu progreso**; un cambio «para todas las minutas» no se aplica desde el chat: el administrador abre Preguntas y reglas y los demás **envían una sugerencia al administrador**.
- **Bitácora**: queda registrado generar el PDF y compartir (correo y menú Compartir); anular un borrador lo **archiva** (no se borra) y el administrador lo ve en **Archivadas** y lo **restaura**.
### Corregido
- Fechas de las tarjetas de Reportes en formato legible.
### Pendiente
- Orden y borrado de las preguntas base del Chat IA (el motor las arma por código), aviso de AnyDesk en la notificación, responder el Chat IA sin conexión.

## [0.0.14] - 2026-11-07
### Corregido
- **Barras del sistema en Android 15**: los botones del sistema ya no tapan la barra de escritura ni los botones de abajo, y las cabeceras ya no quedan bajo la hora y los íconos. La barra de estado se pinta con el azul de la cabecera y el teclado empuja la pantalla en todas las vistas (Asistente IA, Chat IA y el resto).
- Se quitó el relleno fijo de 24–30 dp que cada cabecera llevaba para la barra de estado.

## [0.0.13] - 2026-11-07
### Agregado
- **Bitácora de la minuta** (Ver minuta › ⋮ y Reportes): resumen (creada por y con qué, estado, rechazada por con el motivo y quién la corrigió, validada por), quiénes la editaron en las últimas 48 horas e historial con el detalle en palabras simples.
- **Bloqueo de edición**: mientras el supervisor o el administrador edita una minuta con el Asistente IA, el técnico ve «En edición por …» y no puede editarla; se libera al aplicar o cancelar (y vence a los 30 minutos).
- **Editar plazos** (administrador, Panel › Tiempos): minutos por tipo de local de «antes de la app» y costo por hora; alimentan la comparación **mes anterior vs mes actual**, la mejora promedio y el ahorro estimado.
- **Foto de perfil**: Mi perfil permite subir (JPG/PNG, hasta 3 MB) o quitar la foto; se ve en la cabecera del menú, y en las listas siguen las iniciales.
- **Preguntas y reglas**: pestañas Preguntas › Reglas › Mapa de nodos. Preguntas muestra todas las del Chat IA por sección, completas y numeradas; las agregadas se suben o bajan con ↑ ↓ y Deshacer. El Mapa de nodos muestra Inicio → secciones → Fin con fichas que llevan a cada sección y el detalle de cada pregunta.
- **Modo sin conexión (primera parte)**: barra «Sin conexión · se guarda en el celular · N por subir», cola en el celular para solicitar la validación y para las fotos del Asistente IA, subida automática al volver la señal y apartado «Modo sin conexión y sincronización» en Mi Cuenta.
### Pendiente
- Las preguntas base del Chat IA se arman por código en el motor: se ven, pero su orden y el borrado por deslizamiento en el mapa no se cambian desde la app.
- Sin conexión todavía no se puede responder el Chat IA ni redactar con el Asistente IA (necesitan el servidor).

## [0.0.12] - 2026-11-07
### Agregado
- **Cambio de contraseña con aprobación del administrador** (estilo Google): el técnico pide el cambio, al administrador le llega la notificación con un código de 2 dígitos y el técnico elige ese número entre 3. Vence a los 10 minutos; un número equivocado rechaza la solicitud.
- **Equipo y accesos**: tarjeta «Solicitudes de contraseña» con **Ver código** y **Anular**, botón **ⓘ** con lo que se puede hacer en el módulo (sin texto largo arriba).
- **Acceso temporal a minutas de otros**: el técnico pide la minuta de otro técnico a su supervisor y el supervisor la de otro equipo al administrador, con un motivo. Quien aprueba genera un código de 6 dígitos (24 horas, solo esa minuta y solo para quien la pidió; se puede anular) y quien pidió la ve en solo lectura desde Reportes › Ingresar código.
- **Pedir un cambio al Asistente IA** desde la ficha de Reportes: abre el Asistente con la minuta cargada («Actualizando: …»).
- **Plan** del menú lateral (administrador): plan Empresa, usuarios usados (N de 10) y la lista de personas.
### Cambiado
- Quien aplica cambios a una minuta con el Asistente IA decide su estado: si es el técnico, vuelve a Pendiente; si es el supervisor o el administrador, sigue por validar (Completa) y el técnico recibe «El supervisor … actualizó la minuta a v{n}».

## [0.0.11] - 2026-11-07
### Agregado
- **Reportes programados** del Asistente IA: «cada fin de mes» guarda la consulta y el último día de cada mes llega por notificación; se cancela desde la tarjeta.
- **Inicio de supervisor y administrador**: «Recientes» muestra las últimas minutas del equipo o de la empresa con el técnico, la fecha y el estado.
- **Menú lateral**: punto rojo y «Nuevo» en Notificaciones cuando hay avisos sin leer, cuadro **Plan · Empresa · N de 10 usuarios** para el administrador y Modo noche debajo de Mi Cuenta.
### Cambiado
- **Inicio** como el prototipo: cabecera con el menú, «Hola de nuevo,» con DEMO, el nombre y la campana (sin avatar, robot ni rol); indicadores sin íconos.
- **Mi Cuenta** reemplaza a «Perfil y configuración»: arriba el usuario con su rol y empresa (abre Mi perfil); se quitó Apariencia (el Modo noche está en el menú) y Soporte queda con Ayuda y Versión.
- Texto del Asistente IA y de Reportes en el Inicio según el rol (empresa, equipo, mis minutas).

## [0.0.10] - 2026-11-07
### Agregado
- **Perfil y configuración › Empresa**: nombre y logo (PNG o JPG, hasta 2 MB) que salen en la cabecera de la minuta y del PDF; solo el administrador los cambia; «Restablecer IDBI».
- **Ambiente (administrador)**: «Modo demo» y **Pasar a producción** (irreversible, se escribe «PRODUCCIÓN»). Reportes, Inicio y Panel empiezan en 0; se conservan las minutas demo Completas, En revisión y Validadas y dejan de verse las demás; la base de conocimiento de la IA no se borra. La etiqueta **DEMO** aparece junto al saludo mientras no se pase a producción.
- **Copia de seguridad (administrador)**: frecuencia (diaria, semanal, mensual), correo, «Hacer copia ahora» e historial con descarga (ZIP con las minutas en JSON y Excel).
- **Trazabilidad**: bitácora de solo inserción (crear, editar, solicitar, validar, rechazar, avisos, empresa y copias), visible para el administrador en Perfil y configuración; resumen por minuta para su equipo.
- **Prompts frecuentes ★ en la cuenta**: se guardan en el servidor y se ven igual desde cualquier teléfono.
- **Panel v2**: un solo filtro general (Hoy, Ayer, 7 días, 14 días, Mes, Todo y Rango de fechas) para indicadores (total vs periodo anterior, por validar con riesgo, pendientes, en revisión, validadas y horas ahorradas), apartados **Por día · Estados · Tiempos · Alertas · Negocios** (el Panel se queda en el apartado), avance y tiempos del equipo (tocar un técnico abre Reportes filtrado, «Reporte y mora»), **Por validar primero** y **Actividad reciente**. Las alertas abren la consulta en el Asistente IA.
- **Fotos del Asistente IA**: las evidencias adjuntas se suben a la minuta que se redacta (reducidas a 1600 px), se ven en «Ver minuta» y dejan de figurar como «por subir».
### Cambiado
- Los errores con código propio (por ejemplo 409 al pasar dos veces a producción) ya no se convierten en 500.

## [0.0.9] - 2026-11-07
### Agregado
- **Asistente IA completo** (doc 01): conversación libre sin botones de opciones; 📎 adjuntar evidencias por tipo, 🎤 audio que se transcribe y se revisa antes de enviar, reiniciar la conversación y tarjetas según lo que se pide:
  - **Borrador de minuta**: datos del cliente, conexiones de red (cable, WiFi o USB; si un enlace cambia gana el último medio), contenido y evidencias que faltan; «Agregar más» y «Redactar minuta».
  - **Actualizar minuta**: «Actualiza la minuta de [cliente] del [fecha]: …» muestra los cambios antes de aplicarlos, con la casilla «Fue una nueva visita al local», y sube la versión.
  - **Base de datos**: fecha de visita, observaciones, recomendaciones e historial de una minuta (el técnico solo ve el detalle de las suyas).
  - **Pendientes del día**, **minutas por validar** y **reportes** por periodo, con descarga en archivo que se abre en Excel.
  - Preguntas abiertas con ChatGPT y las reglas de la empresa como contexto.
- **Inicio por rol**: «Hola de nuevo,», indicadores reales (técnico: Pendientes · En revisión · Esperando validación; supervisor y administrador: Por validar · Pendientes · En riesgo), tarjeta **Asistente IA** y Reportes filtrado al tocar un indicador.
- **Notificaciones por rol**: quién envió, local, puntaje y hora; ver minuta, ✓ validar y ✕ rechazar; filtros con cantidad, orden por criticidad o fecha y «Otros avisos». El técnico ve el estado de sus minutas.
- **Recordatorios** del supervisor y del administrador al técnico («Supervisor … te pide: termina la minuta de …»).
- **Reportes del Asistente IA en tabla** (Local, Técnico, Supervisor, Estado, Puntaje, Fecha) con filtros por estado (incompletas, por validar, en revisión, validadas, en riesgo), mes y persona (técnico o equipo de un supervisor); se ajustan sin repetir todo («solo el mes anterior», «sin octubre», «de María Quispe»). **Descargar Excel (.xlsx)** con título, fecha de generación, encabezados con filtro, columna Dirección y puntaje numérico.
- **Prompts frecuentes ★**: la estrella junto a cada mensaje lo guarda; la estrella de arriba abre la lista y escribe el prompt en el chat sin enviarlo.
- **Pedir al técnico** desde la ficha de Reportes: «Pedir que la termine», «Pedir que la corrija» y, solo el administrador, «Recordar al supervisor».
### Cambiado
- Íconos de línea del diseño (clip, micrófono, ojo, topología, ✓, ✕, enviar) en lugar de emojis; sombras de las tarjetas del Inicio como en el prototipo.
- Menú lateral: Inicio, Panel, Notificaciones; sin «Propuestas» (las minutas por validar se ven en Notificaciones).
- La orden «Redacta una minuta nueva» ya no se cuela como observación.

## [0.0.8] - 2026-11-06
### Agregado
- **Fotos obligatorias que ya no traban el Chat IA** (Parte 5): cuando la IA dice «no corresponde» o «no es legible», el técnico puede **Guardar como correcta** con una nota opcional y atajos («La foto es correcta», «Aún no hay puntos», «El cliente no permite», y «Local en obra» si el local está en obra o por inaugurar). En el anexo de la minuta solo se muestra la nota.
- **Subir después** en las fotos obligatorias de la lista del local: el encabezado cuenta «N obligatorias por completar» y al terminar aparece «Subir fotos pendientes (N)». La minuta no se genera mientras falte alguna.
- **Administrador**: Panel de gestión, Equipo y accesos (roles y supervisor) y Preguntas y reglas (activar, editar, crear y eliminar reglas; nombre del sistema de impresiones).
- **Ayuda** del Chat IA con hoja de consulta y **menú lateral por secciones** según el rol.
- **Modo noche** con interruptor en el menú lateral y en Perfil y configuración.
### Cambiado
- La IA rechaza menos fotos buenas: sabe qué lugar es («un lugar de preparación llamado «Calientes»») y el estado del local, y solo rechaza lo que claramente no es un lugar del local.
- El nombre del sistema de impresiones definido por el administrador llega al motor del chat.

## [0.0.7] - 2026-11-02
### Agregado
- **Asistente IA** (módulo nuevo): conversación libre por texto o audio, sin botones de opciones. Redacta minutas con evidencias y contexto, actualiza minutas generadas, responde consultas sobre la base de minutas y da reportes. Responde y analiza con **ChatGPT (API de OpenAI)** usando como contexto las reglas de la app y las minutas.
- Conexiones de red contadas por audio o texto que se dibujan en la topología.
- Ubicación **GPS** con la dirección: enlace al mapa, dirección encontrada, distrito y departamento, y coincidencia con el nombre del local.
- **Empresa configurable**: logo y nombre (IDBI por defecto). El nombre del **sistema de impresiones** (Wrapper en IDBI) se define en Preguntas y reglas.
- Recuperación de contraseña con **aprobación del administrador** (elige el número que ve el usuario).
- Gestión desde el asistente: minutas por validar, pendientes del día, reportes por periodo y reportes programados (por ejemplo, cada fin de mes).
- **Ayuda con ChatGPT y fotos**: preguntas libres y foto del equipo; la IA dice marca, modelo y si el switch es administrable.
- **Evidencias flexibles**: el técnico toma la foto y dice a qué pertenece; por impresora solo el selftest es obligatorio; la IA ordena el anexo y completa datos desde la imagen.
- **Ubicar puerto del switch** sin desconectar cables: la app hace parpadear la luz del puerto de cada impresora (cualquier switch) o lee la tabla de MAC del switch (administrable), y mide la calidad de cada punto de red.
- Pregunta de **rotulado** de los puntos en el switch y **nombres de zonas** definidos una sola vez.
- **Reglas de la revisión de la minuta de El Chinito**: topología y métodos de pago solo si se consideran; nombres simples; equipos apagados no son «con falla»; solo equipos de la implementación (más repetidores, AP y cámaras); IP fijas de las impresoras; reserva de IP con el proveedor; lo resuelto en la visita va en Pasos a seguir y sube el porcentaje; UPS solo con intermitencias de energía; tabla de impresoras hacia el switch bajo consulta; fotos de documentos transcritas con consulta; ubicación exacta con observación.
- **Reunión con el CEO (23/10/2026)**: bitácora de trazabilidad y minutas archivadas (nada se borra); «Revisar primero» para el supervisor (alertas y menor porcentaje); «No se pudo mapear la red» con aviso solo en la notificación; un solo botón de compartir; «¿qué me falta?» en el Asistente IA; modo sin conexión (offline-first) con cola de sincronización; copias de seguridad automáticas al correo de la empresa con frecuencia configurable; versión web del Chat IA y del Asistente IA con la misma cuenta y datos que la app.
- **Nodos que no traban**: editar una respuesta y seguir donde estabas; nota opcional en cada evidencia; foto dudosa «Guardar como correcta»; iMin/Sunmi con o sin impresora integrada (foto si no sabe el modelo); AnyDesk opcional, salvo el del equipo con el sistema de impresiones, que se pide antes de la minuta con opción de no considerarlo.
### Cambiado
- Asistente IA: «Nueva minuta» pregunta Implementación o Soporte / incidencia; flujo de soporte con antecedentes del cliente, motivo, alcance, evidencias leídas, lo realizado, reglas de soporte (S1–S4) y estado final; minuta de soporte con validación del supervisor y versiones.

- Panel: «Reporte y mora» en Avance y tiempos del equipo (app = web); las pestañas del Panel ya no regresan al inicio del módulo.
- Asistente IA (administrador): «¿Qué aprendiste hoy?» con reglas y preguntas sugeridas de las visitas de soporte e información; las agrega con «procede»; guarda el conocimiento y genera la ficha de aprendizaje en PDF.

- Reportes unificado app/web: pestañas por rol y por Roles y permisos (General, Por técnico o Mis técnicos, Mis minutas), filtros de arriba que recalculan lista e indicadores, trazabilidad y Editar plazos también en la app.
- Minuta: tabla «Accesos y contraseñas» (red WiFi del router, portal web de impresoras, repetidor); las contraseñas sí se muestran.

- Actualización de minutas: datos del local en el Chat IA (visita) o el Asistente IA (después, nueva versión); criterios de todas las minutas solo en Preguntas y reglas o por desarrollo, con sugerencia al administrador desde el chat.

- Evaluación: paso opcional «Ubicar el puerto del switch» de cada impresora (pings), con calidad del punto de red y rotulado; tabla Switch · puertos en la minuta.

- Minuta: ubicación del router con detalle, recomendación breve «Reubicación del router», regla de cuello de botella con carga, seguimiento del técnico y «Por parte del cliente»; también desde el Asistente IA.

- Asistente IA: prompts frecuentes con estrella; al elegir uno se escribe en el chat sin enviarse.

- Asistente IA: reportes en tabla con descarga en Excel (.xlsx), según estado, mes, técnico o supervisor y el rol del usuario.

- Reportes web: pestaña Mis minutas para administrador y supervisor.

- Preguntas y reglas: orden Preguntas › Reglas › Mapa de nodos en la web; barra de secciones y vista de celular del mapa en la web; Detalle legible en la app.

- Mi Cuenta: «Avisarme cuando…» en la app; Sincronización, Soporte y Cerrar sesión en la web.

- Reportes por rol en app y web; acceso temporal (código de 24 h, con motivo, solo lectura): el técnico lo pide al supervisor y el supervisor al administrador.

- Sin módulo «Plan de la empresa»: el plan va en el menú lateral del administrador (app y web) y abre el plan con la opción de cambiar a un plan superior.

- Panel unificado app/web (filtro, indicadores, apartados por pestañas, equipo en horas, por validar primero, actividad); Notificaciones unificadas con íconos, filtros y orden; Modo noche en el menú y Cerrar sesión en la web.

- Menú por grupos (Principal, Seguimiento, Configuración, Mi cuenta) en app y web; «Mi Cuenta» reemplaza a «Perfil y configuración»; cabecera del menú web con Network Assistant y la tarjeta del usuario.

- Panel web: Inicio del administrador igual al de la app (indicadores, Nueva Evaluación, Asistente IA, Continuar, Reportes y Recientes, con las mismas acciones); es la primera opción del menú y la pantalla al iniciar sesión.

- Simulador: la vista Web muestra el panel web de su diseño (sin cambios) en lugar de las pantallas de la app.

- Pasar a producción (administrador, irreversible): reportes e indicadores en 0, minutas demo conservadas (Completa, En revisión, Validada) sin contar, base de conocimiento de la IA intacta.

- Bitácora de la minuta: resumen (creada, rechazada, corregida, validada), editores de las últimas 48 h e historial con qué parte se editó.

- «Revisar primero» ordenado de mayor a menor criticidad con ⓘ; minutas NO APTAS con puntaje máximo de 59 %; pregunta P18u y regla del router en un espacio cerrado (apto con seguimiento).

- Inicio: indicadores por rol que no se reinician cada día (Por validar solo con técnicos a cargo, Pendientes propias, En riesgo sin validar) y que llevan a Notificaciones o a Reportes filtrado.

- Panel v2: rango (7/14/30/Todo/fechas), avance por periodo que abre Reportes por técnico con lo que falta y recordatorios, barras con fechas y cifras, estados con cantidades, tiempo por minuta antes/app o mes anterior/actual, tiempos de técnicos y supervisores.

- Equipo y accesos rediseñado: ⓘ con la ayuda del módulo, lista agrupada sin superposición, filtros, asignar supervisor con chips y «Ver código» con la notificación tipo Google.

- Panel: sin «Por validar primero» (está en Inicio); nuevo Avance de hoy por técnico, Indicadores que alertan (abren el Asistente IA), ahorro estimado y, para el administrador, Por supervisor y Plan y usuarios.

- Menú del administrador reordenado (app y web), sin Ayuda ni Mi perfil; Mi perfil se abre desde el avatar y el nombre; «Contactar ayuda» en Perfil y configuración (próximamente).

- Preguntas y reglas: pestañas Preguntas › Reglas › Mapa de nodos; preguntas reordenables (⋮⋮ y ↑ ↓) y editables en la misma pestaña; reglas editables (activar, editar, eliminar, nueva) con la regla SISIMP del sistema de impresiones; deslizar una ficha del mapa la elimina.
- Editar minuta: «Pedir un cambio al Asistente IA» con «Cambios que haré»; el supervisor la mantiene por validar y se avisa al técnico.

- El chat guiado de la evaluación se llama **Chat IA** (antes «Asistente IA»).
- Estado final: solo el equipo con el sistema de impresiones por WiFi es observación; PC, laptop o tablet sin el sistema por WiFi no impide iniciar la operación. Las observaciones resueltas en la visita dejan el local **APTO** y el resumen lo menciona.
- Minuta: sin el texto «generada por el Asistente Virtual de Red», con logo y nombre de la empresa, sin la fila de correo si no hay correo, «Distrito / Departamento» en lugar de «Ciudad», anexo de evidencias agrupado y compacto, historial de actualizaciones y fecha de la nueva visita cuando corresponde.
### Quitado
- «Crear cuenta nueva» en la app: las cuentas las crea el administrador desde la web.
