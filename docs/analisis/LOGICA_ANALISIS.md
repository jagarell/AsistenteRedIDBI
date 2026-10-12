# Lógica de la pantalla "Análisis IA" y de los puntajes

Última actualización: 2026-10-11 (sobre la versión 0.0.3: speedtest por cable, WiFi por zona y canales). Este documento explica cómo se calculan hoy los porcentajes de la pantalla **Análisis IA** (Conectividad, Infraestructura Física, Equipamiento y Cobertura WiFi) y la **lógica nueva de Conectividad** definida por el equipo.

> ⛔ Solo especificación. Se implementa en una rama local y se prueba en local; **no se sube nada a `main` ni a producción** sin aprobación del equipo.

---

## 1. Cómo se calcula hoy (`AsistenteRedIDBI-API/app/analysis.py`)

Es un puntaje por reglas fijas, sin IA. Cada tarjeta parte de un puntaje base y suma o resta puntos según algunas respuestas.

| Puntaje | Estado | Color |
|---|---|---|
| 80 a 100 | Óptimo | verde |
| 65 a 79 | Buena | azul |
| 50 a 64 | Regular | naranja |
| menos de 50 | Deficiente | rojo |

"Requieren atención" cuenta las tarjetas con menos de 65.

| Tarjeta | Base | Suma o resta | Datos que usa |
|---|---|---|---|
| Conectividad de Red | 50 | ≥200 Mbps +30 · ≥100 +20 · ≥50 +10 · menos −10. Fibra +15 · Cable o 4G/5G +5 · Satelital −10 · DSL −5 | `internet_speed` (P15) y `connection_type` (P14) |
| Infraestructura Física | 50 | Switch +20 / −15 · Tomas cerca +15 / −15 · Router identificado +10 / −5 | `has_switches`, `power_outlets` (P33), `router_model` (E3) |
| Equipamiento | 70 | Equipos ÷ puertos del switch: ≤70 % +20 · ≤100 % +5 · más −25 · sin puertos −25 | `pos_count` (P12), impresoras, `camera_count` (P47), `computer_count` (P22) |
| Cobertura WiFi | 35 | +13 por cada área · paredes de concreto −15 | `wifi_zones` (= **todas** las áreas de P11), `wall_type` |
| Puntaje global | 50 | Lo calcula `proposal._compute_score`: velocidad +6/+12/+20 · switch +10 · tomas ±8 · firewall/VLAN/respaldo | No es el promedio de las tarjetas |

### Problemas con el chat de 76 nodos

Estas fórmulas son del chat de 23 nodos. Los datos les llegan por `legacy_adapter.py`, y varios llegan vacíos o con otro significado:

1. **`has_switches` y `switch_ports` siempre vacíos.** Infraestructura siempre resta 15, y Equipamiento casi siempre sale **45, Deficiente**.
2. **`wifi_zones` son todas las áreas de P11, no las zonas con señal.** Más áreas da más puntaje, aunque P46 diga que no hay WiFi.
3. **Conectividad no usa la velocidad medida (E1), la latencia, el respaldo (P16) ni las caídas (P17).** Las opciones nuevas de P14 (coaxial, chip, no tiene) no suman ni restan.
4. **El global no usa las reglas V01–V10 de la minuta.** La app puede decir "óptima" mientras la minuta dice OBSERVADO.
5. `wall_type` ya no se pregunta.
6. **V01 exige 50 Mbps a cualquier speedtest**, aunque se haya hecho por WiFi lejos del router. Un local que funciona bien puede quedar OBSERVADO o NO APTO por el equipo con que se midió (ver `EVIDENCIAS_Y_CONECTIVIDAD.md`).

Sigue igual en la 0.0.3: `app/analysis.py` no cambió en el commit `6a0acec`.

---

## 2. Conectividad: lógica nueva (reglas de negocio 2026-10-08 y 2026-10-10)

Conectividad combina **el plan contratado, la cobertura WiFi de las áreas de atención y si hay cámaras**. La implementación de referencia está en [`conectividad_ref.py`](conectividad_ref.py).

### 2.1 Pasos

