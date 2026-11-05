# Mejoras de la reunión con el CEO (23/10/2026) y nodos que no traban

## 1. Bitácora (trazabilidad)
- Cada acción queda registrada con fecha y hora, persona y rol: redactar, actualizar, corregir una respuesta, editar datos o propuesta, generar o descargar el PDF, compartir, solicitar la validación, validar, devolver, marcar «resuelto», cambiar la copia de seguridad y anular.
- **Nada se borra:** anular un borrador lo **archiva**. El administrador lo puede **restaurar**.
- Ver minuta › **Bitácora de la minuta** (todos los roles). Configuración › **Bitácora de la empresa** (administrador), con las minutas archivadas.

## 2. Prioridad y alertas para el supervisor
- Inicio del supervisor: tarjeta **«Revisar primero»** con las minutas por validar, ordenadas por alertas y por menor porcentaje.
- Alertas: **No apto**, **Internet bajo el 70 %**, **Observado**, **Sin topología**, **Puntaje bajo** (< 60 %).
- «Minutas por validar» del Asistente IA usa el mismo orden.

## 3. «No se pudo mapear la red»
- Editar › **Topología en la minuta: Incluida / No se pudo mapear**.
- Si no se pudo mapear, la topología **no sale en la minuta**.
- Al **solicitar la validación**, la notificación al supervisor agrega: «⚠️ Sin topología de red: no se pudo mapear. Queda como tarea pendiente del cliente: enviar su mapa de red». **Solo en la notificación**, nunca dentro de la minuta.

## 4. Un solo botón de compartir y «¿qué me falta?»
- Ver minuta: se quitan los botones de WhatsApp y Email; queda **Compartir** (que incluye WhatsApp, Gmail, Outlook, Drive…). Propuesta técnica: se quita WhatsApp de la barra; en ⋮ se quita «Compartir a».
- Asistente IA: «¿qué me falta?», «¿cómo voy?» → tarjeta **Tu progreso**: por cada evaluación en curso o minuta pendiente, el porcentaje, la siguiente pregunta, las evidencias saltadas, las obligatorias por subir y si falta solicitar la validación.

## 5. Sin conexión (offline-first)
- La app trabaja **primero en el celular** (base local) y sincroniza cuando hay señal.
- Barra superior «📵 Sin conexión · se guarda en el celular · N por subir» con **Ver**.
- Sin conexión se puede: responder el Chat IA, tomar fotos, redactar con el Asistente IA, editar y solicitar la validación. Quedan **en cola**: la lectura de fotos con IA, compartir, WhatsApp, las consultas y reportes del asistente, y la copia de seguridad.
- Al volver la señal: «Sincronizando N cambios…» y luego «se subieron N cambios. La IA ya revisó las fotos pendientes».
- Configuración › **Modo sin conexión y sincronización**: estado, última subida y cola. Las fotos se suben **con WiFi o con datos móviles** (no hay opción «solo con WiFi»), para no frenar el levantamiento en el Chat IA ni en el Asistente IA.

## 6. Copias de seguridad automáticas (administrador)
Configuración › **Copia de seguridad** (como en WhatsApp):
- Frecuencia: **Desactivada / Diaria / Semanal / Mensual** y hora.
- **Correo de la empresa** que recibe la copia.
- **Incluir fotos y PDF** (sí/no).
- «Última copia» y «Próxima copia», y el botón **Hacer copia ahora**.

## 7. Versión web sincronizada
- La web tiene los mismos módulos que la app (Inicio, **Chat IA**, **Asistente IA**, Reportes, Notificaciones, Preguntas y reglas, Configuración) con la **misma cuenta y los mismos datos**.
- Lo que se hace en la app aparece en la web y al revés. La web muestra «✓ Sincronizado con la app» o «La app está sin conexión: lo nuevo llegará al reconectar».
- Los módulos que ya existían en la web (Reportes, Preguntas y reglas, Configuración, Minutas) se actualizan a lo que tiene la app hoy.

## 8. Nodos que no traban al técnico
| Situación | Cómo queda |
|---|---|
| La IA dice que la foto no corresponde, pero el técnico sabe que sí | **Guardar como correcta**, con nota opcional; la minuta y la web la muestran marcada como guardada por el técnico |
| Evidencia con texto | Campo **Nota (opcional)** en «Esto leí en la captura»; la nota sale en Observaciones de la minuta |
| Quiero corregir una respuesta anterior | Tocar la respuesta → se cambia **solo esa**; lo demás se mantiene y **sigues donde estabas** (si el cambio abre preguntas nuevas, se preguntan después) |
| iMin o Sunmi | Pregunta «¿Tiene impresora integrada?»: Sí / No, solo es pantalla / **No sé el modelo (enviar foto)**. Con la foto, la IA dice si tiene impresora. Si no tiene, recomendación «se necesita una impresora de caja» |
| AnyDesk | Solo importa el de **caja**. Se puede **saltar**: los AnyDesk saltados **no salen como faltantes** en la minuta. El del **equipo con el sistema de impresiones** se pide antes de generar el PDF o solicitar la validación: **Subir la foto ahora** o **No considerarlo en la minuta** (y no sale como faltante) |

## 9. Criterios de aceptación
- **Bitácora:** Dado que el técnico solicita la validación, entonces la bitácora de la minuta muestra «Solicitó la validación» con su nombre, rol, fecha y hora.
- **Archivo:** Dado un borrador anulado, entonces aparece en «Archivadas» y el administrador lo puede restaurar.
- **Prioridad:** Dadas dos minutas por validar de 83 % (No apto) y 58 % (Puntaje bajo), entonces «Revisar primero» muestra primero la que tiene la alerta más grave.
- **Sin topología:** Dada la opción «No se pudo mapear», cuando se solicita la validación, entonces la notificación al supervisor menciona la tarea pendiente del cliente y el PDF no tiene la sección de topología ni ese aviso.
- **Compartir:** Ver minuta tiene un solo botón Compartir.
- **Offline:** Dado el celular sin conexión, cuando el técnico solicita la validación, entonces el cambio queda en la cola y, al volver la señal, la cola queda en 0.
- **Copia:** Dado un correo inválido, cuando se toca «Hacer copia ahora», entonces se muestra «Escribe un correo válido para la copia».
- **Editar y seguir:** Dada la pregunta 6 en curso, cuando el técnico corrige el RUC (pregunta 2), entonces después de responder vuelve a la pregunta 6 sin repetir las 3, 4 y 5.
- **iMin:** Dada la foto de un equipo sin salida de papel, entonces la respuesta queda «No, solo es pantalla (leído en la foto)» y aparece la recomendación de impresora de caja.
- **AnyDesk:** Dado el AnyDesk de la PC con Wrapper saltado, cuando se genera el PDF, entonces se pregunta «Subir la foto ahora / No considerarlo en la minuta»; con «No considerarlo», el PDF no muestra el AnyDesk ni como pendiente.

## 10. Notificación del supervisor y Ver minuta
- La notificación de **solicitud de validación** agrega, solo en el texto de la notificación (nunca en la minuta):
  - «⚠️ Sin topología de red: no se pudo mapear. Queda como tarea pendiente del cliente: enviar su mapa de red».
  - «⚠️ Sin AnyDesk del equipo con {sistema de impresiones} (Caja k): el técnico no lo subió» (cuando el técnico eligió «No considerarlo en la minuta»).
- Cada notificación de minuta tiene: **Ver minuta**, **Ver topología** (bloqueado si no hay topología), y **✓ Validar** / **✕ Rechazar** en la misma notificación (se mantienen para el flujo rápido). Si ya no está por validar, muestra su estado.
- **Ver minuta** abre la minuta en pantalla (app y web) **sin descargarla**. El supervisor tiene **Compartir**, **Validar** y **Rechazar**; en la **web** también **Editar**.
- **Pendientes de hoy** (Asistente IA), según el rol:
  - Técnico: minutas **rechazadas** (con el motivo), evaluaciones **sin terminar** (con su avance) y minutas **por solicitar la validación**. Si no hay nada: «No tienes pendientes hoy».
  - Supervisor/administrador: minutas por validar, rechazadas en espera de corrección, solicitudes de acceso, cambios de contraseña (administrador) y reportes programados.

**Criterios de aceptación**
- Dada una minuta sin topología, cuando el supervisor abre la notificación, entonces «Ver topología» está deshabilitado y el texto dice la tarea pendiente del cliente.
- Dado «No considerarlo en la minuta» para el AnyDesk del equipo con Wrapper, entonces la notificación lo menciona y el PDF no.
- Dado un técnico sin evaluaciones en curso, minutas pendientes ni rechazadas, cuando pregunta «¿Cuáles son mis pendientes de hoy?», entonces el asistente responde que no tiene pendientes.

## 11. Preguntas y reglas (administrador, app y web)
Orden de las pestañas: **Preguntas › Reglas › Mapa de nodos** (igual en la app y en la web).

### Preguntas
- Las preguntas se ven primero. Aviso único: «Toca una pregunta para editarla o cambiar su orden.» (ya no aparece el sistema de impresiones aquí).
- Cada pregunta se ve **completa sin tocarla**: número de orden en su sección, texto entero y debajo «P01 · tipo · opciones · opcional».
- Tocar una pregunta la abre **en la misma pestaña** (texto, tipo, sección, obligatoria, opciones, cuándo aparece, salidas), con «← Preguntas» para volver.
- Cambiar el orden: arrastrar desde **⋮⋮** (web y tablet) o con **↑ ↓** (celular). ↑ deshabilitado en la primera y ↓ en la última de cada sección.
- Al mover: se guarda para **Deshacer**, aviso «P01c ahora va en A · posición 1…» y registro en la bitácora. Llega al Chat IA al **Publicar**.
- Android: `RecyclerView` + `ItemTouchHelper` desde el asa; botones con `contentDescription` «Subir P01» / «Bajar P01». API: `PUT /flujo/borrador/orden` `{ "pregunta": "P01c", "seccion": "A", "posicion": 0 }`.

### Reglas
- Cada regla muestra: ID · nombre, tipo (Observación / Recomendación / Aviso), **cuándo aplica**, **qué dice en la minuta**, interruptor **activa/desactiva**, **Editar** y **Eliminar** (con confirmación). Botón **+ Nueva regla**.
- Una regla desactivada o eliminada no sale en las minutas nuevas; una regla editada usa el texto del administrador.
- Regla fija arriba: **SISIMP · Sistema de impresiones** (Aviso, activa por defecto) con el campo **Sistema de impresiones de la empresa** (por defecto «Wrapper»). Qué hace:
  - antes de generar la minuta pide la foto del **AnyDesk del equipo donde está el sistema de impresiones**;
  - si el técnico elige «No considerarlo», se avisa solo en la notificación al supervisor, no en la minuta;
  - el nombre se usa en las preguntas, en las reglas y en las respuestas del asistente.
  - Desactivada: ya no se pide ese AnyDesk.
- API: `GET/POST /reglas`, `PUT /reglas/{id}` `{ "nombre", "tipo", "cuando", "texto_minuta", "activa" }`, `DELETE /reglas/{id}`; `PUT /empresa/sistema-impresiones` `{ "nombre": "Wrapper" }`.

### Mapa de nodos
- Deslizar una ficha **a la izquierda o a la derecha** fuera de la vista **elimina la pregunta** (las conexiones que llegaban a ella quedan sin conexión y Revisión las muestra). Un deslizamiento corto la devuelve a su lugar. **↶ Deshacer** la recupera.
- Android: `ItemTouchHelper.SimpleCallback` con `ItemTouchHelper.LEFT or RIGHT` y umbral de 40 % del ancho.

**Criterios de aceptación**
- Dada la app o la web, cuando el administrador abre Preguntas y reglas, entonces las pestañas son Preguntas, Reglas y Mapa de nodos, en ese orden.
- Dada la sección A con P01, P01b, P01c, cuando toca ↓ en P01, entonces el orden es P01b, P01, P01c.
- Dada la regla SISIMP activa con «Wrapper POS», cuando un técnico no sube el AnyDesk de ese equipo, entonces se le pide antes de generar la minuta; con la regla desactivada, no se le pide.
- Dada la regla R60 desactivada, cuando se genera una minuta con PC temporal, entonces la recomendación R60 no aparece.
- Dado el mapa, cuando el administrador desliza P01b fuera de la vista, entonces P01b se elimina y ↶ Deshacer la recupera.

