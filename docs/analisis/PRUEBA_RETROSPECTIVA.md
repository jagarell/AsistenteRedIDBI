# Prueba retrospectiva del motor de recomendaciones

Versión 2026-10-11 · Asistente Red IDBI

## 1. Objetivo

Medir cuánto coinciden las recomendaciones que genera el motor del asistente con las que dieron los técnicos en las minutas reales. Usar las diferencias para mejorar las reglas.

## 2. Método

1. **Muestra:**
   - 44 minutas de la base de conocimiento: hojas `kb_minuta`, `kb_minuta_respuesta`, `kb_minuta_recomendacion`, `kb_minuta_equipo` y `kb_minuta_evidencia`.
   - Rubros: 10 oriental, 7 pollería, 6 pescados, 5 fast food, 4 restaurante, 4 restobar, 3 heladería, 2 discoteca o bar, 1 hospedaje, 1 cafetería y 1 retail.
2. **Extracción:**
   - De cada minuta se sacaron sus rasgos: rubro, estado, áreas de preparación, cajas, conexión, proveedor, equipo de caja, impresoras y su conexión, y cámaras.
   - Esos rasgos se convirtieron en respuestas del chat (P09, P11e, P12, P14, P22, P23, E5b, P47…).
   - No se usaron credenciales.
3. **Ejecución:**
   - Cada minuta se pasó por el motor del simulador (`runCaseSync`).
   - Los identificadores del motor se tradujeron a reglas de la base de conocimiento (por ejemplo, `TABLET` es R47 y `CAM_NVR` es R53).
4. **Comparación:**
   - Se compararon solo 11 reglas que se pueden decidir con los datos registrados.
   - Una regla cuenta para una minuta solo si esa minuta tiene el dato que la dispara. Así no se castigan reglas cuando el dato no se registró.
   - Las reglas que dependen de lo visto en sitio no se comparan, porque la minuta no guarda el dato que las dispara: R06 punto de red, R37 conectores, R05/R48 switch, R65 tope de 100 Mbps, R03 supresor.
5. **Métrica principal:**
   - Coincidencia = (TP + TN) / decisiones comparables.
   - También se reportan precisión, exhaustividad (recall) y F1.

## 3. Resultados

| | Antes | Después de agregar R16 |
|---|---|---|
| Decisiones comparables | 126 | 126 |
| Coinciden | 98 | 103 |
| **Coincidencia** | **77.8 %** | **81.7 %** |
| Precisión | 42.4 % | 50.0 % |
| Exhaustividad | 60.9 % | 69.6 % |
| F1 | 0.50 | 0.58 |

### Por regla (después)

| Regla | Nombre | n | TP | TN | FP | FN | Antes | Después |
|---|---|---|---|---|---|---|---|---|
| R56 | Red independiente con TP-Link y chip | 16 | 2 | 11 | 1 | 2 | 81 % | 81 % |
| R02 | Raspberry para el controlador | 20 | 2 | 14 | 1 | 3 | 70 % | 80 % |
| R47 | Tablet en caja: Raspberry | 23 | 3 | 18 | 2 | 0 | 91 % | 91 % |
| R16 | Impresora USB con el equipo con controlador | 4 | 3 | 1 | 0 | 0 | 25 % | 100 % |
| R43 | Impresora USB en caja: laptop/PC con controlador | 1 | 0 | 1 | 0 | 0 | 100 % | 100 % |
| R51 | Impresoras de preparación de red | 4 | 0 | 4 | 0 | 0 | 100 % | 100 % |
| R11 | PC de caja por cable | 4 | 3 | 1 | 0 | 0 | 100 % | 100 % |
| R53 | Cámaras: plan 1 Gbps y red separada | 6 | 2 | 0 | 4 | 0 | 33 % | 33 % |
| R10 | Proteger de grasa y calor | 44 | 0 | 36 | 7 | 1 | 82 % | 82 % |
| R63 | Periféricos de caja (Retail) | 1 | 1 | 0 | 0 | 0 | 100 % | 100 % |
| R26 | Segunda visita | 3 | 0 | 1 | 1 | 1 | 33 % | 33 % |

## 4. Qué se aprendió y qué se cambió

- **R16, nueva en el motor.** En 3 minutas el técnico recomendó mantener la impresora USB de caja conectada a la PC o laptop con el controlador. El motor no lo decía.
  - Se agregó la regla: si la impresora es USB y está conectada a la PC o laptop de caja, y no es de preparación, se recomienda mantenerla así con el controlador.
  - R16 pasó de 0 % a 100 %.
- **R02 (Raspberry).**
  - Con la regla de tablet (R47) separada, bajan los falsos positivos de R02 (de 4 a 1).
  - Quedan 3 omisiones: Barrio Pesquero, Bros y Verbena. Se explican porque la minuta no registró el equipo de caja final.
- **R10 (grasa y calor).** Los 7 falsos positivos son de Rikoton (pollería). El motor la sugiere por el perfil de brasas. El técnico no la escribió porque el equipo ya estaba protegido. Se mantiene: es preventiva y el técnico la puede quitar.
- **R53 (cámaras).**
  - Falsos positivos en Shimaya y Modonese: la minuta no dice si las cámaras son por DVR o NVR, ni si van por la misma red.
  - Con la regla nueva (DVR/NVR por el escáner de IP) esto se resuelve en sitio.
- **R56 (red independiente).** Las diferencias (Los Juárez, Saltao, Don Chalán) dependen de caídas del internet que la minuta no registró.

## 5. Cómo se usa sin mostrar las minutas

- **La minuta y el PDF nunca citan otras minutas.** Las recomendaciones salen del motor de reglas.
- **En el Análisis IA hay una sección oculta "Referencias":**
  - Muestra los 3 casos más parecidos con su % de similitud.
  - Muestra cuántas minutas tuvieron cada recomendación actual ("visto en N minutas").
  - Lista recomendaciones frecuentes en casos parecidos (2 o más de los 5 más parecidos), con el botón "Agregar a la propuesta". El técnico decide; lo agregado entra al TO-BE como texto propio.
- **Similitud:** suma ponderada sobre 100. Si falta un dato, cuenta como medio parecido.

  | Rasgo | Peso | Cómo se compara |
  |---|---|---|
  | Rubro | 30 | Mismo rubro, o la misma familia vale 0.6 |
  | Áreas de preparación | 15 | Diferencia de cantidad |
  | Conexión | 15 | Igual o no |
  | Cajas | 10 | Diferencia de cantidad |
  | Equipo de caja | 10 | Igual o no |
  | Impresora USB | 10 | Igual o no |
  | Cámaras | 5 | Igual o no |
  | Estado | 5 | Igual, o 0.4 si no |

## 6. Límites

- Las 44 minutas son de pocos clientes, y algunos repiten visitas (Rikoton 7, Shimaya 3). Eso pesa en R10 y R53.
- Muchos datos no se registraron en las minutas antiguas (conexión en 28, equipo de caja en 21). Con la app, cada minuta nueva guarda todas las respuestas, así que la prueba se puede repetir con más datos.
- Las reglas de sitio necesitan evidencias (fotos) para compararse. Eso depende del dataset de evidencias.

## 7. Cómo repetirla

```
node backtest/run.js simulador/simulador.html   # genera engine_out.json
python3 backtest/analyze.py result_after.json   # métricas
```