| Paso | Condición | Conectividad | Minuta |
|---|---|---|---|
| 1 | **Sin internet** (P14 = No tiene o No sé) | **0 %** | OBSERVADO + prueba de chip y router con chip si hay comandas |
| 2 | Plan contratado (P15): hasta 399 Mbps | 30 % | — |
|   | 400–699 Mbps | 60 % | — |
|   | 700–899 Mbps | 80 % | — |
|   | 900 Mbps o más | 100 % | — |
| 3 | **Falta WiFi** en una o más áreas de atención (P46) | **−10 puntos una sola vez** (30→20, 60→50, 80→70, 100→90) | OBSERVADO + repetidor, mesh o access point (R19) |
| 4 | **R54 (actualizado 2026-10-11): ver sección 2.5** — antes: el plan no alcanza 200 Mbps por salón | Se mantiene | OBSERVADO + subir el plan a 200 Mbps por salón; mínimo unos 500 si hay que repetir señal (R54) |
| 5 | **Hay cámaras** (P47 = Sí o el escáner E8 muestra cámaras) | Se mantiene | Sugerir **1000 Mbps** (OBSERVADO si el plan es menor) y **separar la red de cámaras** de la red de PC, impresiones e impresoras (R53) |

- **Dónde se revisa el WiFi (2026-10-11):** en **todas las zonas de atención** (de cualquier canal: Mesa, Recojo, Para llevar, Backoffice, Delivery, Otro) y en **todos los lugares de preparación**. No depende del canal.
- **(Reemplazado por la sección 2.5)** Salones para los 200 Mbps: se usan salones y terraza mientras el equipo no confirme si cuentan todas las zonas de atención.
- **Rubro:** en Fast food, Cafetería y Heladería sin mozos con tablet (P09c = No) no se revisa el WiFi por zona (sección 4c de la base de conocimiento).

### 2.1b Velocidad medida por cable (E1), confirmada 2026-10-11

Los tramos usan el **plan contratado (P15)**. La velocidad **medida** se evalúa aparte, solo con el speedtest **por cable directo al router principal** (Cat6, puerto RJ45 gigabit):

| Caso | Resultado |
|---|---|
| La mejor de 2 pruebas llega al 70 % del plan o más | Se mantiene el puntaje del tramo |
| Menos del 70 % | OBSERVADO + revisar con el proveedor |
| Entre 85 y 95 Mbps con un plan mayor | Aviso: velocidad limitada a 100 Mbps por switch, puerto o cable |
| Sin prueba por cable | Plan "sin verificar", sin penalizar |
| Ping con carga > 300 ms | Observación de saturación (no cambia el puntaje) |

Las pruebas por WiFi solo sirven para **Cobertura WiFi** por zona (umbrales en `EVIDENCIAS_Y_CONECTIVIDAD.md`, sección 5).

### 2.2 Ejemplos (calculados con `conectividad_ref.py`)

| Local | Plan | Conectividad | Minuta |
|---|---|---|---|
| Sin internet | — | **0 %** | OBSERVADO: prueba de chip |
| 1 salón con caja, WiFi en todo | 200 Mbps | **30 %** | — |
| 2 salones, Salón 2 sin WiFi | 1000 Mbps | **90 %** | OBSERVADO: repetidor en Salón 2 |
| 2 salones, Salón 2 sin WiFi | 300 Mbps | **20 %** | OBSERVADO: repetidor + subir a 500 Mbps |
| 2 salones + terraza, terraza sin WiFi | 600 Mbps | **50 %** | OBSERVADO: repetidor en Terraza |
| 2 salones + terraza, WiFi en todo | 400 Mbps | **60 %** | OBSERVADO: subir a 600 Mbps (200 × 3) |
| 2 salones, Salón 2 sin WiFi | 800 Mbps | **70 %** | OBSERVADO: repetidor en Salón 2 |
| 1 salón, WiFi en todo, con cámaras | 600 Mbps | **60 %** | OBSERVADO: plan de 1000 Mbps + separar red de cámaras |

### 2.3 Qué cambia en la minuta

- La sección de internet muestra el **porcentaje de Conectividad**, el plan necesario (200 Mbps × salones) y las recomendaciones R19, R53 y R54.
- Si queda OBSERVADO por Conectividad, el estado final de la minuta no puede ser APTO. Sigue apareciendo el aviso de viabilidad.

### 2.4 Por confirmar con el equipo

1. ¿La **Terraza** cuenta como salón para los 200 Mbps? *Supuesto: sí.*
2. Planes de **menos de 200 Mbps**: *supuesto 30 %*.
3. El caso **200–300 Mbps con un salón por repetir**, que antes era 10 %, *pasa a 20 % (30 − 10)*.
4. Más de dos salones: ¿agregar **"Salón 3"** a P11 o preguntar **"¿Cuántos salones tiene?"**?