## 12. Editar una minuta: «Pedir un cambio al Asistente IA»
- No importa si la minuta se hizo con el **Chat IA** o con el **Asistente IA**: es la misma minuta y se edita igual. El origen solo queda en la **bitácora** («Creada con Chat IA / Asistente IA»).
- En **Editar**, la opción «Pedir un cambio en el chat» pasa a **«Pedir un cambio al Asistente IA»** (texto o audio). Siempre abre el **Asistente IA** con la minuta ya cargada: encabezado «Actualizando: {local} · v{n}».
- El asistente muestra **«Cambios que haré»** antes de aplicar (datos, observaciones, recomendaciones, pasos, evidencias y **topología**). Botones **Corregir** (devuelve el texto para cambiarlo) y **Aplicar**. Una indicación de conexión («La impresora de cocina está conectada por cable al switch») cambia la topología y no se copia como observación.

| | Técnico | Supervisor |
|---|---|---|
| Opciones en Editar (app) | Todas | Solo «Pedir un cambio al Asistente IA» y «Topología en la minuta» |
| Opciones en Editar (web) | Todas | Todas (editor completo) |
| Estado al aplicar | Vuelve a **Pendiente**: hay que solicitar otra vez la validación | Sigue **por validar** (Completa); puede validarla al terminar |
| Aviso | — | Notificación al técnico: «El supervisor actualizó la minuta a v{n}: …» |
| Bitácora | «Actualizó la minuta a v{n} con el Asistente IA» · Técnico | Igual · Supervisor |

- Mientras el supervisor edita, el técnico ve «En edición por el supervisor» y no puede editar esa minuta hasta que se aplique o cancele.
- API: `POST /minutas/{id}/cambios/plan` `{ "texto", "audio_url", "evidencias" }` → lista de cambios (ChatGPT); `POST /minutas/{id}/cambios/aplicar` `{ "plan_id" }` → nueva versión. El estado resultante lo decide el rol del token. `POST /minutas/{id}/bloqueo` y `DELETE /minutas/{id}/bloqueo`.

**Criterios de aceptación**
- Dada una minuta Completa, cuando el supervisor pide «La impresora de cocina está conectada por cable al switch» y aplica, entonces la topología cambia, la versión sube, el estado sigue Completa y el técnico recibe la notificación.
- Dado el mismo cambio hecho por el técnico, entonces la minuta vuelve a Pendiente.
- Dada una minuta que el supervisor está editando, cuando el técnico toca Editar, entonces ve que está en edición por el supervisor y no se abre el editor.
- Dada una minuta creada con el Chat IA, cuando se toca «Pedir un cambio al Asistente IA», entonces se abre el Asistente IA con esa minuta cargada.

## 13. Menú del administrador, Mi perfil y «Contactar ayuda» (app y web)
- **Orden del menú del administrador** (menú lateral de la app y barra lateral de la web): Inicio, **Panel**, Notificaciones, Reportes, Nueva evaluación, Asistente IA, Equipo y accesos, Preguntas y reglas, Perfil y configuración. Abajo, Cerrar sesión.
  - Ya no están en el menú del administrador: **Ayuda**, **Plan de la empresa** (está dentro de Equipo y accesos) ni **Mi perfil**.
  - En la web, el supervisor también tiene **Panel** después de Inicio.
- **Mi perfil** sale del menú (todos los roles). Se abre:
  - tocando la tarjeta con el avatar (AD) y el nombre en **Perfil y configuración**;
  - tocando el avatar o el nombre en la cabecera del menú (en la web, el nombre al pie de la barra lateral);
  - desde la fila «Mi perfil» de la sección Cuenta.
  - Muestra lo mismo que antes: evaluaciones, evaluaciones del mes, con análisis, información personal y **Editar perfil** (cambiar nombre, teléfono, ciudad).
- **Ayuda** (soporte de la app): fila en Perfil y configuración › Cuenta, debajo de «Mi perfil» y arriba de «Versión». En 0.0.7 muestra «llegará en una próxima versión»; a futuro abrirá el canal de soporte.
- Android: `menu/drawer_admin.xml` con el nuevo orden; `NavigationView` header con `setOnClickListener` → `PerfilFragment`; en `ConfiguracionFragment` la tarjeta del usuario navega a `PerfilFragment`.

**Criterios de aceptación**
- Dado un administrador, cuando abre el menú, entonces ve Inicio, Panel, Notificaciones, Reportes, Nueva evaluación, Asistente IA, Equipo y accesos, Preguntas y reglas y Perfil y configuración, en ese orden, sin Ayuda ni Mi perfil.
- Dado Perfil y configuración, cuando toca la tarjeta con su avatar y nombre, entonces se abre Mi perfil.
- Dado Perfil y configuración, cuando toca «Ayuda», entonces ve que llegará en una próxima versión.
- Dada la web, cuando el administrador toca su nombre en la barra lateral, entonces se abre Mi perfil.

## 14. Panel de gestión (administrador y supervisor)
Según la reunión con Álvaro: el supervisor no quiere ver todas las minutas, quiere **información para decidir** (cuántas lleva cada técnico hoy, qué indicadores alertan) y el **ahorro** que da la app.
- **Sale «Por validar primero»** del Panel: ya está en Inicio como «Revisar primero».
- Orden: rango (7 / 14 / 30 días) → indicadores (minutas, por validar, pendientes, en revisión, validadas, horas ahorradas) → **Avance de hoy** → **Indicadores que alertan** → minutas por día → minutas por estado → tiempo por minuta con **ahorro estimado** → (solo administrador) **Por supervisor** y **Plan y usuarios**.
- **Avance de hoy**: por técnico, minutas terminadas frente a la meta diaria (3, se configura en la web), cuántas faltan y cuántas están en curso. El supervisor ve solo su equipo; el administrador, a todos.
- **Indicadores que alertan** (en el periodo elegido): internet por debajo de lo contratado, impresoras por WiFi, sin AnyDesk del equipo con el sistema de impresiones, sin topología de red y puntaje bajo (menos de 60 %). Tocar uno abre el **Asistente IA** con la consulta lista («Minutas con internet bajo de los últimos 14 días»), que responde con la lista de minutas.
- **Ahorro estimado**: horas ahorradas × costo por hora de técnico (por defecto S/ 25, configurable en la web).
- **Solo administrador**: «Por supervisor» (por validar y tiempo promedio en validar) y acceso a **Plan y usuarios** (ya no está en el menú).
- API: `GET /panel?dias=14` → `{ kpi, avance_hoy:[{tecnico, terminadas, en_curso, meta}], alertas:[{clave, cantidad}], por_dia, por_estado, tiempos, ahorro:{horas, soles}, por_supervisor? }`; `GET /minutas?alerta=internet_bajo&dias=14`.

**Criterios de aceptación**
- Dado el Panel, cuando lo abre el administrador o el supervisor, entonces no aparece «Por validar primero».
- Dado un supervisor con 3 técnicos, cuando abre el Panel, entonces «Avance de hoy» muestra solo a esos 3.
- Dado «Impresoras por WiFi» con 2, cuando lo toca, entonces el Asistente IA lista esas 2 minutas.
- Dado un supervisor, cuando abre el Panel, entonces no ve «Por supervisor» ni «Plan y usuarios».

## 15. Equipo y accesos (app y web)
- **Sin texto largo arriba**: el botón **ⓘ** de la cabecera abre «Lo que puedes hacer en este módulo» (ver al equipo, cambiar rol, asignar supervisor, atender cambios de contraseña, quitar o devolver acceso, agregar personas desde la web).
- **Solicitudes de contraseña** arriba, en una tarjeta: «{Nombre} quiere cambiar su contraseña», con **Ver código** y **Anular**.
- **Búsqueda** y filtros: Todos, Supervisores, Técnicos, Por atender (con cantidad).
- **Lista agrupada** en Supervisores y Técnicos. Cada fila tiene avatar con color por rol, nombre en una línea, subtítulo corto («Supervisor: Lucía Torres · hoy 08:15» o «2 técnicos a cargo») y estado solo si no está Activo («Pidió código», «Sin acceso»). Filas de 68 px, sin superposición.
- **Ficha de la persona** (al tocar la fila): rol con chips (Técnico, Supervisor, Administrador). Si es técnico, **Su supervisor** con chips de los supervisores, para asignarlo desde la app. Si es supervisor, **Su equipo**. Además, Quitar o Devolver acceso.
  - Si un supervisor deja de serlo, sus técnicos quedan «Sin supervisor asignado».
- **Ver código**: abre la notificación tipo Google con el **código de 2 dígitos**. El administrador se lo dice a la persona (en persona o por llamada); **en la app de la persona aparecen 3 números** y elige ese. Si coincide, se aprueba y crea su nueva contraseña; si elige otro o el administrador toca «No reconozco esta solicitud», se rechaza (ver sección 36).
- Android: `RecyclerView` con `ConcatAdapter` (cabeceras de grupo + filas), `ChipGroup` de filtros, `BottomSheetDialogFragment` para la ficha, `MenuItem` ⓘ en la `Toolbar`.
- API: `GET /usuarios?rol=&estado=&q=`, `PATCH /usuarios/{id}` `{ "rol", "supervisor_id", "activo" }`, `GET /auth/recuperacion?estado=esperando`.

**Criterios de aceptación**
- Dado el módulo, cuando el administrador toca ⓘ, entonces ve qué puede hacer y no hay texto de ayuda fijo arriba.
- Dado Carlos Pérez con Lucía Torres, cuando el administrador toca «Andrés Salas» en Su supervisor, entonces Carlos aparece con «Supervisor: Andrés Salas» y en el equipo de Andrés en el Panel.
- Dada la solicitud de María, cuando el administrador toca Ver código, ve el código; cuando María elige ese número en su app, entonces se aprueba y su estado vuelve a Activo.
- Dada una pantalla de 360 px, cuando se ve la lista, entonces ningún nombre se superpone con su avatar ni con otra fila.

## 16. Logo y nombre de la empresa en la minuta
- El logo y el nombre que el administrador carga en **Perfil y configuración › Empresa** son los que salen en la minuta:
  - en la cabecera de la primera página (logo a 46 px de alto + nombre), y
  - en el encabezado de cada página (logo pequeño + nombre, a la derecha).
- Si no hay logo, sale solo el nombre. «Restablecer» vuelve al logo y nombre por defecto (IDBI).
- API: `PUT /empresa` `{ "nombre" }` y `PUT /empresa/logo` (multipart, PNG/JPG hasta 2 MB); el generador del PDF lee ambos al momento de generar.

**Criterios de aceptación**
- Dado un logo nuevo y el nombre «ACME Redes», cuando se abre o descarga una minuta, entonces cada página muestra ese logo y ese nombre, y ninguna muestra el logo anterior.

## 17. Panel v2: rango, avance por periodo, estados con cifras, tiempos
- **Un solo filtro general** arriba: **Hoy, Ayer, 7 días, 14 días, Mes, Todo** (desde el inicio de la cuenta) y **Rango de fechas** (desde / hasta). Todo el Panel usa ese filtro: indicadores, avance, alertas, gráficos y tiempos. Por defecto: Hoy.
- Los indicadores de arriba (minutas, por validar, pendientes, en revisión, validadas, horas ahorradas) salen de las mismas minutas que el Avance, así que siempre cuadran.
- **Avance** sin filtro propio: usa el filtro general. Meta: 3 minutas por técnico por día hábil (5 por semana).
  - Tocar a un técnico abre **Reportes filtrado por ese técnico** con un bloque **«Lo que falta · por prioridad»**: primero las que faltan validar (menor % primero), luego rechazadas por corregir, pendientes de solicitar validación y sin terminar.
  - Acciones por minuta:
    - Sin terminar → **Pedir que la termine**; Pendiente → **Pedir que solicite la validación**; Rechazada → **Pedir que la corrija** (notificación al técnico).
    - Falta validar → **Recordar al supervisor** (solo administrador, notificación al supervisor) o **✓ Validar** directamente (administrador o supervisor).
  - **Cada minuta de «Lo que falta» se puede tocar** (fila con ›): si ya está terminada abre su **Propuesta técnica** (con Validar / Rechazar según el rol); si está en levantamiento abre la ficha de solo lectura del levantamiento. Lo mismo al tocar cualquier minuta de la lista «Sus minutas».
  - Al volver (←) regresa a Reportes con el mismo filtro de técnico; si se validó, ya no aparece en «Lo que falta».
  - «✕ Quitar» quita el filtro; salir a Reportes desde el menú también lo quita.
