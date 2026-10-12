# Estado de la versión 0.0.3: qué está hecho y qué falta

Última actualización: 2026-10-11. Revisión del APK `AsistenteDeRed-0_0_3.apk` (versión 0.0.3, código 3) y de los repositorios en GitHub, **solo lectura**:

| Repo | Commit | Fecha | Mensaje |
|---|---|---|---|
| Android | `0e2aa95` | 2026-10-10 | Versión 0.0.3: E1b y puertos del router en el chat; docs del flujo completo |
| FastAPI | `6a0acec` | 2026-10-10 | Flujo completo de comandas, internet e impresión (diseño 4b) |
| Gateway | `a254db8` | 2026-10-10 | La minuta muestra el aviso de viabilidad |

El chat tiene **96 nodos** (antes 76). El APK apunta a producción (`https://asistenteredidbi.up.railway.app/`).

> ⛔ Todo lo "pendiente" se implementa en una rama local y se prueba en local. No se sube nada a `main` sin aprobación del equipo.


> **Regla visual de toda la app (confirmada 2026-10-11):** los menús, hojas inferiores, diálogos, botones, chips, burbujas del chat, cabeceras y tarjetas **mantienen la forma visual actual**; solo se agregan las opciones nuevas dentro de esas mismas piezas. Referencia en el código: hoja "Agregar al mapa" y menú de un equipo (`TopologyEditorFragment`: título 18 bold, filas con ícono en cuadro de 42 dp y radio 14, diálogos de 28 dp de radio), respuestas rápidas y chips del chat (`TechnicalChatFragment`: contorno azul #1565C0, radio 18, 36 dp; marcado = relleno azul), burbujas `bg_chat_bot` / `bg_chat_user`, tarjetas `bg_home_card` (radio 22) y botones de la propuesta (radio 18, 52/56 dp). La maqueta "Asistente de Red — maqueta de la lógica nueva" sigue esta regla.

---

## 1. Hecho y desplegado (0.0.3)

| Cambio | Dónde |
|---|---|
| P14: Fibra óptica, Cable coaxial, No sé, No tiene, Internet con chip | Flujo |
| P22: Sí, No, Tablet, No sé, Otro (+ P22f: escribir el equipo) | Flujo |
| P12b "¿En caja saldrán comandas?" y aviso de Sunmi | Flujo |
| Prueba de señal del chip (P14b) y speedtest del chip (E1b) | Flujo |
| Decisión de impresión: Raspberry, PC con controlador, PC básica, Tablet → Raspberry | Flujo (`G_IMPRESION`) |
| Puertos del router leídos en la foto E2 (P18a si no se leen), switch y puntos de red | Flujo (`G_PUERTOS`) |
| Zonas sin WiFi: repetidor o access point (P46a) | Flujo |
| Impresoras de preparación siempre de red por cable (R51) | Flujo y validación de la minuta |
| Áreas de preparación nuevas en P11 (cafetería, parrillas, makis, ramen, panadería) | Flujo |
| Escribir el área con "Otro" en P11 | Android (`askCustomArea`) |
| Aviso de viabilidad en la minuta | Gateway |
| Sin logo ni nombre de IDBI en la app y en la minuta | Android y gateway |

## 2. Pendiente de lo acordado

| Tema | Hoy en la 0.0.3 | Diseño |
|---|---|---|
| Regla general "Otro" (escribir en toda pregunta con Otro) | Solo en P11 (áreas) y P22 (equipo de caja). P13, P29, P09 no piden el texto | `base-conocimiento/…`, "Regla general: opción Otro" |
| P09 rubro + tipo de comida y perfiles por rubro | P09 con las opciones anteriores | `base-conocimiento/…`, sección 4c |
| Canales y zonas de atención | No existen | `base-conocimiento/…`, sección 4c |
| P08 estado del local y antecedentes automáticos | Sigue "motivo de la visita" | `base-conocimiento/…`, sección 4d |
| TP-Link con chip por caídas, sin proveedor o patio de comidas grande (R56) | No está | `base-conocimiento/…`, sección 4c |
| Puntaje de Conectividad nuevo (tramos 30/60/80/100) | Cálculo anterior (`analysis.py`) | `LOGICA_ANALISIS.md` |
| Speedtest del plan por cable y WiFi por zona | V01 exige 50 Mbps a cualquier speedtest | `EVIDENCIAS_Y_CONECTIVIDAD.md` |
| Validación de evidencias (¿es la foto pedida? ¿se lee?) y fotos obligatorias por zona | La IA solo extrae; obligatoria solo E9 | `EVIDENCIAS_Y_CONECTIVIDAD.md` |
| Segmento de impresoras contra el router (R55) | No está | `LOGICA_ANALISIS.md`, 3.1 |
| Barras múltiples, barra → caja, ubicación con Otro | No están | `base-conocimiento/…`, 4b |
| Pasos a seguir editables, anexos de video, observaciones | No están | `base-conocimiento/…`, 4d |
| Vista previa, completar al generar el PDF, WhatsApp y correo siempre activos, marca de agua "SIN VALIDAR" si no está validada | WhatsApp siempre activo; "Marcar completa" manual en la lista | `EDICION_Y_ESTADOS_MINUTA.md` |
| Editar con 3 opciones y que lo editado llegue al PDF | Formulario de 5 campos; lo editado **no** llega al PDF | `EDICION_Y_ESTADOS_MINUTA.md` |
| Corregir lecturas tocando el mensaje, pedir cambios a la minuta desde el chat | "Corregir" solo en el momento; el chat final solo cambia el mapa | `EDICION_Y_ESTADOS_MINUTA.md`, sección 3b |
| Mapa: Ubic./IP, íconos, rayos, tipos de línea, editor | Mapa con líneas recta, discontinua y punteada; sin Ubic./IP | `MAPA_DE_RED.md` |
| Credenciales en tabla cifrada | E3 oculta las credenciales (V09) | `base-conocimiento/…`, sección 7 |

- **Confirmadas el 2026-10-11 (simulador, falta en la app):**
  - Regla R16 (impresora USB con la PC o laptop de caja).
  - Sección Referencias en el Análisis IA.
  - Validación de fotos contra el dataset de evidencias de referencia. Necesita el servicio `POST /evidencias/validar` y la tabla `kb_evidencia_referencia`.
  - X roja para eliminar una lectura en "Esto leí en la captura".
  - Vista previa / Propuesta PDF en hojas A4 con compaginado, desplazamiento y zoom.
  - Teclado del chat (ver `TECLADO_CHAT.md`).

## 3. Para revisar en el celular

- **Textos de ejemplo en los diseños de pantalla:** "Restaurante El Rincón", "Técnico: Carlos Rodríguez", "Validada por: Ana Torres", "72%", "Propuesta_ElRincon_v1.0.pdf", "Vigencia 30 días", "Versión v1.0" y el resumen "…WiFi 6 con al menos 3 puntos de acceso…". Confirmar que en uso real se reemplazan por los datos reales.
- **Formulario de edición:** dice "Nombre del Restaurante"; con los rubros nuevos conviene "Nombre del local".
- **APK de prueba (debug):** firmado con la clave de desarrollo y depurable. Para distribuir a técnicos o clientes se necesita un APK de release firmado.
- **`ProposalController` del gateway** devuelve datos fijos (de prueba) y no guarda la propuesta.