## 3. Las otras tres tarjetas (confirmadas 2026-10-11)

Mientras el equipo no defina su lógica, se propone partir de 100 y restar por cada problema, con datos del chat de 76 nodos:

| Tarjeta | Resta por… |
|---|---|
| **Infraestructura Física** | Sin tomas cerca (P33) · Toma compartida (P34) · Sin UPS (P35) · Extensiones (P36) · Puntos de red que no llegan (P38) · Cable "No sé" (P39) · Gabinete inexistente o desordenado (P40) · Calor o grasa (P41) |
| **Equipamiento** | Equipo de caja no adecuado (P22 con reglas R02/R40–R47) · Impresoras de preparación que no son de red (R51) · **Impresora fuera del segmento del router (R55, V04)** · Impresora no verificada en el escáner (V05) · Faltan puertos en el router (R48, foto E2) · Dependencia de impresora USB (V08) |
| **Cobertura WiFi** | Zonas de atención y lugares de preparación sin señal (P46) o con señal regular/mala en "Revisar WiFi por zona" · Sin repetidores ni AP cuando hacen falta (P45) · PC de caja por WiFi (V03) |

El **puntaje global** sería el promedio de las cuatro tarjetas. Pasaría a **NO APTO** solo si la prueba **por cable** no llega al 70 % del plan y no hay respaldo (V01 nueva), igual que la minuta.

---

### 3.1 Segmento de las impresoras (R55 · validación V04, confirmada 2026-10-10)

Todas las impresoras de red deben estar **en el mismo segmento que el router**.

1. **Segmento del router.** Sale de la **puerta de enlace del ipconfig (E4)**: 192.168.1.1 → segmento 192.168.1.x. Si no hay PC, se usa la IP de gestión de la etiqueta del router (E3) o la puerta de enlace del escáner (E8).
2. **Al subir cada autotest (E5),** la IP de la impresora se compara con ese segmento, no con la puerta de enlace del propio ticket. Hoy `checks.same_subnet` compara la IP con el gateway del ticket, y por eso no detecta una impresora configurada en otra red.
3. **Si está fuera del segmento,** el chat muestra el aviso: "La impresora de {área} está en {segmento impresora} y el router en {segmento router}. Cambia su IP a una del segmento del router y vuelve a subir el autotest". La minuta queda OBSERVADO.
4. **Escáner de IP (E8).** Con la IP del router del ipconfig se revisa:
   - qué equipos están en el segmento;
   - que cada impresora aparezca con su IP y su MAC;
   - si hay equipos sin documentar o fuera del segmento.

| Equipo | IP | Segmento | Resultado |
|---|---|---|---|
| Router (puerta de enlace E4) | 192.168.1.1 | 192.168.1.x | — |
| Impresora Caja (E5) | 192.168.1.20 | 192.168.1.x | ✅ Cumple |
| Impresora Cocina (E5) | 192.168.1.40 | 192.168.1.x | ✅ Cumple |
| Impresora Barra (E5) | 192.168.0.50 | 192.168.0.x | ⚠️ Cambiar a 192.168.1.x |

## 4. Datos que hacen falta en el chat

| Dato | De dónde sale |
|---|---|
| Plan contratado | P15 (ya existe) |
| Áreas de atención | P11 (ya existe) |
| Zonas sin WiFi | P46 (ya existe) |
| Cámaras | P47 (ya existe) **+ nuevo:** la IA detecta cámaras en el escáner E8 |
| Con "No tiene" o "No sé" en P14 | No hay plan: Conectividad = 0 % y se aplica la tabla de internet (prueba de chip) |
| Respuestas "Otro" (por ejemplo, un área escrita como "Cocina 2" o "Salón VIP") | El texto escrito (`{campo}Otro`) entra al análisis. Un área escrita cuenta como área de atención o de preparación según P11c. Si no encaja en ninguna regla, queda "por confirmar" |

## 5. Por definir con el equipo

1. Los puntos pendientes de Conectividad (sección 2.4).
2. La lógica de Infraestructura, Equipamiento y Cobertura WiFi (sección 3).

## 6. Prompt para Claude Code