- **Minutas por hora / día / semana / mes**: Hoy y Ayer por hora; hasta 14 días por día; hasta ~2 meses por semana; más, por mes. Cada barra tiene su fecha y su cantidad, y la tarjeta dice el total y el promedio.
- **Minutas por estado**: anillo con el total y leyenda con la cantidad de cada estado (Validada, Completa por validar, En revisión, Pendiente, Borrador).
- **Tiempo por minuta**: una sola vista, **mes anterior vs mes actual**.
  - El **primer mes en producción**, el «mes anterior» son los tiempos manuales de «Editar plazos» (web), es decir, cómo se trabajaba antes de la app. Ejemplo: Octubre (antes de la app, manual) → Noviembre (con la app, en curso).
  - Desde el segundo mes se compara el mes anterior real con el actual. El mes actual se calcula con las minutas reales y se cierra al terminar el mes.
  - Muestra por rubro el tiempo de cada mes y el % de mejora, la **mejora promedio** por minuta y el **ahorro estimado** en horas y soles.
- **Tiempos del equipo**: técnicos — tiempo promedio por minuta frente a la meta de 2 h; supervisores — tiempo promedio en validar frente a la meta de 24 h y cuántas tiene por validar. El supervisor ve solo su equipo y a sí mismo.
- API: `GET /panel?desde=&hasta=` (o `?dias=` / `?todo=1`), `GET /panel?periodo=hoy|ayer|7|14|mes|todo` o `?desde=&hasta=` (el avance viene en la misma respuesta), `GET /tecnicos/{id}/pendientes`, `POST /notificaciones/recordatorio` `{ "para": "tecnico|supervisor", "minuta_id", "accion": "terminar|solicitar|corregir|validar" }`, `GET /panel/tiempos?mes=2026-11` (devuelve el mes pedido y el anterior; si el anterior es antes del inicio en producción, usa «Editar plazos»).

**Criterios de aceptación**
- Dado el rango del 10/10 al 20/10, cuando se aplica, entonces todas las tarjetas usan ese rango y «Minutas por día» muestra 11 barras con fecha y cantidad.
- Dado «Minutas por estado», entonces cada estado muestra su cantidad y la suma es igual al total del anillo.
- Dado el filtro general «Ayer», cuando el administrador toca a María Quispe en el Avance, entonces se abre Reportes filtrado por María con lo que le falta ordenado por prioridad.
- Dada «Pollería La Brasa» de María en «Lo que falta», cuando el administrador la toca, entonces se abre su Propuesta técnica con «Técnico: María Quispe» y puede validarla; al volver ya no aparece en «Lo que falta».
- Dada una minuta de María sin terminar, cuando toca «Pedir que la termine», entonces María recibe la notificación y el botón queda como «✓ Técnico avisado».
- Dada una minuta que falta validar, cuando el administrador toca «Recordar al supervisor», entonces el supervisor de ese técnico recibe la notificación; si toca «✓ Validar», queda Validada y se avisa al técnico.
- Dado el primer mes en producción, cuando se abre el Panel, entonces «Tiempo por minuta» compara el mes anterior (tiempos de «Editar plazos») con el mes actual (minutas reales) y no hay otra vista.

## 18. P09c (mozos con tablet) se pregunta al final
- **Antes:** P09c iba en la sección B, justo después del rubro (P09).
- **Ahora:** va en la sección **H · WiFi y cámaras**, después de las impresoras y lugares de preparación y **justo antes de P46** (zonas sin señal WiFi), porque solo decide si se revisa el WiFi por zona.
- Se pregunta en todos los rubros con atención en mesa, menos Retail, Otro y locales **en obra** (en obra no se mide el WiFi).
- Mismo valor por defecto que antes: «No» en rubros donde es raro (p. ej. fast food), «Sí» en los demás. Si responde «No», no se revisa el WiFi por zona.
- El orden nuevo ya está en el borrador publicado de Preguntas y reglas (sección H, posición 1).
- Android: mover el nodo `P09c` en `flujo.json` a la sección H antes de `P46` y quitar la espera de `P09c` antes de `P11e`.

**Criterios de aceptación**
- Dado un restaurante en operación, cuando el técnico avanza en el Chat IA, entonces P09c aparece después del último lugar de preparación y antes de P46.
- Dado un local en obra, entonces no se pregunta P09c.
- Dada la respuesta «No» en P09c, entonces no se pregunta P46 ni la revisión del WiFi por zona.

## 19. Evaluaciones que se quedaron en el levantamiento de información
Una evaluación que el técnico empezó pero no terminó (estado interno `EN_CURSO`) todavía **no es minuta**: no tiene análisis, puntaje ni PDF.
- **Reportes (administrador y supervisor):** aparece con la etiqueta **«Borrador»** y, en lugar de la criticidad, una barra con su avance: «31 % · sin minuta todavía».
- Al tocarla **no se abre el Chat IA del técnico**. Se abre una ficha de **solo lectura** «Borrador · sin terminar» con:
  - el avance (preguntas y evidencias respondidas, la sección en la que va y la pregunta en la que se quedó);
  - el aviso de que no tiene puntaje, no cuenta en «Por validar» y no se puede compartir;
  - **lo que ya se levantó** (local, razón social, RUC, dirección, encargado, celular, rubro, estado);
  - las **secciones** con su estado (Completa, En curso, Pendiente);
  - «Ver respuestas» (todas las respuestas, solo lectura);
  - **Pedir al técnico que la termine** (notificación al técnico, queda en la bitácora).
- **El técnico** la sigue abriendo en su Chat IA para continuar donde se quedó (o la puede redactar con el Asistente IA).
- En el Panel, en «Lo que falta» por técnico, también se ve como «Borrador · sin minuta».
- API: `GET /evaluaciones/{id}/levantamiento` → `{ avance, preguntas:{hechas, total}, evidencias:{hechas, total}, seccion_actual, pregunta_actual, datos, secciones[], respuestas[] }`.

**Criterios de aceptación**
- Dada una evaluación al 31 % sin terminar, cuando el administrador abre Reportes, entonces la ve como «Borrador» con «31 % · sin minuta todavía» y sin criticidad.
- Cuando la toca, entonces ve la ficha de solo lectura y no puede responder preguntas.
- Cuando toca «Pedir al técnico que la termine», entonces el técnico recibe la notificación y el botón queda como «✓ Técnico avisado».
- Dado el técnico dueño, cuando la toca en Reportes, entonces se abre su Chat IA en la pregunta donde se quedó.

## 20. Indicadores de Inicio por rol
| Rol | Indicador 1 | Indicador 2 | Indicador 3 |
|---|---|---|---|
| Técnico | **Pendientes**: sus minutas sin terminar o sin solicitar la validación | **En revisión**: rechazadas, por corregir | **Esperando validación**: enviadas, sin validar |
| Supervisor | **Por validar**: de los técnicos de su equipo | **Pendientes**: de su equipo y suyas (sin terminar o sin solicitar la validación) | **En riesgo**: de su equipo, con menos de 60 % y sin validar |
| Administrador | **Por validar**: de toda la empresa | **Pendientes**: de toda la empresa (sin terminar o sin solicitar la validación) | **En riesgo**: de toda la empresa, con menos de 60 % y sin validar |

- El administrador ya no tiene «En revisión» en Inicio.
- **No se reinician cada día.** Una minuta que sigue sin validar, sin terminar o en riesgo sin validar se sigue contando hasta que se resuelve. La tarjeta muestra solo el número y el nombre (sin «desde»); cuántos días lleva cada minuta se ve al abrir el indicador (Notificaciones o Reportes).
- **Al tocar un indicador:**
  - **Por validar** (y **Esperando validación** del técnico) → **Reportes** con el **estado Completa**, solo con esas minutas («Por validar · 2 · Estado: Completa»). Tocar una abre su propuesta para validarla o rechazarla.
  - **Pendientes, En riesgo, En revisión** → **Reportes filtrado** con exactamente esas minutas («En riesgo · sin validar · 2»).
  - En todos los casos, «✕ Ver todas» quita el filtro.
- **Glosario de estados**: *Completa* = el técnico pidió la validación, le toca al supervisor (es lo que cuenta «Por validar»). *En revisión* = el supervisor la rechazó con un motivo, le toca al técnico corregirla y reenviarla. No son lo mismo.
- **Quién creó la minuta:** cada evaluación guarda su autor (técnico, supervisor o administrador). «Pendientes» y «Mis minutas» usan ese dato.
- **Técnicos a cargo del administrador:** en Equipo y accesos, el administrador aparece como opción en «Su supervisor». Si tiene técnicos asignados, su fila dice «Administrador · N técnicos a cargo» y aparece «Por validar» en su Inicio.
- API: `GET /inicio/indicadores` → `[{ clave, cantidad, desde, minuta_ids[] }]` según el rol del token; `evaluaciones.creado_por`; `usuarios.supervisor_id` acepta un administrador.

**Criterios de aceptación**
- Dado un administrador, cuando abre Inicio, entonces ve Por validar, Pendientes y En riesgo de toda la empresa, y no ve En revisión.
- Dado que se le asigna María Quispe y María tiene una minuta Completa, cuando abre Inicio, entonces ve «Por validar 1» y, al tocarlo, va a Reportes con el estado Completa y solo esa minuta.
- Dada una minuta en riesgo sin validar desde el 27/09, cuando pasa un día y sigue sin validar, entonces sigue contando en «En riesgo».
- Cuando el administrador toca «En riesgo», entonces se abre Reportes con solo esas minutas.
- Dado que el administrador empezó una evaluación y no la terminó, entonces aparece en sus Pendientes y no en las del técnico.

## 21. «Revisar primero», puntaje coherente y router en un espacio cerrado
- **Revisar primero** (Inicio de supervisor y administrador):
  - Sin el texto «menor % y alertas»; en su lugar un ícono **ⓘ** que explica el orden.
  - Orden **de mayor a menor criticidad**: primero las de menor porcentaje (en riesgo, incluye las no aptas), luego las observadas y al final las aptas. A igual porcentaje, primero la que tiene más alertas.
- **Puntaje coherente con el estado final**: una minuta **NO APTA** no puede mostrar un porcentaje alto; su puntaje global se limita a **59 %** (franja de riesgo). Ejemplo: Restobar Larco (internet bajo el 70 % y no se puede mejorar) pasa de 83 % a 59 %.
- **Pregunta nueva P18u** (después de la foto de la ubicación del router): «Según la foto, ¿cómo está instalado el router?» → A la vista y ventilado · En un cuarto pequeño con puertas (cerca al piso) · Dentro de un mueble cerrado con puertas · Otro.
- **Regla nueva R_ROUTER_ENC (Aviso)**: si el router está en un cuarto pequeño con puertas cerca al piso o dentro de un mueble cerrado con puertas (por P18u, o porque se indica en el texto/observaciones):
  - el **local sigue APTO** (no es observación que lo baje);
  - en la minuta sale el aviso «Router en un espacio cerrado: dar seguimiento» y, en Pasos a seguir del equipo técnico, «Dar seguimiento al local para que no tenga intermitencias de internet por la ubicación del router».
  - Se puede activar, editar o desactivar en Preguntas y reglas › Reglas.

**Criterios de aceptación**
- Dadas dos minutas por validar de 58 % y 83 %, entonces «Revisar primero» muestra primero la de 58 %.
- Dada una minuta NO APTA con puntaje calculado de 83 %, entonces se muestra con 59 %.
- Dado un local APTO con el router «Dentro de un mueble cerrado con puertas», entonces el estado final sigue APTO y la minuta incluye el aviso y el paso de seguimiento.

## 22. Inicio: «Nueva Evaluación» arriba en todos los roles
- En el Inicio del **administrador** y del **supervisor**, el botón grande **«Nueva Evaluación · Iniciar con el Chat IA»** va justo debajo de los indicadores y **antes del Asistente IA**, igual que en el técnico (antes estaba abajo, entre las tarjetas).
- Orden del Inicio (administrador y supervisor): indicadores → Nueva Evaluación → Asistente IA → Revisar primero → Panel, Notificaciones, Reportes, etc.

