# Mapa de red (topología) y editor

Última actualización: 2026-10-11. Reglas definidas con el equipo. **No implementadas**.

> ⛔ Se implementa en una rama local y se prueba en local. No se sube nada a `main` ni a producción sin aprobación del equipo.

**Hoy (0.0.3):** el mapa tiene tipos internet, router, switch, access point, repetidor, PC, impresora, cámara, POS y otro (`MapNodeTypes`); líneas recta (cable), discontinua (WiFi) y punteada (USB); un equipo "por adquirir" tiene borde punteado; un enlace observado va en rojo. El editor tiene Agregar, Conectar, Borrar y Deshacer; al tocar un equipo se puede renombrar, cambiar tipo o color y eliminar. Al tocar una línea no pasa nada.

> **Lo que NO cambia (confirmado 2026-10-11):** el estilo e interacción actuales del mapa y del editor se mantienen. Cada equipo es un **ícono blanco dentro de un círculo de color** con el nombre debajo; los equipos por implementar son un círculo con borde punteado; se arrastran los equipos y se mantiene presionado para ver las opciones; la cabecera tiene la X, "Editar topología" y el botón Guardar, y la barra inferior tiene los botones con ícono y texto. Todo lo de este documento se **agrega** sobre ese estilo: líneas de Ubic. e IP debajo del nombre, íconos nuevos dentro del mismo círculo, tipos de línea, menú de la línea y los botones nuevos.


