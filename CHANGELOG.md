# Changelog

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
### Cambiado
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