**Criterio de aceptación**
- Dado un administrador, cuando abre Inicio, entonces «Nueva Evaluación» aparece antes del Asistente IA con el mismo diseño que en el técnico y abre el Chat IA.

## 23. Inicio del administrador = Inicio del técnico (indicadores globales)
- El Inicio del administrador tiene **el mismo diseño y orden que el del técnico**: indicadores → **Nueva Evaluación** → **Asistente IA** → **Continuar** (evaluación previa) y **Reportes** → **Recientes**.
- Diferencias:
  - Los **indicadores son globales** (toda la empresa): Por validar, Pendientes y En riesgo (ver sección 20).
  - **Reportes** dice «General y por técnico».
  - **Recientes** muestra las últimas minutas de toda la empresa, con el técnico y la fecha; las que están sin terminar dicen «Borrador». Tocar una abre su propuesta (o la ficha del levantamiento).
  - Sin la tarjeta «Ayuda» (el administrador no tiene ese módulo).
- Panel, Notificaciones, Equipo y accesos, Preguntas y reglas y Perfil y configuración se abren desde el menú.
- El supervisor mantiene su Inicio (con «Revisar primero»).

**Criterios de aceptación**
- Dado un administrador, cuando abre Inicio, entonces ve la misma estructura que el técnico, con indicadores de toda la empresa.
- Cuando toca una minuta de Recientes de otro técnico, entonces se abre su propuesta técnica.

## 24. Bitácora de la minuta
Se abre desde Ver minuta / Propuesta PDF › «Bitácora de la minuta». Tiene tres partes:
1. **Resumen**: creada por (quién y cuándo) · con qué (Chat IA o Asistente IA) · estado actual · rechazada por, el motivo y quién la corrigió después (si fue rechazada) · validada por (si se validó).
2. **Editaron en las últimas 48 horas**: cada usuario con su número de cambios (o «Nadie la editó en las últimas 48 horas»).
3. **Historial** (lo más nuevo arriba), cada evento con quién, rol, fecha y hora, y **qué parte se cambió en palabras simples**:

| Evento | Detalle que se muestra |
|---|---|
| Creó la minuta | «Con el Chat IA (preguntas guiadas)» o «Con el Asistente IA (evidencias y contexto, N fotos)» |
| Editó los datos del local | «Nombre, razón social, RUC, dirección o contacto» |
| Editó la propuesta técnica | «Equipamiento recomendado» / «Diagnóstico actual (AS-IS)» / «Propuesta recomendada (TO-BE)» |
| Editó las observaciones | «Observaciones del técnico, viabilidad o método de pago» |
| Corrigió la lectura de una evidencia | la evidencia corregida |
| Editó la topología de red | «Cambió equipos o conexiones» / «Quitó un equipo» / «Quitó una conexión»; también incluir o quitar la topología |
| Actualizó la minuta con el Asistente IA | el resumen de cambios de la versión |
| Generó / descargó el PDF | — |
| Compartió / envió por correo | por dónde y a quién |
| Solicitó la validación | — |
| Rechazó la minuta | «Motivo: …» |
| Validó la minuta | — |

- Un mismo evento repetido en menos de 3 segundos se registra una sola vez.
- API: `GET /minutas/{id}/bitacora` → `{ resumen:{creada_por, creada_en, herramienta, estado, rechazada_por?, motivo?, corregida_por?, validada_por?}, editores_48h:[{usuario, cambios}], eventos:[{ts, usuario, rol, tipo, accion, detalle}] }`. Tipos: `crear, editar, compartir, descargar, solicitar, validar, rechazar`.

**Criterios de aceptación**
- Dada una minuta rechazada por el supervisor y corregida por el técnico, cuando se abre la bitácora, entonces el resumen muestra quién la rechazó, el motivo y quién la corrigió.
- Dado que el técnico editó los datos del local hace 2 horas, entonces aparece en «Editaron en las últimas 48 horas» con «1 cambio» y en el historial dice «Nombre, razón social, RUC, dirección o contacto».
- Dado que el supervisor la compartió por WhatsApp, entonces el historial muestra «Compartió por WhatsApp · Supervisor».

## 25. Nombre único del estado: «Borrador»
- El estado interno `EN_CURSO` (evaluación empezada en el Chat IA y sin terminar) se muestra en **toda la app** como **«Borrador»**: Reportes (lista y filtro de estado), Panel (Minutas por estado), Inicio (Recientes), «Lo que falta» («Borrador · sin minuta») y la ficha de solo lectura («Borrador · sin terminar»).
- Se deja de usar «En levantamiento». En las secciones 19 y 23, donde dice «En levantamiento», léase «Borrador».
- Ciclo: **Borrador** (sin terminar, sin análisis) → **Pendiente** (terminada, sin solicitar validación) → **Completa** (por validar) → **En revisión** (rechazada, por corregir) o **Validada**.

## 26. Reportes del técnico: indicadores
- En Reportes, el técnico ve tres indicadores: **Total de minutas** · **En revisión** (rechazadas, por corregir) · **Esperando validación** (en estado Completa). Reemplazan «Minutas · Validadas · En riesgo».
- Cuentan lo que se está mostrando: si hay un filtro activo desde Inicio (por ejemplo «Esperando validación · 1»), cuentan solo esas.
- Son tocables: **Total de minutas** quita el filtro; **En revisión** y **Esperando validación** filtran la lista con esas minutas (igual que desde Inicio).
- El supervisor y el administrador mantienen sus indicadores en Reportes.

**Criterio de aceptación**
- Dado un técnico con 3 minutas (1 Completa, 0 rechazadas), cuando abre Reportes, entonces ve «3 Total de minutas · 0 En revisión · 1 Esperando validación»; al tocar «Esperando validación» la lista muestra solo esa minuta.

## 27. Inicio del supervisor = Inicio del administrador
- El supervisor tiene el **mismo Inicio que el administrador** (y que el técnico): indicadores → Nueva Evaluación → Asistente IA → Continuar y Reportes → Recientes.
- **Indicadores del supervisor**: **Por validar** · **Pendientes** · **En riesgo**, todos de **su equipo** (Pendientes también incluye las suyas). El administrador los ve de toda la empresa.
- Reportes dice «Tu equipo y tus minutas»; el Asistente IA, «Redacta o actualiza minutas y pídele reportes de tu equipo, pendientes o minutas por validar».
- Recientes muestra las últimas minutas de su equipo.
- Ya no está «Revisar primero» en Inicio. Lo que más urge se ve tocando **Por validar** o **En riesgo** (Reportes filtrado), o pidiéndole al Asistente IA «minutas por validar», que las ordena de mayor a menor criticidad.

**Criterio de aceptación**
- Dado un supervisor, cuando abre Inicio, entonces ve Por validar, Pendientes y En riesgo de su equipo, Nueva Evaluación arriba del Asistente IA y Recientes de su equipo.

## 28. Ayuda solo dentro del Chat IA y «Mis minutas» para el administrador
- En Perfil y configuración la fila se llama **«Ayuda»** (soporte de la app, próximamente), ya no «Contactar ayuda».
- La **ayuda de evaluación** (¿cómo es un router, un selftest o AnyDesk?, foto con ChatGPT) **solo está dentro del Chat IA**, con el ícono de la cabecera, como hasta ahora. Se quitó de Inicio y del menú lateral en todos los roles.
- **Reportes del administrador**: tiene las mismas pestañas que el supervisor: **General · Por técnico · Mis minutas**. «Mis minutas» muestra las evaluaciones que creó el propio administrador.

**Criterios de aceptación**
- Dado cualquier rol, cuando abre Inicio o el menú, entonces no ve «Ayuda»; cuando está en el Chat IA, ve el ícono de Ayuda en la cabecera.
- Dado un administrador que empezó una evaluación, cuando abre Reportes › Mis minutas, entonces la ve ahí.

## 29. Recordatorios al técnico (administrador y supervisor)
- El **supervisor** tiene las mismas opciones que el administrador para pedirle algo al técnico desde Panel › Avance › técnico (Reportes filtrado) o desde la ficha de una minuta en borrador:
  - Borrador → «Pedir que la termine» · Pendiente → «Pedir que solicite la validación» · En revisión → «Pedir que la corrija».
  - Solo el administrador tiene además «Recordar al supervisor» (el supervisor valida directamente).
- **Al técnico le llega la notificación** con quién la envía: «Supervisor de prueba te pide: termina la minuta de Chifa Dragón Rojo». Se marca el punto rojo en la campana de Inicio.
- Al tocarla, se abre esa minuta: si está en borrador, en el **Chat IA en la pregunta donde se quedó**; si no, su propuesta.
- API: `POST /notificaciones/recordatorio` acepta remitente con rol supervisor o administrador; el destinatario es el técnico dueño de la minuta.

**Criterio de aceptación**
- Dado que el supervisor toca «Pedir que la termine» en la minuta en borrador de un técnico, cuando el técnico abre Notificaciones, entonces ve «Supervisor … te pide: termina la minuta de …» y al tocarla continúa en el Chat IA.

## 30. Saludo de Inicio
- La cabecera de Inicio dice **«Hola de nuevo,»** y debajo el nombre del usuario, en los tres roles (antes «Bienvenido de vuelta,»). Es más corto y no tiene género.
- Android: `strings.xml` → `<string name="home_saludo">Hola de nuevo,</string>`.

## 31. Pasar a producción (solo administrador)
- **Perfil y configuración › AMBIENTE** (solo administrador):
  - Antes: «Modo demo (pruebas)» y el botón **Pasar a producción** («No se puede deshacer»). En Inicio, junto al saludo, aparece la etiqueta **DEMO** (todos los roles).
  - Al tocarlo se abre una hoja que explica qué pasa y pide **escribir «PRODUCCIÓN»** para habilitar el botón.
  - Después: «En producción · desde {fecha} · por {administrador} · no se puede volver a demo», y la **Base de conocimiento de la IA (etapa demo)**: minutas, evidencias y recomendaciones guardadas.
- **Qué pasa al pasar a producción**:

| Información | Qué pasa |
|---|---|
| Reportes, indicadores de Inicio y Panel | Empiezan en **0**. Lo de la etapa demo no se cuenta. |
| Minutas demo Completas, En revisión y Validadas | **Se conservan** y se ven en Reportes › «Minutas de la etapa demo (N)» con la etiqueta DEMO; se pueden abrir, pero no cuentan. |
| Minutas demo en Borrador o Pendiente | Dejan de verse. |
| Respuestas, evidencias y recomendaciones de la etapa demo | **No se borran**: quedan como base de conocimiento de la IA. |
| Reglas, preguntas, mapa de nodos, equipo y datos de la empresa | Se conservan. |
| Notificaciones | Solo quedan las de minutas Completas, En revisión o Validadas. |
| Chat IA | Conserva su historial. |
| Asistente IA | No guarda conversaciones: se borran (también los reportes programados). |

- Las minutas nuevas (hechas en producción) cuentan normalmente desde el primer momento.
- «Tiempo por minuta» en el primer mes compara los tiempos de «Editar plazos» (mes anterior) con las minutas reales del mes en producción.
- API: `POST /empresa/produccion` (solo rol administrador, irreversible; `409` si ya está en producción) → marca `minutas.etapa='demo'`, `visible = estado in (COMPLETA, EN_REVISION, VALIDADA)`; borra conversaciones del Asistente IA y notificaciones de minutas no visibles; guarda `empresa.produccion_desde`. Todos los reportes filtran `etapa='produccion'`. `GET /minutas?etapa=demo` lista las demo visibles. La base de conocimiento usa todas las minutas, sin filtrar por etapa.

**Criterios de aceptación**
- Dado un administrador en modo demo, cuando toca «Pasar a producción» sin escribir PRODUCCIÓN, entonces el botón está deshabilitado.
- Cuando confirma, entonces Inicio, Reportes y Panel muestran 0 y Configuración dice «En producción … no se puede volver a demo», sin botón para volver.
- Dada una minuta demo Validada, entonces aparece en «Minutas de la etapa demo» y no en los indicadores; una demo en Borrador ya no aparece.
- Dado un supervisor o técnico, entonces no ve la opción de pasar a producción.
- Dada una minuta nueva hecha después, entonces sí cuenta en los indicadores.