```
Lee docs/analisis/LOGICA_ANALISIS.md y docs/analisis/conectividad_ref.py.
REGLAS: no hagas push a main ni toques producción; trabaja en una rama local y prueba en local.
1. En AsistenteRedIDBI-API/app/analysis.py reemplaza _connectivity_item por la lógica de la sección 2,
   leyendo P14, P15, P11, P46 y P47 del estado del chat (no las claves legacy).
1b. En app/chat/checks.py, la regla V04 debe comparar la IP de cada autotest (E5) con el segmento de la
   puerta de enlace del ipconfig (E4), y mostrar el aviso al técnico al subir el autotest (sección 3.1).
2. Agrega a lo que la IA extrae del escáner E8 la lista de cámaras (ip, fabricante).
3. Agrega las recomendaciones R19, R53 y R54 a la minuta y a la propuesta.
4. Agrega tests con los 8 ejemplos de la sección 2.2 y el ejemplo de segmento de la sección 3.1.
Muéstrame el diff y los tests antes de hacer commit.
```


## 2.5 Plan necesario: mínimo por equipo y ~150 Mbps por zona de atención (2026-10-11)

Reemplaza la regla de 200 Mbps por salón (R54).

| Nivel | Cálculo | Si el plan no llega |
|---|---|---|
| **Mínimo por equipo** (lo que necesita para funcionar) | Suma por equipo: equipo por WiFi (PC, laptop, tablet de caja, tablet o celular de mozo) **10 Mbps**; PC o laptop por cable (por ejemplo, con impresora USB) **5 Mbps**; Raspberry por cable 5 Mbps. Tablets de mozos: 1 por zona del canal Mesa (supuesto). | **OBSERVADO**: subir el plan. Con internet **por chip** no se observa (el chip cubre a los equipos). |
| **Recomendado** | **~150 Mbps por zona de atención** (todas las zonas de todos los canales) | Recomendación de subir el plan; **no** impide el inicio |

Ejemplo: fast food con una laptop por WiFi y fibra → mínimo 10 Mbps; con 3 zonas de atención el recomendado es 450 Mbps.

## 3b. Tarjetas de Análisis IA (simulador, 2026-10-11)

La pantalla es igual a la de la APK: puntuación global (anillo), áreas evaluadas, requieren atención, recomendaciones, las 4 tarjetas, el **Resumen IA** y el botón **Ver Propuesta Técnica**. Se abre siempre **antes** de la propuesta (desde el chat, Recientes, Historial, Minutas o Notificaciones).

| Tarjeta | Se basa en | Resta (confirmado 2026-10-11) |
|---|---|---|
| Conectividad de Red | Tramos del plan (30/60/80/100 %, 0 % sin internet, −10 si falta WiFi) | — |
| Infraestructura Física | **Cableado y estado de las conexiones** | Sin tomas −15 · faltan puertos o puntos de red −15 · PC de caja por WiFi −10 · cable o conector por cambiar −10 · impresora sin conexión −10 · IP fuera del segmento −5 · velocidad limitada a 100 Mbps −10 |
| Equipamiento | **Equipos del cliente y si son viables** | Caja sin PC o con tablet −15 · PC temporal −10 · impresora de preparación que no es de red −15 · impresora averiada −15 · falta impresora −15 |
| Cobertura WiFi | Zonas y lugares de preparación | Zona sin buena señal −15 · saturación con carga −10 · "No aplica" si el perfil no revisa WiFi |

Global = promedio de las tarjetas que aplican. Estados: Óptimo ≥ 80 · Buena 65–79 · Regular 50–64 · Deficiente < 50.

**Resumen IA verbalizado:** "Para el establecimiento {nombre} se recomienda {acciones en lenguaje simple}. Con estos cambios el local queda listo para iniciar con el punto de venta." (Si es NO APTO: "Mientras no se resuelva el internet, no es viable iniciar.")

## Reglas confirmadas (2026-10-11)
Pasan de propuesta a **confirmadas**: V01 · R67 (prueba por cable < 70 % del plan), R65 (85 a 95 Mbps con plan mayor: aviso de tope de 100 Mbps), R66 · V02 (ping con carga > 300 ms), umbrales de WiFi por zona (Regular o Mala = sin buena señal), pesos de Infraestructura, Equipamiento y Cobertura WiFi, R54 con sus valores por equipo (10 Mbps WiFi, 5 Mbps PC por cable; ~150 Mbps por zona), la pantalla de Análisis IA y todas las reglas propuestas el 2026-10-11 (USB, R10, R_ELEC, obra, por inaugurar, R55 con sistema anterior, Sunmi/iMin de otro proveedor, estado de impresora).