> **Regla visual de toda la app (confirmada 2026-10-11):** los menús, hojas inferiores, diálogos, botones, chips, burbujas del chat, cabeceras y tarjetas **mantienen la forma visual actual**; solo se agregan las opciones nuevas dentro de esas mismas piezas. Referencia en el código: hoja "Agregar al mapa" y menú de un equipo (`TopologyEditorFragment`: título 18 bold, filas con ícono en cuadro de 42 dp y radio 14, diálogos de 28 dp de radio), respuestas rápidas y chips del chat (`TechnicalChatFragment`: contorno azul #1565C0, radio 18, 36 dp; marcado = relleno azul), burbujas `bg_chat_bot` / `bg_chat_user`, tarjetas `bg_home_card` (radio 22) y botones de la propuesta (radio 18, 52/56 dp). La maqueta "Asistente de Red — maqueta de la lógica nueva" sigue esta regla.

---

## 1. Qué se muestra

El mapa sirve para ver rápido **qué equipos de red hay, dónde están, su IP y cómo se conectan** (cable, WiFi, por implementar, averiado).

| Se muestra, con **Ubic.** e **IP** | No se muestra |
|---|---|
| Router, TP-Link (router con chip), switch, switch administrable, access point, repetidor, mesh, servidor, PC, laptop, Raspberry, impresoras, cámaras de seguridad | Gabinete, extensión de energía, tablets y celulares por WiFi, y las zonas o lugares de preparación como elementos del mapa |

**Etiqueta debajo de cada ícono:**
```
IMP. Cocina
Ubic.: Cocina
IP: 192.168.1.40
```
- **Ubic.:** donde el técnico dijo en el chat que está el equipo (P18 para el router, P28 para impresoras, subflujo U para equipos nuevos). Puede ser almacén, piso 3, cuarto de telecomunicaciones, administración, un salón o un lugar de preparación.
- **IP:** del ipconfig (E4: la puerta de enlace es el router), del autotest (E5) y del escáner de IP (E8). Si no se conoce: **"IP: sin dato"** (se completa tocando el equipo, en la vista previa o pidiéndolo en el chat).
- **Impresoras:** "**IMP.**" + el nombre que les dio el técnico en el chat: IMP. Cocina, IMP. Empaque, IMP. Despacho, IMP. Parrillas, IMP. Pizza, IMP. Fríos, IMP. Barra, IMP. Barra 1, IMP. Barra 2, IMP. Caja, IMP. Caja 1, IMP. Caja 2…
- **Switch:** "**Switch x**" + número de puertos: Switch x5, x8, x16, x24, x48. Igual para el administrable (cambia el ícono). El número sale de la foto o la etiqueta; si es un switch recomendado, del tamaño que calculó el asistente (`tamanoSwitch`), con borde punteado.

**Estado de cada equipo:** cable o WiFi (por el tipo de línea) · **por implementar** (borde punteado) · **averiado** (en rojo).

## 2. Íconos

En "Elemento de red" y en el mapa automático:
- Router, TP-Link, **switch**, **switch administrable** (íconos distintos), access point, **repetidor**, mesh, **servidor**, PC/laptop, **Raspberry**, **impresora**, **cámara de vigilancia**.
- **WiFi**, **USB** (símbolo de tres puntas; usar `usb` de Material Icons), **rayo de energía azul** y **rayo de energía rojo**.

**Rayos de energía:**
- **Azul:** hay toma de energía junto al equipo.
- **Rojo:** falta toma.
- Se ponen solos según el chat (U5 / P33: "¿hay toma cerca?"). El técnico puede agregarlos, moverlos o quitarlos.

## 3. Equipos del escáner de IP (E8)

| Lo que muestra el escáner | Ícono |
|---|---|
| Nombre **DESKTOP-…** o **LAPTOP-…** | PC / laptop |
| Fabricante Raspberry Pi o nombre "raspberrypi" | Raspberry |
| Fabricante Epson, Bixolon, Star, 3nStar, Xprinter, POSD, o la IP coincide con un autotest (E5) | Impresora (con su nombre IMP. …) |
| Fabricante Hikvision, Dahua, EZVIZ, Imou, TP-Link Tapo | Cámara de vigilancia |
| IP = puerta de enlace del ipconfig | Router |
| TP-Link, Huawei o ZTE que no es la puerta de enlace | Repetidor / AP / mesh (el técnico confirma) |
| Android, iPhone, Samsung, Galaxy… | No se muestran |
| No reconocido | Otro equipo (el técnico cambia el tipo) |

## 4. Líneas

En **Agregar al mapa** hay una sola opción **Línea** con 5 tipos:

| Tipo | Trazo | Uso |
|---|---|---|
| **Gruesa** | Continua gruesa | Conexiones principales (internet → router → switch) |
| **Fina** | Continua fina | Cable de red hacia un equipo |
| **Fina de trazos cortos** | Discontinua fina (estilo "aristas ocultas") | Cámaras de seguridad |
| **Media de trazos medianos** | Trazos medianos con espacios cortos | WiFi |
| **Flecha USB** | Flecha de la impresora a la **PC o laptop** (nunca a tablet, Raspberry, Sunmi, iMin, router, repetidor ni otro equipo), con la punta en ese equipo y el ícono USB junto a la punta | Impresora por USB |

Al **tocar una línea**: **Cambiar tipo de línea**, **Cambiar color** y **Eliminar** (igual que al tocar un equipo).

## 5. Menú del editor

| Botón | Cambio |
|---|---|
| **Conectar** | Se elige primero el equipo o la línea y luego Conectar con otro equipo. **Si ya están conectados**, el botón se pone **rojo** y su ícono cambia a **desvincular** (sigue diciendo "Conectar"); al tocarlo, los desconecta |
| **Borrar** | Opción **Seleccionar varios** para borrar varios elementos de una vez |
| **Deshacer** | Se agrega **Rehacer** al lado |

## 6. Modelo de datos

```kotlin
data class MapNodeModel(
    val id: String, val label: String,
    val type: String,          // + "switch_managed", "server", "raspberry", "mesh", "tplink", "power_ok", "power_missing", "wifi", "usb"
    val location: String? = null,   // "Ubic.: …"
    val ip: String? = null,         // null → "IP: sin dato" (solo en los tipos de la sección 1)
    val ports: Int? = null,         // switch → "Switch x8"
    val status: String = "ok",      // ok | pending | broken
    val x: Float, val y: Float, val color: String? = null
)
data class MapLinkModel(
    val id: String, val source: String, val target: String,
    val style: String,         // main | cable | camera | wifi | usb
    val color: String? = null
)
```
FastAPI (`topology.py` / `build_state_topology`) llena `location`, `ip`, `ports`, `status` y los rayos de energía desde el estado del chat.

## 7. Prompt para Claude Code

```
Lee docs/analisis/MAPA_DE_RED.md.
REGLAS: no hagas push a main ni toques producción; rama local feature/mapa-de-red; prueba en local.
Muéstrame el diff y capturas del mapa antes de cualquier commit.
1. FastAPI: el mapa automático solo con los equipos de la sección 1, con location, ip (o null), ports y status;
   impresoras "IMP. {nombre}", switch "Switch x{n}", rayos de energía según U5/P33, y la clasificación del escáner (sección 3).
2. Android (MapCanvasView y TopologyEditorFragment): etiquetas Ubic./IP, íconos nuevos, 5 tipos de línea
   (incluida la flecha USB), menú al tocar una línea, Conectar/desvincular, Borrar varios y Rehacer.
3. Compatibilidad: los mapas guardados con solid/dashed/dotted se leen como cable/wifi/usb.
```


## 8. Colores y conexiones (confirmado 2026-10-11)

| Caso | Cómo se ve |
|---|---|
| Conexión que existe | Línea **azul** |
| Impresora con buena conexión de red | Línea **azul** aunque pase por un switch o TP-Link por implementar; el **plomo** es solo cuando la impresora (equipo hijo) o su conexión están por implementar |
| Impresora USB conectada a un equipo que no es PC ni laptop (tablet, Raspberry, Sunmi, iMin, router…) | La impresora se muestra **sin flecha USB** (solo hay flecha hacia una PC o laptop), con "USB a {equipo}: no válido" debajo del nombre (P28u) |
| Impresora USB que no está conectada | Sin flecha, con "USB sin conectar" debajo del nombre |
| Rayo de energía | **Rojo solo si el equipo no tiene toma de energía**; azul si la tiene. Mide el 45 % del ícono |
| Conexión o equipo que se sugiere implementar | Línea **plomo**; el equipo es un **círculo blanco con borde punteado** |
| Impresora que se pide de red y es USB o WiFi | Ícono y nombre en **rojo** y su conexión (flecha USB o línea WiFi) en **rojo** |
| Impresora de red averiada | Ícono y nombre en **rojo**; la línea sigue **azul** |
| Impresora de red bien, pero hay que cambiar el cable o el conector | Solo la **línea en rojo** |
| IP fuera del segmento del router (R55) | La línea "IP:" en rojo |
| La leyenda | "Con falla / observado" |

- **Cada impresora tiene una sola conexión:** cable fino (red), flecha USB, línea WiFi o **sin conexión** (queda sola en el mapa, con "Sin conexión" debajo del nombre) si no se indicó cómo se conecta o no aparece en el escáner de IP.
- **Orden:** equipos por filas (internet, router y TP-Link, switch, equipos de caja e impresoras, repetidores y cámaras). Las líneas bajan en escalera por el espacio entre filas; las que saltan más de una fila van por el costado. Cada impresora USB queda al lado del equipo al que se conecta.
- **USB solo a PC o laptop (2026-10-11):** una impresora USB se conecta únicamente a una PC o laptop. En el editor, la línea "Flecha USB" solo se puede trazar entre una impresora y una PC o laptop; con cualquier otro equipo avisa "Una impresora USB solo se conecta a una PC o laptop". Si el local no tiene PC ni laptop fija (y no se va a adquirir), el chat avisa después de P28 y el análisis agrega la observación **USB** (cambiar a impresora de red o adquirir PC o laptop).
- **Topología en la minuta (2026-10-11):** no es una captura del visor. Se dibuja un gráfico vectorial propio que ocupa **todo el ancho útil del A4** (180 mm, respetando márgenes) y **siempre el 50 % del alto útil** (127,5 mm), con íconos pequeños, nombres en letra chica, hasta 7 equipos por fila y la leyenda dentro del mismo espacio. Es vectorial: se reduce el tamaño, no la calidad.
- **Pregunta nueva P28b** (después de "¿cómo se conecta?"): "IMP. {nombre}: ¿cómo está la impresora?" → Funciona bien · Está averiada · Hay que cambiar el cable o el conector (solo si es de red). P28 suma la opción "Sin conexión (no se ve en el escáner)".

## 9. Agregar al mapa (confirmado 2026-10-11)

Se mantiene la hoja de la APK y sus opciones quedan así:

| Opción | Qué hace |
|---|---|
| **Elemento de red** | Igual que la APK (cuadrícula de tipos, Nombre, "Conectar a" y botón "Agregar al mapa"), con los tipos nuevos: router, TP-Link con chip, switch, switch administrable, access point, repetidor, mesh, servidor, PC, laptop, Raspberry, impresora, cámara de vigilancia, Sunmi o tablet y otro. Cámara se conecta con trazos cortos; access point, repetidor y mesh con WiFi; el resto con cable fino. |
| **Energía** (opción aparte, ya no está dentro de Elemento de red) | Rayo azul (hay toma), rayo rojo (falta toma) o quitar el rayo; luego se toca el equipo. |
| **Línea** | Reemplaza a "Línea recta" y "Línea discontinua": gruesa, fina, trazos cortos, trazos medianos o flecha USB. |
| **Caja de texto** | Igual que la APK: nota que se arrastra, con A−, A+, tres colores de fondo y "Listo". |
| **Imagen** | Igual que la APK: se elige una foto de las evidencias de la visita, se arrastra, tamaño S, M o L, Eliminar y "Listo"; se puede reemplazar la foto. |