## 32. Modo noche (app y web)
**Problema:** el fondo seguía claro y solo algunas tarjetas pasaban a negro, con títulos oscuros sobre fondo oscuro (no se leían) y estados con poco contraste.

**Criterios de diseño**
- Sin negro puro: el fondo es gris carbón y las tarjetas un nivel más claras, para que se distinga la jerarquía (fondo → tarjeta → hoja/diálogo).
- Texto casi blanco (no blanco puro) y texto secundario gris claro; todo con contraste **WCAG AA** (≥ 4.5:1 en texto normal).
- Los colores de estado se **aclaran** (verde, ámbar, rojo, azul, lila) y sus etiquetas usan fondos translúcidos, no bloques de color sólido.
- La cabecera azul se oscurece para no encandilar.
- Los botones contorneados pasan a fondo transparente con borde azul suave; los principales siguen azules con texto blanco.
- **La minuta (PDF) y los diagramas de topología se mantienen claros**, con texto oscuro: son documentos y así se ven al imprimir o compartir.

**Tokens (Android `values-night/colors.xml` y CSS de la web)**

| Token | Color | Uso |
|---|---|---|
| fondo | `#0F141A` | pantalla |
| superficie 1 | `#18202A` | tarjetas, filas, indicadores |
| superficie 2 | `#202A36` | hojas inferiores, diálogos, campos |
| superficie 3 | `#283444` | elementos dentro de hojas, barras de progreso |
| borde | `#2C3846` | divisores y contornos |
| texto | `#E7ECF2` | títulos y texto principal |
| texto 2 | `#A9B4C2` | subtítulos, fechas |
| texto 3 | `#7F8B99` | etiquetas de sección |
| primario (texto) | `#8EC5FF` | enlaces, botones contorneados |
| primario (relleno) | `#2B7BD6` | botones principales, chip activo |
| cabecera | `#13345E` | barras superiores |
| éxito | `#7FD39A` sobre `rgba(76,175,80,.16)` | Completa, Viable |
| aviso | `#FFC27A` sobre `rgba(255,167,38,.16)` | Pendiente, Observado |
| error | `#FF9C9C` sobre `rgba(239,83,80,.16)` | En revisión, Riesgo |
| info | `#8EC5FF` sobre `rgba(66,165,245,.16)` | Validada |
| lila | `#C7B4FF` sobre `rgba(149,117,205,.2)` | Borrador, por validar |

- Se aplica igual en la web (contenido, barra lateral y barra del navegador).
- Android: `Theme.Material3.DayNight` con `values-night`; nunca colores fijos en los layouts, siempre `?attr/colorSurface`, `?attr/colorOnSurface`, etc.

**Criterios de aceptación**
- Dado el modo noche, cuando se abre Reportes, entonces el fondo y las tarjetas son oscuros, los nombres de los locales se leen (contraste ≥ 4.5:1) y las etiquetas de estado tienen fondo translúcido.
- Dado el modo noche, cuando se abre la Propuesta PDF, entonces la minuta se ve blanca con texto oscuro.
- Dado el modo noche en la web, entonces la barra lateral, el contenido y la barra del navegador están en oscuro.

## 33. Cerrar sesión en la web
- La barra lateral de la web tiene **«Cerrar sesión»** abajo, igual que el menú de la app, en todos los roles: en rojo suave, después de las opciones y antes del nombre del usuario.
- Cierra la sesión solo de ese navegador; las otras sesiones (app, otros navegadores) se cierran desde Perfil y configuración › Sesiones abiertas.
- En el Inicio del administrador, la tarjeta Reportes dice «General, por técnico y mis minutas».

**Criterio de aceptación**
- Dado cualquier rol en la web, cuando toca «Cerrar sesión» en la barra lateral, entonces vuelve a la pantalla de inicio de sesión.

## 34. Sin la opción «Subir fotos solo con WiFi»
- Se quitó de Modo sin conexión y sincronización. Las evidencias se suben **con WiFi o con datos móviles**, apenas hay conexión, porque el Chat IA y el Asistente IA necesitan leer las fotos al momento para seguir con el levantamiento.
- Si no hay ninguna conexión, se guardan en la cola sin conexión y se suben al volver la señal (sin cambios).
- Android: quitar la preferencia `wifi_only` y el `NetworkType.UNMETERED` del `WorkManager` de subida de fotos (usar `NetworkType.CONNECTED`).

**Criterio de aceptación**
- Dado un técnico con datos móviles y sin WiFi, cuando sube una evidencia en el Chat IA, entonces se sube y se lee al momento.

## 35. Nombre único, menú igual en app y web, y foto de perfil
**Nombre**: la app y la web se llaman **Network Assistant** (menú, barra lateral de la web, pie del menú). La web: `https://app.networkassistant.pe`. «Asistente de Red» ya no se usa como nombre; donde iba la empresa se muestra el nombre de la empresa (Configuración › Empresa).

**Menú por rol (igual en app y web)**

| Rol | Opciones |
|---|---|
| Técnico | Inicio · Nueva evaluación · Asistente IA · Reportes · Notificaciones · Perfil y configuración · Cerrar sesión |
| Supervisor | Inicio · Panel · Nueva evaluación · Asistente IA · Reportes · Notificaciones · Perfil y configuración · Cerrar sesión (en la web, además **Preguntas y reglas**) |
| Administrador | Inicio · Panel · Notificaciones · Reportes · Nueva evaluación · Asistente IA · Equipo y accesos · Preguntas y reglas · Perfil y configuración · Cerrar sesión |

- La única diferencia permitida entre app y web es Preguntas y reglas (y su mapa de nodos), que en la web se usa con más espacio.

**Cabecera del menú (app y web)**: foto o iniciales, **nombre**, **rol** y **empresa** (p. ej. «Técnico de prueba · Técnico · IDBI»). Al tocarla se abre Mi perfil.

**Foto de perfil**
- En **Mi perfil**, el avatar tiene un botón de cámara para **subir una foto** (JPG/PNG, hasta 3 MB) y la opción **Quitar foto** (vuelven las iniciales).
- La foto se muestra en: cabecera del menú (app y web), avatar de la parte superior de Inicio (app) y Mi perfil.
- **Dentro de los módulos** (listas de Equipo y accesos, Panel, Reportes, etc.) y en la parte superior de la web se siguen mostrando las **iniciales**.
- API: `PUT /usuarios/yo/foto` (multipart) y `DELETE /usuarios/yo/foto`; `GET /usuarios/yo` devuelve `foto_url` o `null`.

**Criterios de aceptación**
- Dado un supervisor, cuando abre el menú en la app y en la web, entonces ve las mismas opciones en el mismo orden (la web suma Preguntas y reglas).
- Dado un técnico que sube su foto en Mi perfil, cuando abre el menú, entonces ve su foto, nombre, rol y empresa; en Equipo y accesos lo ven con sus iniciales.
- Cuando toca «Quitar foto», entonces vuelven sus iniciales.

## 36. Recuperar contraseña: el código le llega al administrador y el técnico elige
Flujo tipo Google (el que confirma es quien tiene el código):
1. El técnico, en «¿Olvidé contraseña?», escribe su correo y envía la solicitud.
2. **Al administrador** le llega la notificación «… quiere restablecer su contraseña. Ábrela para ver el código». Al abrirla (o desde Equipo y accesos › Ver código) ve un **código de 2 dígitos** grande, con «Dile este número (en persona o por llamada, nunca por chat)», y las opciones «No, no reconozco esta solicitud» y «Cerrar».
3. **En la app del técnico** aparecen **3 números** («Elige el código de tu administrador»). Elige el que le dijo el administrador.
4. Si coincide → «Crea tu nueva contraseña». Si elige otro, o el administrador rechazó → solicitud rechazada, debe pedirla de nuevo. El código vence a los 10 minutos.
- En «Pendientes de hoy» del administrador: «Cambio de contraseña de …» con el botón **Ver código**.
- API: `POST /auth/recuperacion` `{email}` → crea `{solicitud_id, numero, opciones[3]}`; el número solo se envía al administrador (`GET /auth/recuperacion/{id}/codigo`, rol ADMINISTRADOR); el técnico recibe solo `opciones` y responde `POST /auth/recuperacion/{id}/respuesta {numero}` → `aprobada | rechazada`; el administrador puede `POST /auth/recuperacion/{id}/rechazar`.

**Criterios de aceptación**
- Dada una solicitud del técnico, cuando el administrador abre la notificación, entonces ve el código y no ve opciones para elegir (salvo en la simulación).
- Cuando el técnico elige el número correcto entre los 3, entonces pasa a crear su nueva contraseña; si elige otro, la solicitud se rechaza.

## 37. Contraseñas con «ojito», plan visible y web (planes y agregar usuarios)
**Ojito en las contraseñas (app y web)**: en Iniciar sesión, Nueva contraseña, Confirmar contraseña y Cambiar contraseña hay solo un ícono, sin texto:
- ojo **cerrado** = la contraseña está oculta (puntos); al tocarlo se muestra;
- ojo **abierto** = se ve la contraseña; al tocarlo se vuelve a ocultar.
- Accesible: `aria-label` «Mostrar contraseña» / «Ocultar contraseña» y `aria-pressed`. Android: `TextInputLayout` con `app:endIconMode="password_toggle"`.

**Nueva contraseña**: al elegir el código correcto, se abre «Crea tu nueva contraseña» con los dos campos **vacíos** (no se rellena nada con lo escrito antes).

**Plan visible para el administrador**: dentro de **Equipo y accesos**, en una línea arriba de la lista de usuarios (ver sección 38); no en Inicio ni como módulo aparte.

**Web (sin cambios respecto al panel web diseñado)**:
- Iniciar sesión con **Gmail**, **Microsoft** o con correo y contraseña.
- El administrador, al entrar, ve **Planes de la empresa**: Free (próximamente), **Empresa** (disponible, plan actual marcado, `[PRECIO] / mes`, hasta 10 usuarios contando al administrador) y Empresarial (próximamente, a medida).
- **Agregar persona** se hace solo en la web, con el flujo del panel web (Equipo y accesos › Agregar persona; ver sección 39). En la app no se agregan usuarios.
- Preguntas y reglas en la web sigue con su propio diseño (editor, mapa de nodos y reglas del panel web).

## 38. Plan de la empresa dentro de Equipo y accesos y Preguntas y reglas en la web
- En la app, el plan **no es un módulo aparte** (no está en el menú ni en Inicio): vive dentro de **Equipo y accesos**, en una línea arriba de la lista de usuarios: «Plan Empresa · N de 10 usuarios», con su barra de uso.
  - La lista de Equipo y accesos ya es la lista de todos los usuarios del plan (administrador, supervisores y técnicos, con rol, supervisor y estado).
  - Web: se mantiene el panel web de su diseño, con su propio módulo Plan de la empresa (ver sección 39).
  - App: la línea abre el detalle del plan («Renueva el …») con «Pedir más usuarios»; cambiar plan o agregar usuarios se hace desde la web.
- **Preguntas y reglas en la web** usa su propia versión del panel web: editor de preguntas, mapa de nodos interactivo y reglas del análisis (pantallas 8, 9 y 8b del diseño del panel web). Los cambios de la sección 11 (orden Preguntas › Reglas › Mapa, preguntas reordenables, reglas editables, deslizar para eliminar) son de la **app**.

## 39. La vista Web del simulador es el panel web de su diseño
- La vista **Web** del simulador ya no reutiliza las pantallas de la app: muestra el **panel web tal como está en su diseño**, sin cambios: inicio de sesión (Gmail, Microsoft o correo), Planes de la empresa, Panel, Reportes y plazos, Equipo y accesos (Personas y códigos de contraseña, Roles y permisos, Agregar persona, asignar supervisor), Notificaciones (minutas por validar), Preguntas y reglas (editor, mapa de nodos y reglas del análisis) y Mi perfil y configuración.
- El menú lateral es el del panel web y se arma según el rol del simulador:
  - Administrador: Inicio (sección 40), Panel, Notificaciones, Reportes, Preguntas y reglas, Equipo y accesos, Plan de la empresa, Mi perfil y configuración.
  - Supervisor: Panel · mi equipo, Notificaciones, Reportes, Mi perfil y configuración.
  - Técnico: Notificaciones, Reportes, Mi perfil y configuración.
