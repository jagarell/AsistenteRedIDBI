# Changelog

## [0.0.7] - 2026-11-02 (propuesta, sin publicar)
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