### Regla nueva: cámaras por DVR o NVR (CAM_DVR / CAM_NVR)
Aplica si el local tiene internet por **fibra o coaxial** (no por chip) y hay cámaras. El escáner de IP (E1d) indica:
- **DVR**: aparece una sola IP (por ejemplo Hikvision u otra marca). Aviso informativo; no se pide cambiar el plan por las cámaras.
- **NVR**: aparecen las IP de varias cámaras.
  - Plan **menor a 500 Mbps** → OBSERVADO: independizar la red de internet de las cámaras de la de los equipos de impresión, o ampliar el plan a 1000 Mbps.
  - Plan de **500 a 999 Mbps** → recomendación: ampliar el plan a 1000 Mbps.
  - Plan de 1000 Mbps o más → sin recomendación.
Si el escáner no muestra cámaras (o el internet es por chip), sigue la regla R53 general.

## Tarjetas medibles: criterios con peso (2026-10-11)
Reemplaza el "parte de 100 y resta por problema". Cada tarjeta suma criterios con peso; cada criterio vale **peso × cumplimiento** (proporción medida de 0 a 1). Si un criterio no tiene datos o no aplica (obra, Retail, Otro, sin prueba por cable), su peso se reparte entre los demás:
**Puntaje = Σ (peso × cumplimiento) ÷ Σ pesos que aplican × 100.** En el Análisis IA cada tarjeta tiene "Ver criterios" con la barra y los puntos de cada uno.

### Infraestructura física (100)
| Criterio | Peso | Cumplimiento |
|---|---|---|
| Puntos de red | 30 | equipos por cable con puerto libre en el router (o punto de red, en el Raspberry) ÷ equipos por cable |
| Tomas de energía | 25 | equipos con toma cerca ÷ equipos (router, equipo de caja, impresoras) |
| Conexión por cable | 20 | equipos que deben ir por cable y lo están (PC de caja, Raspberry, impresoras de preparación) ÷ total |
| Segmento de red | 10 | impresoras de red en el segmento del router ÷ impresoras de red con IP |
| Calidad del cableado | 10 | 1 si la prueba por cable no tiene tope de 100 Mbps (R65); 0 si lo tiene |
| Protección del cableado | 5 | 1 si ningún cable pasa cerca de electrodomésticos (P34c); 0 si pasa |

### Equipamiento (100)
| Criterio | Peso | Cumplimiento |
|---|---|---|
| Equipo de caja viable | 30 | por caja: PC o laptop permanente o Raspberry existente = 1; equipo por adquirir = 0,5; sin equipo = 0 |
| Impresoras existentes | 25 | impresoras que ya existen ÷ impresoras necesarias |
| Impresora adecuada | 20 | impresoras del tipo correcto (preparación: de red por cable; USB solo con PC o laptop) ÷ impresoras conectadas |
| Estado de las impresoras | 15 | promedio: Nueva o Viable = 1; Necesita mantenimiento = 0,5; Con fallas = 0 |
| Acceso remoto (AnyDesk) | 10 | equipos de caja con código de AnyDesk leído ÷ equipos a los que se pidió |

### Cobertura WiFi (100; no aplica sin mozos con tablet, en obra, Retail u Otro)
| Criterio | Peso | Cumplimiento |
|---|---|---|
| Zonas con buena señal | 65 | zonas y lugares de preparación con buena señal ÷ total |
| Calidad medida | 20 | promedio de la medición por zona: Excelente 1, Buena 0,85, Regular 0,5, Mala 0 (si no se midió, no aplica) |
| Solución definida | 15 | 1 si no hay zonas sin señal o ya se eligió repetidor o access point; 0 si falta definirla |

### Sustento de los pesos (para el paper: análisis y resultados)
1. **Método de puntaje: modelo de suma ponderada** (Weighted Sum Model, decisión multicriterio). Cada criterio se normaliza a un cumplimiento de 0 a 1 y se multiplica por su peso; los criterios sin dato redistribuyen su peso. Referencias: Fishburn (1967), *Additive Utilities with Incomplete Product Sets*; Triantaphyllou (2000), *Multi-Criteria Decision Making Methods: A Comparative Study*.
2. **Asignación inicial: criticidad del modo de falla** (lógica de severidad del FMEA, IEC 60812). Más peso a lo que, si falla, impide operar el punto de venta:

| Nivel de criticidad | Efecto si falla | Criterios | Peso |
|---|---|---|---|
| Bloquea la operación | La impresora no imprime, no hay controlador, el mozo no envía comandas | Puntos de red, equipo de caja viable, zonas con buena señal | 30 a 60 |
| Bloquea, se resuelve rápido | Se compra o se instala | Tomas de energía, impresoras existentes | 25 |
| Funciona pero falla con el tiempo | Caídas en hora punta | Conexión por cable, impresora adecuada, estado de las impresoras | 15 a 20 |
| Degrada la calidad | Lentitud o errores intermitentes | Calidad del cableado, segmento de red, estabilidad con carga, calidad medida | 10 a 20 |
| Riesgo o soporte | Interferencia o falta de soporte remoto | Protección del cableado, AnyDesk, solución WiFi definida | 5 a 10 |

3. **Validación: AHP con expertos** (Saaty, 1980, *The Analytic Hierarchy Process*). El equipo técnico compara los criterios de cada tarjeta de dos en dos con la escala de Saaty (1 igual, 3 moderado, 5 fuerte, 7 muy fuerte, 9 extremo; 2, 4, 6, 8 intermedios; recíprocos al invertir). Los pesos son el vector propio principal normalizado y se acepta la matriz si la **razón de consistencia CR = CI / RI < 0,10**, con CI = (λmáx − n) / (n − 1) e RI de Saaty (n = 4: 0,90; n = 5: 1,12; n = 6: 1,24). Plantilla en `analisis/AHP_PESOS_TARJETAS.md` (las cuatro tarjetas).
4. **Trabajo futuro: calibración con datos de campo.** Comparar el puntaje de minutas reales con el resultado posterior (inicio sin incidencias, segunda visita, caídas) y ajustar los pesos.

Estado: pesos iniciales asignados por criticidad (analista); pendiente la validación AHP con el equipo técnico.

### Conectividad de red (100) — reemplaza los tramos por plan (2026-10-11)
Sin internet: 0 %. Internet por contratar: 0 % hasta la segunda visita. En obra: no aplica (se valida en la segunda visita).
| Criterio | Peso | Cumplimiento |
|---|---|---|
| Velocidad real frente a la contratada | 30 | mejor prueba por cable ÷ plan, normalizado al 70 %: se cumple al 100 % desde el 70 % (velocidad mínima garantizada, Ley N.° 31207 y OSIPTEL) |
| Cobertura de la demanda | 30 | plan ÷ ancho de banda recomendado (150 Mbps por zona de atención) |
| Mínimo por equipo | 20 | plan ÷ mínimo de los equipos (10 Mbps por equipo WiFi, 5 Mbps por PC por cable); por chip = 1 |
| Estabilidad con carga | 10 | 1 si el ping con carga es ≤ 300 ms; 0 si es mayor |
| Continuidad del servicio | 10 | 1 si tiene respaldo o no tiene caídas; 0 si tiene caídas sin respaldo; no aplica si no se preguntó |

Criticidad: que el internet no llegue a lo contratado o no alcance para la demanda bloquea el punto de venta (30 cada uno); el mínimo por equipo es la base técnica (20); la latencia con carga degrada y las caídas son riesgo (10 cada uno). La señal WiFi por zona ya no resta aquí: se mide solo en Cobertura WiFi para no contarla dos veces. Las reglas V01, R54 y R66 siguen generando sus observaciones y recomendaciones aparte.

**Cobertura WiFi** pasa a 3 criterios (la estabilidad con carga se mide en Conectividad): zonas con buena señal 65, calidad medida 20, solución definida 15.

**Global** = promedio de las tarjetas que aplican.


## Referencias (sección oculta del Análisis IA, 2026-10-11)

- **Dónde aparece:** solo en la pantalla Análisis IA, como sección plegable "Referencias". No aparece en la minuta, en el PDF ni en lo que se comparte.
- **Casos más parecidos:**
  - Se muestran los 3 con mayor similitud.
  - Similitud = suma ponderada sobre 100: rubro 30, áreas de preparación 15, conexión 15, cajas 10, equipo de caja 10, impresora USB 10, cámaras 5, estado 5.
  - Si falta un dato, cuenta como 0.5.
  - Si el rubro no es el mismo pero es de la misma familia, cuenta 0.6.
- **Visto en N minutas:** para cada recomendación actual, cuántas minutas la tuvieron.
- **Sugerencias:**
  - Se listan reglas de sitio presentes en 2 o más de los 5 casos más parecidos que el motor no generó.
  - El técnico las agrega con "Agregar a la propuesta" solo si lo vio en la visita.
  - Entran al TO-BE como texto propio, sin citar la minuta de origen.
- **Sustento:** la prueba retrospectiva (`PRUEBA_RETROSPECTIVA.md`).
