# Revisión de la minuta de El Chinito: cambios en la app

Las reglas completas y sus criterios están en `AsistenteRedIDBI-API/docs/0.0.7/09-reglas-revision-el-chinito.md`. En la app:

- **Editar › Topología en la minuta: Incluida / No incluida.**
- **Chat IA:** si el escáner de IP muestra 2 o más equipos conocidos apagados, preguntar «¿El local tiene intermitencias de energía?». Mostrar los equipos sin respuesta como **apagado**; si el técnico luego indica una falla en ese punto, preguntar «¿Lo dejo como apagado o con falla?».
- **Chat IA:** con más de 4 impresoras o con switch, preguntar «¿Quieres levantar a qué puerto del switch va cada impresora?» antes de abrir «Ubicar puerto».
- **Chat IA y Asistente IA:** al subir la foto de un documento (RUC, factura, dirección), mostrar los datos transcritos y preguntar «¿Agrego la foto como evidencia en la minuta?» (Sí / No).
- **Análisis IA:** al marcar «Se resolvió», el porcentaje y el estado se recalculan, y la acción pasa a Pasos a seguir como «Resuelto en la visita».
- **Captura de equipos:** nombres simples (tipo + zona); no autocompletar descriptores que el técnico no dijo; la ubicación se escribe corta y la observación (mueble, electrodomésticos) se agrega aparte.