- Al cambiar a la vista Web se abre la pantalla equivalente a la de la app (por ejemplo, Equipo y accesos → Personas); la barra de direcciones muestra la ruta (`/equipo-y-accesos`, `/equipo-y-accesos/roles`, `/preguntas-y-reglas/mapa-de-nodos`…).
- Los cambios de la 0.0.7 descritos en las secciones anteriores (plan dentro de Equipo y accesos, menú unificado, Network Assistant, foto de perfil) son de la **app**. El panel web no se modifica en esta versión.

## 40. Inicio en el panel web (administrador): el mismo de la app
- El panel web del administrador tiene **Inicio**, igual al Inicio del administrador en la app: es la primera opción del menú y la pantalla a la que se llega al iniciar sesión (Gmail, Microsoft o correo) o al crear la cuenta de la empresa.
- Mismo contenido, mismo orden y mismos datos (misma cuenta):
  1. «Hola de nuevo,» con el nombre y la etiqueta DEMO mientras la cuenta no pasa a producción.
  2. Indicadores globales: **Por validar**, **Pendientes** y **En riesgo**.
  3. **Nueva Evaluación** (iniciar con el Chat IA).
  4. **Asistente IA** (redacta o actualiza minutas y da reportes de la empresa, pendientes o minutas por validar).
  5. **Continuar** (evaluación previa) y **Reportes** (general, por técnico y mis minutas).
  6. **Recientes** de toda la empresa con su estado y «Ver todo».
- Mismas acciones que en la app: cada opción abre el mismo módulo de la app dentro del navegador (Chat IA, Asistente IA, Reportes filtrados por el indicador, la propuesta o el borrador de la minuta). «← Volver a Inicio» regresa al Inicio del panel web. Si no hay evaluación en curso, «Continuar» muestra el mismo aviso que en la app.
- Supervisor y técnico mantienen su menú del panel web (sin Inicio).
- Menú del administrador en la web: Inicio, Panel, Notificaciones, Reportes, Preguntas y reglas, Equipo y accesos, Plan de la empresa, Mi perfil y configuración.

## 41. Menú por grupos y «Mi Cuenta» (app y web)
- **Mismo menú en la app y en la web**, separado por grupos (cada rol ve solo lo suyo):
  - **Principal**: Inicio, Panel, Notificaciones.
  - **Seguimiento**: Reportes, Nueva evaluación, Asistente IA.
  - **Configuración**: Preguntas y reglas, Equipo y accesos y, en la web, Plan de la empresa (en la app el plan está dentro de Equipo y accesos, sección 38).
  - **Mi cuenta**: Mi Cuenta.
  - Supervisor: Principal (Inicio en la app, Panel, Notificaciones), Seguimiento y Mi cuenta. Técnico: Principal (Inicio en la app, Notificaciones), Seguimiento y Mi cuenta.
- **Cabecera del menú web** como en la app: arriba el nombre del aplicativo, **Network Assistant**; debajo, la tarjeta del usuario con su nombre, rol y empresa (abre Mi perfil). El plan del menú web solo lo ve el administrador.
- En la web, **Nueva evaluación** y **Asistente IA** abren el mismo módulo de la app dentro del navegador, con «← Volver».
- **Inicio web** con la cabecera de la app: «Hola de nuevo,» y el nombre (con DEMO), notificaciones y la foto o iniciales; en pantallas angostas la barra superior muestra el saludo en lugar de «Asistente de Red».
- **«Mi Cuenta»** reemplaza a «Perfil y configuración» en el menú y como título del módulo (app y web).
  - Arriba, en lugar de «Administrador · se guarda en tu cuenta (app y web)», se muestra el nombre del usuario, su rol y su empresa; al tocarlo se abre **Mi perfil**.
  - En la app, el apartado de abajo ya no se llama «Cuenta» ni tiene la fila «Mi perfil»: queda **Soporte** con Ayuda y Versión.

## 42. Menú: Modo noche, Mi Cuenta y Cerrar sesión (app y web)
- **Mi Cuenta** pertenece al grupo **Configuración** (ya no hay grupo «Mi cuenta»); usa el mismo ícono (engranaje) en la app y en la web.
- **Modo noche** va en el menú lateral, debajo de Mi Cuenta, y no dentro de un módulo: se quitó «Apariencia» de Mi Cuenta (app y web). El modo elegido es el mismo en la app y en la web.
- La web tiene **Cerrar sesión** al final del menú, como la app.
- En la web angosta, la barra superior muestra el ícono de **Notificaciones** (con punto si hay nuevas) en lugar de un número.
- Se quitaron las iniciales del usuario al costado de Notificaciones (Inicio de la app y de la web): el usuario ya está en el menú.

## 43. Panel unificado (app y web, mismos datos y mismo filtro)
- **Filtro** igual en ambos: Hoy, Ayer, 7 días, 14 días, Mes, Todo y Rango de fechas. Lo que se elige en uno se ve en el otro.
- Sin saludo ni texto de resumen arriba (ya están los indicadores) y sin «Plan y usuarios · renueva el …» (eso es del plan, dentro de Equipo y accesos / Plan de la empresa).
- **Indicadores** (administrador y supervisor): **Total minutas** (vs. periodo anterior), **Por validar** (N con riesgo), **Pendientes de cerrar** (N con análisis IA listo), **En revisión** (devueltas al técnico), **Validadas** (% del total) y **Horas ahorradas** (% menos por minuta).
- **Apartados** (en la app y en la web angosta, uno a la vez con pestañas para no hacer scroll; en la web ancha, en cuadrícula):
  - **Por día** · «Minutas por día terminadas» (Completa, En revisión y Validada). La unidad cambia con el filtro: Hoy y Ayer por hora; 7 y 14 días por día; Mes por semana; un rango de hasta 12 meses (y Todo) por mes; más de 12 meses por año.
  - **Estados** · dona con el total al centro y Borrador, Pendiente, Completa, En revisión y Validada (en ese orden) con cantidad y porcentaje.
  - **Tiempos** · tiempo por minuta por tipo de negocio (antes vs con la app) con **Mejora vs mes anterior** y **Ahorro estimado**.
  - **Alertas** · Indicadores que alertan (reemplaza a «Puntaje promedio»); cada uno abre la consulta en el Asistente IA.
  - **Negocios** · locales por tipo de negocio según el filtro.
- **Avance y tiempos del equipo**: por técnico, barra por estado de sus minutas, cantidad y **horas promedio por minuta** (meta 2 h); supervisores aparte con sus minutas por validar y horas promedio en validar (meta 24 h). Tocar un técnico abre Reportes con lo que le falta.
- **Por validar primero** (menor puntaje = más riesgo) y **Actividad reciente** (quién hizo qué y cuándo) en la app y en la web.
- En la app se mantiene además el bloque **Avance** (meta de 3 minutas por técnico al día hábil).

## 44. Notificaciones del supervisor y del administrador (app y web)
- Cada aviso muestra **quién envió**, el **local**, el **puntaje** y la **hora**, con íconos: **ver minuta**, **ver topología**, **✓ validar** y **✕ rechazar** (solo íconos).
- Filtros con cantidad: **Todas** (la opción inicial), **Completas · por validar**, **En revisión** y **Validadas**.
- Orden con dos opciones: **Por criticidad** (primero las de menor puntaje, más riesgo) o **Por fecha** (primero las más recientes); el orden se aplica sobre el filtro elegido.
- Arriba: el supervisor ve «Solo ves lo que te enviaron tus técnicos: …»; el administrador, si en Mi Cuenta tiene activado recibir notificaciones, ve las de **todos los usuarios**.
- En la web se quitó la lista «ordenadas por puntaje de riesgo» y el detalle desplegado de la minuta (para eso está «ver minuta»). Ver minuta, ver topología y rechazar abren la app dentro del navegador con «← Volver a Notificaciones».
- Los demás avisos (por ejemplo, códigos de contraseña) aparecen debajo, en «Otros avisos».

## 45. Plan en el menú lateral (administrador, app y web)
- Ya **no existe el módulo «Plan de la empresa»** (ni en la app ni en la web) ni la línea del plan dentro de Equipo y accesos.
- Menú del administrador, igual en app y web:
  - Principal: Inicio, Panel, Notificaciones · Seguimiento: Reportes, Nueva evaluación, Asistente IA · Configuración: Preguntas y reglas, Equipo y accesos, **Mi Cuenta**.
  - Al final del menú (se desplazan junto con las opciones, no quedan fijos): **Modo noche**, el cuadro **Plan · Empresa · N de M usuarios** (con su barra; solo el administrador) y **Cerrar sesión** (en rojo, separado por una línea, como en la app).
- Al tocar el cuadro del plan se abre **Plan**: plan actual, usuarios usados y la renovación, la lista de todos los usuarios del plan, **Pedir más usuarios** y **Cambiar a un plan superior** (Planes: Free y Empresarial próximamente; Empresa, plan actual). En la web se abre dentro del navegador con «← Volver».

## 46. Reportes por rol y acceso temporal a otras minutas (app y web)
- **Técnico**: solo ve sus minutas. **Supervisor**: las de sus técnicos y las suyas. **Administrador**: todas.
- **Ver una minuta de otro técnico (técnico)**: en Reportes elige la minuta y el **motivo** (visita de seguimiento al mismo local, reemplazo a otro técnico, mismo cliente en otro local) y envía la solicitud a **su supervisor**.
- **Ver una minuta de otro equipo (supervisor)**: igual, pero la solicitud va al **administrador**. Solo se listan minutas de técnicos que no son de su equipo.
- Quien aprueba la ve en **Notificaciones › Otros avisos** (con el motivo) y elige **Generar código** o **Rechazar**. El código es de 6 dígitos, vale **24 horas**, solo abre esa minuta y solo para quien la pidió; se puede anular.
- Quien pidió escribe el código en Reportes y ve la minuta en **solo lectura** (no se edita, descarga ni comparte). Al vencer las 24 horas el acceso termina.
- En la web: Reportes respeta el rol de la cuenta (sin «Ver como») y muestra «Pedir acceso», «Ingresar código» y los accesos temporales; las hojas se abren dentro del navegador. En Notificaciones web, «Revisar solicitud» abre la aprobación.

## 47. Mi Cuenta igual en la app y en la web
- **App**: en Notificaciones se agrega **Avisarme cuando…** (como la web), según el rol:
  - Administrador: una minuta queda completa y espera validación; un supervisor devuelve una minuta a revisión; alguien pide un código para su contraseña; una minuta pasa su plazo (mora).
  - Supervisor: un técnico de su equipo envía una minuta; una minuta de su equipo pasa su plazo; un técnico corrige una minuta que devolvió.
  - Técnico: validan una de sus minutas; le devuelven una minuta a revisión; se acerca el plazo de una minuta.
- **Web**: se agregan **Sincronización** (estado, última subida, por subir y «Sincronizar ahora»), **Soporte** (Ayuda y Versión) y el botón **Cerrar sesión** al final, como en la app.
- Lo que se marca en un lado se ve igual en el otro (misma cuenta).

## 48. Preguntas y reglas: mismo orden y mapa de nodos (app y web)
- Pestañas en el mismo orden en la app y en la web: **Preguntas › Reglas › Mapa de nodos** (en la web, «Reglas del análisis» pasa a llamarse **Reglas**).
- **Web (computadora)**: el mapa de nodos se mantiene tal cual (lienzo, conexiones, zoom, minimapa, panel lateral). El selector «Ir a la sección…» se reemplaza por la barra de la app: **Ordenar nodos** y una ficha por sección (A, B, C…) que lleva directo a esa sección con un toque.
- **Web en el celular** (pantalla angosta): el mapa se ve como en la app: Inicio, cada sección con sus preguntas en orden (ícono del tipo, código, tipo y texto) y Fin; las fichas de sección hacen scroll hasta esa sección y tocar una pregunta abre **Detalle**.
- **App · Detalle de una pregunta**: «Lo que verá el técnico» y «Cuándo aparece» se muestran completos (el cuadro crece con el texto, letra más grande) y se puede escribir con comodidad; los demás campos y botones ya no se aplastan.

