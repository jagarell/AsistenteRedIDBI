# Plan de la prueba de usabilidad con técnicos

Basado en la sección IV (Validación) del paper y en las recomendaciones del CEO en la reunión del 23/10/2026.

## 1. Objetivo
Comprobar que un técnico de campo puede hacer una minuta completa con la app **sin capacitación extensa** y medir los indicadores del paper en el **Escenario B** (con el asistente), frente a la línea base del **Escenario A** (proceso manual).

## 2. Participantes y muestra
| Perfil | Cantidad | Rol en la prueba |
|---|---|---|
| Técnicos de campo de IDBI | 4 | Usan la app en visitas reales |
| Especialistas en infraestructura de red | 2 o más | Jueces expertos: revisan la precisión de cada propuesta con la rúbrica |
| Supervisor de proyectos (CEO) | 1 | Revisa y valida las minutas en la app o la web |
| Desarrollo de Negocios | 1 | Opina sobre la utilidad para el onboarding |

- **Escenario B:** al menos **25 evaluaciones** con la app, repartidas entre los 4 técnicos.
- **Escenario A (línea base):** **25 minutas manuales** de los últimos 12 meses.
- **Prueba piloto previa:** en la oficina, con los técnicos del CEO (como acordó en la reunión), antes de ir a campo.

## 3. Cómo se aplica (protocolo)
1. **Inducción mínima (máximo 5 minutos):** cómo abrir el Chat IA y el Asistente IA. Sin manual. Es lo que pidió el CEO: «esa persona debería usarlo sin ninguna duda».
2. **Observación silenciosa:** un integrante del equipo acompaña la visita **sin ayudar ni explicar**. Solo anota en el registro de incidencias. Si el técnico se traba, se anota el punto y, si no puede seguir después de 3 minutos, se le ayuda y se marca como «tarea asistida».
3. **Tiempos:** se toman de la **bitácora de la app** (inicio del Chat IA → solicitar validación), no a mano.
4. **Al terminar la visita:** el técnico responde el **SUS** (10 ítems) y la **escala Likert** de satisfacción en Microsoft Forms.
5. **En máximo 3 días:** los especialistas revisan la propuesta con la rúbrica y marcan si se aprueba sin correcciones.
6. **Análisis:** estadística descriptiva y comparación con el Escenario A.

## 4. Tareas que hace cada técnico
| N.° | Tarea | Éxito si… |
|---|---|---|
| T1 | Iniciar una minuta (Chat IA o Asistente IA) | La minuta queda creada |
| T2 | Registrar los datos del local con texto o audio y el GPS | Datos generales completos |
| T3 | Subir las evidencias (speedtest, router, selftest, AnyDesk de caja) | Lecturas confirmadas o guardadas como correctas |
| T4 | Contar cómo están conectados los equipos | La topología coincide con el local |
| T5 | Corregir una respuesta anterior | Sigue el flujo sin repetir preguntas |
| T6 | Revisar el análisis y marcar algo resuelto en la visita (si aplica) | Estado y porcentaje actualizados |
| T7 | Ver la minuta y solicitar la validación | Estado Completa y supervisor notificado |
| T8 | Preguntar algo a la Ayuda o al Asistente IA | Respuesta útil sin salir del chat |

## 5. Indicadores (Tabla X del paper)
| Indicador | Fórmula | Meta | Fuente |
|---|---|---|---|
| Errores técnicos | (Errores antes − Errores después) / Errores antes × 100 | ≥ 50 % | Rúbrica de especialistas vs. Escenario A |
| Tiempo de elaboración | (Tiempo actual − Tiempo con asistente) / Tiempo actual × 100 | ≥ 40 % | Bitácora vs. Escenario A |
| Precisión de propuestas | Aprobadas sin correcciones / Total × 100 | ≥ 85 % | Rúbrica |
| Automatización | Tareas automatizadas / Total de tareas × 100 | ≥ 70 % | Lista de tareas del proceso |
| Disponibilidad | Tiempo activo / Tiempo total × 100 | ≥ 95 % | Monitoreo de la API |
| Satisfacción | Likert 1–5 | ≥ 4 | Formulario |
| **Usabilidad (SUS)** | (Σ ítems impares − 1 + Σ 5 − ítems pares) × 2,5 | **> 70** | Formulario |

**Criterio de éxito (paper):** SUS > 70, al menos 4 de las 5 condiciones de rendimiento (errores, tiempo, precisión, automatización y disponibilidad) y Likert ≥ 4.

Interpretación del SUS: ≥ 85 excelente · 71–84 buena · 51–70 aceptable · ≤ 50 deficiente.

## 6. Instrumentos
- **SUS** (10 ítems de la Tabla XI) y **Likert** en Microsoft Forms.
- **Registro de incidencias**: tarea, momento, qué pasó, si fue asistida, severidad (bloquea / molesta / cosmética).
- **Rúbrica de precisión** para los especialistas.
- **Tabla de tiempos por rubro** (la llena el equipo con la experiencia del técnico, antes de la prueba, como línea base de referencia):

| Rubro | Ejemplo | Tiempo manual de referencia (min) | Tiempo con la app (min) |
|---|---|---|---|
| Cafetería / heladería (1 barra) | | | |
| Restaurante (cocina + barra) | | | |
| Sanguchería / fast food | | | |
| Pollería / parrilla | | | |
| Restobar | | | |
| Discoteca | | | |
| Retail / supermercado | | | |

## 7. Qué se espera aprender
- Dónde se traba el técnico (de las incidencias) → ajustes de los nodos.
- Si las validaciones de texto (por ejemplo, contraseña, RUC, celular) rechazan entradas válidas.
- Si el audio y el Asistente IA reducen el tiempo frente al chat pregunta por pregunta.

## 8. Ética y datos
Consentimiento informado de cada técnico; los datos del cliente se tratan según la Ley N.° 29733 y solo se usan para la investigación.
