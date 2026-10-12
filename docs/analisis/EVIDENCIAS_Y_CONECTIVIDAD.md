# Evidencias, speedtest y WiFi por zona

Última actualización: 2026-10-11. Reglas definidas con el equipo. **No implementadas** (la 0.0.3 no las tiene).

> ⛔ Se implementa en una rama local y se prueba en local. No se sube nada a `main` ni a producción sin aprobación del equipo.


> **Regla visual de toda la app (confirmada 2026-10-11):** los menús, hojas inferiores, diálogos, botones, chips, burbujas del chat, cabeceras y tarjetas **mantienen la forma visual actual**; solo se agregan las opciones nuevas dentro de esas mismas piezas. Referencia en el código: hoja "Agregar al mapa" y menú de un equipo (`TopologyEditorFragment`: título 18 bold, filas con ícono en cuadro de 42 dp y radio 14, diálogos de 28 dp de radio), respuestas rápidas y chips del chat (`TechnicalChatFragment`: contorno azul #1565C0, radio 18, 36 dp; marcado = relleno azul), burbujas `bg_chat_bot` / `bg_chat_user`, tarjetas `bg_home_card` (radio 22) y botones de la propuesta (radio 18, 52/56 dp). La maqueta "Asistente de Red — maqueta de la lógica nueva" sigue esta regla.

---

## 1. La IA valida cada evidencia antes de leerla

**Hoy:** `app/chat/evidence.py` extrae los datos y deja en `null` lo que no se lee. No revisa si la foto es la evidencia pedida (una foto de un gabinete pasa como "router") ni pide repetirla. Todas se pueden omitir menos E9.

**Cambio:** en cada evidencia, la IA responde primero dos preguntas y luego extrae los datos.

**Opciones al pedir una evidencia (confirmado 2026-10-11):** solo **Tomar foto**, **Subir foto** y **Saltar evidencia**. No hay otras opciones; los mensajes de error aparecen solo después de que la IA revisa la foto.

| La IA responde | Si falla, el chat dice | Opciones |
|---|---|---|
| `esEvidenciaSolicitada` (¿la foto muestra lo que se pidió? Ej.: llega la foto de una silla) | "La foto no corresponde a lo solicitado (parece {lo que ve}). Vuelve a subir la foto o salta la evidencia." | **Volver a subir foto** · **Saltar evidencia** |
| `legible` (¿se leen los datos?) | "La evidencia no es legible. Vuelve a tomar la foto o salta la evidencia." | **Volver a tomar foto** · **Saltar evidencia** |

- **Cuando la IA lee los datos** muestra la lectura editable con **Correcto** y **No es correcto**. Con "No es correcto" el chat ofrece **Volver a tomar foto**, **Volver a subir foto** o **Continuar sin esta lectura** (queda pendiente). Así el técnico nunca queda trabado (por ejemplo, una impresora USB sin IP).
- Lo saltado queda como pendiente en la minuta y se puede completar después (vista previa o chat).
- Las **evidencias obligatorias** (sección 2) tienen solo Tomar foto y Subir foto, sin "Saltar".
- Aplica a todas: speedtest (E1, E1b), router (E2), etiqueta (E3), ipconfig (E4), autotest (E5), puntos de red (E6), extensión (E7), escáner (E8) y fotos de zonas.

```jsonc
// Prompt de E*: se agregan dos claves antes de los datos
{ "esEvidenciaSolicitada": true, "queMuestra": "router TP-Link en un mueble", "legible": true, ...datos }
// Android: la evidencia muestra solo ["Tomar foto", "Subir foto", "Saltar evidencia"]
// flow_engine: esEvidenciaSolicitada=false → followUp ["Volver a subir foto", "Saltar evidencia"]
//              legible=false              → followUp ["Volver a tomar foto", "Saltar evidencia"]
// obligatorias: sin "Saltar evidencia"
```

**Reglas para leer el speedtest (E1, E1b):**
- **Ignorar la publicidad.** Las capturas traen anuncios ("Plan regular 500 Mbps", "1000 Mbps"); solo se leen DESCARGA, SUBIDA, Ping, ping durante la descarga, ping durante la subida, proveedor y servidor.
- **Aceptar fotos de la pantalla**, no solo capturas.
- **Prueba incompleta:** si no se ve la subida o el proveedor, pedir otra foto con el resultado completo.
- Leer la **banda** si se ve en la barra de estado ("5" junto al ícono del WiFi = 5 GHz). Solo aplica a pruebas por WiFi (sección 5).

## 2. Evidencias obligatorias

Reemplazan a E9 ("fotos generales del local"). **Sin ellas no se pasa al análisis.** Se piden aunque el local esté en obra o remodelación, sin router, sin internet o sin puntos de red.

| Evidencia | Cuántas |
|---|---|
| **Frontis del local** | 1 |
| **Cada lugar de preparación** indicado en el chat (Cocina, Barra 1, Parrilla…) | 1 por lugar |
| **Cada zona de atención** de cada canal (Salón 1, Terraza, Mostrador, Despacho…) | 1 por zona. Si la zona es la propia **Caja**, basta la foto de la caja |
| **Cada caja** (Caja 1, Caja 2…) | 1 por caja |

Las zonas escritas con "Otro" también se piden.

### 2b. Evidencias de cada impresora (confirmado 2026-10-11)

**Antes** de la pregunta "IMP. {nombre}: ¿cómo se conecta?" (P28), el chat pide por cada impresora, en este orden:

| # | Evidencia | La IA lee |
|---|---|---|
| 1 | Foto de la **etiqueta** donde se vean el **número de serie (SN)** y el **modelo** | marca, modelo, SN |
| 2 | Foto de la **parte de atrás con sus conexiones** | qué cable está conectado: red, USB o ninguno (WiFi) |
| 3 | Foto del **selftest (hoja de prueba)** | Confirma **marca y modelo** (los completa si no salieron en la etiqueta) y registra **IP, MAC y puerta de enlace** (reemplaza al autotest E5). Impresora **USB**: se pide igual, pero no tiene IP ni MAC; solo marca y modelo |

- Cada una tiene Tomar foto · Subir foto · Saltar evidencia y la validación de la sección 1.
- P28 se sigue preguntando, pero la opción que se ve en la foto de conexiones viene marcada como sugerida ("Red por cable (se ve en la foto)"). El técnico confirma o cambia.
- Con la IP del selftest se aplica R55/V04: si no está en el segmento del router (puerta de enlace del ipconfig E4), aviso para cambiar la IP y volver a subir el selftest; la minuta queda OBSERVADO.
- Marca, modelo y SN van a la tabla de equipos de la minuta y al mapa ("IMP. {nombre}").

## 3. Speedtest del plan (E1): por cable directo al router

**Regla del equipo:** la prueba del internet **no se hace por WiFi**. Se hace desde una **PC o laptop conectada con cable de red (Cat6) a un puerto RJ45 gigabit del router principal**. Si la PC de caja ya está conectada así, basta hacerla ahí.

**Instrucción de la evidencia:**
> "Conecta una PC o laptop por cable de red (Cat6) a un puerto RJ45 gigabit del router principal y haz el speedtest. Espera a que termine y toma la captura con la descarga, la subida y el ping. Haz 2 pruebas."

**Pregunta nueva P19a:** "¿Desde qué equipo hiciste la prueba?" → *PC de caja por cable al router* · *Laptop por cable al router* · *Por WiFi*.
- **Por WiFi:** "La prueba de internet debe hacerse por cable directo al router. Conecta una PC o laptop por cable y repítela." Se puede saltar → el plan queda **"sin verificar"**.
- Cruce con el ipconfig (E4): si el adaptador de la PC de caja es WiFi, la prueba no vale para el plan.

**Evaluación:**
| Caso | Resultado |
|---|---|
| La mejor de las 2 pruebas llega al **70 % del plan contratado** (P15) o más | Cumple |
| Menos del 70 % | OBSERVADO: "El internet no llega a lo contratado. Revisar con el proveedor." |
| Entre **85 y 95 Mbps** con un plan mayor | Aviso: "La velocidad parece limitada a 100 Mbps por el switch, el puerto de red o el cable. Repite la prueba directo al router principal, con cable Cat6 y en un puerto RJ45 gigabit (10/100/1000) en el router y en la PC o laptop." |
| No hay prueba por cable | Plan **"sin verificar"**, sin penalizar |

- Se guarda el **servidor** de cada prueba (proveedor y ciudad).
- **Cambio en V01:** deja de exigir 50 Mbps fijos a cualquier speedtest y pasa a usar solo la prueba por cable, con las reglas de la tabla.
- **Hallazgo en las minutas cargadas:** varias pruebas dieron casi 94 Mbps (Dorcher 94.0/94.4, RKT Bayóvar 93.2, RKT Mariscal 93.9, RKT Hacienda 88.9–92.6, Gelato Ejército 89.4). Es probable que estuvieran limitadas por un switch 10/100, un puerto de 100 Mbps o el cable, no por el plan.

## 4. Saturación (ping con carga)

El speedtest muestra tres pings: **en reposo**, **durante la descarga** y **durante la subida**. Mientras más bajo, mejor.

| Ping | Calificación |
|---|---|
| < 50 ms | Excelente |
| 50–100 | Bueno |
| 100–300 | Regular: pequeñas demoras |
| > 300 | Malo: demoras de 1 a 3 s por acción; si se mantiene, envíos que fallan o se duplican |

- Si el ping **durante la descarga o la subida supera los 300 ms**, la red **se satura con carga**.
- **No impide el inicio.** Sale como **observación**: "La red se satura con carga (ping de hasta {n} ms): en horas de mucho uso las comandas pueden demorar."
- **Recomendaciones:** separar el WiFi de clientes (R32), PC de caja por cable (R11) y, si el router lo permite, prioridad al tráfico del punto de venta.

## 5. WiFi por zona (opcional)

**Botón "Revisar WiFi por zona"** en el bloque de WiFi. El técnico ya está conectado al WiFi del local con el nombre y la clave que dio el cliente; la app usa esa conexión ("Conectado a: {red}. ¿Es la red del local?").

- Se mide en **todas las zonas de atención y todos los lugares de preparación** (no depende del canal).
- En cada uno, el técnico toca **"Medir aquí"**: señal (dBm), banda (2.4 / 5 GHz) y latencia al router. Opcionalmente, un speedtest por WiFi en esa zona.

**Umbrales para el punto de venta** (la demora pesa más que la velocidad):

| Métrica | Excelente | Buena | Regular | Mala |
|---|---|---|---|---|
| Ping en reposo | < 50 ms | 50–100 | 100–200 | > 200 |
| Aumento del ping con carga | < 100 ms | 100–200 | 200–300 | > 300 (observación) |
| Descarga | ≥ 20 Mbps | 10–20 | 5–10 | < 5 |
| Subida | ≥ 10 Mbps | 5–10 | 2–5 | < 2 |
| Señal | ≥ −60 dBm | −60 a −67 | −67 a −75 | < −75 |

- **Zona apta** si ninguna métrica sale "Mala". Una zona regular o mala se trata como zona sin señal (P46): repetidor o access point (R19) y −10 en Conectividad una sola vez.
- **Las pruebas por WiFi no se comparan con el plan:** dependen del equipo (antenas, tarjeta WiFi) y de la banda.
- **En la minuta:** tabla corta zona · señal · calificación · banda.

**Diagnóstico antes de recomendar:**
| Lo que se ve | Diagnóstico | Recomendación |
|---|---|---|
| Buena señal o 5 GHz, pero poca velocidad | El equipo o la banda | Tablets en 5 GHz; PC de caja por cable (R11) |
| Mala señal y poca velocidad | Falta cobertura | Repetidor o access point (R19) |
| Por cable junto al router, por debajo del 70 % del plan | Internet, plan o router | Revisar con el proveedor |
| Ping en reposo bien, con carga muy alto | Saturación | R32, R11, prioridad al punto de venta |
| Pruebas muy distintas entre sí | Variación normal | Usar la mejor de 2 y guardar el servidor |

## 6. Ejemplo (pruebas del 2026-10-11, router en el 1.er piso, pruebas en el 2.º)

| Prueba | Descarga | Subida | Ping reposo | Ping con carga | Lectura |
|---|---|---|---|---|---|
| Celular WiFi 2.4 GHz | 25.94 | no se ve | 36 | no se ve | Buena; pedir prueba completa |
| PC WiFi (Colinanet, Miraflores) | 69.71 | 55.02 | 5 | 387 / 320 | Excelente; observación de saturación |
| Celular WiFi 5 GHz | 137.92 | 77.11 | 13 | 135 / 150 | Excelente |
| PC WiFi (Movistar, San Borja) | 88.43 | 38.55 | 5 | 274 / 392 | Excelente; observación de saturación |

- **Plan:** sin verificar (ninguna prueba fue por cable junto al router).
- **2.º piso:** apto para el punto de venta. Observación: la red se satura con carga → PC de caja por cable y WiFi de clientes separado.

## 7. Por confirmar

1. *(Resuelto 2026-10-11: ~150 Mbps por zona de atención y mínimo por equipo; ver LOGICA_ANALISIS.md 2.5.)* Los **200 Mbps por zona** (recomendación comercial del plan): ¿se calculan con todas las zonas de atención o solo con salones y terraza?
2. Los umbrales de la sección 5 y el 70 % del plan son propuestos; confirmar con el equipo.

## 8. Prompt para Claude Code

```
Lee docs/analisis/EVIDENCIAS_Y_CONECTIVIDAD.md.
REGLAS: no hagas push a main ni toques producción; rama local feature/evidencias-conectividad; prueba en local.
Muéstrame el diff y los tests antes de cualquier commit.
1. evidence.py: agrega esEvidenciaSolicitada, queMuestra y legible a todos los prompts; para E1/E1b agrega
   pingDescargaMs, pingSubidaMs, servidor, completo y banda, e ignora la publicidad.
2. flow_engine: si la evidencia no es la pedida o no es legible, followUp "Volver a subir" / "Saltar evidencia"
   (sin "Saltar" en las obligatorias). Reemplaza E9 por las fotos obligatorias de la sección 2.
3. P19a (equipo de la prueba), regla del 70 % del plan, aviso de 85–95 Mbps y V01 solo con la prueba por cable.
4. Observación de saturación (ping con carga > 300 ms).
5. Android: botón "Revisar WiFi por zona" (señal, banda, latencia) y envío de las mediciones al gateway.
6. Tests con los 4 ejemplos de la sección 6 y con una foto que no corresponde (gabinete en lugar de router).
```