## 49. Reportes web: «Mis minutas» para el administrador y el supervisor
- En la web, Reportes tiene las mismas pestañas que la app: **General · Por técnico · Mis minutas** para el administrador y el supervisor (ambos también generan minutas); el técnico solo tiene **Mis minutas**.
- «Mis minutas» muestra solo las minutas que creó esa persona, con los mismos indicadores, filtros por columna y trazabilidad.

## 50. Asistente IA: reportes en tabla y descarga en Excel
- Cuando se le pide un **reporte** (por ejemplo: «Genérame un reporte con todas las minutas incompletas del mes de octubre indicando a qué técnico pertenece, su supervisor y a qué local pertenece»), el Asistente IA responde con una **tabla**: Local, Técnico, Supervisor, Estado, Puntaje y Fecha, y el número de minutas encontradas.
- Entiende:
  - **Estado**: incompletas (Borrador, Pendiente y En revisión), en borrador, pendientes, por validar / completas, en revisión / rechazadas, validadas, en riesgo (menos de 60 %).
  - **Periodo**: un mes por su nombre (enero… diciembre) o «este mes».
  - **Persona**: el nombre de un técnico o el equipo de un supervisor.
- Respeta el rol: el técnico solo obtiene sus minutas; el supervisor, las de su equipo y las suyas; el administrador, las de toda la empresa.
- **Descargar Excel**: genera un archivo **.xlsx** con título, fecha de generación, encabezados con filtro, la columna Dirección y el puntaje como número. **Ver en Reportes** abre el módulo Reportes.
- **Seguimiento del reporte**: después de un reporte se puede ajustar sin repetir todo: «solo del mes anterior», «no de setiembre» / «sin octubre», «solo las validadas», «de María Quispe». El asistente actualiza la misma consulta y muestra una tabla nueva («Actualicé el reporte…»). «Mes anterior» toma el mes anterior al actual.
- En la app real la consulta la interpreta ChatGPT (API de OpenAI, desde FastAPI) con los datos de la empresa como contexto, por eso entiende cualquier redacción y el contexto de la conversación; en el simulador se interpreta con reglas (frases como las de arriba).

## 51. Regla de trabajo: todo cambio de la app se revisa en la web
- Cada cambio que se haga en la app se revisa en la web: si la web ya lo tiene, se deja igual; si no, se agrega en la web en la misma entrega (y al revés).
- En esta revisión se completó en la web:
  - **Inicio para supervisor y técnico** (antes solo el administrador): el mismo Inicio de la app con sus indicadores (supervisor: Por validar, Pendientes, En riesgo; técnico: Pendientes, En revisión, Esperando validación), Nueva Evaluación, Asistente IA, Continuar, Reportes y Recientes de su alcance. Inicio es la primera opción del menú web y la pantalla al entrar.
  - **Mi Cuenta**: Empresa (nombre; el administrador cambia logo y nombre), y para el administrador Copia de seguridad, Trazabilidad (bitácora de la empresa) y Ambiente (modo demo o pasar a producción). Las acciones abren el mismo apartado de la app dentro del navegador.
  - El reporte en tabla con descarga en Excel del Asistente IA (sección 50) funciona igual en la web, porque el Asistente IA abre el mismo módulo.

## 52. Asistente IA: prompts frecuentes (★) — app y web
- Junto a cada mensaje que escribe el usuario aparece una **estrella ☆**: al tocarla, ese texto se guarda como **prompt frecuente** (la estrella se pinta ★). Tocarla de nuevo lo quita.
- Arriba, al lado del nombre **Asistente IA**, hay una **estrella ★** con la cantidad guardada. Al tocarla se abre **Prompts frecuentes**:
  - al elegir uno, el texto **se escribe en el chat pero no se envía**: el usuario lo revisa, lo ajusta y lo envía cuando quiere;
  - cada prompt tiene su ★ para quitarlo de la lista.
- Los prompts frecuentes son de cada usuario (se guardan en su cuenta) y se ven igual en la app y en la web.

## 53. Minuta: ubicación del router y seguimiento (app y web)
- **Internet › Ubicación del router**: el área y cómo está instalado, más el detalle que indique el técnico (altura, mueble, paredes, medidas del cuarto). Va en rojo si el router está en un espacio cerrado.
- **Recomendación breve y lista para aplicar: «Reubicación del router»**: sacarlo del mueble o del espacio cerrado y fijarlo a 1–1.5 m del suelo, en una pared despejada y lo más central posible; si la fibra no alcanza, conectar por cable un equipo WiFi (Deco o repetidor) a esa altura en el área de atención.
- **Validación técnica**:
  - «Ubicación del router: altura y obstáculos» → Observado;
  - «Velocidad con carga (cuello de botella)» → Seguimiento, cuando el ping con carga sube a 4 veces o más (entre 80 y 300 ms).
- **Estado**: la minuta puede seguir APTA; el Resumen ejecutivo indica que el router queda en observación y seguimiento.
- **Pasos**:
  - Por parte del equipo técnico: «Dar seguimiento al router: si se presentan intermitencias o lentitud en los puntos de red, revisar su ubicación y, si se confirma que es necesario, indicar al cliente que lo reubique según la recomendación.» (sin fechas);
  - el encabezado de los pasos del local es **«Por parte del cliente»**, y la reubicación no se le pide de forma directa: depende del seguimiento del técnico.
- **Formato**: el Resumen ejecutivo no repite el nombre del local; la ubicación del router no se repite como observación suelta.
- **Desde el Asistente IA** (app y web), en modo actualizar minuta: por ejemplo «Actualiza la minuta de [local]: el router está casi al ras del suelo dentro de un mueble de melamina, en un cuarto pequeño de 1.5 m de profundidad, entre paredes gruesas».
  - La tarjeta «Cambios que haré» muestra la ubicación del router y la recomendación; al tocar **Aplicar** se genera la nueva versión con todo lo anterior.
  - Si no se marca «Fue una nueva visita al local», **se mantiene la fecha de la minuta**.
  - En el simulador esto se interpreta con reglas; en la app real lo interpreta ChatGPT (OpenAI, vía FastAPI) con las mismas salidas.

## 54. Ubicar el puerto del switch de cada impresora — paso opcional (app y web)
- Aparece en el Chat IA de la evaluación, después de las preguntas de impresoras, **solo si hay impresoras de red conectadas a un switch y con IP**.
- Es **opcional**: la app pregunta «¿quieres ubicar en qué puerto del switch está cada impresora?» con dos opciones, **Sí, ubicar los puertos** o **Saltar este paso**. Si el técnico lo salta, la minuta no cambia (no hay observación ni descuento).
- Si acepta:
  1. indica cuántos puertos tiene el switch (5, 8, 16, 24, 48) y el puerto de subida al router, que se descarta;
  2. por cada impresora, la app envía pings a su IP (2 s sí / 2 s no durante 20 s) y muestra el dibujo del switch; el técnico toca el puerto cuya luz sigue el ritmo, o «Ninguna luz sigue el ritmo» (no está en este switch), o **salta esa impresora**;
  3. con los mismos pings se mide la calidad del punto de red: con más de 2 % de pérdida o más de 50 ms, el punto queda **en observación**;
  4. pregunta si se rotularon los puntos en el switch (Sí / Parcial / No).
- En la minuta: tabla **Switch · puertos** (puerto, impresora, IP, ping y pérdida, estado) después de Impresoras; reglas «Impresoras ubicadas en el switch» y «Calidad de los puntos de red (ping)»; observación por punto de red con pérdida y la recomendación «Rotular los puntos de red en el switch» si no se rotularon.
- Solo se usan pings (ICMP); nunca se envían datos al puerto 9100 de la impresora. El celular debe estar en el WiFi del local. Solo se ubican puertos con un equipo encendido y con IP.
- En el simulador el resultado del ping es de ejemplo; en la app real lo mide el celular.

## 55. Dónde se actualiza cada cosa de una minuta (app y web)
| Qué cambia | Cuándo | Dónde | Resultado |
|---|---|---|---|
| Datos del local (ubicación del router, puerto del switch de cada impresora…) | Durante la visita | **Chat IA de la evaluación** | El dato entra con su foto o prueba |
| Datos del local | Después de cerrar la minuta | **Asistente IA**: «Actualiza la minuta de [local]: …» | Nueva versión con historial; vuelve a validación |
| Criterios de todas las minutas (formato, reglas, textos de recomendaciones) | Cuando se decida | **Administrador en Preguntas y reglas** o **equipo de desarrollo** | Nunca desde un chat |

- **Chat IA de la evaluación**:
  - después de la foto de ubicación del router pregunta **«¿cómo está instalado el router?»**: a la vista a 1–1.5 m, a la vista cerca al piso, dentro de un mueble cerrado con puertas, en un cuarto pequeño con puertas, u Otro (describir altura, mueble, paredes, medidas). Si no está a la vista y a buena altura, la minuta lleva la recomendación «Reubicación del router» y el seguimiento del técnico (sección 53);
  - los puertos del switch, con el paso opcional «Ubicar puerto» (sección 54);
  - al terminar la evaluación, si el técnico escribe la ubicación del router o el puerto de una impresora, se registra como **dato** de la minuta (no como una nota suelta), con «Aplicar» o «Descartar».
- **Asistente IA** (después de cerrar): además de la ubicación del router, entiende los puertos del switch, por ejemplo «Actualiza la minuta de [local]: la IMP. Barra 1 va al puerto 3 del switch de 8 puertos». La tarjeta «Cambios que haré» los muestra; al aplicar, la minuta tiene la tabla Switch · puertos, sube de versión y vuelve a Pendiente. La fecha se mantiene salvo que se marque «Fue una nueva visita al local».
- **Criterios**: si en cualquiera de los dos chats se pide un cambio para todas las minutas («las recomendaciones deben ser más breves en todas las minutas», «cambia la regla del ping», «de ahora en adelante…»), el chat **no lo aplica** y explica que lo hace el administrador en Preguntas y reglas o el equipo de desarrollo:
  - el administrador ve el botón **Abrir Preguntas y reglas**;
  - el técnico y el supervisor ven **Enviar sugerencia al administrador**, que le llega en Notificaciones.
- Las minutas ya terminadas no se reabren por las preguntas nuevas: toman su valor por defecto.

## 56. Reportes unificado: la misma pantalla en la app y en la web, por rol y por Roles y permisos
Reportes es **una sola pantalla** con los mismos datos (misma cuenta) en la app y en la web. En la app se ve igual, adaptada al celular y sin menú lateral.

### Pestañas por rol
| Rol | Pestañas |
|---|---|
| Administrador | **General** · **Por técnico** (todos los técnicos) · **Mis minutas** |
| Supervisor | **Mis técnicos** (los asignados) · **Mis minutas** |
| Técnico | **Mis minutas** |

En **Equipo y accesos › Roles y permisos** (web y app del administrador) hay un grupo **Reportes** que decide las pestañas de cualquier rol, también de un rol nuevo:
- **General** = ve todas las minutas de la empresa (permiso `m_all`).
- **Por técnico** (todos los técnicos, `r_tec_all`) **o** **Mis técnicos** (solo los asignados, `r_tec_mine`), o ninguno. Son excluyentes: marcar uno quita el otro.
- **Mis minutas** = puede crear minutas (`a_create`).

### Qué muestra cada pestaña
| Pestaña | Filtros de arriba | Indicadores | Contenido |
|---|---|---|---|
| General | Fecha · Técnico · Supervisor · Rubro · Estado · Criticidad · Nivel de esfuerzo · Plazo | Minutas completadas · A tiempo · Tiempo promedio · En riesgo | Lista de minutas y **Trazabilidad** de la minuta elegida (con «Ver minuta») |
| Por técnico / Mis técnicos | Ninguno (solo las pestañas) | — | Gráficos «Tiempo promedio por minuta» y «Minutas a tiempo», **Plazos de la empresa** (solo administrador, con «Editar plazos») y tabla por técnico (avance, completas, a tiempo, tiempo promedio, en mora, devueltas) |
| Mis minutas | Fecha · Rubro · Estado · Criticidad · Nivel de esfuerzo · Plazo | Mis minutas completadas · A tiempo · Tiempo promedio · En riesgo | Lista de minutas y **Trazabilidad**; en la app, botón + para una nueva evaluación |

