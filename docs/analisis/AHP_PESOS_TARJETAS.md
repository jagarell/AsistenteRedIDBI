# Plantilla AHP: pesos de las tarjetas del Análisis IA

Para validar con el equipo técnico los pesos de Conectividad de red, Infraestructura física, Equipamiento y Cobertura WiFi (ver `LOGICA_ANALISIS.md`, "Sustento de los pesos").

## Cómo llenarla
1. Cada experto responde por separado. En cada celda de la diagonal hacia arriba: ¿cuánto más importante es el criterio de la **fila** que el de la **columna** para que el punto de venta funcione?
2. Escala de Saaty: 1 igual · 3 moderadamente más · 5 fuertemente más · 7 muy fuertemente más · 9 extremadamente más (2, 4, 6, 8 intermedios). Si la columna es más importante, se pone el recíproco (1/3, 1/5…).
3. Debajo de la diagonal va el recíproco de la celda simétrica.
4. Se agregan los juicios de los expertos con la media geométrica celda por celda.
5. Pesos = vector propio principal normalizado. CR = CI / RI; se acepta si CR < 0,10.

Como referencia, los pesos iniciales asignados por criticidad equivalen a estos juicios (matriz perfectamente consistente, CR = 0). El equipo los reemplaza con sus propios juicios.

## Infraestructura física (n = 6, RI = 1,24)
| | Puntos de red | Tomas | Conexión por cable | Segmento | Calidad cableado | Protección |
|---|---|---|---|---|---|---|
| Puntos de red | 1 | 6/5 | 3/2 | 3 | 3 | 6 |
| Tomas de energía | | 1 | 5/4 | 5/2 | 5/2 | 5 |
| Conexión por cable | | | 1 | 2 | 2 | 4 |
| Segmento de red | | | | 1 | 1 | 2 |
| Calidad del cableado | | | | | 1 | 2 |
| Protección del cableado | | | | | | 1 |
| **Peso inicial** | 0,30 | 0,25 | 0,20 | 0,10 | 0,10 | 0,05 |

## Equipamiento (n = 5, RI = 1,12)
| | Equipo de caja | Impresoras existentes | Impresora adecuada | Estado | AnyDesk |
|---|---|---|---|---|---|
| Equipo de caja viable | 1 | 6/5 | 3/2 | 2 | 3 |
| Impresoras existentes | | 1 | 5/4 | 5/3 | 5/2 |
| Impresora adecuada | | | 1 | 4/3 | 2 |
| Estado de las impresoras | | | | 1 | 3/2 |
| Acceso remoto (AnyDesk) | | | | | 1 |
| **Peso inicial** | 0,30 | 0,25 | 0,20 | 0,15 | 0,10 |

## Cobertura WiFi (n = 3, RI = 0,58)
| | Zonas con buena señal | Calidad medida | Solución definida |
|---|---|---|---|
| Zonas con buena señal | 1 | 13/4 | 13/3 |
| Calidad medida | | 1 | 4/3 |
| Solución definida | | | 1 |
| **Peso inicial** | 0,65 | 0,20 | 0,15 |

## Conectividad de red (n = 5, RI = 1,12)
| | Velocidad real | Cobertura de la demanda | Mínimo por equipo | Estabilidad con carga | Continuidad |
|---|---|---|---|---|---|
| Velocidad real frente a la contratada | 1 | 1 | 3/2 | 3 | 3 |
| Cobertura de la demanda | | 1 | 3/2 | 3 | 3 |
| Mínimo por equipo | | | 1 | 2 | 2 |
| Estabilidad con carga | | | | 1 | 1 |
| Continuidad del servicio | | | | | 1 |
| **Peso inicial** | 0,30 | 0,30 | 0,20 | 0,10 | 0,10 |

Nota: con juicios reales de expertos la matriz ya no será perfectamente consistente; por eso se calcula CR y, si supera 0,10, se pide al experto revisar sus comparaciones.