- Los filtros **sí filtran**: la lista y los cuatro indicadores se calculan con las minutas que quedan. Cada filtro muestra cuántos valores tiene marcados y hay un botón **Limpiar**.
- Al abrir una minuta y volver, Reportes conserva la pestaña, los filtros y la minuta elegida.
- Los indicadores del Inicio abren Reportes con el filtro que corresponde (por ejemplo, Esperando validación → Estado: Completa). Si el rol no tiene General, abren la lista del indicador.
- «Ver una minuta de otro técnico / de otro equipo» (acceso temporal con código) sigue en Mis minutas del técnico y en Mis técnicos del supervisor.
- **Editar plazos** (Tiempos de trabajo por tipo de local, nivel de esfuerzo y mapa de red) también se abre en la app del administrador.
- Sin textos de ayuda en botones ni ventanas.

### API (FastAPI) — `GET /reportes`
`GET /reportes?vista=general|tecnicos|mis&desde=&hasta=&tecnico=&supervisor=&rubro=&estado=&criticidad=&esfuerzo=&plazo=` → `{ "pestanas": ["general","tecnicos","mis"], "indicadores": {...}, "minutas": [...], "tecnicos": [...] }`.
- `pestanas` sale de los permisos del rol del usuario (`m_all`, `r_tec_all` / `r_tec_mine`, `a_create`); una vista no permitida responde 403.
- `tecnicos` (vista `tecnicos`): todos los técnicos con `r_tec_all` o solo los asignados al supervisor con `r_tec_mine`.
- Trazabilidad: `GET /minutas/{id}/eventos`.

### Criterios de aceptación
- Dado un técnico, cuando abre Reportes en la app o en la web, entonces solo ve la pestaña Mis minutas.
- Dado un supervisor, entonces ve Mis técnicos y Mis minutas; si el administrador le marca General en Roles y permisos, también ve General.
- Dado el filtro Estado = Validada en General, entonces la lista y los cuatro indicadores solo cuentan minutas validadas.
- Dada la pestaña Por técnico, entonces no hay filtros, solo las pestañas.

## 57. Contraseñas en la minuta (app y web)
Las contraseñas que se registran **sí se muestran** en la minuta, en **Desarrollo y hallazgos › Accesos y contraseñas** (Equipo · Red o usuario · Contraseña):
- **Red WiFi del router**: el cuadro «Datos del router» (lectura de las 3 fotos del router) ahora también lee de la etiqueta el **nombre de la red WiFi** y su **contraseña**; el técnico los valida o corrige.
- **Portal web de una impresora**, **repetidor**, **router** u otra red: se escriben en el chat. Durante la visita, en el **Chat IA** al terminar («el portal web de la IMP. Caja usuario admin contraseña 123») → «¿Lo aplico?» → Aplicar. Después de cerrar, en el **Asistente IA** («Actualiza la minuta de [local]: la contraseña del WiFi MOVISTAR_F653 es …») → nueva versión.
- Si se registra otra contraseña para el mismo equipo, reemplaza a la anterior.
- API: `PATCH /minutas/{id}` con `accesos: [{ "equipo": "IMP. Caja · portal web", "usuario": "admin", "contrasena": "123" }]`; se guardan cifradas y se muestran en la minuta y en el PDF.

## 58. Panel: «Reporte y mora» y pestañas que no regresan al inicio (app y web)
- En **Avance y tiempos del equipo** está el botón **Reporte y mora** (como en la web): abre Reportes en la pestaña **Por técnico** (administrador) o **Mis técnicos** (supervisor), donde están el avance, la mora y los plazos.
- En la app, al tocar **Por día · Estados · Tiempos · Alertas · Negocios**, el rango de fechas o «Ver toda la actividad», el Panel **se queda en ese apartado**: no vuelve al inicio del módulo.

## 59. Asistente IA: «¿Qué aprendiste hoy?», reglas nuevas y ficha de aprendizaje (administrador, app y web)
Solo para el rol **Administrador** (a otro rol el asistente le indica que es una consulta del administrador y le ofrece enviar lo que aprendió como sugerencia).

1. **«¿Qué aprendiste hoy?»** → tarjeta **Lo que aprendí hoy** con:
   - cuántas visitas revisó del día (evaluación, **soporte** e **información**), las reglas activas, las minutas terminadas y las fichas de la base de conocimiento;
   - por cada caso: tipo (Soporte / Información), local, qué pasó, **qué aprendió** y la propuesta: **regla nueva** (ID, nombre, tipo, cuándo se activa) y **pregunta nueva** (ID, sección, opciones), o «ya lo cubre la regla …»;
   - las notas de los técnicos que todavía no son regla y las sugerencias de criterio que le enviaron;
   - la sugerencia: «Te sugiero agregar N reglas y N preguntas. ¿Procedo?».
2. **«Procede»** (o «agrega las reglas», «genera las preguntas», o el botón **Agregar reglas y preguntas**) → el asistente **se autogestiona**: crea las reglas (activas) y las preguntas en su sección de **Preguntas y reglas**, sube la versión del flujo y lo deja en el historial de cambios («El Asistente IA creó la regla … (aprobada por el administrador)»). Se puede pedir solo reglas o solo preguntas. Aparecen igual en la app y en la web.
3. **«Guarda el conocimiento y genera la ficha de aprendizaje»** (o el botón) → guarda cada caso en la **base de conocimiento** y genera una **ficha de aprendizaje por caso** (código FA-AAAAMMDD-NN): tipo de visita, local, técnico, quién la registró, casuística, causa, solución aplicada, aprendizaje, cómo se aplica en las próximas visitas (regla y pregunta) y estado. Botón **Descargar ficha (PDF)**. Se puede pedir solo la de soporte o la de información.
4. Desde entonces, el Asistente IA **responde con ese conocimiento** (por ejemplo, «¿qué hago si la impresora cambió de IP después de reiniciar el router?») y cita la regla y la ficha.

Ejemplo del día: soporte en Muna Cocina Palestina → regla **R_IP_FIJA** «IP fija en las impresoras de red» (Observación) y pregunta **P28f** «¿La IP de la impresora es fija o la asigna el router?» en Impresoras; información de zona de temporada → regla **R_ZONA_TEMP** (Recomendación) y pregunta **P11t** en Negocio y canales.

### API (FastAPI)
- `GET /aprendizaje/hoy` (solo administrador) → `{ "visitas": {"evaluacion":n,"soporte":n,"informacion":n}, "casos": [{ "id", "tipo", "local", "caso", "causa", "solucion", "aprendizaje", "regla_sugerida": {...}, "pregunta_sugerida": {...}, "cubierta_por": "R51" }], "notas": [...], "sugerencias": [...] }`. ChatGPT redacta el aprendizaje con las minutas de soporte e información del día, las reglas y la base de conocimiento como contexto.
- `POST /aprendizaje/aplicar` `{ "casos": ["L1"], "solo": "reglas|preguntas|null" }` → crea reglas y preguntas (nueva versión del flujo, con auditoría).
- `POST /conocimiento/fichas` `{ "casos": [...] }` → guarda en la base de conocimiento y devuelve `{ "fichas": [{ "codigo": "FA-20261105-01", ... }], "pdf_url": "..." }`.

### Criterios de aceptación
- Dado el administrador, cuando escribe «¿Qué aprendiste hoy?», entonces ve los casos del día con sus reglas y preguntas sugeridas.
- Cuando responde «procede», entonces las reglas y preguntas quedan activas en Preguntas y reglas (app y web) y en el historial.
- Cuando pide «guarda el conocimiento y genera la ficha», entonces puede descargar el PDF y el asistente responde después con ese conocimiento.
- Dado un técnico, cuando pregunta «¿Qué aprendiste hoy?», entonces el asistente no muestra el resumen.

## 60. Asistente IA: minuta de Soporte / incidencia (app y web)
Al pedir **«Nueva minuta»** (botón «Redactar minuta nueva» o «redacta una minuta…»), el Asistente IA pregunta primero **Implementación** o **Soporte / incidencia**. Si el mensaje ya dice «soporte» o «incidencia», entra directo al flujo de soporte; Implementación sigue el flujo de siempre.

### Flujo de soporte (un paso a la vez en el chat)
| Paso | Qué pide | Qué hace el asistente |
|---|---|---|
| 1. Cliente | Nombre del local o razón social | Busca sus **minutas e incidencias anteriores** y las muestra como **Antecedentes** (tipo, fecha, estado y lo principal). Si no hay, la minuta va sin antecedentes. Toma de la minuta de implementación la razón social, la dirección y el rubro. |
| 2. Motivo | La incidencia que reportó el cliente | Va a Antecedentes («Motivo de la visita»). |
| 3. Alcance | Los puntos de la visita (separados por «;» o uno por línea) | Arma «Objetivo y alcance». |
| 4. Evidencias | Fotos y capturas con 📎 (o «sin evidencias») | Las **lee** (selftest: IP, MAC y si es fija o automática; repetidor; configuración WiFi; router; controlador) y muestra lo que encontró; van al Anexo A. |
| 5. Lo realizado | Qué se hizo en cada punto (uno por uno) | Va a «Desarrollo técnico»; de ese texto saca las impresoras (IP, conexión, respaldo) y las contraseñas. |
| 6. Reglas de soporte | Confirmar o corregir cada regla | Las propone según lo realizado: Cumple · No cumple · No aplica. |
| 7. Estado final | **Operativo** · **Operativo con pendiente** · **Observado** | Sugiere uno: Observado si una regla no se cumple; Operativo con pendiente si algo quedó pendiente; si no, Operativo. |

«cancelar» sale del flujo.

### Reglas de soporte
| ID | Regla | Si no cumple: recomendación · paso del equipo técnico · paso del cliente |
|---|---|---|
| S1 | Un solo sistema de impresiones activo | Un solo sistema de impresiones · desactivar el duplicado y probar la impresión |
| S2 | IP fijas y sin conflictos | IP fija y reservada en cada impresora · fijar y registrar la IP · solicitar al proveedor de internet la reserva de las IP |
| S3 | Respaldo de cada impresora | Respaldo (USB o WiFi) con su propia IP · configurar los respaldos que faltan |
| S4 | Lugares de preparación con su impresora asignada | Impresora en cada lugar de preparación · asignarla en el sistema y probar la comanda · indicar qué impresora usará cada lugar |

### Minuta de soporte
Mismo formato y logo que la minuta de implementación: Datos generales (tipo de visita: Soporte / incidencia) · Antecedentes y motivo · Objetivo y alcance · Desarrollo técnico (por punto) · Impresoras y red (con Accesos y contraseñas) · Validación técnica (S1–S4) · Recomendaciones (solo si hay reglas que no se cumplen o se agregan) · Resumen ejecutivo (estado final) · Pasos a seguir · Anexo A. Evidencias.

Misma lógica que hoy: reglas de redacción acordadas (sin redundancia; las contraseñas van solo en su tabla; «Por parte del cliente»; los pasos no llevan fechas), sin mencionar que se usó otra minuta como referencia; queda **Pendiente** en Ver minuta con Editar, Compartir y **Solicitar validación** al supervisor; al actualizarla con el Asistente IA sube de versión con historial y vuelve a validación. En Reportes aparece como una minuta más del técnico y al abrirla va directo a Ver minuta.

### API (FastAPI)
- `POST /minutas` con `tipo: "implementacion" | "soporte"`.
- Soporte: `{ "tipo": "soporte", "cliente_id", "motivo", "alcance": [..], "evidencias": [..], "realizado": [..], "reglas": {"S1":"cumple|no_cumple|no_aplica",...}, "estado_final": "operativo|operativo_pendiente|observado" }`.
- `GET /clientes/{id}/antecedentes` → minutas e incidencias anteriores del cliente.
- ChatGPT redacta con las reglas de soporte y de redacción como contexto; la minuta va a validación del supervisor como hoy.

### Criterios de aceptación
- Dado «Nueva minuta», entonces el asistente pregunta Implementación o Soporte / incidencia.
- Dado un cliente con visitas anteriores, entonces aparecen como antecedentes.
- Dada una regla de soporte marcada «No cumple», entonces la minuta tiene su recomendación, sus pasos y el estado sugerido es Observado.
- Dada una minuta de soporte redactada, entonces queda Pendiente con «Solicitar validación»; al actualizarla, sube de versión con historial.
